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

import org.openchai.tcp.rpc.{SolverServerIF, TcpClient, TcpServer}
import org.openchai.tcp.util.TcpUtils

/**
 * TcpCommTest
 *
 */
object TcpTest {

  val weightsMergePolicy: String = "best"
  val TestPort = TcpServer.DefaultPort

  def main(args: Array[String]) {
    val server = TcpServer(TcpUtils.getLocalHostname, TestPort, new SolverServerIF(weightsMergePolicy))
    server.start
    TcpClient.main(Array(TcpUtils.getLocalHostname,"" + TestPort))
    Thread.currentThread.join
  }
}
