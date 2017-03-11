package org.openchai.tcp.util

import java.io.{ByteArrayOutputStream, File, FileOutputStream, ObjectOutputStream}
import java.nio.file.{Files, Path, Paths}

object TcpCommon {
  def serialize(a: Any): Array[Byte] = {
    // TODO: determine how to properly size the bos
    val bos = new ByteArrayOutputStream(2 ^ 18)
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(a)
    val out = bos.toByteArray
//    Files.write(Paths.get("/tmp/xout"),out)
    val fs = new FileOutputStream(new File("/tmp/xout")).write(out)
    val test = deserialize(out)
    if (!a.isInstanceOf[Array[Float]]) {
      val b  = a
    }
    out
  }

  def deserialize(a: Array[Byte]): Any = {
    import java.io._
    // TODO: determine how to properly size the bos
    val bis = new ByteArrayInputStream(a)
    val ois = new ObjectInputStream(bis)
    ois.readObject
  }

}
