package org.openchai.tensorflow

import java.io.{File, FileReader}
import java.nio.file.{Files, Paths}
import java.util.Base64
import java.util.concurrent.{Callable, ConcurrentNavigableMap, Executors}

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
  def allowsExt(ext: String) = extWhiteList.isEmpty || extWhiteList.get.contains(ext.toLowerCase)

  private var _extWhiteList = Option("jpg jpeg png gif svg tiff".split(" ").toSeq)
  def extWhiteList = _extWhiteList

  def setExtWhiteList(exts: Option[Seq[String]])  = _extWhiteList = exts
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
        val msg = s"TfWebServer: TfSubmitter can not run Spark jobs: invoke SparkSubmitter instead.."
        error(msg)
        msg
      } else {
        debug(s"TfWebServer: TfSubmitter.labelImg..")
        val lresp = TfSubmitter.labelImg(TfClient(conf), LabelImgRest(labelImgWebReq))
        lresp.value.toString
      }
      info(s"TfWebServer: result is $resp")
      resp
    }
  }

  @inline def getResetWatermarkFileName(dir: String) = s"$dir/watermark.reset.txt"
  @inline def getWatermarkFileName(dir: String) = s"$dir/watermark.txt"

  val NumExt = "^[\\d+]$".r

  @inline def isDigitsExt(fname: String) = NumExt.findAllIn(fileExt(fname)).nonEmpty

  def labelImg(tfClient: TfClient, lireq: LabelImgRest) = {
    val fmd5 = md5(lireq.contents)
    val res = tfClient.labelImg(LabelImgStruct(lireq.outputTag, lireq.imgApp, lireq.path, lireq.outPath,
      lireq.contents, fmd5))
    info(s"TfSubmitter.labelImg: LabelImgResp=$res")
    res
  }

  lazy val getTx1s = {
    val f = "/shared/tx1-slaves.txt"
    val slaves = scala.io.Source.fromFile(f).getLines.map { l => l.split(":") }.map { arr => (arr(0), arr(1).toInt) }.toSeq
    info(s"Slaves from $f: ${slaves.mkString(",")}")
    slaves
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
      val msg = s"TfWebServer: TfSubmitter can not run Spark jobs: invoke SparkSubmitter instead.."
      throw new IllegalArgumentException(msg)
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
