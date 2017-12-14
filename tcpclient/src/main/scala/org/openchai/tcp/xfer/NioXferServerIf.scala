package org.openchai.tcp.xfer

import java.nio.file.{Files, Paths}
import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc.{P2pReq, P2pResp, TcpParams}
import org.openchai.tcp.util.FileUtils
import org.openchai.tcp.util.Logger._

class NioXferServerIf(tcpParams: TcpParams) extends XferServerIf {

  import org.openchai.tcp.util.FileUtils.md5

  private val nReqs = new AtomicInteger(0)

  val AllocSize = 24 * (math.pow(2,20)-1).toInt // 24MB
  var buf = java.nio.ByteBuffer.allocate(AllocSize)

  def writeNio(path: DataPtr, data: RawData, md5In: RawData) = {
    // Allocating nio is not really necessary but just here
    // to simulate whatever rdma done down the road
    if (data.length > buf.capacity) {
      buf = java.nio.ByteBuffer.allocate(data.length)
    }
    buf.clear

    buf.put(data)
    FileUtils.checkMd5(path, buf.array.slice(0,buf.position), md5In)
    val out = Files.write(Paths.get(path), buf.array.slice(0,buf.position))
    (buf, buf.position)
  }

  def readNio(path: DataPtr) = {
    val len = Files.size(Paths.get(path))
    assert(len < Int.MaxValue, s"Attempting to read too large file $path len=$len")
    info(s"Reading path $path of len=$len ..")
    if (len > buf.capacity) {
      buf = java.nio.ByteBuffer.allocate(len.toInt)
    }
    buf.clear
    buf.put(Files.readAllBytes(Paths.get(path)))
    buf.mark
    val _md5 = md5(buf.array.slice(0,buf.position))
    (buf, len, _md5)
  }

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: XferWriteReq =>
        val req = o.value
        FileUtils.checkMd5(req.config.finalPath, req.data, req.md5)
        val start = System.currentTimeMillis
        val (buf, len) = writeNio(req.config.finalPath, req.data, req.md5)
        val elapsed = System.currentTimeMillis - start
        XferWriteResp("abc", len, elapsed, req.md5)

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
