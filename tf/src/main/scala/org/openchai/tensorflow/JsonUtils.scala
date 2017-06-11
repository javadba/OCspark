package org.openchai.tensorflow

import com.blazedb.spark.reports.YamlConf

object JsonUtils {
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
}
