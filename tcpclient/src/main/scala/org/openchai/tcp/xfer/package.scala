package org.openchai.tcp

import org.openchai.tcp.rpc.P2pResp

package object xfer {

  type RawData = Array[Byte]
  type DataPtr = String

  type AnyQEntry = Any

  type PackedData = (DataPtr, RawData, RawData)
  type UnpackedData = (DataPtr, Any, RawData)

  case class TaggedEntry(tag: String, data: Array[Byte])


}
