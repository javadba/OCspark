package org.openchai.spark.rdma

import org.apache.spark.SparkContext
import org.openchai.spark.p2p.{TcpParams, TcpServer}
import org.openchai.spark.util.ProcessUtils.ExecParams
import org.openchai.spark.util.TcpUtils

object RexecTest {
  def main(args: Array[String]) = {
    val master = args(0)
    val server = if (master.startsWith("spark")) {
      master.substring(master.lastIndexOf("/") + 1, master.lastIndexOf(":"))
    } else if (master.startsWith("local")) {
      TcpUtils.getLocalHostname
    } else {
      throw new IllegalArgumentException(s"Unable to parse the server from the master $master")
    }

    val sc = new SparkContext(master, "RexecTest")
    val tcpParams = TcpParams(server, TcpServer.DefaultPort)
    val rexecServer = RexecServer(tcpParams)
    rexecServer.start
    val rexecClient = RexecClient(tcpParams)
      val res = rexecClient.run(RexecParams(ExecParams("ls",Some("-lrta .".split(" ")),None,"/shared")), 5)
    println(s"Result: $res")
  }
}