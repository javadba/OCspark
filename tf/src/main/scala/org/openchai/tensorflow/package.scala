package org.openchai

package object tensorflow {

  type AnyQEntry = Any

  type CaosQEntry = (Array[String], Any)

//  type ConfMap = Map[String,Map[String,Map[String,Map[String,String]]]]
  type ConfMap = Map[String,_]

  type AnyMap = Map[String,_]

  type StringMap = Map[String,String]

  type MapMap = Map[String,AnyMap]

}
