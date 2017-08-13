package org.openchai.tensorflow

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{ExecParams, FileUtils, ProcessUtils, TcpCommon}
import org.openchai.tcp.xfer._
import org.openchai.util.{YamlConf, YamlStruct}

// The main thing we need to override here is using XferQConServerIf inside the server object
class TfServer(val yamlConf: YamlConf, val outQ: BlockingQueue[TaggedEntry], val tfTcpParams: TcpParams,
  val tcpParams: TcpParams, val xtcpParams: TcpParams) {

  val xferServer = new QXferConServer(outQ/*.asInstanceOf[BlockingQueue[TaggedEntry]]*/,
    tcpParams, xtcpParams)
  println(s"*** TfServer")
  val tfServer = new TcpServer(tfTcpParams.server, tfTcpParams.port, new TfServerIf(yamlConf, outQ, tfTcpParams.port))

  def start() = {
    xferServer.start
    tfServer.start
    Thread.sleep(100)
  }

}

object TfServer {

  val cfile = s"${System.getProperty("openchai.tfserver.config.file")}"
  println(s"Configfile=$cfile")

  import collection.JavaConverters

  val yamlConf = readConfig(cfile)
  val imagesDir = "/tmp/images"
  val f = new java.io.File(imagesDir)
  if (!f.exists() && !f.mkdirs) {
    throw new IllegalStateException(s"Unable to create image dirs ${f.getAbsolutePath}")
  }

  def apply(yamlConf: YamlConf, outQ: BlockingQueue[TaggedEntry], tfTcpParams: TcpParams, tcpParams: TcpParams,
    xtcpParams: TcpParams) = {
    val server = new TfServer(yamlConf, outQ, tfTcpParams, tcpParams, xtcpParams)
    server.start
  }

  def readConfig(path: String): YamlConf = {
//    parseJsonToMap(FileUtils.readFileAsString(path))
    YamlStruct(path)
  }

  def main(args: Array[String]): Unit = {

    val q = new ArrayBlockingQueue[TaggedEntry](1000)
    val (host, port, xhost, xport, ahost, aport, configFile) = if (args.length == 0) {
      val cont = XferConCommon.TestControllers
      (cont.conHost, cont.conPort, cont.dataHost, cont.dataPort, cont.appHost, cont.appPort, cont.configFile)
    } else {
      XferConServer.makeXferConnections(args)
    }
    val server = apply(yamlConf, q, TcpParams(ahost, aport), TcpParams(host, port), TcpParams(xhost, xport))
  }

}


class TfServerIf(val yamlConf: YamlConf, val q: BlockingQueue[TaggedEntry], port: Int = 0) extends ServerIf("TfServerIf") {

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

  val LabelImageTag = "tensorflow-labelimage"
  val DarknetYoloTag = "darknet-yolo"
  val DefaultApp = DarknetYoloTag

  case class LabelImgExecStruct(istruct: LabelImgStruct, cmdline: String, appName: String, runDir: String, tmpDir: String)

  def labelImg(estruct: LabelImgExecStruct): LabelImgRespStruct = {
    println(s"LabelImg: processing $estruct ..")
    val istruct = estruct.istruct
//    if (istruct.data.isEmpty) {
//      throw new IllegalStateException(s"Non empty md5 for empty data on $istruct")
//    } else {
//      FileUtils.checkMd5(istruct.fpath, istruct.data, istruct.md5)
//    }

    val e = QXferConServer.findInQ(q, istruct.tag)
//    println(s"LabelImg: Found entry ${e.getOrElse("[empty]")}")
    val tEntry = TcpCommon.deserializeObject(e.get.data).asInstanceOf[TaggedEntry]
    val data = tEntry.data
    val path = s"${TfServer.imagesDir}/${new java.util.Random().nextInt(200) + "." + istruct.fpath.substring(istruct.fpath.lastIndexOf("/") + 1)}"
    FileUtils.writeBytes(path, data)
    val exe = estruct.cmdline.substring(0, estruct.cmdline.indexOf(" "))
    val exeResult = ProcessUtils.exec(ExecParams(estruct.appName, s"${exe}",
      Option(estruct.cmdline.replace("${1}",path).split(" ").tail), Some(Seq(estruct.runDir)), estruct.runDir))
    println(s"Result: $exeResult")
    LabelImgRespStruct(istruct.tag, istruct.fpath,exeResult)
  }


  val os = System.getProperty("os.name") match {
    case "Mac OS X" => "osx"
    case x => x.toLowerCase
  }

  //  val isLinux = os == "linux"
  override def service(req: P2pReq[_]): P2pResp[_] = {
    import collection.JavaConverters._
    req match {
      case o: LabelImgReq =>
        val struct = o.value
        val app = struct.optApp.getOrElse(DefaultApp)
        val envmap = yamlConf.toMap("environments").apply(os).asInstanceOf[MapMap]("env").asInstanceOf[StringMap]
        val emap = yamlConf.toMap("defaults").apply("apps").asInstanceOf[AnyMap](app).asInstanceOf[StringMap].map { case (k, v) =>
          val vnew = envmap.foldLeft(v) { case (vv, (ke, ve)) => /* println(vv); */ vv.replace(s"$${$ke}", ve) }
          (k, vnew)
        }
        println(s"Service: Invoking LabelImg: struct=$struct")

        val estruct = LabelImgExecStruct(struct, emap("cmdline"), app, emap("rundir"), emap("tmpdir"))
        val resp = labelImg(estruct)
        LabelImgResp(resp)
      case _ =>
        throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName} on port ${port}")
    }
  }

}
