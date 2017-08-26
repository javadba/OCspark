package org.openchai.tensorflow.web


import java.io.{ByteArrayInputStream, ByteArrayOutputStream, StringBufferInputStream}
import java.net.{InetSocketAddress, URLDecoder}
import java.sql.Date
import java.text.Normalizer

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD._

import scala.collection.mutable
import org.openchai.tcp.util.Logger._


abstract class SimpleHttpServer(inPort: Int) {

  val MinWaitMillis = 500
  val DoNormalize = false
  val MaxPrint = 2*1024
//  var (lastRun,lastParams) = (System.currentTimeMillis,"FirstTime")
//  var priorTs = System.currentTimeMillis
//  var dupCalls: Int = 0

  def run(map: Map[String,Any]) = {
    new Thread() {
      override def run() {
        val port = map.getOrElse("port", inPort).asInstanceOf[Int]
        val ctx = map.getOrElse("ctx", "/").asInstanceOf[String]
        val server = new NanoHTTPD(inPort) {
          override def serve(session: IHTTPSession): Response = {
            handle(session)
          }

          //        val server = HttpServer.create(new InetSocketAddress(port), 0)
          //        server.createContext(ctx, handler)
          //        server.setExecutor(null)
        }

        info(s"Starting SimpleHttpServer on port $port..")
        server.start()
        Thread.currentThread.join
      }
    }.start
    this
  }


  def handle(session: IHTTPSession) = {
    info(s"in handle")
    val curTs = System.currentTimeMillis

    val rawQuery = session.getQueryParameterString
    val rparams = rawQuery
    val params = if (rparams != null && rparams.nonEmpty) rparams
    else HttpUtils.readStream(session.getInputStream)
    //      if (sameParams && interval <= 60) {
    val pmap = params.split("&").map(_.split("=")).map { a => (a(0), a(1)) }.toMap
    val eparams = mutable.HashMap() ++ pmap.mapValues(pv =>
      URLDecoder.decode(pv)
    )
    eparams.update("query", rawQuery)

    val resp = process(Map() ++ eparams.toSeq)
    info(s"Result (first $MaxPrint chars): ${resp.substring(0, math.min(MaxPrint, resp.length))}")
    val os = new ByteArrayOutputStream
//    val outb = resp.getBytes("UTF-8")
    val nout = if (DoNormalize) {
      Normalizer.normalize(resp, Normalizer.Form.NFD)
    } else resp
    val repout = nout.replaceAll("[^\\x00-\\x7F]", "")
    val repoutb = repout.getBytes
    os.write(repoutb)
    os.close()
    newChunkedResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream",new ByteArrayInputStream(os.toByteArray))
  }

  def process(params: Map[String,String]): String

}