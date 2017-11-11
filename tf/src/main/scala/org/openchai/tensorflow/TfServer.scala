package org.openchai.tensorflow

import java.net.ConnectException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.Logger._
import org.openchai.tcp.util._
import org.openchai.tcp.xfer._
import org.openchai.util.{AppConfig, TfConfig}

// The main thing we need to override here is using XferQConServerIf inside the server object
class TfServer(val appConfig: AppConfig, val outQ: BlockingQueue[TaggedEntry], val tfTcpParams: TcpParams,
  val tcpParams: TcpParams, val xtcpParams: TcpParams) {

  val ConnectWait = 3

  val xferServer = new QXferConServer(outQ/*.asInstanceOf[BlockingQueue[TaggedEntry]]*/,
    tcpParams, xtcpParams)
  info(s"*** TfServer")
  val tfServer = new TcpServer(tfTcpParams.server, tfTcpParams.port, new TfServerIf(appConfig, outQ, tfTcpParams.port))

  def start() = {
    var connected = false
    xferServer.start
    tfServer.start
    while (!connected) {
      try {
        GpuRegistry.registerGpuAlternate(appConfig("connections.gpuRegistryHost"), appConfig("connections.gpuRegistryPort").toInt, TfConfig.getHostName, tcpParams.port)
        connected = true
        error(s"Connected to Gpu Registry")
      } catch {
        case ce: ConnectException =>
          error(s"Unable to connect to GpuRegistry - will try again in $ConnectWait seconds ..")
          Thread.sleep(1000 * ConnectWait)
      }
    }
    Thread.sleep(100)
  }

}

object TfServer {


  val cfile = s"${System.getProperty("openchai.tfserver.config.file")}"
  info(s"Configfile=$cfile")

  val yamlConf = readConfig(cfile)
//  val imagesDir = s"${yamlConf("tmpdir")}/images"
//  info(s"Writing images to tmpdir=$imagesDir")
//  val f = new java.io.File(imagesDir)
//  if (!f.exists() && !f.mkdirs) {
//    throw new IllegalStateException(s"Unable to create image dirs ${f.getAbsolutePath}")
//  }

  def apply(yamlConf: AppConfig, outQ: BlockingQueue[TaggedEntry], tfTcpParams: TcpParams, tcpParams: TcpParams,
    xtcpParams: TcpParams) = {
    val server = new TfServer(yamlConf, outQ, tfTcpParams, tcpParams, xtcpParams)
    server.start
  }

  def os = System.getProperty("os.name") match {
    case "Mac OS X" => "osx"
    case x => x.toLowerCase
  }

  def readConfig(path: String): AppConfig = {
//    parseJsonToMap(FileUtils.readFileAsString(path))
    new AppConfig(path, os)
  }

  def main(args: Array[String]): Unit = {

    val iargs = if (args.nonEmpty && args(0) == getClass.getName) {
      args.slice(1, args.length)
    } else args

    val q = new ArrayBlockingQueue[TaggedEntry](1000)
    val (host, port, xhost, xport, ahost, aport) = if (args.length == 0) {
      val cont = XferConCommon.TestControllers
      (cont.conHost, cont.conPort, cont.dataHost, cont.dataPort, cont.appHost, cont.appPort)
    } else {
      XferConServer.makeXferConnections(iargs)
    }
    val server = apply(yamlConf, q, TcpParams(ahost, aport), TcpParams(host, port), TcpParams(xhost, xport))
  }

}


class TfServerIf(val appConfig: AppConfig, val q: BlockingQueue[TaggedEntry], port: Int = 0) extends ServerIf("TfServerIf") {

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

  val LabelImageTag = "tensorflow-labelimage"
  val DarknetYoloTag = "darknet-yolo"
  val DefaultApp = DarknetYoloTag

  case class LabelImgExecStruct(istruct: LabelImgStruct, cmdline: String, appName: String, runDir: String, tmpDir: String)

  def labelImg(estruct: LabelImgExecStruct): LabelImgRespStruct = {
    info(s"LabelImg: processing $estruct ..")
    val istruct = estruct.istruct
//    if (istruct.data.isEmpty) {
//      throw new IllegalStateException(s"Non empty md5 for empty data on $istruct")
//    } else {
//      FileUtils.checkMd5(istruct.fpath, istruct.data, istruct.md5)
//    }

    val e = QXferConServer.findInQ(q, istruct.tag)
//    info(s"LabelImg: Found entry ${e.getOrElse("[empty]")}")
    val tEntry = TcpCommon.deserializeObject(e.get.data).asInstanceOf[TaggedEntry]
    val data = tEntry.data
    val dir = s"${estruct.tmpDir}" // /${istruct.fpath.substring(istruct.fpath.lastIndexOf("/") + 1)}"
    FileUtils.mkdirs(dir)
    val path = "%s/%s".format(dir,istruct.fpath.substring(istruct.fpath.lastIndexOf("/") + 1))
    FileUtils.writeBytes(path, data)
    val exe = estruct.cmdline.substring(0, estruct.cmdline.indexOf(" "))
    val exeResult = ProcessUtils.exec(ExecParams(estruct.appName, s"${exe}",
      Option(estruct.cmdline.replace("${1}",path).replace("${2}",istruct.tag).split(" ").tail), Some(Seq(estruct.runDir)), estruct.runDir))
    info(s"Result: $exeResult")
    LabelImgRespStruct(istruct.tag, istruct.fpath, istruct.outPath, exeResult)
  }

  //  val isLinux = os == "linux"
  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: LabelImgReq =>
        val struct = o.value
        val app = struct.imgApp
        info(s"Service: Invoking LabelImg: struct=$struct")

        val estruct = LabelImgExecStruct(struct, appConfig(app, "cmdline"), app, appConfig(app, "rundir"), appConfig(app, "tmpdir"))
        val resp = labelImg(estruct)
        LabelImgResp(resp)
      case _ =>
        throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName} on port ${port}")
    }
  }

}
