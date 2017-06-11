package com.blazedb.spark.reports

import java.io.FileInputStream

import org.yaml.snakeyaml.Yaml

trait YamlConf {

  def apply(key: String): Any

  def apply(key: String, default: String): Any

  def toMap(key: String): Map[String,_]
}

case class YamlStruct(ymlFile: String) extends YamlConf {
  val yaml = new Yaml

  import collection.JavaConverters._
  import collection.mutable

  def expand(o: Any): Any = {
    o match {
      case lst: java.util.List[_] =>
        Seq(lst.asScala.map{ o => expand(o) })
      case m: java.util.Map[String, _] =>
        m.asScala.map { case (k, v) =>
          (k, expand(v))
        }.toMap
      case _ =>
//        println("o type is %s".format(o.getClass.getName))
        o
    }
//          omap.++=(sm.iterator)
  }
  val yamlConf = {
    val iy = (for (y <- yaml.loadAll(new FileInputStream(ymlFile)).iterator.asScala) yield y).toList.head // toMap[String,Any]
//    val omap = new mutable.HashMap[String, _]()
    val omap = expand(iy).asInstanceOf[Map[String,_]]
    println(s"omap: ${omap}")
    omap
//    collection.immutable.HashMap[String,String](omap.iterator.toSeq:_*)
  }

  def getConfiguration = yamlConf

  override def toString() = {
    getConfiguration.mkString(",")
  }


  override def toMap(key: String) = apply(key).asInstanceOf[Map[String,_]]

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
    val s = """
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
    println(y.getConfiguration.map{ case (k,v) => s"$k=$v"}.mkString("\n"))
  }
}