package org.openchai.tensorflow

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s.DefaultFormats._
import org.json4s.jackson.Serialization.write
import org.openchai.util.YamlConf
import org.openchai.tcp.util.Logger._

object JsonUtils {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  def parseJsonToMap(json: String): YamlConf = {
    parseJson(json).asInstanceOf[YamlConf]
  }

  def parseJson(json: String): Any = {
    import org.json4s._
    import org.json4s.native.JsonMethods._

    def explode(jv: JValue): Any = {
      jv.values match {
        case m: Map[_, _] =>
          m.keys.map { k =>
            val v = m(k)
            (k, explode(v.asInstanceOf[JValue]))
          }.toMap
        case s: Set[_] =>
          val s2 = s.asInstanceOf[Set[JValue]]
          s2.map(explode)
        case lst: Seq[_] =>
          val lst2 = lst.asInstanceOf[List[JValue]]
          lst2.map(explode)
        case o => o
      }
    }

    val jobj = parse(json)
    explode(jobj)
  }

  import scala.reflect.runtime.universe.TypeTag
  def parseJsonToCaseClass[A <: Product: Manifest](inJson: String): A = {
    val json1 = inJson.replace("\\","")
    val json = json1.substring(1,json1.length-1)  // Remove double quotes around whole json doc..
//    val x = """{"name":"john", "age": 28}"""
//    val jsValue = parse(x)
//    case class Person(name:String, age: Int)
//    val p = jsValue.extract[Person]
//    info(p.getClass.getName)
    try {
      val p = parse(json)
      p.extract[A]
    } catch {
      case t: Throwable =>
        t.printStackTrace
        null.asInstanceOf[A]
    }
  }

  def toJson(a: AnyRef) = {
    compact(write(a))
  }
}
