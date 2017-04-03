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

case class CQTestParams(master: String, cHost: String, cPort: Int, sHost: String, sPort: Int)
case class ApiTestParams(ctestParams: CQTestParams)

import CaosTest._

class CaosApiTest(params: ApiTestParams = DefaultApiTestParams) extends FlatSpec {

  import CaosTest._

  "APITest" should
    "run ApiTest" in {
    apiTest()
  }
}


class CaosQTest(params: CQTestParams = DefaultQTestParams) extends FlatSpec {

  import CaosTest._
  "QTest" should
  "run qtest" in {
    basicTest(DefaultQTestParams)
  }
}

object CaosTest {

  import org.openchai.tcp.xfer.XferConCommon._
  val cont = TestControllers
  val DefaultQTestParams = CQTestParams("local", cont.conHost, cont.conPort, cont.dataHost, cont.dataPort)

  def toString(msg: Any): String = {
    msg match {
      case q: Array[_] => q.foldLeft(""){ case (ss,s) => ss+","+toString(s)}
      case q: Product => q.productIterator.foldLeft("("){ case (ss,s) => ss+","+toString(s)} + ")"
      case x => x.toString
    }
  }

  val DefaultApiTestParams = ApiTestParams(DefaultQTestParams)

  def apiTest(params: ApiTestParams = DefaultApiTestParams) = {
    val (server,qs) = startServer(params.ctestParams)
    CaosQClient.testClient
  }

  def startServer(params: CQTestParams) = {
    val QSize = 1000
    val qs = new ArrayBlockingQueue[AnyQEntry](QSize)
    val qserver = new CaosServer(qs, TcpParams(params.cHost, params.cPort+2),
        TcpParams(params.cHost, params.cPort), TcpParams(params.sHost, params.sPort))
    qserver.start
    Thread.sleep(100)
    (qserver, qs)
  }

  def basicTest(params: CQTestParams) = {
    val QSize = 1000
    val (server,qs) = startServer(params)

    val StringArrayCount=3
    val FloatArrayCount=20
    val q = new ArrayBlockingQueue[AnyQEntry](QSize)
    for (i <- 1 to QSize) {
      q.offer( (Array.tabulate(StringArrayCount){ j => s"Hello there $i-$j"},
        TcpCommon.serialize(Array.tabulate[Float](FloatArrayCount){ f => f*(f+1.0).toFloat})))
    }
    val controllers = XferConClient.makeXferControllers(TestControllers)
    val qclient = new XferQClient[AnyQEntry](q,controllers)
    val sleep = 4000
    println(s"Sleeping $sleep ..")
    Thread.sleep(sleep)
    var i = 0
    if (qs.peek==null) {
      throw new IllegalStateException(s"No output queue entries")
    }
    while (qs.peek != null) {
      val msg = qs.take
      if (i % 50 == 0) {
        println(s"Output queue entry[$i]=${toString(msg)}")
      }
      i += 1
    }
  }

  def main(args: Array[String]): Unit = {
    apiTest()
//    basicTest(DefaultQTestParams)
  }
}
