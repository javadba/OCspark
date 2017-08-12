package org.openchai.tensorflow

import java.io.File
import java.util.Base64
import java.util.concurrent.{Callable, ConcurrentNavigableMap, Executors}

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.{FileUtils, ProcessUtils, TcpUtils}
import org.openchai.tensorflow.JsonUtils._
import org.openchai.tensorflow.web.{HttpUtils, TfWebServer}

import scala.collection.mutable

case class LabelImgRest(restHostAndPort: Option[String], master: String, tfServerHostAndPort: String, workerName: String,
  imgApp: String, path: String, outputTag: String, contents: Array[Byte])

object LabelImgRest {
  def apply(liwreq: LabelImgWebRest) = {
    if (liwreq.contentsBase64 != null) {
      val bytes = if (liwreq.restHostAndPort.startsWith("/")) {
        FileUtils.readFileBytes(liwreq.restHostAndPort)
      } else {
        Base64.getDecoder.decode(liwreq.contentsBase64)
      }
      new LabelImgRest(Option(liwreq.restHostAndPort), liwreq.master, liwreq.tfServerHostAndPort, liwreq.workerName, liwreq.imgApp, liwreq.path, liwreq.outputTag, bytes)
    } else {
      new LabelImgRest(Option(liwreq.restHostAndPort), liwreq.master, liwreq.tfServerHostAndPort, liwreq.workerName, liwreq.imgApp, liwreq.path, liwreq.outputTag, null)
    }
  }
}

case class LabelImgWebRest(restHostAndPort: String, master: String, tfServerHostAndPort: String, workerName: String, imgApp: String, path: String, outputTag: String, contentsBase64: String)

object LabelImgWebRest {
  def apply(lireq: LabelImgRest) =
    new LabelImgWebRest(lireq.restHostAndPort.get, lireq.master, lireq.tfServerHostAndPort, lireq.workerName, lireq.imgApp, lireq.path, lireq.outputTag,
      if (lireq.contents!=null) {Base64.getEncoder.encodeToString(lireq.contents)} else "")
}

case class TfSubmitter() {
}

object TfSubmitter {
  val UseWeb = false

  def process(params: Map[String, String]) = {
    val UseSpark = true // TODO: make configurable with direct

    if (!params.contains("json")) {
      s"ERROR: no json in payload"
    } else {
      println(s"Received request ${params.map { p => val ps = p.toString; ps.substring(0, Math.min(ps.length, 300)) }.mkString(",")}")
      val json = params("json")
      val labelImgWebReq = parseJsonToCaseClass[LabelImgWebRest](json)
      val resp = if (UseSpark) {
        println(s"TfWebServer: TfSubmitter.runSparkJob..")
        val lr = labelImgWebReq
        val lresp = TfSubmitter.runSparkJobs(lr.master, lr.tfServerHostAndPort, lr.imgApp, lr.path)
        lresp
      } else {
        println(s"TfWebServer: TfSubmitter.labelImg..")
        val lresp = TfSubmitter.labelImg(TfClient(), LabelImgRest(labelImgWebReq))
        lresp.value.toString
      }
      println(s"TfWebServer: result is $resp")
      resp
    }
  }


  def labelImg(tfClient: TfClient, lireq: LabelImgRest) = {
    val md5 = FileUtils.md5(lireq.contents)
    val res = tfClient.labelImg(LabelImgStruct(lireq.outputTag, lireq.path,
      lireq.contents, md5, Option(lireq.imgApp)))
    res
  }

  lazy val getTx1s = {
    val f = "/shared/tx1-slaves.txt"
    scala.io.Source.fromFile(f).getLines.map{ l => l.split(":")}.map{ arr => (arr(0),arr(1).toInt)}.toSeq
  }

  lazy val pool = Executors.newFixedThreadPool(getTx1s.length)
  val inUse: ConcurrentNavigableMap[Int,Boolean] = new java.util.concurrent.ConcurrentSkipListMap[Int,Boolean]()
  Range(0,getTx1s.length).map{ ix  => inUse.put(ix,true)}

  def runSparkJobs(master: String, tfServerHostAndPort: String, imgApp: String, dir: String, nPartitions: Int = 1) = synchronized {
    val files = new File(dir).listFiles.map{ f => (f.getAbsolutePath, f.length) }.sortBy(_._1).toSeq
    val totSize = files.map(_._2).sum
    import collection.JavaConverters._
    val workers = inUse.entrySet.asScala.filter(_.getValue).toSeq.sortBy(_.getKey)
    if (workers.length < inUse.keySet.size) {
      println(s"runSparkJobs: there are ${workers.length < inUse.keySet.size} workers still busy from earlier jobs ..")
    }
    val nWorkers = workers.length
    var ixs = mutable.ArrayBuffer[(Int,Long)]()
    var cursum: Long = 0L
    for (i <- Range(0,files.length)) {
      cursum += files(i)._2.toLong
      if (cursum >= totSize / nWorkers) {
        ixs += Tuple2(i,cursum)
        cursum = 0
      }
    }
    ixs += Tuple2(files.length-1,cursum)
    println(s"Group sizes = ${ixs.mkString(",")}")
    import java.nio.file._
    val franges = Range(0,Math.min(nWorkers,ixs.length+1)).map { p =>
      val ndir = s"$dir/${workers(p).getKey}"
      new File(ndir).mkdir
      files.slice(if (p == 0) 0 else ixs(p - 1)._1, ixs(p)._1).map { case (path, len) =>
        val link = s"$ndir/${new File(path).getName}"
        if (!new File(link).exists) {
          Files.createSymbolicLink(Paths.get(link), Paths.get(path))
        }
      }
      (ndir, new File(ndir).listFiles)
    }
    println(s"Dividing work into: ${franges.map(fr=> s"(${fr._1}:${fr._2.mkString(",")})").mkString("\n")}")
    franges.zipWithIndex.foreach { case ((dir, files),ix) =>
      pool.submit(
        new Callable[String]() {
          override def call(): String = {
            val workerNum = workers(ix).getKey
            inUse.put(workerNum,true)
            val res =runSparkJob(master, tfServerHostAndPort, imgApp, dir, ix)
//            println(s"Callable: result from worker=$workerNum = $res")
            assert(dir.length >= 8)
            try {
              ProcessUtils.exec(s"DeleteDir-$dir", s"rm -rf $dir")
            } catch {
              case e: Exception =>
                println(s"TX$ix: Callable: ERROR: unable to delete $dir")
                e.printStackTrace
            }
            if (new File(dir).exists) {
              println(s"TX$ix: Callable: ERROR: unable to delete $dir")
            } else {
              println(s"TX$ix: Callable: Finished worker $ix")
            }
            inUse.put(workerNum,false)
            res
          }
        }
      )
    }
    s"Submitted ${franges.length} groups of image processing jobs"
  }

  def runSparkJob(master: String, tfServerHostAndPort: String, imgApp: String, dir: String, ntx1: Int) = {

    val spark = SparkSession.builder.master(master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val bcTx1= sc.broadcast(getTx1s.apply(ntx1))
    val irdd = sc.binaryFiles(dir,1)
    println(s"TX$ntx1: runsparkJob($ntx1): binary files rdd on $dir ready")
    val out = irdd.mapPartitionsWithIndex { case (np, part) =>
      println(s"TX$ntx1: MapPartitions for part=$np on tx1#$ntx1")
      val (tx1Host, tx1Port) = bcTx1.value
      val tfClient = TfClient(tx1Host, tx1Port)
      println(s"TX$ntx1: Running on tx1=(${bcTx1.value}")
      part.map { case (path, contents) =>
        val outputTag = s"${TcpUtils.getLocalHostname}-Part$np-$path"
        val imgLabel = LabelImgRest(None, master, tfServerHostAndPort, s"SparkPartition-$np", imgApp, path, outputTag, contents.toArray)
        println(s"TX$ntx1: Running labelImage on tx1=(${bcTx1.value} for $imgLabel")
        val res = labelImg(tfClient, imgLabel)
        val pout =res.toString.split("\n").map{ l => s"TX$ntx1: $l"}
        println(s"TX$ntx1: LabelImage result from ${bcTx1.value}: $pout")
        res
      }
    }
    val c = out.collect
    println(s"Finished runSparkJob for tx1=$ntx1")
    c.mkString("\n")
  }

  def labelImgViaRest(lireq: LabelImgRest) = {
    println(s"Sending labelImgRequest to webserver: $lireq ..")
    val map = Map("json" -> JsonUtils.toJson(LabelImgWebRest(lireq)))
    val resp = if (UseWeb) {
      val resp = HttpUtils.post(s"${TfWebServer.getUri(lireq.restHostAndPort.get)}", map)
    } else {
      process(map)
    }
    resp
  }

  def main(args: Array[String]): Unit = {
    if (args(0).equalsIgnoreCase("--spark")) {
      val Array(master, tfServerHostAndPort, imgApp, dir, nPartitions) = args.slice(1, args.length)
      val dir2 = s"${System.getProperty("user.dir")}/src/main/resources/images/"
      val res = runSparkJobs(master, tfServerHostAndPort, imgApp, s"file:///$dir2", nPartitions.toString.toInt)
      println("results: " + res)
    } else {
      val res = if (args(0).equalsIgnoreCase("--rest")) {
        val Array(restHost, tfServerHost, master, imgApp, imgPath, imgTag) = args.slice(1, args.length)
        labelImgViaRest(LabelImgRest(Option(restHost), tfServerHost, master, "TFViaRest", imgApp, imgPath, imgTag, null))
      } else {
        val Array(master, tfServerHost, imgApp, imgPath, imgTag) = args
        labelImg(TfClient(), LabelImgRest(None, master, tfServerHost, "TFCommandLine", imgApp, imgPath, imgTag, FileUtils.readFileBytes(imgPath)))
      }
      println(res)
    }
  }
}
