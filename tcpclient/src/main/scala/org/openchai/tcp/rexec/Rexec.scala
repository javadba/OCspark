package org.openchai.tcp.rexec

import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.ProcessUtils.{ExecParams, ExecResult}
import org.openchai.tcp.util.Logger._
import org.openchai.tcp.util.ProcessUtils

case class RexecParams(execParams: ExecParams /*, tcpParams: TcpParams */)

  case class Rexec(execParams: ExecParams)
  case class RexecReq(rexec: Rexec) extends P2pReq[Rexec] {
    override def value(): Rexec = rexec
  }

  case class RexecResp(res: ExecResult) extends P2pResp[ExecResult] {
    override def value(): ExecResult = res
  }

object RexecServer {

  var server: TcpServer = _

  def apply(tcpParams: TcpParams) = {
    server = TcpServer(tcpParams.server, tcpParams.port,
      new RexecServerIf(tcpParams))
    server
  }
}

class RexecIf extends ServiceIF {

  private val nReqs = new AtomicInteger(0)

  def rexec(execParams: ExecParams): RexecResp = {
    val resp = getRpc().request(RexecReq(Rexec(execParams)))
    resp.asInstanceOf[RexecResp]
  }

  def run(execParams: ExecParams, nLoops: Int) = {
    for (n <- 0 until nLoops) {
      // while (keepGoing(n).value) {
      debug(s"Loop $n: Sending request: $execParams ..")
      val result = rexec(execParams)
      debug(s"Loop #$n: Result is $result")
    }
  }

}

case class RexecClient(tcpParams: TcpParams) {

  val rexecIf = new RexecIf
  var tcpClient: TcpClient = connect()

  def connect() = {
    new TcpClient(tcpParams, rexecIf)
  }

  def run(rexecParams: RexecParams, nLoops: Int) = {
    val result = rexecIf.run(rexecParams.execParams, nLoops)
    println(s"Client: got result $result")
  }
}

class RexecServerIf(tcpParams: TcpParams) extends ServerIF {

  val rexecIf = new RexecIf

  private val nReqs = new AtomicInteger(0)

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: RexecReq => {
        val eres = ProcessUtils.exec(o.rexec.execParams)
        RexecResp(eres)
      }
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }

}


