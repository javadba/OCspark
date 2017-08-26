package org.openchai.tcp.xfer

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc.{TcpParams, TcpServer}
import org.openchai.tcp.xfer.XferConCommon.XferControllerArgs
import org.openchai.tcp.util.Logger._

object QXferConServer {

  def findInQ(q: BlockingQueue[TaggedEntry],tag: String) = {
    val aq = q.asInstanceOf[ArrayBlockingQueue[TaggedEntry]]
    info(s"FindInQ: looking for $tag: entries=${aq.size}")
    val e = {
      var p: Option[TaggedEntry] = None
        val it = aq.iterator
        while (it.hasNext && !aq.isEmpty) {
          val pv = it.next
//          info(s"Queue entry: ${pv}")
          if (pv.tag == tag) {
            info(s"Found entry ${pv.tag}")
            p = Option(pv)
            aq.remove(pv)
          } else {
            None
          }
        }
      p
    }
    e.flatMap { ee => info(s"For tag=$tag found q entry $ee"); Some(ee) }.getOrElse("No q entry found for tag=$tag")
    e
  }

  def makeXferControllers(q: ArrayBlockingQueue[TaggedEntry], args: XferControllerArgs) = {
    val tcpParams = TcpParams(args.conHost, args.conPort)
    val xtcpParams = TcpParams(args.dataHost, args.dataPort)
    val server = new QXferConServer(q, tcpParams, xtcpParams /*, xferConf */)
    server
  }

  def apply(q: BlockingQueue[TaggedEntry], cTcpParams: TcpParams, sTcpParams: TcpParams) = {

    val qserver = new QXferConServer(q, cTcpParams, sTcpParams)
    qserver.start
    Thread.currentThread.join
  }

 def main(args: Array[String]): Unit = {
    val q = new ArrayBlockingQueue[TaggedEntry](1000)
    import org.openchai.tcp.xfer.XferConCommon._
    val cont = TestControllers
    val params = QTestParams("local", cont.conHost, cont.conPort, cont.dataHost, cont.dataPort)
    apply(q, TcpParams(params.cHost, params.cPort), TcpParams(params.sHost, params.sPort))
//    val (host,port,xhost,xport,ahost, aport, configFile) = XferConServer.makeXferConnections(args)
//    val server = makeXferControllers(q, XferControllerArgs(host,port,xhost,xport,ahost,aport, configFile,Array.empty[Byte],null,null))
    Thread.currentThread.join
  }

}

class QXferConServer(val q: BlockingQueue[TaggedEntry], override val tcpParams: TcpParams, override val xtcpParams: TcpParams)
  extends XferConServer(tcpParams, xtcpParams) {

  override lazy val xferServerIf = new QXferServerIf(q, xtcpParams)

  override def start() = {
    xferServerThread.start
    // Notice the use of XferQConServerIf as third parameter: but still using xferServerIf inside that constructor
    server = TcpServer(tcpParams.server, tcpParams.port, new XferQServerIf(q, tcpParams, xferServerIf))
    server.start
    this
  }

  override lazy val xferServerThread = new Thread() {
    override def run() = {
      val thread = XferServer.apply(q, xtcpParams)
      thread.start
    }
  }

}