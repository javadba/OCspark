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
package org.openchai.tcp

import org.openchai.tcp.util.{FileUtils, TcpUtils}
import org.openchai.tcp.xfer.{DataPtr, RawData}

package object rpc {

  trait P2pConnectionParams

  val BufSize = (64 * Math.pow(2, 20)-1).toInt   // 64MB
  abstract class ServiceIf(val name: String) {

    import reflect.runtime.universe.TypeTag
    def request[U: TypeTag, V: TypeTag](req: P2pReq[U]): P2pResp[V] = {
      getRpc.request(req)
    }
    protected[tcp] var optRpc: Option[P2pRpc] = None

    protected def getRpc() = optRpc match {
      case None => throw new IllegalStateException("RPC mechanism has not been set")
      case _ => optRpc.get
    }

    val clientName = TcpUtils.getLocalHostname
  }

  abstract class ServerIf(val name: String ="ServerIf") {
    def service(req: P2pReq[_]): P2pResp[_]
  }

  sealed trait P2pMessage[T] extends java.io.Serializable {
    def path(): DataPtr = getClass.getName
    def value(): T
  }

  trait P2pReq[T] extends P2pMessage[T] with java.io.Serializable

  trait P2pResp[T] extends P2pMessage[T] with java.io.Serializable

  trait ArrayData[V] {
    def tag: String
    def dims: Seq[Int]
    def toArray: V
  }

  import reflect.runtime.universe._
  abstract class XferReq[T: TypeTag](val value: T) extends P2pReq[T]

  abstract class XferResp[T: TypeTag](val value: T) extends P2pResp[T]

  type DArray = Array[Double]
  case class MData(override val tag: String, override val dims: Seq[Int], override val toArray: DArray) extends ArrayData[DArray]
  type AnyData = MData
  case class TData(label: Double, data: Vector[Double])
}
