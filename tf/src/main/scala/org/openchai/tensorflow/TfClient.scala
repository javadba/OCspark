package org.openchai.tensorflow

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.Logger._
import org.openchai.tcp.util.{ExecResult, FileUtils, TcpCommon}
import org.openchai.tcp.xfer.XferConCommon._
import org.openchai.tcp.xfer._
import org.openchai.tensorflow.DmaXferConClient.DmaXferControllers
import org.openchai.tensorflow.api.Logger
import org.openchai.util.{TfAppConfig, TfConfig}


object TfClient extends Logger {

  def apply(conf: TfAppConfig, controllers: DmaXferControllers, tcpParams: TcpParams) = {

    val client = new TfClient(tcpParams, TfSimpleConfig(conf.outTag, conf.inDir, conf.outDir), controllers.client)
    client
  }


  def apply(conf: TfAppConfig): TfClient = {
    apply(conf,DmaXferConClient.makeDmaXferControllers(TestControllers), AppTcpArgs)
  }

  def apply(conf: TfAppConfig, server: String, port: Int = 0): TfClient = {
    val base = if (port > 0) port else 61234
    val controllers = DmaXferConClient.makeDmaXferControllers(remoteControllers(server, base))
    apply(conf, controllers, remoteTcpArgs(server,base+2))
  }

  def defaultConf() = TfConfig.getAppConfig("submitter.yml")

  def testClient(): Unit = {
//    val testImg = "/images/pilatus800.jpg"
    val testImg = "/images/JohnNolteAndDad.jpg"

    val is = this.getClass().getResourceAsStream(testImg)
    val buf = new Array[Byte](16 * 1024 *1024)

//    val imgBytes = this.getClass().getResourceAsStream(testImg) //.getBytes("ISO-8859-1")
    if (is == null) {
      throw new IllegalArgumentException(s"Unable to access $testImg")
    }
    val n = is.read(buf)
    val rbuf = buf.slice(0,n)
    val tfClient = TfClient(defaultConf())
    val md5 = FileUtils.md5(rbuf.slice(0,n))
    val label = tfClient.labelImg(LabelImgStruct("funnyPic",TensorflowApp.name, s"${System.getProperty("user.dir")}/tf/src/main/resources/$testImg",
      "/tmp", rbuf, md5))
    info(s"Received label result: $label")
  }

  def main(args: Array[String]): Unit = {
    val resp = testClient
    info(resp)
    info("We're done!")
  }

}

case class TfSimpleConfig(name: String, imgDir: String, outDir: String) // placeholder

case class LabelImgStruct(tag: String, imgApp: String, fpath: String, outPath: String,
  data: Array[Byte] = Array.empty[Byte], md5: Array[Byte] = Array.empty[Byte]) {

  override def toString: DataPtr = s"LabelImg: tag=$tag path=$fpath " +
    s"datalen=${if (data!=null) data.length else -1} md5len=${if (md5!=null) md5.length else -1}"
}

case class LabelImgReq(value: LabelImgStruct) extends P2pReq[LabelImgStruct]

case class LabelImgRespStruct(tag: String, fpath: String, outDir: String, cmdResult: ExecResult, nImagesProcessed: Int=1)

case class LabelImgResp(val value: LabelImgRespStruct) extends P2pResp[LabelImgRespStruct]

class TfClient(val tcpParams: TcpParams, val config: TfSimpleConfig, val xferClient: DmaXferConClient)
  extends TcpClient(tcpParams, TfClientIf(tcpParams, config, xferClient)) {
  val tfIf = serviceIf.asInstanceOf[TfClientIf]

  def labelImg(struct: LabelImgStruct) = tfIf.labelImg(struct)
}
case class TfClientIf(tcpParams: TcpParams, config: TfSimpleConfig, tfClient: DmaXferConClient) extends ServiceIf("TfClient") {

//  val controllers = XferConClient.makeXferControllers(XferConCommon.TestControllers)
  def labelImg(s: LabelImgStruct): LabelImgResp = {
    FileUtils.mkdirs(s.outPath)
    info(s"LabelImg: $s")

//    val fdata = FileUtils.readFileBytes(s.fpath)
    val wparams = XferWriteParams(s.tag, tfClient.config,
    TcpCommon.serializeObject(s.fpath, TaggedEntry("taggedPic", s.data)))
    val xferConf = TcpXferConfig(s.tag, s.fpath)
    tfClient.prepareWrite(xferConf)
//    tfClient.write(XferWriteParams(s.tag, xferConf, s.data))
//    val wres = tfClient.write(tfClient.config, wparams)
    val wres = tfClient.write(wparams)
    tfClient.completeWrite(xferConf)
//    val resp = getRpc().request(LabelImgReq(s.copy(data = s.data)))
    val newLiReq = LabelImgReq(LabelImgStruct(s.tag, s.imgApp, s.fpath, s.outPath))
//    LabelImgReq(s.copy(data = Array.empty[Byte], md5 = Array.empty[Byte]))
    val resp = getRpc().request(newLiReq)
    info(s"LabelImg response: $resp")
    resp.asInstanceOf[LabelImgResp]
  }

}

