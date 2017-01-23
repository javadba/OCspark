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

/** XferController Interface.  Handles the
* signalling to Prepare and Complete the data transfers
*/
case class XferConIf(tcpParams: TcpParams, config: XferConfig) extends ServiceIF("XferCon") {

  private val nReqs = new AtomicInteger(0)

  def prepareWrite(config: TcpXferConfig): PrepResp = {
    println(s"PrepareWrite ..")
    val resp = getRpc().request(PrepWriteReq(config))
    println(s"PrepareWrite response: $resp")
    resp.asInstanceOf[PrepResp]
  }

  def completeWrite(config: TcpXferConfig): CompletedResp = {
    val resp = getRpc().request(new CompleteWriteReq(config))
    println(s"CompleteWrite response: $resp")
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

/** XferController Client Interface.  Invoke this one first which handles the
* signalling to Prepare , Invoke (via Xfer interface), and Complete the data transfers
*/
case class XferConClient(tcpParams: TcpParams, xferTcpParams: TcpParams,config: XferConfig)
  extends TcpClient(tcpParams, XferConIf(tcpParams, config)) {

  val xferConIf = serviceIf.asInstanceOf[XferConIf]
  val xferIf = new XferIfClient(xferTcpParams, config)

  def write(params: TcpXferConfig, writeParams: XferWriteParams) = {
    println(s"Client: beginning Write Controller for $params")
    val presult = xferConIf.prepareWrite(params)
    val result = xferIf.write(writeParams)
    val cresult = xferConIf.completeWrite(params)
    println(s"Client: got result $result")
  }

  def read(params: TcpXferConfig, readParams: XferReadParams) = {
    println(s"Client: beginning Read Controller for $params")
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
    val xhost = host
    val xport = port + 1
    val xtcpParams = TcpParams(xhost, xport)
    val xferConf = new TcpXferConfig("/tmp/x","/tmp/y")
    val client = XferConClient(tcpParams, xtcpParams, xferConf)
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





