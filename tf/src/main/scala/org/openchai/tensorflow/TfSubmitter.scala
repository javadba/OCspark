package org.openchai.tensorflow

import java.io.{File, FileReader}
import java.util.Base64
import java.util.concurrent.{Callable, ConcurrentNavigableMap, Executors}

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.{FileUtils, ProcessUtils, TcpUtils}
import org.openchai.tensorflow.JsonUtils._
import org.openchai.tensorflow.web.{HttpUtils, TfWebServer}
import org.openchai.tcp.util.Logger._
import org.openchai.util.{TfAppConfig, TfConfig, YamlStruct, YamlUtils}

import scala.collection.mutable

case class LabelImgRest(restHostAndPort: Option[String], master: String, tfServerHostAndPort: String, workerName: String,
  imgApp: String, path: String, outPath: String, outputTag: String, contents: Array[Byte]) {

}

object LabelImgRest {
  def apply(liwreq: LabelImgWebRest) = {
    if (liwreq.contentsBase64 != null) {
      val bytes = if (liwreq.restHostAndPort.startsWith("/")) {
        FileUtils.readFileBytes(liwreq.restHostAndPort)
      } else {
        Base64.getDecoder.decode(liwreq.contentsBase64)
      }
      new LabelImgRest(Option(liwreq.restHostAndPort), liwreq.master, liwreq.tfServerHostAndPort, liwreq.workerName, liwreq.imgApp, liwreq.path, liwreq.outDir, liwreq.outputTag, bytes)
    } else {
      new LabelImgRest(Option(liwreq.restHostAndPort), liwreq.master, liwreq.tfServerHostAndPort, liwreq.workerName, liwreq.imgApp, liwreq.path, liwreq.outDir, liwreq.outputTag, null)
    }
  }
}

case class LabelImgWebRest(restHostAndPort: String, master: String, tfServerHostAndPort: String, workerName: String, imgApp: String, path: String, outDir: String, outputTag: String, contentsBase64: String) {

}

object LabelImgWebRest {
  def apply(lireq: LabelImgRest) =
    new LabelImgWebRest(lireq.restHostAndPort.get, lireq.master, lireq.tfServerHostAndPort, lireq.workerName, lireq.imgApp, lireq.path, lireq.outPath, lireq.outputTag,
      if (lireq.contents!=null) {Base64.getEncoder.encodeToString(lireq.contents)} else "")
}

case class TfSubmitter() {
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
        info(s"TfWebServer: TfSubmitter.runSparkJob..")
        val lr = labelImgWebReq
        val lresp = TfSubmitter.runSparkJobs(conf, lr.master, lr.tfServerHostAndPort, lr.imgApp, lr.path, lr.outDir)
        lresp
      } else {
        info(s"TfWebServer: TfSubmitter.labelImg..")
        val lresp = TfSubmitter.labelImg(TfClient(conf), LabelImgRest(labelImgWebReq))
        lresp.value.toString
      }
      info(s"TfWebServer: result is $resp")
      resp
    }
  }


  def labelImg(tfClient: TfClient, lireq: LabelImgRest) = {
    val md5 = FileUtils.md5(lireq.contents)
    val res = tfClient.labelImg(LabelImgStruct(lireq.outputTag, lireq.path, lireq.outPath,
      lireq.contents, md5, Option(lireq.imgApp)))
    res
  }

  lazy val getTx1s = {
    val f = "/shared/tx1-slaves.txt"
    val slaves = scala.io.Source.fromFile(f).getLines.map{ l => l.split(":")}.map{ arr => (arr(0),arr(1).toInt)}.toSeq
    info(s"Slaves from $f: ${slaves.mkString(",")}")
    slaves
  }

  lazy val pool = Executors.newFixedThreadPool(getTx1s.length)
  val inUse: ConcurrentNavigableMap[Int,Boolean] = new java.util.concurrent.ConcurrentSkipListMap[Int,Boolean]()
  Range(0,getTx1s.length).map{ ix  => inUse.put(ix,true)}

  val sizeWeight = 0.4 // 40% size 60% evenly distributed by count
  val countWeight = 1-sizeWeight

  def runSparkJobs(conf: TfAppConfig, master: String, tfServerHostAndPort: String, imgApp: String, dir: String, outDir: String, nPartitions: Int = 1) = synchronized {
    var files = mutable.ArrayBuffer[(String,Long)](
      new File(dir).listFiles.map{
        f => (f.getAbsolutePath, f.length) }
        .sortBy(_._1)
        .toSeq:_*)
    import collection.JavaConverters._
    val workers = inUse.entrySet.asScala.filter(_.getValue).toSeq.sortBy(_.getKey)
    if (workers.length < inUse.keySet.size) {
      info(s"runSparkJobs: there are ${workers.length < inUse.keySet.size} workers still busy from earlier jobs ..")
    }
    val nWorkers = workers.length
    var nGroupsRemaining = nWorkers
    var ixs = mutable.ArrayBuffer[(Int,Long)]()
    var cursum: Long = 0L
    var totItems: Int = 0
    var totSize: Long = 0L
    var curItems: Int = 0
    for (i <- Range(0,files.length)) {
      if (totItems ==0) {
        totItems = files.length -i
        totSize = files.slice(i,files.length).map(_._2).sum
      }
      curItems += 1
      cursum += files(i)._2.toLong
      val level = sizeWeight * (cursum * 1.0/totSize) + (1-sizeWeight)*(curItems*1.0/totItems)
      info(s"$level with $nGroupsRemaining remaining..")
      if (level >= 1.0/nGroupsRemaining) {
        ixs += Tuple2(i,cursum)
        curItems = 0
        cursum = 0
        totSize = 0
        totItems = 0
        nGroupsRemaining -= 1
      }
    }
    ixs += Tuple2(files.length-1,cursum)
    info(s"Group sizes = ${ixs.mkString(",")}")
    val ts = System.currentTimeMillis.toString.substring(6,10)
    import java.nio.file._
    val franges = Range(0,Math.min(nWorkers,ixs.length+1)).map { p =>
      val ndir = s"$dir/$ts/${workers(p).getKey}"
      new File(ndir).mkdirs
      files.slice(if (p == 0) 0 else ixs(p - 1)._1, ixs(p)._1).map { case (path, len) =>
        val link = s"$ndir/${new File(path).getName}"
        if (!new File(link).exists) {
          Files.createSymbolicLink(Paths.get(link), Paths.get(path))
        }
      }
      (ndir, new File(ndir).listFiles)
    }
    info(s"Dividing work into: ${franges.map(fr=> s"(${fr._1}:${fr._2.mkString(",")})").mkString("\n")}")
    franges.zipWithIndex.foreach { case ((dir, files),ix) =>
      pool.submit(
        new Callable[String]() {
          override def call(): String = {
            val workerNum = workers(ix).getKey
            inUse.put(workerNum,true)
            val res =runSparkJob(conf, master, tfServerHostAndPort, imgApp, dir, outDir, ix)
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
            inUse.put(workerNum,false)
            res
          }
        }
      )
    }
    s"Submitted ${franges.length} groups of image processing jobs"
  }

  def runSparkJob(conf: TfAppConfig, master: String, tfServerHostAndPort: String, imgApp: String, dir: String, outDir: String, ntx1: Int) = {

    def txPrint(tx1: Int, msg: String) = {
      def ix = Array(92,91,93,94,95,96,32,33,35,36,97,37)
//      info(("\n" + msg).replace("\n", s"\n\033[${ix(tx1)}m TX$tx1 >> \033[0m"))
      info(("\n" + msg + "\033[0m").replace("\n", s"\n\033[${ix(tx1)}m TX$tx1 >> "))
    }
    val spark = SparkSession.builder.master(master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val bcTx1= sc.broadcast(getTx1s.apply(ntx1))
    val irdd = sc.binaryFiles(dir,1)
    txPrint(ntx1,s"runsparkJob: binary files rdd on $dir ready")
    val out = irdd.mapPartitionsWithIndex { case (np, part) =>
      val (tx1Host, tx1Port) = bcTx1.value
      val tfClient = TfClient(conf, tx1Host, tx1Port)
      part.map { case (path, contents) =>
        val outputTag = s"${TcpUtils.getLocalHostname}-Part$np-$path"
        val imgLabel = LabelImgRest(None, master, tfServerHostAndPort, s"SparkPartition-$np", imgApp, path, outDir, outputTag, contents.toArray)
        txPrint(ntx1,s"Running labelImage for $imgLabel")
        val res = labelImg(tfClient, imgLabel)
//        val pout =res.toString.split("\n").map{ l => s"TX$ntx1: $l"}
        txPrint(ntx1,s"LabelImage result: $res")
        res
      }
    }
    val c = out.collect
    c.foreach { li =>
      val fp = if (li.value.fpath.startsWith("file:")) {
        val f = li.value.fpath.substring("file:".length)
        val e = f.zipWithIndex.find{_._1 != '/'}.get._2
        '/' + f.substring(e)
      } else {
        li.value.fpath
      }
      val fname = fp.substring(fp.lastIndexOf("/")+1)
      FileUtils.writeBytes(s"${li.value.outDir}/$fname.result", li.value.cmdResult.stdout.getBytes("ISO-8859-1"))
//      info(s"${li.value.fpath}.result", li.value.cmdResult.stdout.getBytes("ISO-8859-1"))
    }
    info(s"Finished runSparkJob for tx1=$ntx1")
    c.mkString("\n")
  }

  def labelImgViaRest(conf: TfAppConfig, lireq: LabelImgRest) = {
    info(s"Sending labelImgRequest to webserver: $lireq ..")
    val map = Map("json" -> JsonUtils.toJson(LabelImgWebRest(lireq)))
    val resp = if (UseWeb) {
      val resp = HttpUtils.post(s"${TfWebServer.getUri(lireq.restHostAndPort.get)}", map)
    } else {
      process(map)
    }
    resp
  }

  /*
  --rest localhost:8190 local[*] 192.168.0.2 tensorflow-labelimage /data/scenery scenery
  */
  def main(args: Array[String]): Unit = {
    if (args.length!=1) {
      System.err.println("Usage: TfSubmitter <submitter.yml file>")
      System.exit(-1)
    }
    val conf = TfConfig.getAppConfig(args(0))
    if (conf.isSpark) {
      val res = runSparkJobs(conf, conf.master, conf.tfServerAndPort, conf.imgApp, s"${conf.inDir}", s"${conf.outDir}",
        conf.nPartitionsPerTx1.toString.toInt)
      info("results: " + res)
    } else {
      val res = if (conf.isRest) {
        labelImgViaRest(conf, LabelImgRest(Option(conf.restHostAndPort), conf.tfServerAndPort, conf.master, "TFViaRest", conf.imgApp,
          conf.inDir, conf.outDir, conf.outTag, null))
      } else {
        labelImg(TfClient(conf), LabelImgRest(None, conf.master, conf.tfServerAndPort, "TFCommandLine", conf.imgApp, conf.inDir, conf.outDir, conf.outTag, FileUtils.readFileBytes(conf.inDir)))
      }
      info(res.toString)
    }
  }
}
