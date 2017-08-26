package org.openchai.tcp.xfer

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import org.openchai.tcp.util.Logger._

import org.openchai.tcp.rpc._

case class CompleteReadReq(value: XferConfig) extends P2pReq[XferConfig]

case class PrepReadReq(value: XferConfig) extends P2pReq[XferConfig]

case class PrepWriteReq(value: XferConfig) extends P2pReq[XferConfig]
class CompleteWriteReq(val value: XferConfig) extends P2pReq[XferConfig]

case class PrepRespStruct(len: Int, elapsed: Int, path: String)

case class PrepResp(val value: PrepRespStruct) extends P2pResp[PrepRespStruct]

case class CompletedResp(value: PrepRespStruct) extends P2pResp[PrepRespStruct]


trait XferConIf {
  def prepareWrite(config: XferConfig): PrepResp
  def completeWrite(config: XferConfig): CompletedResp
  def prepareRead(params: XferConfig): PrepResp
  def completeRead(config: XferConfig): CompletedResp
}

/** XferController Interface.  Handles the
* signalling to Prepare and Complete the data transfers
*/
class XferConIfBase(tcpParams: TcpParams, config: XferConfig) extends ServiceIf("XferCon")  with XferConIf {

  private val nReqs = new AtomicInteger(0)

  override def prepareWrite(config: XferConfig): PrepResp = {
//    info(s"PrepareWrite ..")
    val resp = getRpc().request(PrepWriteReq(config))
//    info(s"PrepareWrite response: $resp")
    resp.asInstanceOf[PrepResp]
  }

  override def completeWrite(config: XferConfig): CompletedResp = {
    val resp = getRpc().request(new CompleteWriteReq(config))
//    info(s"CompleteWrite response: $resp")
    resp.asInstanceOf[CompletedResp]
  }

  override def prepareRead(params: XferConfig): PrepResp = {
    val resp = getRpc().request(PrepReadReq(params))
    resp.asInstanceOf[PrepResp]
  }

  override def completeRead(config: XferConfig): CompletedResp = {
    val resp = getRpc().request(CompleteReadReq(config))
    resp.asInstanceOf[CompletedResp]
  }
}


/** XferController Client Interface.  Invoke this one first which handles the
* signalling to Prepare , Invoke (via Xfer interface), and Complete the data transfers
*/
case class XferConClient(tcpParams: TcpParams, xferTcpParams: TcpParams,config: XferConfig)
  extends TcpClient(tcpParams, new XferConIfBase(tcpParams, config)) {

  val xferConIf = serviceIf.asInstanceOf[XferConIfBase]
  val tcpXferIf = new TcpXferIfClient(xferTcpParams, config)
  var xferIf: XferIfClient = tcpXferIf

  def write(params: XferConfig, writeParams: XferWriteParams) = {
    info(s"Client: beginning Write Controller for $params")
    val presult = xferConIf.prepareWrite(params)
    val result = xferIf.write(writeParams)
    val cresult = xferConIf.completeWrite(params)
    info(s"Client: got result $result")
    cresult
  }

  def read(params: XferConfig, readParams: XferReadParams) = {
    info(s"Client: beginning Read Controller for $params")
    val presult = xferConIf.prepareRead(params)
    val result = xferIf.read(readParams)
    val cresult = xferConIf.completeRead(params)
    info(s"Client: got result $result")
    cresult
  }
}

object XferConClient {

  import XferConCommon._
  case class XferControllers(client: XferConClient, xferConf: XferConfig, wparams: XferWriteParams, rparams: XferReadParams)


  def makeXferControllers(args: XferControllerArgs) = {
    val tcpParams = TcpParams(args.conHost, args.conPort)
    val xtcpParams = TcpParams(args.dataHost, args.dataPort)
    val xferConf = new TcpXferConfig(args.outboundDataPaths._1, args.outboundDataPaths._2)
    val client = XferConClient(tcpParams, xtcpParams, xferConf)
    val wparams = XferWriteParams("WriteParams", xferConf, args.data)
    val rparams = XferReadParams("ReadParams", xferConf, args.inboundDataPath)
    XferControllers(client, xferConf, wparams, rparams)
  }

  def makeXferConnections(args: Array[String]) = {
    val host = args(0)
    val port = args(1).toInt
    val configFile = args(2)
    val xhost = host
    val xport = port + 1
    val ahost = host
    val aport = port + 2
    (host,port,xhost,xport,ahost, aport, configFile)
  }

  def main(args: Array[String]): Unit = {
    val data = LoremIpsum.getBytes("ISO-8859-1")
    val (host,port,xhost,xport,ahost, aport, configFile) = makeXferConnections(args)
    val controllers = makeXferControllers(XferControllerArgs(host, port, xhost, xport, ahost, aport, configFile,
      data, ("/tmp/xferout1", "/tmp/xferout2"), "/tmp/xferin"))
    val wres = controllers.client.write(controllers.xferConf, controllers.wparams)
    val buf = ByteBuffer.allocate(1e4.toInt)
    val rres = controllers.client.read(controllers.xferConf, controllers.rparams)
    info(rres.toString)
  }

}





