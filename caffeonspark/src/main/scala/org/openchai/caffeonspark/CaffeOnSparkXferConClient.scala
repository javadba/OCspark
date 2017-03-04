package org.openchai.caffeonspark

import org.openchai.tcp.util.TcpCommon
import org.openchai.tcp.xfer.{XferConClient, XferWriteParams}

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

import java.util.concurrent.ArrayBlockingQueue

import org.openchai.tcp.util.Logger._
import org.openchai.tcp.xfer.XferConClient._

import reflect.runtime.universe.TypeTag

class XferQConClient[T: TypeTag](queue: ArrayBlockingQueue[T],
  controllers: XferControllers) {

  val qclient = new Thread() {

    var canceled = false

    override def run(): Unit = {
      while (!canceled) {
        if (queue.peek() == null) {
          debug("XferQClient:  QReader is waiting ..")
        } else {
          debug("XferQClient:  QReader found an entry immediately")
        }
        val payload = queue.take
        debug(s"XferQClient: payload is $payload")
        val wparams = XferWriteParams(controllers.xferConf,
          TcpCommon.serialize(payload))
        val wres = controllers.client.write(controllers.xferConf, wparams)
        info(s"Xferqclient: QReader completed with resp=$wres")
      }
    }
  }
  qclient.start
}

case class Howdy(name: String, metric: Int)

object XferQConClient {
  import org.openchai.tcp.xfer.XferConCommon._
  def main(args: Array[String]): Unit = {
    val nEntries = 20
    val q = new ArrayBlockingQueue[QueueEntry](5)
    val controllers = makeXferControllers(testControllers)
    val client = new XferQConClient[QueueEntry](q,controllers)
    val entries = for (i <- 1 until nEntries) yield {
      (Array("a", "b", "c"), TcpCommon.serialize(Howdy(s"Hi${i}!", i*i)))
    }

  }
}


