package org.openchai.spark.rdma

import java.util.concurrent.atomic.AtomicInteger

import org.openchai.spark.p2p._
import org.openchai.spark.rdma.SparkRexec.{RexecReq, RexecResp, SendRexecResp}
import org.openchai.spark.util.Logger._

case class RexecParams(exec: String, tcpConnectionParams: TcpConnectionParams)

object RexecServer {

  var server: TcpServer = _
  def start(params: RexecParams) = {
    server = TcpServer(params.tcpConnectionParams.server, params.tcpConnectionParams.port,
      new RexecServerIf(params.tcpConnectionParams))
  }
}

class RexecIf extends ServiceIF {

  import SparkRexec._

  private val nReqs = new AtomicInteger(0)
  def rexec(exec: String): RexecResp = {
    val resp = getRpc().request(SendRexecReq(RexecReq(exec,nReqs.incrementAndGet)))
    resp.asInstanceOf[RexecResp]
  }

  def run(exec: String, nLoops: Int) = {
    for (n <- 0 until nLoops) {
      // while (keepGoing(n).value) {
      debug(s"Loop $n: Sending request: $exec ..")
      val result = rexec(exec)
      debug(s"Loop #$n: Result is $result")
    }
  }

}

class SparkRexecClient(val rexecParams: RexecParams) {

  val rexecIf = new RexecIf
  var tcpClient : TcpClient = _
  def connect() = {
    tcpClient = new TcpClient(rexecParams.tcpConnectionParams, rexecIf)
  }
  def run() = {
    rexecIf.run(rexecParams.exec,5)
  }
}

class RexecServerIf(tcpParams: TcpConnectionParams) extends ServerIF {

  val rexecIf = new RexecIf
//  var tcpServer : TcpServer = _
//  def connect() = {
//    tcpServer = new TcpServer(tcpParams.server, tcpParams.port, rexecIf)
//  }

  private val nReqs = new AtomicInteger(0)
  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: RexecReq => {
        import sys.process._
        val out = o.exec.!!
        SendRexecResp(RexecResp(out, nReqs.incrementAndGet))
      }
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }


}

  object SparkRexec {

    case class RexecReq(exec: String, nExecs: Int)

    case class SendRexecReq(override val value: RexecReq) extends P2pReq[RexecReq]

    case class RexecResp(execOut: String, nExecs: Int)
    case class SendRexecResp(override val value: RexecResp) extends P2pResp[RexecResp]
  }


