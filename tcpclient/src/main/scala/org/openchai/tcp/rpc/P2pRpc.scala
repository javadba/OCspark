package org.openchai.tcp.rpc

trait P2pRpc {

  import reflect.runtime.universe.TypeTag

  def connect(connParam: P2pConnectionParams): Boolean
  def isConnected: Boolean

  def request[U: TypeTag, V: TypeTag](req: P2pReq[U]): P2pResp[V]

}
