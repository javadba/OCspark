package org.openchai.tcp.xfer

import java.nio.file.{Files, Paths}
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc.{P2pReq, P2pResp, TcpParams, TcpServer}
import org.openchai.tcp.util.{FileUtils, TcpCommon}

case class QTestParams(master: String, cHost: String, cPort: Int, sHost: String, sPort: Int)

class XferQServerIf(outQ: BlockingQueue[TaggedEntry], tcpParams: TcpParams, qServerIf: QXferServerIf)
  extends XferConServerIf(/*tcpParams, qServerIf */) {
  println("Created XferQServerIf")

  override def consume(config: TcpXferConfig) = {
    val payload = super.defaultConsume(config).asInstanceOf[TaggedEntry]
    println(s"Consuming message of length ${payload.toString.length}")
    val res = outQ.offer(payload)
    res
  }

}


class QXferServerIf(q: BlockingQueue[TaggedEntry], tcpParams: TcpParams) extends XferServerIf {

  def writeQ(path: DataPtr, data: TaggedEntry) = {
//    val _md5 = md5(buf.array.slice(0,buf.position))
    q.offer(data)
  }

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: XferWriteReq =>
        val params = o.value.asInstanceOf[XferWriteParams]
//        FileUtils.checkMd5(params.config.finalPath, params.data, params.md5)
        println(s"XferWriteReq! datalen=${params.data.length}")
//        FileUtils.checkMd5(path, FileUtils.md5(data), md5In)
        val start = System.currentTimeMillis
        val len = writeQ(params.config.finalPath, TaggedEntry(params.tag, params.data))
        val elapsed = System.currentTimeMillis - start
        XferWriteResp("abc", params.data.length, elapsed, Array.empty[Byte])
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")

    }
  }

}

// The main thing we need to override here is using XferQConServerIf inside the server object
class QXferServer(outQ: BlockingQueue[TaggedEntry], tcpParams: TcpParams, xtcpParams: TcpParams)
  extends XferConServer(tcpParams, xtcpParams) {
  override val xferServerIf = new QXferServerIf(outQ, xtcpParams)

  override def start() = {
    xferServerThread.start
    // Notice the use of XferQConServerIf as third parameter: but still using xferServerIf inside that constructor
    server = TcpServer(tcpParams.server, tcpParams.port, new XferQServerIf(outQ, tcpParams, xferServerIf))
    server.start
    this
  }

}

object QXferServer {
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
    val q = new ArrayBlockingQueue[TaggedEntry](1000)

    import org.openchai.tcp.xfer.XferConCommon._
    val cont = TestControllers
    val params = QTestParams("local", cont.conHost, cont.conPort, cont.dataHost, cont.dataPort)
    val qserver = new QXferServer(q, TcpParams(params.cHost, params.cPort), TcpParams(params.sHost, params.sPort))
    qserver.start
    Thread.currentThread.join
  }
}
