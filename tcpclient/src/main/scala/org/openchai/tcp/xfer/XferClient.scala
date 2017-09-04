package org.openchai.tcp.xfer

import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc.{ServiceIf, TcpClient, TcpParams}
import org.openchai.tcp.util.Logger._

trait XferIfClient {
//  var xferIf: XferIf

  def read(params: XferReadParams): XferReadResp

  def write(params: XferWriteParams): XferWriteResp

}

class XferIf(config: XferConfig) extends ServiceIf("Xfer") with XferIfClient {

  private val nReqs = new AtomicInteger(0)

  def write(xferParams: XferWriteParams) = {
    // while (keepGoing(n).value) {
//    val md5 = FileUtils.md5(xferParams.data)
    info(s"XferIf: Sending request: $xferParams")
    val resp = getRpc().request(XferWriteReq(
//      XferWriteParams(TcpXferConfig("", Paths.get(xferParams.config.finalPath).toString), xferParams.data))).asInstanceOf[XferWriteResp]
      xferParams)).asInstanceOf[XferWriteResp]
    info(s"XferIf: Result is $resp")
    resp
  }

  def read(xferParams: XferReadParams) = {
    // while (keepGoing(n).value) {
    debug(s"Sending request: $xferParams ..")
    val resp = getRpc().request(XferReadReq(xferParams.dataPtr)).asInstanceOf[XferReadResp]
    debug(s"Result is $resp")
    resp
  }

}



class TcpXferIfClient(tcpParams: TcpParams, config: XferConfig)
  extends TcpClient(tcpParams, new XferIf(config)) with XferIfClient {

  val xferIf = serviceIf.asInstanceOf[XferIf]

  def read(params: XferReadParams) = xferIf.read(params)

  def write(params: XferWriteParams) = xferIf.write(params)


}


case class XferParams(configFile: String, data: Array[Byte]) {

  override def toString: String = s"XferParams using $configFile and with datasize=${data.length}"

}

