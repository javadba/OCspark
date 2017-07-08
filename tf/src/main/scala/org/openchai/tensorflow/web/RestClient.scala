package org.openchai.tensorflow.web

import com.blazedb.spark.reports.YamlStruct

object RestClient {
  def main(args: Array[String]): Unit = {
    if (args.length > 0) {
    if (args(0) == "--restserver") {
      startRestServer
    }
    val testInfoFile = args(0)
    println(s"Reading TfClient setup from $testInfoFile..")
    val yml = YamlStruct(testInfoFile)

  } else {

}

object YamlClient {
  def main(args: Array[String]): Unit = {
    val testInfoFile = args(0)
    println(s"Reading TfClient setup from $testInfoFile..")
    val yml = YamlStruct(testInfoFile)
  }

}

