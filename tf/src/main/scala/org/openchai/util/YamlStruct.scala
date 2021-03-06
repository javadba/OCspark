package org.openchai.util

import java.io.FileInputStream

import org.yaml.snakeyaml.Yaml
import org.openchai.tcp.util.Logger._
import org.openchai.tensorflow.{AnyMap, MapMap, StringMap}

class AppConfig(yamlPath: String, os: String) {
  val yamlConf = YamlStruct(yamlPath)
  val envmap = yamlConf.getMap("environments").apply(os).asInstanceOf[MapMap]("env").asInstanceOf[StringMap]

  private def emap(app: String) = yamlConf.getMap("defaults").apply("apps").asInstanceOf[AnyMap](app).asInstanceOf[StringMap].map { case (k, v) =>
    val vnew = envmap.foldLeft(v) { case (vv, (ke, ve)) => /* info(vv); */ vv.replace(s"$${$ke}", ve) }
    (k, vnew)
  }

  def apply(dottedKey: String) = {
    val keys = dottedKey.split("\\.")
    yamlConf.getMap(keys(0)).asInstanceOf[StringMap].get(keys(1)).get
  }

  def apply(app: String, key: String, default: String = "") = emap(app).getOrElse(key, default)

  def getMap(app: String, key: String) = emap(app)(key).asInstanceOf[Map[String, String]]
}

trait YamlConf {

  def apply(key: String): Any

  def apply(key: String, default: String): Any

  def getMap(key: String): Map[String, _]
}

case class YamlStruct(ymlFile: String) extends YamlConf {
  val yaml = new Yaml

  import collection.JavaConverters._

  def expand(o: Any): Any = {
    o match {
      case lst: java.util.List[_] =>
        Seq(lst.asScala.map { o => expand(o) })
      case m: java.util.Map[_, _] =>
        m.asScala.map { case (k, v) =>
          (k, expand(v))
        }.toMap
      case _ =>
        o
    }
  }

  val yamlConf = {
    val iy = (for (y <- yaml.loadAll(new FileInputStream(ymlFile)).iterator.asScala) yield y).toList.head // toMap[String,Any]
    val omap = expand(iy).asInstanceOf[Map[String, _]]
    info(s"omap: ${omap}")
    omap
  }

  def getConfiguration = yamlConf

  override def toString() = {
    getConfiguration.mkString(",")
  }

  override def getMap(key: String) = apply(key).asInstanceOf[Map[String, _]]

  override def apply(key: String) = {
    yamlConf(key)
  }

  override def apply(key: String, default: String) = {
    yamlConf.getOrElse(key, default)
  }

}

object YamlStruct {

  def main(args: Array[String]) {
    val f = java.io.File.createTempFile("yaml-test", null)
    val s =
      """
abcdef: |
      abc
      def
      g hi hi again
AnotherKey:
 key1: key value 1
 key2: key value 2
 Map parent:
    Map Child:
      mapkey1: MapChildVal1
      mapkey2:
        MapGrandChild:
         - MapGrandChildVal1
         - MapGrandChildVal2
      mapkey3:
        "MapChild1Val2"
    MapChild2 :
      intval: 33
      mapchild2: MapChild2 Val 1
            """
    tools.nsc.io.File(f).writeAll(s)
    val y = new YamlStruct(f.getAbsolutePath)
    info(y.getConfiguration.map { case (k, v) => s"$k=$v" }.mkString("\n"))
  }
}