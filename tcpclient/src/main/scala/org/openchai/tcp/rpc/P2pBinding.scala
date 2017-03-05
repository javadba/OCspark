package org.openchai.tcp.rpc

trait P2pBinding {

  def bind(rpc: P2pRpc, serviceIf: ServiceIf) = {
    assert(rpc.isConnected)
    serviceIf.optRpc = Some(rpc)
  }

  val reconnectEveryRequest: Boolean = false
}
