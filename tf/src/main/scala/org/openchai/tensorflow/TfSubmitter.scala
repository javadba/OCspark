package org.openchai.tensorflow

import java.io.{File, FileReader}
import java.nio.file.{Files, Paths}
import java.util.Base64
import java.util.concurrent.{Callable, ConcurrentNavigableMap, Executors}

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.{FileUtils, ProcessUtils, TcpUtils}
import org.openchai.tensorflow.JsonUtils._
import org.openchai.tensorflow.web.{HttpUtils, TfWebServer}
import org.openchai.tcp.util.Logger._
import org.openchai.util.{TfAppConfig, TfConfig, YamlStruct, YamlUtils}
import collection.mutable.{ArrayBuffer => AB}
import FileUtils._

import scala.collection.mutable

case class LabelImgRest(restHostAndPort: Option[String], master: String, tfServerHostAndPort: String, workerName: String,
  imgApp: String, path: String, outPath: String, outputTag: String, contents: Array[Byte]) {
}

object LabelImgRest {
  def apply(liwreq: LabelImgWebRest) = {
    if (liwreq.contentsBase64 != null) {
      val bytes = if (liwreq.restHostAndPort.startsWith("/")) {
        readFileBytes(liwreq.restHostAndPort)
      } else {
        Base64.getDecoder.decode(liwreq.contentsBase64)
      }
      new LabelImgRest(Option(liwreq.restHostAndPort), liwreq.master, liwreq.tfServerHostAndPort, liwreq.workerName, liwreq.imgApp, liwreq.path, liwreq.outDir, liwreq.outputTag, bytes)
    } else {
      new LabelImgRest(Option(liwreq.restHostAndPort), liwreq.master, liwreq.tfServerHostAndPort, liwreq.workerName, liwreq.imgApp, liwreq.path, liwreq.outDir, liwreq.outputTag, null)
    }
  }
}

case class LabelImgWebRest(restHostAndPort: String, master: String, tfServerHostAndPort: String, workerName: String, imgApp: String, path: String, outDir: String, outputTag: String, contentsBase64: String)

object LabelImgWebRest {
  def apply(lireq: LabelImgRest) =
    new LabelImgWebRest(lireq.restHostAndPort.get, lireq.master, lireq.tfServerHostAndPort, lireq.workerName, lireq.imgApp, lireq.path, lireq.outPath, lireq.outputTag,
      if (lireq.contents != null) {
        Base64.getEncoder.encodeToString(lireq.contents)
      } else "")
}

object ImagesMeta {
  val whiteListExt = "jpg jpeg png gif svg tiff".split(" ").toSeq
}

object TfSubmitter {
  val UseWeb = false

  def process(params: Map[String, String]) = {
    val UseSpark = true // TODO: make configurable with direct

    // tODO: conf from params..
    var conf: TfAppConfig = null
    if (!params.contains("json")) {
      s"ERROR: no json in payload"
    } else {
      info(s"Received request ${params.map { p => val ps = p.toString; ps.substring(0, Math.min(ps.length, 300)) }.mkString(",")}")
      val json = params("json")
      val labelImgWebReq = parseJsonToCaseClass[LabelImgWebRest](json)
      val resp = if (UseSpark) {
        debug(s"TfWebServer: TfSubmitter.runSparkJob..")
        val lr = labelImgWebReq
        val lresp = TfSubmitter.runSparkJobs(conf, lr.master, lr.tfServerHostAndPort, lr.imgApp, lr.path, lr.outDir, continually = false)
        lresp
      } else {
        debug(s"TfWebServer: TfSubmitter.labelImg..")
        val lresp = TfSubmitter.labelImg(TfClient(conf), LabelImgRest(labelImgWebReq))
        lresp.value.toString
      }
      info(s"TfWebServer: result is $resp")
      resp
    }
  }

  @inline def getWatermarkFileName(dir: String) = s"$dir/watermark.txt"

  val NumExt = "^[\\d+]$".r

  @inline def isDigitsExt(fname: String) = NumExt.findAllIn(fileExt(fname)).nonEmpty

  def labelImg(tfClient: TfClient, lireq: LabelImgRest) = {
    val fmd5 = md5(lireq.contents)
    val res = tfClient.labelImg(LabelImgStruct(lireq.outputTag, lireq.path, lireq.outPath,
      lireq.contents, fmd5, Option(lireq.imgApp)))
    info(s"TfSubmitter.labelImg: LabelImgResp=$res")
    res
  }

  lazy val getTx1s = {
    val f = "/shared/tx1-slaves.txt"
    val slaves = scala.io.Source.fromFile(f).getLines.map { l => l.split(":") }.map { arr => (arr(0), arr(1).toInt) }.toSeq
    info(s"Slaves from $f: ${slaves.mkString(",")}")
    slaves
  }

  lazy val pool = Executors.newFixedThreadPool(getTx1s.length)
  val inUse: ConcurrentNavigableMap[Int, Boolean] = new java.util.concurrent.ConcurrentSkipListMap[Int, Boolean]()
  Range(1, getTx1s.length + 1).map { ix => inUse.put(ix, false) }

  def runSparkJobs(conf: TfAppConfig, master: String, tfServerHostAndPort: String, imgApp: String, dir: String, outDir: String,
    nPartitions: Int = 1, continually: Boolean = true) = {
    var warnPrinted = false
    val res = for (iters <- Range(0, if (continually) Integer.MAX_VALUE else 1)) yield {
      val oldWatermark = readFileOption(getWatermarkFileName(dir)).flatMap(s => Option(s.toLong)).getOrElse(0L)
      try {
        val watermark = System.currentTimeMillis
        write(getWatermarkFileName(dir), watermark.toString, silent = true)
        var files = mutable.ArrayBuffer[(String, Long)](
          new File(dir).listFiles
            .filter(f => f.isFile
              && f.lastModified > oldWatermark
              && f.getName.contains(".")
              && (ImagesMeta.whiteListExt.contains(fileExt(f.getName))
              || isDigitsExt(f.getName))
            ).map { f => (f.getAbsolutePath, f.length) }
            .sortBy(_._1)
            .toSeq: _*)
        import collection.JavaConverters._
        val workers = inUse.entrySet.asScala.filter(!_.getValue).toSeq.sortBy(_.getKey)
        if (workers.length < getTx1s.length) {
          //        debug(s"runSparkJobs: there are ${inUse.keySet.size - workers.length} workers still busy from earlier jobs ..")
        }
        if (files.isEmpty) {
          if (!warnPrinted) {
            info(s"No files are present to process: reset/delete the watermark.txt to reprocess the existing images")
            warnPrinted = true
          }
          None
        } else if (workers.isEmpty) {
          if (!warnPrinted) {
            info("No workers/tx1's are presently available: please wait for them to complete existing work")
            warnPrinted = true
          }
          None
        } else {
          val nWorkers = workers.length
          var nGroupsRemaining = nWorkers

          val (fnamesWithAffinity, otherFnames) = files.map {
            _._1
          }.partition(fname => isDigitsExt(fname))
          val franges = if (fnamesWithAffinity.nonEmpty) {
            val extRanges = fnamesWithAffinity.map { fn =>
              val ext = NumExt.findFirstIn(fileExt(fn))
                .getOrElse(throw new IllegalStateException(s"How did we lose the numeric extension: $fn ?")).toInt
              (ext, fn)
            }
            extRanges.map(_._1).foreach(e =>
              assert(e >= 1 && e <= getTx1s.length, s"Affinity extension not within range of available tx1's: $e"))
            val agroups = extRanges.groupBy(_._1).mapValues(_.map(_._2)).toSeq.sortBy(_._1)
            var fctr = 0
            val allg = AB.tabulate(getTx1s.length)(ix => (ix + 1, AB[String]()))
            for (ag <- agroups) {
              allg(ag._1 - 1) = (ag._1, AB(ag._2: _*))
            }
            val avgFilesPerWorker = math.ceil(files.length.toDouble / nWorkers).toInt
            val excessAffinity = allg.map(_._2.length).filter(_ >= avgFilesPerWorker).map(x => (x - avgFilesPerWorker, 1))
              .aggregate((0, 0))(seqop = (a, b) => (a._1 + b._1, a._2 + b._2), combop = (a, b) => (a._1 + b._1, a._2 + b._2))
            val workersWithCapacity = nWorkers - excessAffinity._2
            val avgRemainingPerWorker = math.ceil(otherFnames.length.toDouble / workersWithCapacity).toInt
            val workersCapacity = allg.map { case (g, flist) => (g, math.max(0, avgRemainingPerWorker - flist.length)) }
            val totalCap = workersCapacity.map(_._2).sum
            val newCap = math.ceil((otherFnames.length.toDouble - totalCap) / workersWithCapacity).toInt
            val adjCapacity = workersCapacity.map { case (g, cap) => (g, if (cap == 0) 0 else cap + newCap) }
            val finalFilesPerWorker = avgRemainingPerWorker + newCap
            assert(adjCapacity.map(_._2).sum >= otherFnames.length, s"remainingCapacity calc error: only ${adjCapacity.map(_._2).sum} which is < ${otherFnames.length}")
            assert(finalFilesPerWorker * workersWithCapacity >= otherFnames.length, s"finalFilesPerWorker * nWorkers calc error: only ${finalFilesPerWorker * workersWithCapacity} which is < ${otherFnames.length}")

            for (f <- mutable.Queue(otherFnames: _*);
                 worker <- getTx1s.indices if ({
                debug(s"\nworker: $worker"); true
              }) && !inUse.get(worker) && adjCapacity(worker)._2 > 0
                && ({
                debug(s" notInUse"); true
              }) && (allg(worker)._2.length < finalFilesPerWorker)
                && ({
                debug(s" lessthanAvgRemaining: ${avgRemainingPerWorker - allg(worker)._2.length}"); true
              })) {
              allg(worker)._2 += f
            }

            assert(allg.foldLeft(0) { case (sum, v) => sum + v._2.length } == files.length, s"We did not capture all files in the affinity groups")
            val franges = for (grp <- allg) yield {
              val ndir = s"$outDir/${grp._1}"
              debug(s"Worker dir=$ndir")
              for (path <- grp._2) yield {
                new File(ndir).mkdirs
                val link = s"$ndir/${new File(path).getName}"
                val olink = if (isDigitsExt(link)) removeExt(link) else link
                if (!new File(olink).exists) {
                  Files.createSymbolicLink(Paths.get(olink), Paths.get(path))
                }
              }
              (ndir, new File(ndir).listFiles)
            }
            franges
          } else { // Use load balanced scheduler
            val sizeWeight = 0.4 // 40% size 60% evenly distributed by count
          val countWeight = 1 - sizeWeight

            var ixs = mutable.ArrayBuffer[(Int, Long)]()
            var cursum: Long = 0L
            var totItems: Int = 0
            var totSize: Long = 0L
            var curItems: Int = 0
            for (i <- Range(0, files.length)) {
              if (totItems == 0) {
                totItems = files.length - i
                totSize = files.slice(i, files.length).map(_._2).sum
              }
              curItems += 1
              cursum += files(i)._2.toLong
              val level = sizeWeight * (cursum * 1.0 / totSize) + (1 - sizeWeight) * (curItems * 1.0 / totItems)
              debug(s"$level with $nGroupsRemaining remaining..")
              if (level >= 1.0 / nGroupsRemaining) {
                ixs += Tuple2(i, cursum)
                curItems = 0
                cursum = 0
                totSize = 0
                totItems = 0
                nGroupsRemaining -= 1
              }
            }
            ixs += Tuple2(files.length - 1, cursum)
            if (ixs.length == 1 && ixs(0)._2 == 0) {
              ixs.clear
            }
            if (ixs.nonEmpty) {
              debug(s"Group sizes = ${ixs.mkString(",")}")
            }
            val ts = System.currentTimeMillis.toString.substring(6, 10)
            import java.nio.file._
            val franges = Range(0, Math.min(nWorkers, ixs.length)).map { p =>
              val ndir = s"$outDir/${workers(p).getKey}"
              debug(s"Worker dir=$ndir")
              new File(ndir).mkdirs
              files.slice(if (p == 0) 0 else ixs(p - 1)._1, ixs(p)._1).map { case (path, len) =>
                val link = s"$ndir/${new File(path).getName}"
                if (!new File(link).exists) {
                  Files.createSymbolicLink(Paths.get(link), Paths.get(path))
                }
              }
              (ndir, new File(ndir).listFiles)
            }
            franges
          }
          if (franges.nonEmpty) {
            debug(s"Dividing work into: ${franges.map(fr => s"(${fr._1}:${fr._2.mkString(",")})").mkString("\n")}")
          }

          franges.zipWithIndex.foreach { case ((dir, files), ix) =>
            val tx1 = ix + 1
            info(s"TX$tx1: Submitting from dir=$dir for files=${files.mkString(",")}")
            pool.submit(
              new Callable[String]() {
                override def call(): String = {
                  val workerNum = workers(ix).getKey
                  inUse.put(workerNum, true)
                  debug(s"TX$ix: invoking runSparkJob..")
                  val res = runSparkJob(conf, master, tfServerHostAndPort, imgApp, dir, dir, tx1)
                  // info(s"Callable: result from worker=$workerNum = $res")
                  assert(dir.length >= 8)
                  try {
                    //              ProcessUtils.exec(s"DeleteDir-$dir", s"rm -rf $dir")
                    //              ProcessUtils.exec(s"DeleteDir-$dir", s"mv $dir/ /tmp/$ts/")
                  } catch {
                    case e: Exception =>
                      info(s"TX$ix: Callable: ERROR: unable to delete $dir")
                      e.printStackTrace
                  }
                  inUse.put(workerNum, false)
                  res
                }
              }
            )
          }
          val res = s"Submitted ${franges.length} groups of image processing jobs"
          debug(res)
          Option(res)
        }
      } catch {
        case e: Exception =>
          error(s"Error in runSparkJobs", e)
          write(getWatermarkFileName(dir), oldWatermark.toString)
          throw e
      } finally {
        Thread.sleep(200)
      }
    }
    res.flatten
    res.asInstanceOf[Seq[String]].mkString("\n\n")
  }

  def runSparkJob(conf: TfAppConfig, master: String, tfServerHostAndPort: String, imgApp: String, dir: String, outDir: String, tx1: Int) = {

    def ix = Array(92, 91, 93, 94, 95, 96, 32, 33, 35, 36, 97, 37)

    def txDebug(tx1: Int, msg: String) = {
      debug(("\n" + msg + "\033[0m").replace("\n", s"\n\033[${ix(tx1-1)}m TX$tx1 >> "))
    }

    def txInfo(tx1: Int, msg: String) = {
      info(("\n" + msg + "\033[0m").replace("\n", s"\n\033[${ix(tx1-1)}m TX$tx1 >> "))
    }

    txDebug(tx1, s"Connecting to spark master $master ..")
    val spark = SparkSession.builder.master(master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val bcTx1 = sc.broadcast(getTx1s.apply(tx1 - 1))
    //    cleanDir(dir,ImagesMeta.whiteListExt :+ "result")
    val irdd = sc.binaryFiles(dir, 1)
    txDebug(tx1, s"runsparkJob: binary files rdd on $dir ready")
    val out = irdd.mapPartitionsWithIndex { case (np, part) =>
      val (tx1Host, tx1Port) = bcTx1.value
      val tfClient = TfClient(conf, tx1Host, tx1Port)
      part.flatMap { case (ipath, contents) =>
        val path = if (fileExt(ipath) == tx1.toString) ipath.substring(0, ipath.lastIndexOf(".")) else ipath
        if (path.endsWith("result.result")) {
          error(s"Got a double result path $path")
        }
        if (!ImagesMeta.whiteListExt.contains(fileExt(path))) {
          if (fileExt(path) != "result") {
            info(s"Skipping non image file $path")
          }
          None
        } else {
          val outputTag = s"${TcpUtils.getLocalHostname}-Part$np-$path"
          val imgLabel = LabelImgRest(None, master, tfServerHostAndPort, s"SparkPartition-$np", imgApp, path, outDir, outputTag, contents.toArray)
          txDebug(tx1, s"Running labelImage for $imgLabel")
          val res = labelImg(tfClient, imgLabel)
          txInfo(tx1, s"LabelImage result: $res")
          Some(res)
        }
      }
    }
    val c = out.collect
    c.foreach { li =>
      val fp = if (li.value.fpath.startsWith("file:")) {
        val f = li.value.fpath.substring("file:".length)
        val e = f.zipWithIndex.find {
          _._1 != '/'
        }.get._2
        '/' + f.substring(e)
      } else {
        li.value.fpath
      }
      val fname = fileName(fp)
      writeBytes(s"${li.value.outDir}/$fname.result", (li.value.cmdResult.stdout + li.value.cmdResult.stderr).getBytes("ISO-8859-1"))
      //      debug(s"${li.value.fpath}.result", li.value.cmdResult.stdout.getBytes("ISO-8859-1"))
    }
    info(s"Finished runSparkJob for tx1=$tx1")
    c.mkString("\n")
  }

  def labelImgViaRest(conf: TfAppConfig, lireq: LabelImgRest) = {
    debug(s"Sending labelImgRequest to webserver: $lireq ..")
    val map = Map("json" -> JsonUtils.toJson(LabelImgWebRest(lireq)))
    val resp = if (UseWeb) {
      val resp = HttpUtils.post(s"${TfWebServer.getUri(lireq.restHostAndPort.get)}", map)
    } else {
      process(map)
    }
    resp
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      System.err.println("Usage: TfSubmitter <submitter.yml file>")
      System.exit(-1)
    }
    val conf = TfConfig.getAppConfig(args(0))
    if (conf.isSpark) {
      val res = runSparkJobs(conf, conf.master, conf.tfServerAndPort, conf.imgApp, s"${conf.inDir}", s"${conf.outDir}",
        conf.nPartitionsPerTx1.toString.toInt, continually = true)
      info("results: " + res)
    } else {
      val res = if (conf.isRest) {
        labelImgViaRest(conf, LabelImgRest(Option(conf.restHostAndPort), conf.tfServerAndPort, conf.master, "TFViaRest", conf.imgApp,
          conf.inDir, conf.outDir, conf.outTag, null))
      } else {
        labelImg(TfClient(conf), LabelImgRest(None, conf.master, conf.tfServerAndPort, "TFCommandLine", conf.imgApp, conf.inDir, conf.outDir, conf.outTag, readFileBytes(conf.inDir)))
      }
      info(res.toString)
    }
  }
}
