package org.openchai.tcp.rexec

import org.openchai.tcp.rpc.{TcpParams, TcpServer}
import org.openchai.tcp.util.ProcessUtils.ExecParams

object RexecTest {
  def main(args: Array[String]) = {
//    val master = args(0)
//    val server = if (master.startsWith("spark")) {
//      master.substring(master.lastIndexOf("/") + 1, master.lastIndexOf(":"))
//    } else if (master.startsWith("local")) {
//      TcpUtils.getLocalHostname
//    } else {
//      throw new IllegalArgumentException(s"Unable to parse the server from the master $master")
//    }
//
//    val sc = new SparkContext(master, "RexecTest")
    val server = args(0)
    val tcpParams = TcpParams(server, TcpServer.DefaultPort)
    val rexecServer = RexecServer(tcpParams)
    rexecServer.start
    val rexecClient = RexecClient(tcpParams)
      val res = rexecClient.run(RexecParams(ExecParams("ls",Some("-lrta .".split(" ")),None,"/etc/pam.d")), 5)
    println(s"Result: $res")
  }
}
