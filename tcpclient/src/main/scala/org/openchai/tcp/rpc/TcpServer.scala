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
import java.util
import java.util.concurrent.Executors

import org.openchai.tcp.util.Logger._
import org.openchai.tcp.util.TcpCommon

import scala.collection.mutable

object TcpServer {
  val DefaultPort = 8989
  val BufSize = (Math.pow(2, 24) - 1).toInt
}

case class TcpServer(host: String, port: Int, serverIf: ServerIf) extends P2pServer with P2pBinding {

  import TcpServer._

  private var serverThread: Thread = _
  private var serverSocket: ServerSocket = _
  private var stopRequested: Boolean = _
  val MaxListeners = 3
  val pool = Executors.newFixedThreadPool(MaxListeners)

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
    info(s"Starting ${serverIf.name} on $host:$port ..")
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
          val sock = serverSocket.accept()
          pool.submit(new Runnable() {
            override def run() = {
              serve(sock)
            }
          })
        }
      }
    }
    serverThread.start
    this
  }

  import TcpCommon._

  val MaxTcpWaitSecs = 1

  def serve(socket: Socket): Unit = {
    val buf = new Array[Byte](BufSize)
    val sockaddr = socket.getRemoteSocketAddress.asInstanceOf[InetSocketAddress]
    info(s"Received connection request from ${sockaddr.getHostName}@${sockaddr.getAddress.getHostAddress} on socket ${socket.getPort}")
    var msgPrinted = false
    var msgCounter = 0

    val is = new DataInputStream(socket.getInputStream)
    val os = new BufferedOutputStream(socket.getOutputStream)
    var nEmpty = 0
    val nWaitCycles = Option(System.getProperty("tcpserver.wait.cycles")).getOrElse("12").toInt
    do {
      if (!msgPrinted) {
        debug("Listening for messages..");
        msgPrinted = true
      }
      var totalRead = 0
      var nNone = 0
      do {
        val available = is.available
        if (available <= 0) {
          Thread.sleep(50)
          if (nNone % 20 == 0) {
            debug(s"None available: $nNone")
          }
          nNone += 1
        } else {
          nNone = 0
          do {
            var innerWait = 0
            do {
              val nread = is.read(buf, totalRead, buf.length - totalRead)
              totalRead += nread
              debug(s"in loop: nread=$nread totalRead=$totalRead is.available=${is.available}")
              Thread.sleep(50)
              innerWait += 1
              if (innerWait % 20 == 0) {
                debug(s"InnerWait=$innerWait")
              }
            } while (is.available > 0)
            var outerLoopCnt = 0
            do {
              Thread.sleep(100)
              outerLoopCnt += 1
              if (outerLoopCnt % 10 ==0) {
                debug(s"OuterloopCnt=$outerLoopCnt")
              }
            } while (totalRead > 5000 && is.available <= 0 && outerLoopCnt <= MaxTcpWaitSecs * 10)
          } while (is.available > 0)
        }
      } while (totalRead <= 0 && nNone <= 50)
      if (totalRead > 0) {
        debug(s"Serve: totalRead=$totalRead")
        val unpacked = unpack("/tmp/serverReq.out", buf.slice(0, totalRead))
        val req = unpacked.asInstanceOf[P2pReq[_]]
        //          val req = unpacked._2.asInstanceOf[P2pReq[_]]
        //          debug(s"Message received: ${req.toString}")
        val resp = serverIf.service(req)
        //          debug(s"Sending response:  ${resp.toString}")
        val ser = serializeStream("/tmp/serverResp.out", pack("/tmp/serverResp.pack.out", resp))
        os.write(ser)
        os.flush
        Thread.sleep(200)
      } else {
        nEmpty += 1
      }
      debug(s"outer outer: $nEmpty")
    } while (nEmpty <= nWaitCycles)
    info(s"Killing thread")
  }

  override def stop() = {
    serverThread.stop
    true
  }
}

