package org.openchai.tcp.xfer

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.caffeonspark.QueueEntry
import org.openchai.tcp.rpc._

import org.openchai.tcp.util.Logger._

// The main thing we need to override here is using XferQConServerIf inside the server object
class XferQServer(outQ: BlockingQueue[QueueEntry], tcpParams: TcpParams, xtcpParams: TcpParams) extends XferConServer(tcpParams, xtcpParams) {
  override val xferServerIf = new QXferServerIf[QueueEntry](outQ, xtcpParams)

  override def start() = {
    xferServerThread.start
    // Notice the use of XferQConServerIf as third parameter: but still using xferServerIf inside that constructor
    server = TcpServer(tcpParams.server, tcpParams.port, new XferQServerIf(outQ, tcpParams, xferServerIf))
    server.start
    this
  }

}

case class QTestParams(master: String, cHost: String, cPort: Int, sHost: String, sPort: Int)

object XferQServer {
  def main(args: Array[String]): Unit = {
    val q = new ArrayBlockingQueue[QueueEntry](1000)

    import org.openchai.tcp.xfer.XferConCommon._
    val cont = testControllers
    val params = QTestParams("local", cont.conHost, cont.conPort, cont.dataHost, cont.dataPort)
    val qserver = new XferQServer(q, TcpParams(params.cHost, params.cPort), TcpParams(params.sHost, params.sPort))
    qserver.start
    Thread.currentThread.join
  }
}

class XferQServerIf(outQ: BlockingQueue[QueueEntry], tcpParams: TcpParams, qServerIf: QXferServerIf[QueueEntry])
  extends XferConServerIf(tcpParams, qServerIf) {

  override def consume(config: TcpXferConfig) = {
    val payload = super.defaultConsume(config).asInstanceOf[QueueEntry]
    val res = outQ.offer(payload)
    res
  }

}

