package org.openchai.tcp.rexec

import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.ProcessUtils.{ExecParams, ExecResult}
import org.openchai.tcp.util.Logger._
import org.openchai.tcp.util.ProcessUtils

case class RexecParams(execParams: ExecParams)

  case class Rexec(execParams: ExecParams)
  case class RexecReq(rexec: Rexec) extends P2pReq[Rexec] {
    override def value(): Rexec = rexec
  }

  case class RexecResp(res: ExecResult) extends P2pResp[ExecResult] {
    override def value(): ExecResult = res
  }

object Rexec {
  def main(args: Array[String]): Unit = {
    val serverOrClient = args(0)
    System.setProperty("java.net.preferIPv4Stack","true")

    if (serverOrClient.toLowerCase().endsWith("server")) {
      RexecServer.main(args.tail)
    } else {
      RexecTcpClient.main(args.tail)
    }

  }
}
object RexecServer {

  var server: TcpServer = _

  def apply(tcpParams: TcpParams) = {
    server = TcpServer(tcpParams.server, tcpParams.port,
      new RexecServerIf(tcpParams))
    server
  }

  def main(args: Array[String]): Unit = {
    val host = args(0)
    val port = args(1).toInt
    val server = apply(TcpParams(host, port))
    server.start
    Thread.currentThread.join
  }
}

class RexecIf extends ServiceIf("Rexec") {

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

object RexecTcpClient {
  def main(args: Array[String]): Unit = {
    val host = args(0)
    val port = args(1).toInt
    val cmd = args(2)
    val params = if (args.length >= 4) Some(args(3).split(":").toSeq) else None
    val env = if (args.length >= 5) Some(args(4).split(":").toSeq) else None
    val dir = if (args.length >= 6) args(5) else "."
    val client = RexecTcpClient(TcpParams(host, port))
    val rparams = RexecParams(ExecParams(cmd, params, env, dir))
    val res = client.run(rparams, 1)
    println(res)
  }

}

case class RexecTcpClient(tcpParams: TcpParams) extends TcpClient(tcpParams, new RexecIf) {

  // TODO: see how to use typeclass annotation for the RexecIf
  val rexecIf = serviceIf.asInstanceOf[RexecIf]

  connect(tcpParams)

  def run(rexecParams: RexecParams, nLoops: Int) = {
    val result = rexecIf.run(rexecParams.execParams, nLoops)
    println(s"Client: got result $result")
  }
}

class RexecServerIf(tcpParams: TcpParams) extends ServerIf {

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


