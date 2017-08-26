package org.openchai.tensorflow

import org.openchai.tcp.rpc.TcpParams
import org.openchai.tcp.xfer._
import org.openchai.tensorflow.api.PcieDMAClient

// placeholder: Burcak will inform what more needed
case class DmaConfig(name: String)

class DmaXferConClient(val dmaConfig: DmaConfig, val tcpParams: TcpParams, val xferTcpParams: TcpParams, val config: XferConfig) extends XferConIf with XferIfClient {
  val controller = XferConClient(tcpParams, xferTcpParams, config)
  val tcpXferIf = controller.tcpXferIf
  val dmaXferIfTcpParams = TcpParams(tcpParams.server, tcpParams.port)
  val dmaXferIf = new DmaXferIfClient(new DmaParams(dmaConfig.name), dmaConfig, dmaXferIfTcpParams, config)
  controller.xferIf = tcpXferIf  // TODO: decide how to do switching tcp/dma
//  override var xferIf: XferIfClient = controller.xferIf

  override def read(params: XferReadParams): XferReadResp =
    controller.xferIf.read(params)

  override def write(params: XferWriteParams): XferWriteResp =
    controller.xferIf.write(params)

  override def prepareWrite(config: XferConfig): PrepResp = controller.xferConIf.prepareWrite(config)

  override def completeWrite(config: XferConfig): CompletedResp = controller.xferConIf.completeWrite(config)

  override def prepareRead(params: XferConfig): PrepResp = controller.xferConIf.prepareRead(params)

  override def completeRead(config: XferConfig): CompletedResp = controller.xferConIf.completeRead(config)

}

object DmaXferConClient {

  import XferConCommon._
  case class DmaXferControllers(client: DmaXferConClient, xferConf: XferConfig,
    wparams: XferWriteParams, rparams: XferReadParams)

  def makeDmaXferControllers(args: XferControllerArgs) = {
    val tcpParams = TcpParams(args.conHost, args.conPort)
    val xtcpParams = TcpParams(args.dataHost, args.dataPort)
    val xferConf = TcpXferConfig(args.outboundDataPaths._1, args.outboundDataPaths._2)
    val client = new DmaXferConClient(DmaConfig("foo"), tcpParams, xtcpParams, xferConf)
    val wparams = XferWriteParams("WriteParams", xferConf, args.data)
    val rparams = XferReadParams("ReadParams", xferConf, args.inboundDataPath)
    DmaXferControllers(client, xferConf, wparams, rparams)
  }

}
case class DmaParams(name: String) // placeholder

class DmaXferIfClient(dmaParams: DmaParams, dmaConfig: DmaConfig, tcpParams: TcpParams, config: XferConfig) extends XferIfClient {

  val tcpXferClient = new TcpXferIfClient(tcpParams, config)

  val SetupChannelJson = """{ "SetupChannelKey": "SetupChannelKey Value"}"""
  val PrepareWriteJson = """{ "PrepareWriteKey": "PrepareWrite Value"}"""
  val WriteJson = """{ "WriteKey": "Write Value"}"""
  val CompleteWriteJson = """{ "CompleteWriteKey": "CompleteWrite Value"}"""
  val PrepareReadJson = """{ "PrepareReadKey": "PrepareRead Value"}"""
  val ReadJson = """{ "ReadKey": "Read Value"}"""
  val CompleteReadJson = """{ "CompleteReadKey": "CompleteRead Value"}"""

  val client = new PcieDMAClient

  override def write(params: XferWriteParams) = {
    val start = System.currentTimeMillis
    client.prepareWrite(PrepareWriteJson)
    val writeResult = client.write(WriteJson, params.data, params.md5)
    val res = client.completeWrite(CompleteWriteJson)
    val elapsed = System.currentTimeMillis - start
    XferWriteResp("abc", writeResult.dataStruct.dataLen, elapsed, writeResult.dataStruct.md5)
  }

  override def read(params: XferReadParams) = {
    val start = System.currentTimeMillis
    client.prepareWrite(PrepareReadJson)
    val readResult = client.read(ReadJson)
    val res = client.completeRead(CompleteReadJson)
    val elapsed = System.currentTimeMillis - start
    XferReadResp("abc", readResult.dataStruct.dataLen, elapsed, readResult.dataStruct.data,
      readResult.dataStruct.md5)
  }

}

