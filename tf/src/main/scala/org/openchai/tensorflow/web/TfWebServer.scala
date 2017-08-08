package org.openchai.tensorflow.web

import org.openchai.tensorflow.TfSubmitter

object TfWebServer {
  val TfWebContext = "/tfweb"
  val DefaultPort = 8190
  def getUri(restHostAndPort: String) = s"http://${restHostAndPort}$TfWebContext"

  def run(port: Int) {

    val server = new SimpleHttpServer(DefaultPort) {
      override def process(params: Map[String, String]) = {
        TfSubmitter.process(params)
      }
    }
    val params = Map[String, String]("ctx" -> "/tfweb")
    server.run(params)
    Thread.currentThread.join
  }

  def main(args: Array[String]): Unit = {
    run(if (args.nonEmpty) args(0).toInt else DefaultPort)
  }

}
