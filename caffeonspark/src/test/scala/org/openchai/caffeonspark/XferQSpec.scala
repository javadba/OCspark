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
package org.openchai.caffeonspark

import java.util.concurrent.ArrayBlockingQueue

import org.openchai.tcp.rpc.TcpParams
import org.openchai.tcp.util.TcpCommon
import org.openchai.tcp.xfer.XferConClient
import org.scalatest.FlatSpec
import CaosTest._

//case class QTestParams(master: String, cHost: String, cPort: Int, sHost: String, sPort: Int)
//
//class XferQClientTest(params: CQTestParams = DefaultQTestParams) extends FlatSpec {
//
//  import CaosTest._
//  "basicTest" should
//  "run qtest" in {
//    basicQTest(DefaultQTestParams)
//  }
//}
//object XferQClientTest {
//
//  import org.openchai.tcp.xfer.XferConCommon._
//  val cont = testControllers
//  val DefaultQTestParams = CQTestParams("local", cont.conHost, cont.conPort, cont.dataHost, cont.dataPort)
//
//  def basicQTest(params: CQTestParams) = {
//    val QSize = 1000
//    val StringArrayCount=3
//    val FloatArrayCount=20
//    val q = new ArrayBlockingQueue[AnyQEntry](QSize)
//    for (i <- 1 to QSize) {
//      q.offer( (Array.tabulate(StringArrayCount){ j => s"Hello there $i-$j"},
//        TcpCommon.serialize(Array.tabulate[Float](FloatArrayCount){ f => f*(f+1.0).toFloat})))
//    }
//    val qserver = new XferQServer(q, TcpParams(params.cHost, params.cPort), TcpParams(params.sHost, params.sPort))
//    qserver.start
//    Thread.sleep(100)
//    val controllers = XferConClient.makeXferControllers(testControllers)
//    val qclient = new XferQClient[AnyQEntry](q,controllers)
//    Thread.currentThread.join
//  }
//  def main(args: Array[String]): Unit = {
//    CaosQTest.basicQTest(DefaultQTestParams)
//
//  }
//}
