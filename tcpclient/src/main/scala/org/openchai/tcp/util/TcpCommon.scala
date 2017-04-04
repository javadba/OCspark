package org.openchai.tcp.util

import java.io.{ByteArrayOutputStream, File, FileOutputStream, ObjectOutputStream}
import java.nio.file.{Files, Path, Paths}

import org.openchai.tcp.xfer.{DataPtr, PackedData, RawData, UnpackedData}

object TcpCommon {

  def serializeStream(a: Any): Array[Byte] = serializeObject(a)

  def serializeObject(a: Any): Array[Byte] = {
    // TODO: determine how to properly size the bos
    val bos = new ByteArrayOutputStream(2 ^ 22)
    val oos = new ObjectOutputStream(bos)
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
    val bis = new ByteArrayInputStream(a)
    val ois = new ObjectInputStream(bis)
    val o = ois.readObject
    o
  }

  def pack(path: DataPtr, o: Any): PackedData = {
    val ser = serializeObject(o)
    val md5 = FileUtils.md5(ser)
    (path, ser, md5)
  }

  def unpack(raw: RawData): UnpackedData = {
    val packedAny = deserializeStream(raw)
    val packed = packedAny.asInstanceOf[PackedData]
    FileUtils.checkMd5(packed)
    val obj = deserializeObject(packed._2)
    println(s"unpacked ${obj.getClass.getName}")
    (packed._1, obj, packed._3)
  }
}
