package or.openchai

package object tf {

  type AnyQEntry = Any

  case class TaggedEntry(tag: String, data: Array[Byte])

  type CaosQEntry = (Array[String], Any)

}
