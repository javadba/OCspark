package org.openchai.tensorflow

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{ExecResult, FileUtils, ProcessResult, TcpCommon}
import org.openchai.tcp.util.ProcessUtils._
import org.openchai.tcp.xfer._
import org.openchai.tensorflow.TfServer


object TfClient {

  def apply() = {

    import XferConCommon._
    val controllers =XferConClient.makeXferControllers(TestControllers)
    val client = new TfClient(AppTcpArgs, TfConfig("TestLabeler"), controllers.client)
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

//    val fdata = FileUtils.readFileBytes(s.fpath)
    val wparams = XferWriteParams(controllers.xferConf,
      TcpCommon.serializeObject(TaggedEntry("funnyPic", s.data)))
    val wres = controllers.client.write(controllers.xferConf, wparams)
    val resp = getRpc().request(LabelImgReq(s.copy(data = s.data)))
    println(s"LabelImg response: $resp")
    resp.asInstanceOf[LabelImgResp]
  }

}

