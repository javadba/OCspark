package org.openchai.tensorflow

import java.util.Base64

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.{FileUtils, TcpUtils}
import org.openchai.tensorflow.web.{HttpUtils, TfWebServer}

case class LabelImgRest(restHostAndPort: Option[String], tfServerHost: String, master: String, workerName: String,
  imgApp: String, path: String, outputTag: String, contents: Array[Byte])

object LabelImgRest {
  def apply(liwreq: LabelImgWebRest) = {
    val bytes = if (liwreq.restHostAndPort.startsWith("/")) {
      FileUtils.readFileBytes(liwreq.restHostAndPort)
    } else {
      Base64.getDecoder.decode(liwreq.contentsBase64)
      //    val outf = "/tmp/base64.after.dat"
      //    println(s"Writing decoded base64 bytes to $outf..")
      //    FileUtils.writeBytes(outf, bytes)
    }
    new LabelImgRest(Option(liwreq.restHostAndPort), liwreq.tfServerHost, liwreq.master, liwreq.workerName, liwreq.imgApp, liwreq.path, liwreq.outputTag,
      bytes)
  }
}

case class LabelImgWebRest(restHostAndPort: String, tfServerHost: String, master: String, workerName: String, imgApp: String, path: String, outputTag: String, contentsBase64: String)

object LabelImgWebRest {
  def apply(lireq: LabelImgRest) =
    new LabelImgWebRest(lireq.restHostAndPort.get, lireq.tfServerHost, lireq.master, lireq.workerName, lireq.imgApp, lireq.path, lireq.outputTag,
      Base64.getEncoder.encodeToString(lireq.contents))
}

case class TfSubmitter() {
}

object TfSubmitter {

  def labelImg(lireq: LabelImgRest) = {
    val tfClient = TfClient(lireq.tfServerHost)
    val md5 = FileUtils.md5(lireq.contents)
    val res = tfClient.labelImg(LabelImgStruct(lireq.outputTag, lireq.path,
      lireq.contents, md5, Option(lireq.imgApp)))
    //      println(s"${lireq.workerName}: Received label result: $res")
    res
  }

  def getTx1s() = {
    val f = "/shared/tx1-slaves.txt"
    scala.io.Source.fromFile(f).getLines.map{ l => l.split(":")}.map{ arr => (arr(0),arr(1).toInt)}
  }

  def runSparkJob(master: String, tfServerHost: String, imgApp: String, dir: String, nPartitions: Int = 1) = {

    val spark = SparkSession.builder.master(master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val tx1s = getTx1s
    val nparts = tx1s.length
    val bcTx1s = sc.broadcast(tx1s)
    val irdd = sc.binaryFiles(dir, nparts)
    val out = irdd.mapPartitionsWithIndex { case (np, part) =>
      val (tx1Host, tx1Port) = bcTx1s.value
      val tfClient = TfClient(tx1Host, tx1Port)
      part.map { case (path, contents) =>
        val outputTag = s"${TcpUtils.getLocalHostname}-Part$np-$path"
        val res = labelImg(LabelImgRest(None, tfServerHost, master: String, s"SparkPartition-$np", imgApp, path, outputTag, contents.toArray))
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
    val resp = HttpUtils.post(s"${TfWebServer.getUri(lireq.restHostAndPort.get)}",
      Map("json" -> JsonUtils.toJson(LabelImgWebRest(lireq)))
    )
    resp
  }

  def main(args: Array[String]): Unit = {
    if (args(0).equalsIgnoreCase("--spark")) {
      val Array(master, tfServerHost, imgApp, dir, nPartitions) = args.slice(1, args.length)
      val dir2 = s"${System.getProperty("user.dir")}/src/main/resources/images/"
      runSparkJob(master, tfServerHost, imgApp, s"file:///$dir2", nPartitions.toString.toInt)
    } else {
      val res = if (args(0).equalsIgnoreCase("--rest")) {
        val Array(restHost, tfServerHost, master, imgApp, imgPath, imgTag) = args.slice(1, args.length)
        val bytes = FileUtils.readFileBytes(imgPath)
        //        val outf="/tmp/base64.initial-diskread.dat"
        //        println(s"main: writing data just after reading from disk to file $outf..")
        //        FileUtils.writeBytes(outf,bytes)
        labelImgViaRest(LabelImgRest(Option(restHost), tfServerHost, master, "TFViaRest", imgApp, imgPath, imgTag, bytes))
      } else {
        val Array(master, tfServerHost, imgApp, imgPath, imgTag) = args
        labelImg(LabelImgRest(None, tfServerHost, master, "TFCommandLine", imgApp, imgPath, imgTag, FileUtils.readFileBytes(imgPath)))
      }
      println(res)
    }
  }
}
