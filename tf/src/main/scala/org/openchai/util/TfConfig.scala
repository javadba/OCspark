package org.openchai.util

import org.openchai.tcp.util.FileUtils

import scala.collection.immutable.HashMap

case class TfAppConfig(appType: String, imageExtensions: Seq[String], batchSize: Int=1, gpuRegistryHost: String, gpuRegistryPort: Int) {
  def isSpark = appType == "spark"
  def isRest = appType == "rest"
  def isDirect = appType == "direct"
}

case class TfAppConfigs(var map:  Map[String,TfAppConfig] = new HashMap[String,TfAppConfig]())

object TfAppConfigs {
    var map:  Map[String,TfAppConfig] = new HashMap[String,TfAppConfig]()
}

object TfConfig {
  def getHostName = FileUtils.readFileAsString("/shared/conf/hostname").trim

  def getAppConfig(fpath: String): TfAppConfig = {
    val yml = FileUtils.readFileAsString(fpath)
    YamlUtils.toScala[TfAppConfigs](yml).map("main")
  }
}