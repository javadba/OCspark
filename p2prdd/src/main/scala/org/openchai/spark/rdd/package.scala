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
package org.openchai.spark

import org.openchai.tcp.rpc
import org.openchai.tcp.rpc.ArrayData

/**
 * package
 *
 */
package object rdd {
  import org.openchai.tcp

  type DArray = rpc.DArray // Array[Double]
  type MData = rpc.MData
  // case class MData(override val tag: String, override val dims: Seq[Int], override val toArray: DArray) extends ArrayData[DArray]
  type AnyData = MData
  type TData = rpc.TData // case class TData(label: Double, data: Vector[Double])

}
