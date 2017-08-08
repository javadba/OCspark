package org.openchai.tcp.xfer

import java.nio.file.{Files, Paths}
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc.{P2pReq, P2pResp, TcpParams, TcpServer}
import org.openchai.tcp.util.{FileUtils, TcpCommon}

case class QTestParams(master: String, cHost: String, cPort: Int, sHost: String, sPort: Int)

class XferQServerIf(outQ: BlockingQueue[TaggedEntry], tcpParams: TcpParams, qServerIf: QXferServerIf)
  extends XferConServerIf(tcpParams) {
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
//    println(s"writeq: data with tag=${data.tag} len=${data.data.length}")
    q.offer(data)
//    println(s"After writeQ: qsize=${q.size}")
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

