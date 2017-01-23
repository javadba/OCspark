package org.openchai.tcp.rexec

import org.openchai.tcp.rpc.{TcpParams, TcpServer}
import org.openchai.tcp.util.ProcessUtils.ExecParams

object RexecTest {
  def main(args: Array[String]) = {
    val server = args(0)
    val tcpParams = TcpParams(server, TcpServer.DefaultPort)
    val rexecServer = RexecServer(tcpParams)
    rexecServer.start
    val rexecClient = RexecTcpClient(tcpParams)
      val res = rexecClient.run(RexecParams(ExecParams("ls",Some("-lrta .".split(" ")),None,"/etc/pam.d")), 5)
    println(s"Result: $res")
  }
}
