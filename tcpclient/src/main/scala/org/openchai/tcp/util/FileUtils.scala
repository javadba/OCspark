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
package org.openchai.tcp.util

import java.io.{File, FileInputStream}
import java.nio.file.Paths
import java.util.Scanner
import java.util.concurrent.{Callable, Executors, Future}

import scala.collection.mutable.ArrayBuffer

object FileUtils {
  import Logger._
  type TaskResult = Array[Byte]

  def mkdirs(dir: String) = {
    val fdir = new File(dir)
    if (!fdir.exists()) {
      info(s"Creating directory ${fdir.getPath}")
      fdir.mkdirs
    }
  }
  def rmdirs(dir: String): Array[(String, Boolean)] = {
//    if (fdir.exists()) {
//      debug(s"Removing directory ${fdir.getPath}")
    Option(new File(dir).listFiles)
      .map(_.flatMap(f => rmdirs(f.getPath))).getOrElse(Array()) :+ (dir -> new File(dir).delete)
  }

  def write(path: String, data: String): Unit = tools.nsc.io.File(path).writeAll(data)


  def readPath(path: String, recursive: Boolean = true, multiThreaded: Boolean = true): TaskResult = {
    val nThreads = if (multiThreaded) {
      Runtime.getRuntime.availableProcessors * 2
    } else {
      1
    }
    val tpool = Executors.newFixedThreadPool(nThreads)
    class ReadTask(path: String) extends Callable[TaskResult] {
      override def call(): TaskResult = {
        readFileBytes(path)
      }
    }
//    val sb = new StringBuffer // Make sure StringBUFFER not BUILDER because of multithreaded!!
    val taskResult = new ArrayBuffer[Byte](1024*128) // Make sure StringBUFFER not BUILDER because of multithreaded!!

    import collection.mutable
    val tasksBuf = mutable.ArrayBuffer[Future[TaskResult]]()
    def readPath0(fpath: String): TaskResult = {
      val paths = new File(fpath).listFiles.filter { f => !f.getName.startsWith(".") }
      paths.foreach { f =>
        if (f.isDirectory) {
          if (recursive) {
            debug(s"Descending into ${f.getPath} ..")
            readPath0(f.getPath)
          } else {
            debug(s"Recursive is false so NOT descending into ${f.getPath}")
          }
        } else {
          tasksBuf += tpool.submit(new ReadTask(f.getPath))
        }
      }
      tasksBuf.foreach { t => taskResult ++= t.get }
      taskResult.toArray
    }
    readPath0(path)
  }

  def readFileBytes(fpath: String): Array[Byte] = readFileAsString(fpath).getBytes("ISO-8859-1")

  def readFileAsString(fpath: String) = {
//    val content = new Scanner(Paths.get(fpath)).useDelimiter("\\Z").next()
    val content = scala.io.Source.fromFile(fpath,"ISO-8859-1").getLines.mkString("")
    content
  }
}
