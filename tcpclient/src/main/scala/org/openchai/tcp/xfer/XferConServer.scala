package org.openchai.tcp.xfer

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{FileUtils, TcpCommon}



case class XferConServer(tcpParams: TcpParams, xtcpParams: TcpParams) {

  var server: TcpServer = _

  lazy val xferServerIf: XferServerIf = {
    if (System.currentTimeMillis > 0) {
      throw new IllegalStateException("Why in xferserverif should be overwritten")
    }
    new NioXferServerIf(xtcpParams)
  }

  lazy val  xferServerThread = new Thread() {
    override def run() = {
      XferServer.main(Array(xtcpParams.server, xtcpParams.port.toString))
    }
  }

  def start() = {
    if (System.currentTimeMillis > 0) {
      throw new IllegalStateException("Why are we inside XferConServer.start: should be overridden")

    }
    xferServerThread.start
    server = TcpServer(tcpParams.server, tcpParams.port,
      new XferConServerIf(tcpParams))
    server.start
    this
  }

  case class XferControllerArgs(conHost: String, conPort: Int, dataHost: String, dataPort: Int,
    configFile: String)


  def makeXferControllers(args: XferControllerArgs) = {
    val tcpParams = TcpParams(args.conHost, args.conPort)
    val xtcpParams = TcpParams(args.dataHost, args.dataPort)
    val server = XferConServer(tcpParams, xtcpParams /*, xferConf */)
    server
  }
}

object XferConServer {
  def makeXferConnections(args: Array[String]) = {
    val host = args(0)
    val port = args(1).toInt
    val configFile = args(2)
    val xhost = host
    val xport = port + 1
    val ahost = host
    val aport = port + 2
    (host, port, xhost, xport, ahost, aport, configFile)
  }

 def main(args: Array[String]): Unit = {
    val (host,port,xhost,xport,ahost, aport, configFile) = makeXferConnections(args)
    val server = XferConServer(TcpParams(host, port), TcpParams(xhost, xport))
   server.start
    Thread.currentThread.join
  }
}

class XferConServerIf(tcpParams: TcpParams) extends ServerIf("XferConServerIf") {

//  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()
//
//  private val nReqs = new AtomicInteger(0)
//
//  def claimPaths(paths: Seq[String], config: TcpXferConfig) = {
//    for (path <- paths) {
//      assert(!pathsMap.containsKey(path), s"Server says the path you requested is in use/busy: $path")
//      pathsMap.put(path, config)
//    }
//  }
//
//  def releasePaths(paths: Seq[String], config: TcpXferConfig) = {
//    for (path <- paths) {
//      if (!pathsMap.containsKey(path)) {
//        println(s"WARN: Trying to release Path $path that is not found in the PathsMap ")
//      } else {
//        pathsMap.remove(path, config)
//      }
//    }
//  }

  def consume(config: TcpXferConfig): Any = defaultConsume(config)

  def defaultConsume(config: TcpXferConfig): Any = {
    val payload = TcpCommon.unpack(config.finalPath, readFile(config.finalPath))
    println(s"DefaultConsume: received data of type ${payload.getClass.getSimpleName}")
    payload
  }

  def readFile(path: String) = FileUtils.readFileBytes(path)

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: PrepWriteReq => {
        val config = o.value
        println(s"Prepping the Datawrite config=$config")
//        claimPaths(Seq(config.tmpPath, config.finalPath), config)
        new PrepResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: CompleteWriteReq => {
        val config = o.value
        println(s"Completed Write for ${config} the Datawrite config=$config")
//        val res = consume(config)
//        releasePaths(Seq(config.tmpPath, config.finalPath), config)
        CompletedResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: PrepReadReq => {
        val config = o.value
        println(s"Prepping the Datawrite config=$config")
//        claimPaths(Seq(config.tmpPath, config.finalPath), config)
        new PrepResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: CompleteReadReq => {
        val config = o.value
        println(s"Completed Write for ${config} the Datawrite config=$config")
//        releasePaths(Seq(config.tmpPath, config.finalPath), config)
        CompletedResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case _ => throw new IllegalArgumentException(s"XferConServerIf: Unknown service type ${req.getClass.getName} " +
        s"on port ${tcpParams.port}")
    }
  }

}
