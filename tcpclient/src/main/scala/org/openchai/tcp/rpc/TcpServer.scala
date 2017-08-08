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

import java.io.{BufferedOutputStream, DataInputStream}
import java.net._

import org.openchai.tcp.util.Logger._
import org.openchai.tcp.util.TcpCommon

import scala.collection.mutable

object TcpServer {
  val DefaultPort = 8989
  val BufSize = (Math.pow(2, 22) - 1).toInt
}

case class TcpServer(host: String, port: Int, serverIf: ServerIf) extends P2pServer with P2pBinding {

  import TcpServer._

  private var serverThread: Thread = _
  private var serverSocket: ServerSocket = _
  private var stopRequested: Boolean = _
  val threads = mutable.ArrayBuffer[Thread]()

  type ServerType = TcpServer

  def CheckPort = false

  def checkPort(port: Int) = {
    import scala.concurrent.duration._
    val socketTimeout = 200
    val duration: Duration = 10 seconds
  }

  override def start() = {
    System.setProperty("java.net.preferIPv4Stack", "true")
    serverSocket = new ServerSocket()
    println(s"Starting ${serverIf.name} on $host:$port ..")
    try {
      if (host == "*" || host == "all" || host == "localhost") {
        serverSocket.bind(new InetSocketAddress(port))
      } else {
        serverSocket.bind(new InetSocketAddress(host, port))
      }
    } catch {
      case e: Exception => throw new Exception(s"BindException on $host:$port", e)
    }
    serverThread = new Thread() {
      override def run() {
        while (!stopRequested) {
          val t = serve(serverSocket.accept())
          t.start
          threads += t
        }
      }
    }
    serverThread.start
    this
  }

  import TcpCommon._

  val MaxTcpWaitSecs = 1
  def serve(socket: Socket): Thread = {
    val sockaddr = socket.getRemoteSocketAddress.asInstanceOf[InetSocketAddress]
    info(s"Received connection request from ${sockaddr.getHostName}@${sockaddr.getAddress.getHostAddress} on socket ${socket.getPort}")
    val t = new Thread() {
      var msgPrinted = false
      var msgCounter = 0

      override def run() = {
        val is = new DataInputStream(socket.getInputStream)
        val os = new BufferedOutputStream(socket.getOutputStream)
        do {
          val buf = new Array[Byte](BufSize)
          if (!msgPrinted) {
            debug("Listening for messages..");
            msgPrinted = true
          }
          var totalRead = 0
          do {
            val available = is.available
            if (available <= 0) {
              Thread.sleep(200)
            } else {
              do {
                var innerWait = 0
                do {
                  val nread = is.read(buf, totalRead, buf.length - totalRead)
                  totalRead += nread
                  //                debug(s"in loop: nread=$nread totalRead=$totalRead")
                  Thread.sleep(50)
                  innerWait += 1
                  if (innerWait %20==0) {println(s"InnerWait=%d")}
                } while (is.available > 0)
                var outerLoopCnt = 0
                do {
                  Thread.sleep(100)
                  outerLoopCnt += 1
//                  println(s"OuterloopCnt=$outerLoopCnt")
                } while (totalRead > 5000 && is.available <= 0 && outerLoopCnt <= MaxTcpWaitSecs * 10)
              } while (is.available > 0)
            }
          } while (totalRead <= 0)
//          println(s"Serve: totalRead=$totalRead")
          val unpacked = unpack("/tmp/serverReq.out", buf.slice(0, totalRead))
          val req = unpacked.asInstanceOf[P2pReq[_]]
          //          val req = unpacked._2.asInstanceOf[P2pReq[_]]
          //          debug(s"Message received: ${req.toString}")
          val resp = serverIf.service(req)
          //          debug(s"Sending response:  ${resp.toString}")
          val ser = serializeStream("/tmp/serverResp.out", pack("/tmp/serverResp.pack.out", resp))
          os.write(ser)
          os.flush
        } while (!reconnectEveryRequest)
        Thread.sleep(5000)
      }
    }
    t
  }

  override def stop(): Boolean = ???
}

