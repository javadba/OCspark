package org.openchai.tcp.xfer

import org.openchai.tcp.rpc._

abstract class XferConfig(val tmpPath: String, val finalPath: String) extends java.io.Serializable // Probably a json, yaml, or TypeSafe Config

case class TcpXferConfig(override val tmpPath: String, override val finalPath: String) extends XferConfig(tmpPath, finalPath)

case class XferReadParams(tag: String, config: TcpXferConfig, dataPtr: DataPtr)

case class XferWriteReq(override val value: XferWriteParams) extends P2pReq[XferWriteParams]

case class XferReadReq(override val value: DataPtr) extends P2pReq[DataPtr]

case class XferWriteResp(magic: String, len: Int, elapsed: Long, md5: Array[Byte]) extends XferResp

case class XferReadResp(magic: String, len: Int, elapsed: Long, md5: Array[Byte], data: RawData) extends XferResp

object Xfer {
  def main(args: Array[String]): Unit = {
    val serverOrClient = args(0)
    System.setProperty("java.net.preferIPv4Stack", "true")

    if (serverOrClient.toLowerCase().endsWith("server")) {
      XferConServer.main(args.tail)
    } else {
      XferConClient.main(args.tail)
    }

  }
}


case class XferWriteResult(size: Int, elapsed: Long)

case class XferReadResult(size: Int, elapsed: Long, dataPtr: RawData)

