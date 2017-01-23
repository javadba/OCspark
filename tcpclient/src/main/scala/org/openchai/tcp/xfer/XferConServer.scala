package org.openchai.tcp.xfer

import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._

object XferConServer {

  var server: TcpServer = _
  var xferServerThread: Thread = _
  def apply(tcpParams: TcpParams, xtcpParams: TcpParams) = {
    val xferServerIf = new XferServerIf(xtcpParams)
    xferServerThread = new Thread() {
      override def run() = {
        XferServer.main(Array(xtcpParams.server, xtcpParams.port.toString))
      }
    }
    xferServerThread.start

    server = TcpServer(tcpParams.server, tcpParams.port,
      new XferConServerIf(tcpParams, xferServerIf))

    server
  }

  def main(args: Array[String]): Unit = {
    val host = args(0)
    val port = args(1).toInt
    val xhost = host
    val xport = port + 1
    val server = apply(TcpParams(host, port), TcpParams(xhost, xport))
    server.start
    Thread.currentThread.join
  }
}

class XferConServerIf(tcpParams: TcpParams, xferServerIf: XferServerIf) extends ServerIF {

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

  def claimPaths(paths: Seq[String], config: TcpXferConfig) = {
    for (path <- paths) {
      assert(!pathsMap.containsKey(path), s"Server says the path you requested is in use/busy: $path")
      pathsMap.put(path, config)
    }
  }

  def releasePaths(paths: Seq[String], config: TcpXferConfig) = {
    for (path <- paths) {
      if (!pathsMap.containsKey(path)) {
        println(s"WARN: Trying to release Path $path that is not found in the PathsMap ")
      } else {
        pathsMap.remove(path, config)
      }
    }
  }

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: PrepWriteReq => {
        val config = o.value
        println(s"Prepping the Datawrite config=$config")
        claimPaths(Seq(config.tmpPath, config.finalPath), config)
        new PrepResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: CompleteWriteReq => {
        val config = o.value
        println(s"Completed Write for ${config} the Datawrite config=$config")
        releasePaths(Seq(config.tmpPath, config.finalPath), config)
        CompletedResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: PrepReadReq => {
        val config = o.value
        println(s"Prepping the Datawrite config=$config")
        claimPaths(Seq(config.tmpPath, config.finalPath), config)
        new PrepResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case o: CompleteReadReq => {
        val config = o.value
        println(s"Completed Write for ${config} the Datawrite config=$config")
        releasePaths(Seq(config.tmpPath, config.finalPath), config)
        CompletedResp(PrepRespStruct(0,0,config.tmpPath))
      }
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }

}
