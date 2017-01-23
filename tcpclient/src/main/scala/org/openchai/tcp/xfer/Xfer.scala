package org.openchai.tcp.xfer

import java.nio.file.{Path, Paths}
import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.Logger._

trait XferConfig extends java.io.Serializable // Probably a json, yaml, or TypeSafe Config

case class TcpXferConfig(val tmpPath: String, val finalPath: String) extends XferConfig

//case class WriteTcpXferConfig(override val tmpPath: String, override val finalPath: String) extends TcpXferConfig(tmpPath, finalPath)
//case class ReadTcpXferConfig(override val tmpPath: String, override val finalPath: String) extends TcpXferConfig(tmpPath, finalPath)

case class XferWriteParams(config: TcpXferConfig, dataPtr: RawData)

case class XferReadParams(config: TcpXferConfig, dataPtr: DataPtr)

case class XferWriteReq(override val value: (String, RawData)) extends P2pReq[(String,RawData)]

case class XferReadReq(override val value: DataPtr) extends P2pReq[DataPtr]

//case class XferWriteResult(elapsed: Long, rc: Int, stdout: String, stderr: String)

//class XferWriteResp(params: XferWriteParams, elapsed: Long) extends XferResp

//case class XferReadResult(elapsed: Long, rc: Int, stdout: String, stderr: String)

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

