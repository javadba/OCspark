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

import org.openchai.tcp.util.TcpCommon._
import org.openchai.tcp.util.TcpUtils

case class TcpParams(server: String, port: Int) extends P2pConnectionParams

//class BinaryTcpClient(connParams: TcpParams) extends TcpClient(connParams, new BinaryIf)

class TcpClient(val connParams: TcpParams, val serviceIf: ServiceIf)
  extends P2pRpc with P2pBinding {

  import java.io._
  import java.net._

  import reflect.runtime.universe._

  private var sock: Socket = _
  private var os: OutputStream = _
  private var is: InputStream = _

  val MaxTcpWaitSecs = 2

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
    val buf = new Array[Byte](Math.pow(2,22).toInt)
    val serreq = serializeStream(req.path, pack(req.path, req))
    os.write(serreq)
    os.flush

    val dis = new DataInputStream(is)
      var totalRead = 0
      do {
        val available = dis.available
        if (available <= 0) {
          Thread.sleep(200)
        } else {
          do {
            var innerWait = 0
            do {
              val nread = dis.read(buf, totalRead, buf.length - totalRead)
              totalRead += nread
              //                debug(s"in loop: nread=$nread totalRead=$totalRead")
              Thread.sleep(50)
              innerWait += 1
              if (innerWait %20==0) {
//                println(s"InnerWait=%d")
              }
            } while (dis.available > 0)
            var outerLoopCnt = 0
            do {
              Thread.sleep(100)
              outerLoopCnt += 1
//              println(s"OuterloopCnt=$outerLoopCnt")
            } while (totalRead > 5000 && dis.available <= 0 && outerLoopCnt <= MaxTcpWaitSecs * 10)
          } while (dis.available > 0)
        }
      } while (totalRead <= 0)
      println(s"Serve: totalRead=$totalRead")
      val o = unpack("/tmp/serverReq.out", buf.slice(0, totalRead))
//    info(s"request: received $nread bytes")
//    val (path, o, md5) = unpack(buf.slice(0,nread))
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
    import SolverIf._
    val server = if (args.length >= 1) args(0) else TcpUtils.getLocalHostname
    val port = if (args.length >= 2) args(1) else TestPort
    val serviceIf = new SolverIf
    val client = new TcpClient(TcpParams(server, TestPort), serviceIf)
    val w = serviceIf.run(ModelParams(new DefaultModel(), new DefaultHyperParams()),TestData.mdata(10,100), 3)
  }
}
