package org.openchai.tensorflow

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{ExecParams, FileUtils, ProcessUtils}
import org.openchai.tcp.xfer._

// The main thing we need to override here is using XferQConServerIf inside the server object
class TfServer(val outQ: BlockingQueue[TaggedEntry], val tfTcpParams: TcpParams,
  val tcpParams: TcpParams, val xtcpParams: TcpParams) {

  val xferServer = new QXferServer(outQ.asInstanceOf[BlockingQueue[TaggedEntry]],
    tcpParams, xtcpParams)
  val tfServer = new TcpServer(tfTcpParams.server, tfTcpParams.port, new TfServerIf /*[TaggedEntry]*/ (outQ))

  def start() = {
    xferServer.start
    tfServer.start
    Thread.sleep(100)
  }

}

object TfServer {

  val isTx1 = System.getProperty("os.name") == "Linux"
  val (tfExec,tfExecDir): (String,String) = if (isTx1) {
    // s"${System.getProperty("user.dir")}/bin/fake_label_image.sh"
    (s"${System.getProperty("openchai.tensorflow.cmdline")}",s"${System.getProperty("openchai.tensorflow.dir")}")
  } else {
    ("/shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image","/shared/tensorflow")
  }
  // val tfExecDir = "/shared/tensorflow"
  val imagesDir = "/tmp/images"
  val f = new java.io.File(imagesDir)
  if (!f.exists() && !f.mkdirs) {
    throw new IllegalStateException(s"Unable to create image dirs ${f.getAbsolutePath}")
  }
//  val tfExec = "/shared/label_image/label_image"

  def apply(outQ: BlockingQueue[TaggedEntry], tfTcpParams: TcpParams, tcpParams: TcpParams,
    xtcpParams: TcpParams) = {
    val server = new TfServer(outQ, tfTcpParams, tcpParams, xtcpParams)
    server.start
  }

  def main(args: Array[String]): Unit = {

    val q = new ArrayBlockingQueue[TaggedEntry](1000)
    val (host, port, xhost, xport, ahost, aport, configFile) = if (args.length == 0) {
      val cont = XferConCommon.TestControllers
      (cont.conHost, cont.conPort, cont.dataHost, cont.dataPort, cont.appHost, cont.appPort, cont.configFile)
    } else {
      XferConServer.makeXferConnections(args)
    }
    val server = apply(q, TcpParams(ahost, aport), TcpParams(host, port), TcpParams(xhost, xport))
  }

}


class TfServerIf /*[T]*/ (/*tcpParams: TcpParams, xferServerIf: XferServerIf, */ q: BlockingQueue[TaggedEntry]) extends ServerIf("TfServerIf") {
  // class TfServerIf extends ServerIf {

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

  //  def readFile(path: String) = FileUtils.readFileBytes(path)


  def labelImg(struct: LabelImgStruct): LabelImgRespStruct = {

    println(s"LabelImg: processing $struct ..")
    if (struct.data.isEmpty) {
      throw new IllegalStateException(s"Non empty md5 for empty data on $struct")
    } else {
      FileUtils.checkMd5(struct.fpath, struct.data, struct.md5)
    }

    val e = QXferServer.findInQ(q, struct.tag)
    println(s"LabelImg: Found entry ${e.getOrElse("[empty]")}")
    val dir = TfServer.tfExecDir
    val path = s"${TfServer.imagesDir}/${struct.fpath.substring(struct.fpath.lastIndexOf("/")+1)}"
    FileUtils.writeBytes(path, struct.data)
    val exeResult = ProcessUtils.exec(ExecParams("LabelImage", s"${TfServer.tfExec}", Option(s"""--image=$path""".split(" ")),None,dir))

    LabelImgRespStruct(exeResult)
  }

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: LabelImgReq =>
        val struct = o.value
        println(s"Service: Invoking LabelImg: struct=$struct")
        val resp = labelImg(struct)
        LabelImgResp(resp)
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }

}
