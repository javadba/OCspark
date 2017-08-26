package org.openchai.util

//import org.json4s.DefaultFormats
//import org.json4s._
//import org.json4s.jackson.Serialization
//import org.json4s.jackson.Serialization.read
//import org.json4s.native.JsonMethods._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class Prop(url: List[String])

object YamlUtils {

//  def extract[T: Manifest](strJson: String) = {
//    implicit val formats = DefaultFormats
//    val jobj = parse(strJson)
//    jobj.extract[T]
//  }

  import scala.reflect._
  def toScala[T](strJson: String)(implicit ctag: ClassTag[T]): T = {
    val mapper: ObjectMapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(strJson, ctag.runtimeClass.asInstanceOf[Class[T]])
  }
}




