package org.openchai.util

import org.openchai.tcp.util.FileUtils

import scala.collection.immutable.HashMap
//import net.jcazevedo.moultingyaml._
/*
main :
  isSpark: true
  isRest: true
  restHostAndPort: localhost:8190
  master: local[*]
  tfServer: 192.168.0.2
  imgApp: tensorflow-labelimage
  inDir: /data/scenery
  outDir: /data/out/scenery
  outTag: scenery
 */
case class TfAppConfig(isSpark: Boolean, isRest: Boolean, restHostAndPort: String, master: String,
  tfServerAndPort: String, imgApp: String, inDir: String, outDir: String, outTag: String, nPartitionsPerTx1: Int)

//object TfAppConfigYamlProtocol extends DefaultYamlProtocol {
//  implicit val appConfigFormat = yamlFormat10(TfAppConfig)
//}

case class TfAppConfigs(var map:  Map[String,TfAppConfig] = new HashMap[String,TfAppConfig]())

object TfAppConfigs {
    var map:  Map[String,TfAppConfig] = new HashMap[String,TfAppConfig]()
}

object TfConfig {
//  import TfAppConfigYamlProtocol._
  def getAppConfig(fpath: String): TfAppConfig = {
//    val yml = FileUtils.readFileAsString(fpath).parseYaml
//    yml.convertTo[TfAppConfig]
    val yml = FileUtils.readFileAsString(fpath)
    YamlUtils.toScala[TfAppConfigs](yml).map("main")
  }
}