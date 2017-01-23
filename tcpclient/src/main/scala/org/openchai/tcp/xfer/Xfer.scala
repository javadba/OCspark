package org.openchai.tcp.xfer

import java.nio.ByteBuffer
import java.nio.file.{FileSystem, Files, Path, Paths}
import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.Logger._
import sun.nio.fs.MacOSXFileSystemProvider
import sun.security.provider.MD5

trait XferConfig  // Probably a json, yaml, or TypeSafe Config

class TcpXferConfig(val tmpPath: String, val finalPath: String) extends XferConfig

case class WriteTcpXferConfig(override val tmpPath: String, override val finalPath: String) extends TcpXferConfig(tmpPath, finalPath)
case class ReadTcpXferConfig(override val tmpPath: String, override val finalPath: String) extends TcpXferConfig(tmpPath, finalPath)

case class XferWriteParams(config: TcpXferConfig, dataPtr: RawData)

case class XferReadParams(config: TcpXferConfig, dataPtr: DataPtr)

case class XferWriteReq(override val value: (Path, RawData)) extends P2pReq[(Path,RawData)]

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
      XferServer.main(args.tail)
    } else {
      XferConClient.main(args.tail)
    }

  }
}

object XferServer {

  var server: TcpServer = _

  def apply(tcpParams: TcpParams) = {
    server = TcpServer(tcpParams.server, tcpParams.port,
      new XferServerIf(tcpParams))
    server
  }

  def main(args: Array[String]): Unit = {
    val host = args(0)
    val port = args(1).toInt
    val server = apply(TcpParams(host, port))
    server.start
    Thread.currentThread.join
  }
}

class XferIf extends ServiceIF {

  private val nReqs = new AtomicInteger(0)

  def write(xferParams: XferWriteParams) = {
    // while (keepGoing(n).value) {
    debug(s"Sending request: $xferParams ..")
    val resp = getRpc().request(XferWriteReq(Paths.get(xferParams.config.finalPath), xferParams.dataPtr)).asInstanceOf[XferWriteResp]
    debug(s"Result is $resp")
    resp
  }

  def read(xferParams: XferReadParams) = {
    // while (keepGoing(n).value) {
    debug(s"Sending request: $xferParams ..")
    val resp = getRpc().request(XferReadReq(xferParams.dataPtr)).asInstanceOf[XferWriteResp]
    debug(s"Result is $resp")
    resp
  }

}


class XferServerIf(tcpParams: TcpParams) extends ServerIF {

  private val nReqs = new AtomicInteger(0)

  import java.security.MessageDigest

  val md = MessageDigest.getInstance("MD5")

  def md5(arr: Array[Byte]) = {
    md.update(arr)
    md.digest
  }

  def writeNio(path: DataPtr, data: RawData) = {
    // Allocating nio is not really necessary but just here
    // to simulate whatever rdma done down the road
    val buf = java.nio.ByteBuffer.allocate(data.length)
    buf.put(data)
    val _md5 = md5(buf.array.slice(0,buf.position))
    val out = Files.write(Paths.get(path), buf.array.slice(0,buf.position))
    (buf, buf.position, _md5)
  }

  def readNio(path: DataPtr) = {
    val len = Files.size(Paths.get(path))
    println(s"Reading path $path of len=$len ..")
    val buf = java.nio.ByteBuffer.allocate(len.toInt)
    buf.put(Files.readAllBytes(Paths.get(path)))
    buf.mark
    val _md5 = md5(buf.array.slice(0,buf.position))
    (buf, len, _md5)
  }

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: XferWriteReq =>
        val (path,data) = o.value
        val start = System.currentTimeMillis
        val (buf, len, md5) = writeNio(path.toString, data)
        val elapsed = System.currentTimeMillis - start
        XferWriteResp("abc", len, elapsed, md5)

      case o: XferReadReq =>
        val data = o.value
        val start = System.currentTimeMillis
        val (buf, len, md5) = readNio(o.value)
        val elapsed = System.currentTimeMillis - start
        XferReadResp("abc", data.length, elapsed, md5, buf.array)
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }

}


case class XferParams(configFile: String, data: Array[Byte]) {

  override def toString: String = s"XferParams using $configFile and with datasize=${data.length}"

}

case class XferWriteResult(size: Int, elapsed: Long)

case class XferReadResult(size: Int, elapsed: Long, dataPtr: RawData)

