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
    XferConClient.makeXferControllers(XferConCommon.TestControllers))

  val client = new CaosConClient(new TcpParams(XferConCommon.TestControllers.conHost,
    XferConCommon.TestControllers.conPort+2), CaosConfig("fum"), qClient)

}

object CaosQClient {
  def testClient(): Unit = {
    val trainData = "Hi I'm some training data!".getBytes
    val validData = "what IS validation data actually?".getBytes
    val qClient = new CaosQClient
    qClient.client.sync(SyncStruct("foo"))
    qClient.client.train(TrainStruct("foo"), trainData)
    qClient.client.trainValid(TrainValidStruct("foo"),trainData,validData)
    qClient.client.testMode(TestModeStruct("foo"))
    qClient.client.shutdown(ShutdownStruct("foo"))
  }
  def main(args: Array[String]): Unit = {
    testClient
    println("We're done!")
  }
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

case class CaosConfig(foo: String) // placeholder

class CaosConClient(tcpParams: TcpParams, config: CaosConfig, qClient: XferQClient[Any])
  extends TcpClient(tcpParams, CaosConClientIf(tcpParams, config, qClient)) {

  val caosIf = serviceIf.asInstanceOf[CaosConClientIf]

  def sync(struct: SyncStruct) = caosIf.sync(struct)
  def train(struct: TrainStruct, train: TrainingData) = caosIf.train(struct, train)
  def trainValid(struct: TrainValidStruct, train: TrainingData, valid: TrainingData)
    = caosIf.trainValid(struct, train, valid)
  def validation(struct: ValidationStruct, valid: ValidationData) = caosIf.validation(struct, valid)
  def testMode(struct: TestModeStruct) = caosIf.testMode(struct)
  def shutdown(struct: ShutdownStruct) = caosIf.shutdown(struct)
}

case class CaosConClientIf(tcpParams: TcpParams, config: CaosConfig, qClient: XferQClient[Any]) extends ServiceIf("CaosClient") {

  private val nReqs = new AtomicInteger(0)

  def sync(s: SyncStruct): SyncResp = {
    println(s"Sync..")
    val resp = getRpc().request(SyncReq(s))
    println(s"Sync response: $resp")
    resp.asInstanceOf[SyncResp]
  }

  def train(s: TrainStruct, data: TrainingData): TrainResp = {
    println(s"Train with dataSize= ${data.length} ..")
    qClient.queue.offer(data)
    val resp = getRpc().request(TrainReq(s))
    println(s"Train response: $resp")
    resp.asInstanceOf[TrainResp]
  }

  def trainValid(s: TrainValidStruct, train: TrainingData, valid: TrainingData): TrainValidResp = {
    println(s"TrainValid..")
    qClient.queue.offer(train)
    qClient.queue.offer(valid)
    val resp = getRpc().request(TrainValidReq(s))
    println(s"TrainValid response: $resp")
    resp.asInstanceOf[TrainValidResp]
  }

  def validation(s: ValidationStruct, valid: ValidationData): ValidationResp = {
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
