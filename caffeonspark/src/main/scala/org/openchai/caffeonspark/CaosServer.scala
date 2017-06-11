package org.openchai.caffeonspark

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{FileUtils, TcpCommon}
import org.openchai.tcp.util.Logger._
import org.openchai.tcp.xfer._

// The main thing we need to override here is using XferQConServerIf inside the server object
class CaosServer(val outQ: BlockingQueue[TaggedEntry], val qTcpParams: TcpParams,
  val tcpParams: TcpParams, val xtcpParams: TcpParams) {

  val qServer = new QXferConServer(outQ, tcpParams, xtcpParams)
  val caosServer = new TcpServer(qTcpParams.server, qTcpParams.port, new CaosServerIf[TaggedEntry](outQ))

  def start() = {
    qServer.start
    caosServer.start
    // Notice the use of XferQConServerIf as third parameter: but still using xferServerIf inside that constructor
    this
  }

}

object CaosServer {
  def apply(outQ: BlockingQueue[TaggedEntry], qTcpParams: TcpParams, tcpParams: TcpParams,
    xtcpParams: TcpParams) = {
    val server = new CaosServer(outQ, qTcpParams, tcpParams, xtcpParams)
    server.start
  }

  def main(args: Array[String]): Unit = {
    val q = new ArrayBlockingQueue[TaggedEntry](1000)
    val (host,port,xhost,xport,ahost, aport, configFile) = XferConServer.makeXferConnections(args)
    val server = apply(q,TcpParams(ahost, aport), TcpParams(host,port), TcpParams(xhost,xport))
  }

}

class CaosServerIf[T](/*tcpParams: TcpParams, xferServerIf: XferServerIf, */q: BlockingQueue[T]) extends ServerIf("CaosServerIf") {

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

//  def consume(config: TcpXferConfig): Any = {
//    val payload = defaultConsume(config).asInstanceOf[T]
//    q.offer(payload)
//    payload
//  }
//
//  def defaultConsume(config: TcpXferConfig): Any = {
//    val payload = TcpCommon.deserialize(readFile(config.finalPath))
//    println(s"DefaultConsume: received data of type ${payload.getClass.getSimpleName}")
//    payload
//  }

  def readFile(path: String) = FileUtils.readFileBytes(path)

  def sync(struct: SyncStruct): SyncRespStruct = SyncRespStruct(1, 0, "Sync")

  def train(struct: TrainStruct, trainData: TrainingData): TrainRespStruct = TrainRespStruct(1, 0, "Train")

  def trainValid(struct: TrainValidStruct, trainData: TrainingData, testData: TrainingData): TrainValidRespStruct
    = TrainValidRespStruct(1, 0, "TrainValid")

  def validation(struct: ValidationStruct, validationData: ValidationData): ValidationRespStruct
    = ValidationRespStruct(1, 0, "Validation")

  def testMode(struct: TestModeStruct): TestModeRespStruct = TestModeRespStruct(1, 0, "TestMode")

  def shutdown(struct: ShutdownStruct): ShutdownRespStruct = ShutdownRespStruct(1, 0, "Shutdown")

  implicit def arr(qEntry: TaggedEntry): TrainingData = qEntry.asInstanceOf[TrainingData]

//  def arr(qEntry: TaggedEntry) = qEntry.asInstanceOf[TrainingData]

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: SyncReq =>
        val struct = o.value
        println(s"Invoking Sync: struct=$struct")
        val resp = sync(struct)
        SyncResp(sync(struct))
//      case o: TrainReq =>
//        val struct = o.value
//        println(s"Invoking Train: struct=$struct")
//        val resp = train(struct, q.take)
//        TrainResp(resp)
//      case o: TrainValidReq =>
//        val struct = o.value
//        println(s"Invoking TrainValid: struct=$struct")
//        val resp = trainValid(struct, q.take, q.take)
//        TrainValidResp(resp)
//      case o: ValidationReq =>
//        val struct = o.value
//        println(s"Invoking Validation: struct=$struct")
//        val resp = validation(struct, q.take)
//        ValidationResp(resp)
      case o: TestModeReq =>
        val struct = o.value
        println(s"Invoking TestMode: struct=$struct")
        val resp = testMode(struct)
        TestModeResp(resp)
      case o: ShutdownReq =>
        val struct = o.value
        println(s"Invoking Shutdown: struct=$struct")
        val resp = shutdown(struct)
        ShutdownResp(resp)
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }

}
