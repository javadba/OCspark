package org.openchai.tcp

import org.openchai.tcp.rpc.P2pResp
import org.openchai.tcp.util.FileUtils

package object xfer {

  type RawData = Array[Byte]
  type DataPtr = String

  type AnyQEntry = Any

  type PackedData = RawData
//  type PackedData = (DataPtr, RawData, RawData)
//  type UnpackedData = (DataPtr, Any, RawData)
  type UnpackedData = Any

  case class TaggedEntry(tag: String, data: Array[Byte])

  case class TypedEntry[T](tag: String, t: T)

  case class XferWriteParams(tag: String, config: XferConfig, data: RawData, md5: RawData) {

    override def toString: DataPtr = s"XferWriteParams: config=$config datalen=${data.length}} md5len=${md5.length}}"
  }

  object XferWriteParams {
    def apply(tag: String, config: XferConfig, data: RawData) = new XferWriteParams(tag, config, data, FileUtils.md5(data))
  }


}
