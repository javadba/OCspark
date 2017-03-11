package org.openchai.tcp.xfer

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._

case class CompleteReadReq(value: TcpXferConfig) extends P2pReq[TcpXferConfig]

case class PrepReadReq(value: TcpXferConfig) extends P2pReq[TcpXferConfig]

case class PrepWriteReq(value: TcpXferConfig) extends P2pReq[TcpXferConfig]
class CompleteWriteReq(val value: TcpXferConfig) extends P2pReq[TcpXferConfig]

case class PrepRespStruct(len: Int, elapsed: Int, path: String)

case class PrepResp(val value: PrepRespStruct) extends P2pResp[PrepRespStruct]

case class CompletedResp(value: PrepRespStruct) extends P2pResp[PrepRespStruct]

/** XferController Interface.  Handles the
* signalling to Prepare and Complete the data transfers
*/
case class XferConIf(tcpParams: TcpParams, config: XferConfig) extends ServiceIf("XferCon") {

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
    cresult
  }

  def read(params: TcpXferConfig, readParams: XferReadParams) = {
    println(s"Client: beginning Read Controller for $params")
    val presult = xferConIf.prepareRead(params)
    val result = xferIf.read(readParams)
    val cresult = xferConIf.completeRead(params)
    println(s"Client: got result $result")
    cresult
  }
}

object XferConClient {

  import XferConCommon._
  case class XferControllers(client: XferConClient, xferConf: TcpXferConfig, wparams: XferWriteParams, rparams: XferReadParams)


  def makeXferControllers(args: XferControllerArgs) = {
    val tcpParams = TcpParams(args.conHost, args.conPort)
    val xtcpParams = TcpParams(args.dataHost, args.dataPort)
    val xferConf = new TcpXferConfig(args.outboundDataPaths._1, args.outboundDataPaths._2)
    val client = XferConClient(tcpParams, xtcpParams, xferConf)
    val wparams = XferWriteParams(xferConf, args.data)
    val rparams = XferReadParams(xferConf, args.inboundDataPath)
    XferControllers(client, xferConf, wparams, rparams)
  }

  def makeXferConnections(args: Array[String]) = {
    val host = args(0)
    val port = args(1).toInt
    val configFile = args(2)
    val xhost = host
    val xport = port + 1
    (host,port,xhost,xport,configFile)
  }

  def main(args: Array[String]): Unit = {
    val data = LoremIpsum.getBytes("ISO-8859-1")
    val (host,port,xhost,xport,configFile) = makeXferConnections(args)
    val controllers = makeXferControllers(XferControllerArgs(host, port, xhost, xport, configFile,
      data, ("/tmp/xferout1", "/tmp/xferout2"), "/tmp/xferin"))
    val wres = controllers.client.write(controllers.xferConf, controllers.wparams)
    val buf = ByteBuffer.allocate(1e4.toInt)
    val rres = controllers.client.read(controllers.xferConf, controllers.rparams)
    println(rres)
  }

}





