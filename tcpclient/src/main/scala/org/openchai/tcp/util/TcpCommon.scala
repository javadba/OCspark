package org.openchai.tcp.util

import java.io.{ByteArrayOutputStream, File, FileOutputStream, ObjectOutputStream}
import java.nio.file.{Files, Path, Paths}

import org.openchai.tcp.xfer._

object TcpCommon {

  def serializeStream(a: Any): Array[Byte] = serializeObject(a)

  def serializeObject(a: Any): Array[Byte] = {
    // TODO: determine how to properly size the bos
    val bos = new ByteArrayOutputStream(2 ^ 20)
    val oos = new ObjectOutputStream(bos)
    println(s"serializeObject: a is ${a.toString}")
    if (a.isInstanceOf[XferWriteReq]) {
      println(s"serializeObject: XferWriteReq: datalen=${a.asInstanceOf[XferWriteReq].value.data.length}")
    } else {
      println(s"NOT XferWriteReq .. Type is ${a.getClass.getName}")
    }
    oos.writeObject(a)
    val out = bos.toByteArray
    //    Files.write(Paths.get("/tmp/xout"),out)
    //    val fs = new FileOutputStream(new File("/tmp/xout")).write(out)
    //    val test = deserializeObject(out)
    out
  }

  def deserializeStream(a: Array[Byte]): Any = deserializeObject(a)

  def deserializeObject(a: Array[Byte]): Any = {
    import java.io._
    // TODO: determine how to properly size the bos
    println(s"deserializeObject: inputlen=${a.length}")
    println(s"input bytes = ${new String(a.slice(0,100))}")
    val bis = new ByteArrayInputStream(a)
    println(s"bis length = ${bis.available}")
    val ois = new ObjectInputStream(bis)
    println(s"ois ${ois.available}")
    val o = ois.readObject
    o
  }

  def pack(/* path: DataPtr, */ o: Any): PackedData = {
    val ser = serializeObject(o)
    val md5 = FileUtils.md5(ser)
    ser // (path, ser, md5)
  }

  def unpack(raw: RawData): UnpackedData = {
    val packedAny = deserializeStream(raw)
    val packed = packedAny.asInstanceOf[PackedData]
//    FileUtils.checkMd5(packed)
    val obj = deserializeObject(packed)
    println(s"unpacked ${obj.getClass.getName}")
    obj
  }
}

