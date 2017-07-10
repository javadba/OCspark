package org.openchai.tensorflow

import com.blazedb.spark.reports.YamlStruct
import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{ExecResult, FileUtils, TcpCommon}
import org.openchai.tcp.xfer._


object TfClient {

  def apply() = {

    import XferConCommon._
    val controllers =DmaXferConClient.makeDmaXferControllers(TestControllers)
    val client = new TfClient(AppTcpArgs, TfConfig("TestLabeler"), controllers.client)
    client
  }

  import XferConCommon._
  def apply(server: String) = {
    val controllers = DmaXferConClient.makeDmaXferControllers(remoteControllers(server))
    val client = new TfClient(remoteTcpArgs(server), TfConfig("TestLabeler"), controllers.client)
    client
  }

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
    val tfClient = TfClient()
    val md5 = FileUtils.md5(rbuf.slice(0,n))
    val label = tfClient.labelImg(LabelImgStruct("funnyPic",s"${System.getProperty("user.dir")}/tf/src/main/resources/$testImg",
      rbuf, md5))
    println(s"Received label result: $label")
  }

  def main(args: Array[String]): Unit = {
    testClient
    println("We're done!")
  }
  
}

case class TfConfig(name: String, imgDir: String = "/tmp/images") // placeholder

case class LabelImgStruct(tag: String, fpath: String,
  data: Array[Byte] = Array.empty[Byte], md5: Array[Byte] = Array.empty[Byte], optApp: Option[String] = None) {
  override def toString: DataPtr = s"LabelImg: tag=$tag path=$fpath " +
    s"datalen=${if (data!=null) data.length else -1} md5len=${if (md5!=null) md5.length else -1}"
}

case class LabelImgReq(value: LabelImgStruct) extends P2pReq[LabelImgStruct]

case class LabelImgRespStruct(cmdResult: ExecResult)

case class LabelImgResp(val value: LabelImgRespStruct) extends P2pResp[LabelImgRespStruct]

class TfClient(val tcpParams: TcpParams, val config: TfConfig, val xferClient: DmaXferConClient)
  extends TcpClient(tcpParams, TfClientIf(tcpParams, config, xferClient)) {
  val tfIf = serviceIf.asInstanceOf[TfClientIf]

  def labelImg(struct: LabelImgStruct) = tfIf.labelImg(struct)
}
case class TfClientIf(tcpParams: TcpParams, config: TfConfig, tfClient: DmaXferConClient) extends ServiceIf("TfClient") {

//  val controllers = XferConClient.makeXferControllers(XferConCommon.TestControllers)
  def labelImg(s: LabelImgStruct): LabelImgResp = {
    println(s"LabelImg: $s")

//    val fdata = FileUtils.readFileBytes(s.fpath)
    val wparams = XferWriteParams(s.tag, tfClient.config,
      TcpCommon.serializeObject(s.fpath, TaggedEntry("funnyPic", s.data)))
    val xferConf = TcpXferConfig("blah", s.fpath)
    tfClient.prepareWrite(xferConf)
    tfClient.write(XferWriteParams(s.tag, xferConf, s.data))
    tfClient.completeWrite(xferConf)
//    val wres = tfClient.write(tfClient.config, wparams)
    val wres = tfClient.write(wparams)
//    val resp = getRpc().request(LabelImgReq(s.copy(data = s.data)))
    val resp = getRpc().request(LabelImgReq(s.copy(data = Array.empty[Byte], md5 = Array.empty[Byte])))
//    println(s"LabelImg response: $resp")
    resp.asInstanceOf[LabelImgResp]
  }

}

