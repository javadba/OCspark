package org.openchai.tcp.xfer

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc._

abstract class XferServerIf extends ServerIf("XferServerIf")

object XferServer {

  var server: TcpServer = _

  def apply(tcpParams: TcpParams) = {
//    if (System.currentTimeMillis > 0) {
//      throw new IllegalStateException("Why in apply(NioServer) ?")
//    }
    server = TcpServer(tcpParams.server, tcpParams.port,
      new NioXferServerIf(tcpParams))
    server
  }

  def apply(q: BlockingQueue[TaggedEntry], tcpParams: TcpParams) = {
    server = TcpServer(tcpParams.server, tcpParams.port,
      new QXferServerIf(q, tcpParams))
    server
  }

  def main(args: Array[String]): Unit = {
    val host = args(0)
    val port = args(1).toInt
//    val server = apply(TcpParams(host, port))
    val q = new ArrayBlockingQueue[TaggedEntry](1000)
    val qreader = new Thread() {
      override def run(): Unit = {
        println("QReader thread started")
        while (true) {
          val v = q.take
          println(s"QReader: received $v")
        }
      }
    }
    qreader.start
    val server = apply(q, TcpParams(host, port))
    server.start
    Thread.currentThread.join
  }
}


