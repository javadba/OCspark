package org.openchai.tcp.xfer

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._

case class CompleteReadReq(value: TcpXferConfig) extends P2pReq[TcpXferConfig]

case class PrepReadReq(value: TcpXferConfig) extends P2pReq[TcpXferConfig]

case class PrepWriteReq(value: TcpXferConfig) extends P2pReq[TcpXferConfig]
class CompleteWriteReq(val value: TcpXferConfig) extends P2pReq[TcpXferConfig]

case class PrepRespStruct(len: Int, elapsed: Int, path: String)

class PrepResp(val value: PrepRespStruct) extends P2pResp[PrepRespStruct]

case class CompletedResp(value: PrepRespStruct) extends P2pResp[PrepRespStruct]

case class XferConIf(tcpParams: TcpParams, config: XferConfig) extends ServiceIF {

  private val nReqs = new AtomicInteger(0)

  def prepareWrite(config: TcpXferConfig): PrepResp = {
    val resp = getRpc().request(PrepWriteReq(config))
    resp.asInstanceOf[PrepResp]
  }

  def completeWrite(config: TcpXferConfig): CompletedResp = {
    val resp = getRpc().request(new CompleteWriteReq(config))
    resp.asInstanceOf[CompletedResp]
  }

  def prepareRead(params: TcpXferConfig): PrepResp = {
    val resp = getRpc().request(PrepReadReq(params))
    resp.asInstanceOf[PrepResp]
  }

  def completeRead(config: TcpXferConfig): CompletedResp = {
    val resp = getRpc().request(CompleteReadReq(config))
    resp.asInstanceOf[CompletedResp]
  }
}

case class XferConClient(tcpParams: TcpParams, config: XferConfig)
  extends TcpClient(tcpParams, new XferConIf(tcpParams, config)) {

  val xferConIf = serviceIf.asInstanceOf[XferConIf]
  val xferIf = new XferIf

  def write(params: TcpXferConfig, writeParams: XferWriteParams) = {
    val presult = xferConIf.prepareWrite(params)
    val result = xferIf.write(writeParams)
    val cresult = xferConIf.completeWrite(params)
    println(s"Client: got result $result")
  }

  def read(params: TcpXferConfig, readParams: XferReadParams) = {
    val presult = xferConIf.prepareRead(params)
    val result = xferIf.read(readParams)
    val cresult = xferConIf.completeRead(params)
    println(s"Client: got result $result")
  }
}

object XferConClient {
  def main(args: Array[String]): Unit = {
    val host = args(0)
    val port = args(1).toInt
    val configFile = args(2)
    val tcpParams = TcpParams(host, port)
    val xferConf = new TcpXferConfig("/tmp/x","/tmp/y")
    val client = XferConClient(tcpParams, xferConf)
    val data = LoremIpsum
    val xparams = XferWriteParams(xferConf, LoremIpsum.getBytes("ISO-8859-1"))
    val wres = client.write(xferConf, xparams)
    val buf = ByteBuffer.allocate(1e4.toInt)
    val rparams = XferReadParams(xferConf, "/tmp/readBack" )
    val rres = client.read(xferConf, rparams)
    println(rres)
  }

  val LoremIpsum =
    """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur et nibh sagittis, aliquam metus nec, faucibus neque.
      Proin ullamcorper facilisis turpis at sollicitudin. Mauris facilisis, turpis ut facilisis rhoncus, mi erat ultricies ligula,
      id pretium lacus ante id est. Morbi at lorem congue, consectetur nunc vel, facilisis urna. Fusce mollis pulvinar sagittis.
      Aenean eu imperdiet augue. Morbi sit amet enim tristique nisl tristique efficitur vitae eget diam. Vestibulum non metus eros.

Nulla in nunc interdum, pulvinar dolor et, euismod leo. Mauris at enim nec felis hendrerit porttitor nec vel lorem. Duis a euismod
augue. Maecenas scelerisque, ipsum placerat suscipit ultricies, odio nulla laoreet ex, a varius lacus sem quis sapien.
Aliquam condimentum tellus id tempus posuere. Nullam volutpat, tellus in euismod hendrerit, dui lacus fermentum quam,
a pretium nisi eros a nisl. Duis vehicula eros sit amet nunc fermentum, vel faucibus erat ornare. Suspendisse sed
ligula scelerisque, lobortis est sit amet, dignissim leo. Ut laoreet, augue non efficitur egestas, justo lorem faucibus
    """.stripMargin
}


class XferConServerIf(tcpParams: TcpParams) extends ServerIF {

  val xferServerIf = new XferServerIf(tcpParams)

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

  def claimPaths(paths: Seq[String], config: TcpXferConfig) = {
    for (path <- paths) {
      assert(pathsMap.containsKey(path), s"Server says the path you requested is in use/busy: $path")
      pathsMap.put(path, config)
    }
  }

  def releasePaths(paths: Seq[String], config: TcpXferConfig) = {
    for (path <- paths) {
      if (!pathsMap.containsKey(path)) {
        println(s"WARN: Trying to release Path $path that is not found in the PathsMap ")
      } else {
        pathsMap.remove(path, config)
      }
    }
  }

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: PrepWriteReq => {
        val config = o.value
        println(s"Prepping the Datawrite config=$config")
        claimPaths(Seq(config.tmpPath, config.finalPath), config)
        new PrepResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: CompleteWriteReq => {
        val config = o.value
        println(s"Completed Write for ${config} the Datawrite config=$config")
        releasePaths(Seq(config.tmpPath, config.finalPath), config)
        CompletedResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: PrepReadReq => {
        val config = o.value
        println(s"Prepping the Datawrite config=$config")
        claimPaths(Seq(config.tmpPath, config.finalPath), config)
        new PrepResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: CompleteReadReq => {
        val config = o.value
        println(s"Completed Write for ${config} the Datawrite config=$config")
        releasePaths(Seq(config.tmpPath, config.finalPath), config)
        CompletedResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }

}




