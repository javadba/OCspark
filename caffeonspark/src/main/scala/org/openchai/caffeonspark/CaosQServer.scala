package org.openchai.caffeonspark

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{FileUtils, TcpCommon}
import org.openchai.tcp.xfer._

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class CaosQClient {

  val qClient = new XferQClient(new ArrayBlockingQueue[Any](1000),
    XferConClient.makeXferControllers(XferConCommon.testControllers))


}


/*
1. Discover and initialize/startup the Caffe Processors
2. Synchronize processors
3 Run Training
4 Run Training with Validation
5 Retrieve Validation results
6 Run test mode
7 Shutdown
 */

case class SyncStruct(foo: String)

case class SyncReq(value: SyncStruct) extends P2pReq[SyncStruct]

case class SyncRespStruct(nWorkers: Int, elapsed: Int, path: String)

case class SyncResp(val value: SyncRespStruct) extends P2pResp[SyncRespStruct]

case class TrainStruct(foo: String)

case class TrainReq(value: TrainStruct) extends P2pReq[TrainStruct]

case class TrainRespStruct(nWorkers: Int, elapsed: Int, path: String)

case class TrainResp(val value: TrainRespStruct) extends P2pResp[TrainRespStruct]

case class TrainValidStruct(foo: String)

case class TrainValidReq(value: TrainValidStruct) extends P2pReq[TrainValidStruct]

case class TrainValidRespStruct(nWorkers: Int, elapsed: Int, path: String)

case class TrainValidResp(val value: TrainValidRespStruct) extends P2pResp[TrainValidRespStruct]

case class ValidationStruct(foo: String)

case class ValidationReq(value: ValidationStruct) extends P2pReq[ValidationStruct]

case class ValidationRespStruct(nWorkers: Int, elapsed: Int, path: String)

case class ValidationResp(val value: ValidationRespStruct) extends P2pResp[ValidationRespStruct]

case class TestModeStruct(foo: String)

case class TestModeReq(value: TestModeStruct) extends P2pReq[TestModeStruct]

case class TestModeRespStruct(nWorkers: Int, elapsed: Int, path: String)

case class TestModeResp(val value: TestModeRespStruct) extends P2pResp[TestModeRespStruct]

case class ShutdownStruct(foo: String)

case class ShutdownReq(value: ShutdownStruct) extends P2pReq[ShutdownStruct]

case class ShutdownRespStruct(nWorkers: Int, elapsed: Int, path: String)

case class ShutdownResp(val value: ShutdownRespStruct) extends P2pResp[ShutdownRespStruct]

case class CaosConClientIf(tcpParams: TcpParams, config: XferConfig) extends ServiceIf("CaosClient") {

  private val nReqs = new AtomicInteger(0)

  def sync(s: SyncStruct): SyncResp = {
    println(s"Sync..")
    val resp = getRpc().request(SyncReq(s))
    println(s"Sync response: $resp")
    resp.asInstanceOf[SyncResp]
  }

  def train(s: TrainStruct): TrainResp = {
    println(s"Train..")
    val resp = getRpc().request(TrainReq(s))
    println(s"Train response: $resp")
    resp.asInstanceOf[TrainResp]
  }

  def trainValid(s: TrainValidStruct): TrainValidResp = {
    println(s"TrainValid..")
    val resp = getRpc().request(TrainValidReq(s))
    println(s"TrainValid response: $resp")
    resp.asInstanceOf[TrainValidResp]
  }

  def validation(s: ValidationStruct): ValidationResp = {
    println(s"Validation..")
    val resp = getRpc().request(ValidationReq(s))
    println(s"Validation response: $resp")
    resp.asInstanceOf[ValidationResp]
  }

  def testMode(s: TestModeStruct): TestModeResp = {
    println(s"TestMode..")
    val resp = getRpc().request(TestModeReq(s))
    println(s"TestMode response: $resp")
    resp.asInstanceOf[TestModeResp]
  }

  def shutdown(s: ShutdownStruct): ShutdownResp = {
    println(s"Shutdown..")
    val resp = getRpc().request(ShutdownReq(s))
    println(s"Shutdown response: $resp")
    resp.asInstanceOf[ShutdownResp]
  }

}


class CaosConServerIf(tcpParams: TcpParams, xferServerIf: XferServerIf) extends ServerIf {

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

  def consume(config: TcpXferConfig): Any = defaultConsume(config)

  def defaultConsume(config: TcpXferConfig): Any = {
    val payload = TcpCommon.deserialize(readFile(config.finalPath))
    println(s"DefaultConsume: received data of type ${payload.getClass.getSimpleName}")
    payload
  }

  def readFile(path: String) = FileUtils.readFile(path).getBytes("ISO-8859-1")

  def sync(struct: SyncStruct): SyncRespStruct = SyncRespStruct(1, 0, "Sync")

  def train(struct: TrainStruct): TrainRespStruct = TrainRespStruct(1, 0, "Train")

  def trainValid(struct: TrainValidStruct): TrainValidRespStruct = TrainValidRespStruct(1, 0, "TrainValid")

  def validation(struct: ValidationStruct): ValidationRespStruct = ValidationRespStruct(1, 0, "Validation")

  def testMode(struct: TestModeStruct): TestModeRespStruct = TestModeRespStruct(1, 0, "TestMode")

  def shutdown(struct: ShutdownStruct): ShutdownRespStruct = ShutdownRespStruct(1, 0, "Shutdown")

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: SyncReq =>
        val struct = o.value
        println(s"Invoking Sync: struct=$struct")
        val resp = sync(struct)
        SyncResp(sync(struct))
      case o: TrainReq =>
        val struct = o.value
        println(s"Invoking Train: struct=$struct")
        val resp = train(struct)
        TrainResp(resp)
      case o: TrainValidReq =>
        val struct = o.value
        println(s"Invoking TrainValid: struct=$struct")
        val resp = trainValid(struct)
        TrainValidResp(resp)
      case o: ValidationReq =>
        val struct = o.value
        println(s"Invoking Validation: struct=$struct")
        val resp = validation(struct)
        ValidationResp(resp)
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
