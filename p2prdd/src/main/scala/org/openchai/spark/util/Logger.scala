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
package org.openchai.spark.util

trait Logger {
  def f(msg: String) = {
    val d = new java.util.Date
    "%02d%02d-%02d:%02d:%02d.%03d".format(d.getMonth, d.getDate, d.getHours, d.getMinutes, d.getSeconds, d.getTime / 10e10.toInt)
  }

  def debug(msg: String) = Logger.debug(msg)
  def info(msg: String) = Logger.info(msg)
  def error(msg: String) = Logger.error(msg)
}
object Logger {
  def f(msg: String) = {
    val d = new java.util.Date
    "%02d%02d-%02d:%02d:%02d.%03d".format(d.getMonth, d.getDate, d.getHours, d.getMinutes, d.getSeconds, d.getTime / 10e10.toInt)
  }
  def debug(msg: String) = println(s"Debug: $msg")
  def info(msg: String) = println(s"Info: $msg")
  def error(msg: String) = println(s"ERROR: $msg")
}
