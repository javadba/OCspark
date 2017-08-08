package org.openchai.tensorflow

import java.io.File
import java.util.Base64
import java.util.concurrent.{Callable, ConcurrentNavigableMap, Executors}

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.{FileUtils, TcpUtils}
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
        //    val outf = "/tmp/base64.after.dat"
        //    println(s"Writing decoded base64 bytes to $outf..")
        //    FileUtils.writeBytes(outf, bytes)
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
    // call to TfSubmitter.labelImg if desired. for now we don't want
    // too many config knobs so hardcode to spark

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
//    val (s,p) = lireq.tfServerHostAndPort.split(":").grouped(2).map(arr => (arr(0),arr(1).toInt)).toSeq.head
//    val tfClient = TfClient(s,p)
    val md5 = FileUtils.md5(lireq.contents)
    val res = tfClient.labelImg(LabelImgStruct(lireq.outputTag, lireq.path,
      lireq.contents, md5, Option(lireq.imgApp)))
    //      println(s"${lireq.workerName}: Received label result: $res")
    res
  }

  lazy val getTx1s = {
    val f = "/shared/tx1-slaves.txt"
    scala.io.Source.fromFile(f).getLines.map{ l => l.split(":")}.map{ arr => (arr(0),arr(1).toInt)}.toSeq
  }

  lazy val pool = Executors.newFixedThreadPool(getTx1s.length)
  val inUse: ConcurrentNavigableMap[Int,Boolean] = new java.util.concurrent.ConcurrentSkipListMap[Int,Boolean]()
//  Range(0,getTx1s.length).map{ ix  => inUse.put(ix,getTx1s(ix))}
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
    var ixs = mutable.ArrayBuffer[Int]()
    var cursum: Int = 0
    for (i <- Range(0,files.length)) {
      cursum += files(i)._2.toInt
      if (cursum >= totSize / nWorkers) {
        ixs += i
      }
    }
    import java.nio.file._
    val franges = Range(0,Math.min(nWorkers,ixs.length)).map { p =>
      val ndir = s"$dir/${workers(p).getKey}"
      new File(ndir).mkdir
      files.slice(if (p == 0) 0 else (p - 1), ixs(p)).map { case (path, len) =>
//        new File(path).renameTo(new File(s"$ndir/${new File(path).getName}"))
        Files.createSymbolicLink(Paths.get(s"$ndir/${new File(path).getName}"),Paths.get(path))
      }
      (ndir, new File(ndir).listFiles)
    }
    println(s"Dividing work into: ${franges.mkString("\n")}")
    franges.zipWithIndex.foreach { case ((dir, files),ix) =>
      pool.submit(
        new Callable[String]() {
          override def call(): String = {
            val workerNum = workers(ix).getKey
            inUse.put(workerNum,true)
            val res =runSparkJob(master, tfServerHostAndPort, imgApp, dir, ix + 1)
            import java.nio.file._
            Files.delete(Paths.get(dir))
            inUse.put(workerNum,false)
            println(s"Callable: result from worker=$workerNum = $res")
            res
          }
        }
      )
    }
    s"Submitted ${franges.length} groups of image processing jobs"
  }

  def runSparkJob(master: String, tfServerHostAndPort: String, imgApp: String, dir: String, tx1Num: Int) = {

    val spark = SparkSession.builder.master(master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val bcTx1= sc.broadcast(getTx1s.apply(tx1Num))
    val irdd = sc.binaryFiles(dir,1)
    println(s"runsparkJob($tx1Num): binary files rdd on $dir ready")
    val out = irdd.mapPartitionsWithIndex { case (np, part) =>
      println(s"MapPartitions for part=$np on tx1#$tx1Num")
      val (tx1Host, tx1Port) = bcTx1.value
      val tfClient = TfClient(tx1Host, tx1Port)
      println(s"Running on tx1=(${bcTx1.value}")
      part.map { case (path, contents) =>
        val outputTag = s"${TcpUtils.getLocalHostname}-Part$np-$path"
        val imgLabel = LabelImgRest(None, master, tfServerHostAndPort, s"SparkPartition-$np", imgApp, path, outputTag, contents.toArray)
        println(s"Running labelImage on tx1=(${bcTx1.value} for $imgLabel")
        val res = labelImg(tfClient, imgLabel)
        println(s"LabelImage result from ${bcTx1.value}: $res")
        res
      }
    }
    val c = out.collect
    c.mkString("\n")
  }

  def runSparkJobOld(master: String, tfServerHostAndPort: String, imgApp: String, dir: String, nPartitions: Int = 1) = {

    val spark = SparkSession.builder.master(master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val tx1s =  getTx1s
    val nparts = tx1s.length
    val bcTx1s = sc.broadcast(tx1s)
    val irdd = sc.binaryFiles(dir, nparts)
    val out = irdd.mapPartitionsWithIndex { case (np, part) =>
      val tx1s = bcTx1s.value
      val serverNum = np % tx1s.length
      println(s"mapPartitions: associating partition #$np with server#$serverNum")
      val (tx1Host, tx1Port) = bcTx1s.value(serverNum)
      val tfClient = TfClient(tx1Host, tx1Port)
      part.map { case (path, contents) =>
        val outputTag = s"${TcpUtils.getLocalHostname}-Part$np-$path"
        val res = labelImg(tfClient, LabelImgRest(None, master, tfServerHostAndPort, s"SparkPartition-$np", imgApp, path, outputTag, contents.toArray))
        res
      }
    }
    val c = out.collect
    c
  }

  def labelImgViaRest(lireq: LabelImgRest) = {
    //    println(s"POSTing $lireq")
    //    val outf="/tmp/base64.before-post.dat"
    //    println(s"labelImg: writing data before POST to file $outf..")
    //    FileUtils.writeBytes(outf,lireq.contents)
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
//        val bytes = FileUtils.readFileBytes(imgPath)
//        labelImgViaRest(LabelImgRest(Option(restHost), tfServerHost, master, "TFViaRest", imgApp, imgPath, imgTag, bytes))
      } else {
        val Array(master, tfServerHost, imgApp, imgPath, imgTag) = args
        labelImg(TfClient(), LabelImgRest(None, master, tfServerHost, "TFCommandLine", imgApp, imgPath, imgTag, FileUtils.readFileBytes(imgPath)))
      }
      println(res)
    }
  }
}
