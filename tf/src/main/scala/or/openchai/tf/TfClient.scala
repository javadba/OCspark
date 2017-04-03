package org.openchai.caffeonspark

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import or.openchai.tf.TaggedEntry
import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{ExecResult, FileUtils, ProcessResult, TcpCommon}
import org.openchai.tcp.util.ProcessUtils._
import org.openchai.tcp.xfer._
import org.openchai.tf.TfServer


object TfClient {

  def apply() = {

    import XferConCommon._
    val controllers =XferConClient.makeXferControllers(TestControllers)
    val client = new TfClient(AppTcpArgs, TfConfig("TestLabeler"), controllers.client)
    client
  }

  def testClient(): Unit = {
    val testImg = "/images/pilatus800.jpg"

    val is = this.getClass().getResourceAsStream(testImg)
    val buf = new Array[Byte](2 * 1024 *1024)

    val imgBytes = this.getClass().getResourceAsStream(testImg) //.getBytes("ISO-8859-1")
    is.read(buf)
    val tfClient = TfClient()
    val md5 = FileUtils.md5(buf)
    val label = tfClient.labelImg(LabelImgStruct("funnyPic",s"${System.getProperty("user.dir")}/tf/src/main/resources/$testImg",
      buf, md5))
    println(s"Received label result: $label")
  }
  def main(args: Array[String]): Unit = {
    TfServer.main(Array.empty[String])
    testClient
    println("We're done!")
  }
}

case class TfConfig(name: String, imgDir: String = "/tmp/images") // placeholder

case class LabelImgStruct(tag: String, fpath: String,
  data: Array[Byte] = Array.empty[Byte], md5: Array[Byte] = Array.empty[Byte]) {
  override def toString: DataPtr = s"LabelImg: tag=$tag path=$fpath " +
    s"datalen=${if (data!=null) data.length else -1} md5len=${if (md5!=null) md5.length else -1}"
}

case class LabelImgReq(value: LabelImgStruct) extends P2pReq[LabelImgStruct]

case class LabelImgRespStruct(cmdResult: ExecResult)

case class LabelImgResp(val value: LabelImgRespStruct) extends P2pResp[LabelImgRespStruct]

class TfClient(val tcpParams: TcpParams, val config: TfConfig, val xferClient: XferConClient)
  extends TcpClient(tcpParams, TfClientIf(tcpParams, config, xferClient)) {

  val tfIf = serviceIf.asInstanceOf[TfClientIf]

  def labelImg(struct: LabelImgStruct) = tfIf.labelImg(struct)
}

  import XferConClient._
case class TfClientIf(tcpParams: TcpParams, config: TfConfig, tfClient: XferConClient) extends ServiceIf("TfClient") {

  val controllers = XferConClient.makeXferControllers(XferConCommon.TestControllers)
  def labelImg(s: LabelImgStruct): LabelImgResp = {
    println(s"LabelImg..")

    val fdata = FileUtils.readFileBytes(s.fpath)
    val wparams = XferWriteParams(controllers.xferConf,
      TcpCommon.serialize(TaggedEntry("funnyPic", fdata)))
    val wres = controllers.client.write(controllers.xferConf, wparams)
    val resp = getRpc().request(LabelImgReq(s.copy(data = fdata)))
    println(s"LabelImg response: $resp")
    resp.asInstanceOf[LabelImgResp]
  }

}

