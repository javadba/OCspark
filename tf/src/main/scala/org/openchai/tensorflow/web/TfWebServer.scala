package org.openchai.tensorflow.web

import org.openchai.tcp.util.TcpUtils
import org.openchai.tensorflow.JsonUtils._
import org.openchai.tensorflow.{LabelImgRest, LabelImgWebRest, TfSubmitter}

object TfWebServer {
  val TfWebContext = "/tfweb"
  val DefaultPort = 8190
  val UseSpark = true // TOdo: make configurable with direct
                    // call to TfSubmitter.labelImg if desired. for now we don't want
                    // too many config knobs so hardcode to spark

  def getUri(restHostAndPort: String) = s"http://${restHostAndPort}$TfWebContext"

  def run(port: Int, host: String = TcpUtils.getLocalHostname): Unit = {
    val tfWebHandler = new RootHandler {
      override def process(params: Map[String, String]) = {
//        try {
//          throw new IllegalStateException("Foobar is not a State")
//        } catch {
//          case e: Exception => e.printStackTrace
//        }
        if (!params.contains("json")) {
          s"TfWebServer: ERROR: no json in payload"
        } else {
          println(s"Received request ${params.map { p => val ps = p.toString; ps.substring(0, Math.min(ps.length, 300)) }.mkString(",")}")
          val json = params("json")
          val labelImgWebReq = parseJsonToCaseClass[LabelImgWebRest](json)
          val resp = if (UseSpark) {
            println(s"TfWebServer: TfSubmitter.runSparkJob..")
            val lr = labelImgWebReq
            val lresp = TfSubmitter.runSparkJob(lr.master, lr.tfServerHost, lr.imgApp, lr.path)
            lresp.mkString("\n")
          } else {
            println(s"TfWebServer: TfSubmitter.labelImg..")
            val lresp = TfSubmitter.labelImg(LabelImgRest(labelImgWebReq))
            lresp.value.toString
          }
          println(s"TfWebServer: result is $resp")
          resp
        }
      }
    }
    val server = new SimpleHttpServer(DefaultPort, tfWebHandler)
    val params = Map[String,String]("ctx"->"/tfweb")
    server.run(params)
  }


  def main(args: Array[String]): Unit = {
    run(if (args.nonEmpty) args(0).toInt else DefaultPort)
  }

}
