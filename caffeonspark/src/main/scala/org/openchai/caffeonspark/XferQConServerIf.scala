package org.openchai.tcp.xfer

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.caffeonspark.QueueEntry
import org.openchai.tcp.rpc._

import org.openchai.tcp.util.Logger._

// The main thing we need to override here is using XferQConServerIf inside the server object
class XferQConServer(outQ: BlockingQueue[QueueEntry], tcpParams: TcpParams, xtcpParams: TcpParams) extends XferConServer(tcpParams, xtcpParams) {
  override val xferServerIf = new QXferServerIf[QueueEntry](outQ, xtcpParams)
  override def start() = {
    xferServerThread.start
    // Notice the user of XferQConServerIf as third parameter: but still using xferServerIf inside that constructor
    server = TcpServer(tcpParams.server, tcpParams.port,
      new XferQConServerIf(outQ, tcpParams, xferServerIf))
    server.start
    this
  }

}

case class QTestParams(master: String, cHost: String, cPort: Int, sHost: String, sPort: Int)

object XferQConServer {
  def main(args: Array[String]): Unit = {
//    val host = args(0)
//    val port = args(1).toInt
//    val xhost = host
//    val xport = port + 1
    val q = new ArrayBlockingQueue[QueueEntry](1000)
//
//    val qReader = new Thread() {
//      var canceled: Boolean = _
//      override def run(): Unit = {
//        while (!canceled) {
//          val out = q.take
//          info(s"QReader: received msg [$out]")
//        }
//      }
//    }
//    qReader.start
//    val server = new XferQConServer(q, TcpParams(host, port), TcpParams(xhost, xport))
//    server.start
//    Thread.currentThread.join

  import org.openchai.tcp.xfer.XferConCommon._
  val cont = testControllers
  val params = QTestParams("local", cont.conHost, cont.conPort, cont.dataHost, cont.dataPort)
    val qserver = new XferQConServer(q, TcpParams(params.cHost, params.cPort), TcpParams(params.sHost, params.sPort))
    qserver.start
//    Thread.sleep(100)
    Thread.currentThread.join
  }
}

class XferQConServerIf(outQ: BlockingQueue[QueueEntry], tcpParams: TcpParams, qServerIf: QXferServerIf[QueueEntry])
  extends XferConServerIf(tcpParams, qServerIf) {

  override def consume(config: TcpXferConfig) = {
    val payload = super.defaultConsume(config).asInstanceOf[QueueEntry]
    val res = outQ.offer(payload)
    res
  }

}

