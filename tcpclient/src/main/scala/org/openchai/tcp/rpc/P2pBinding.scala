package org.openchai.tcp.rpc

trait P2pBinding {

  def bind(rpc: P2pRpc, serviceIf: ServiceIF) = {
    assert(rpc.isConnected)
    serviceIf.optRpc = Some(rpc)
  }

  val reconnectEveryRequest: Boolean = false
}
