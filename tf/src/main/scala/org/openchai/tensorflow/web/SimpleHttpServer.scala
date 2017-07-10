package org.openchai.tensorflow.web


import java.net.{InetSocketAddress, URLDecoder}
import java.text.Normalizer

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

import scala.collection.mutable

class SimpleHttpServer(inPort: Int, handler: RootHandler) {

  def run(map: Map[String,Any]) = {
    new Thread() {
      override def run() {
        val port = map.getOrElse("port", inPort).asInstanceOf[Int]
        val ctx = map.getOrElse("ctx", "/").asInstanceOf[String]
        val server = HttpServer.create(new InetSocketAddress(port), 0)
        server.createContext(ctx, handler)
        server.setExecutor(null)

        println(s"Starting SimpleHttpServer on port $port..")
        server.start()
        Thread.currentThread.join
      }
    }.start
    this
  }

}

abstract class RootHandler extends HttpHandler {

  val DoNormalize = false
  val MaxPrint = 2*1024
  var (lastRun,lastParams) = (System.currentTimeMillis,"FirstTime")
  def handle(t: HttpExchange) = {
//    val res = process(t.getRequestBody)
    val rparams = t.getRequestURI.getQuery
    val params = if (rparams!=null && rparams.nonEmpty) rparams
          else HttpUtils.readStream(t.getRequestBody)
    val interval = System.currentTimeMillis - lastRun
    val sameParams = params == lastParams
    lastRun = System.currentTimeMillis
    lastParams = params
    if (sameParams && interval <= 6000) {
        println(s"Warning: Ignoring Duplicate request after $interval ms")
    } else {
      if (sameParams) {
        println(s"Warning: *PROCESSING* duplicate request after $interval ms")
      }
      val pmap = params.split("&").map(_.split("=")).map{ a => (a(0),a(1))}.toMap
      val query = t.getRequestURI.getRawQuery
      val eparams = mutable.HashMap() ++ pmap.mapValues(pv=>
        URLDecoder.decode(pv)
      )
      eparams.update("query", query)

      val res = process(Map() ++ eparams.toSeq)
      System.err.println(s"Result (first $MaxPrint chars): ${res.substring(0,math.min(MaxPrint, res.length))}")
      sendResponse(t, res)
    }
  }

  def sendResponse(t: HttpExchange, resp: String) {
    t.sendResponseHeaders(200, resp.length())
    val os = t.getResponseBody
//    val outb = resp.getBytes("UTF-8")
    val nout = if (DoNormalize) {
      Normalizer.normalize(resp, Normalizer.Form.NFD)
    } else resp
    val repout = nout.replaceAll("[^\\x00-\\x7F]", "")
    val repoutb = repout.getBytes
    os.write(repoutb)
    os.close()
  }

  def process(params: Map[String,String]): String

}