package org.openchai.tcp.xfer

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc.{TcpParams, TcpServer}

case class QTestParams(master: String, cHost: String, cPort: Int, sHost: String, sPort: Int)

class XferQServerIf(outQ: BlockingQueue[AnyQEntry], tcpParams: TcpParams, qServerIf: QXferServerIf[AnyQEntry])
  extends XferConServerIf(/*tcpParams, qServerIf */) {
  println("Created XferQServerIf")

  override def consume(config: TcpXferConfig) = {
    val payload = super.defaultConsume(config).asInstanceOf[AnyQEntry]
    println(s"Consuming message of length ${payload.toString.length}")
    val res = outQ.offer(payload)
    res
  }

}


// The main thing we need to override here is using XferQConServerIf inside the server object
class XferQServer(outQ: BlockingQueue[AnyQEntry], tcpParams: TcpParams, xtcpParams: TcpParams)
  extends XferConServer(tcpParams, xtcpParams) {
  override val xferServerIf = new QXferServerIf[AnyQEntry](outQ, xtcpParams)

  override def start() = {
    xferServerThread.start
    // Notice the use of XferQConServerIf as third parameter: but still using xferServerIf inside that constructor
    server = TcpServer(tcpParams.server, tcpParams.port, new XferQServerIf(outQ, tcpParams, xferServerIf))
    server.start
    this
  }

}

object XferQServer {
  def findInQ(q: BlockingQueue[TaggedEntry],tag: String) = {
    val aq = q.asInstanceOf[ArrayBlockingQueue[TaggedEntry]]
    println(s"FindInQ: looking for $tag: entries=${aq.size}")
    val e = {
      var p: Option[TaggedEntry] = None
        while (aq.iterator.hasNext && !aq.isEmpty) {
          val pv = aq.iterator.next
          println(s"Queue entry: ${pv}")
          if (pv.tag == tag) {
            println(s"Found entry ${pv.tag}")
            p = Option(pv)
            aq.remove(pv)
          } else {
            None
          }
        }
      p
    }
    e.flatMap { ee => println(s"For tag=$tag found q entry $ee"); Some(ee) }.getOrElse("No q entry found for tag=$tag")
    e
  }

  def main(args: Array[String]): Unit = {
    val q = new ArrayBlockingQueue[AnyQEntry](1000)

    import org.openchai.tcp.xfer.XferConCommon._
    val cont = TestControllers
    val params = QTestParams("local", cont.conHost, cont.conPort, cont.dataHost, cont.dataPort)
    val qserver = new XferQServer(q, TcpParams(params.cHost, params.cPort), TcpParams(params.sHost, params.sPort))
    qserver.start
    Thread.currentThread.join
  }
}
