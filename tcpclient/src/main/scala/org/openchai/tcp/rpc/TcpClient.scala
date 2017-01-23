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
package org.openchai.tcp.rpc

import org.openchai.tcp.util.Logger._
import org.openchai.tcp.util.TcpCommon._
import org.openchai.tcp.util.TcpUtils

case class TcpParams(server: String, port: Int) extends P2pConnectionParams

//class BinaryTcpClient(connParams: TcpParams) extends TcpClient(connParams, new BinaryIf)

class TcpClient(val connParams: TcpParams, val serviceIf: ServiceIF)
  extends P2pRpc with P2pBinding {

  import java.io._
  import java.net._

  import reflect.runtime.universe._

  private var sock: Socket = _
  private var os: OutputStream = _
  private var is: InputStream = _

  {
      connect(connParams)
  }
  override def isConnected: Boolean = is != null && os != null

  override def connect(connParam: P2pConnectionParams): Boolean = {
    savedConnParam = connParam
    val tconn = connParams.asInstanceOf[TcpParams]
    println(s"TcpClient: Connecting ${serviceIf.name} to ${tconn.server}:${tconn.port} ..")
    sock = new Socket(tconn.server, tconn.port)
    os = sock.getOutputStream
    is = sock.getInputStream
    bind(this, serviceIf)
    println(s"TcpClient: Bound ${serviceIf.name} to ${tconn.server}:${tconn.port}")
    is != null && os != null
  }

  private var savedConnParam: P2pConnectionParams = _

  override def request[U : TypeTag , V :  TypeTag](req: P2pReq[U]): P2pResp[V] = {
    // TODO: determine how to properly size the bos
    if (!isConnected) {
      connect(savedConnParam)
    }
    val buf = new Array[Byte](Math.pow(2,20).toInt)
    val serreq = serialize(req)
    os.write(serreq)
    val nread = is.read(buf)
    info(s"request: received $nread bytes")
    val o = deserialize(buf)
    val out = o.asInstanceOf[P2pResp[V]]
    if (reconnectEveryRequest) {
      sock.close
      sock = null
      os = null
      is = null
    }
    out
  }

}

object TcpClient {
  val TestPort = 8989
  def main(args: Array[String]) {
    import SolverIF._
    val server = if (args.length >= 1) args(0) else TcpUtils.getLocalHostname
    val port = if (args.length >= 2) args(1) else TestPort
    val serviceIf = new SolverIF
    val client = new TcpClient(TcpParams(server, TestPort), serviceIf)
    val w = serviceIf.run(ModelParams(new DefaultModel(), new DefaultHyperParams()),TestData.mdata(10,100), 3)
  }
}
