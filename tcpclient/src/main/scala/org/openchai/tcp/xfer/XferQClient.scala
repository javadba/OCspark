package org.openchai.tcp.xfer

import org.openchai.tcp.util.TcpCommon

import java.util.concurrent.ArrayBlockingQueue

import org.openchai.tcp.util.Logger._
import org.openchai.tcp.xfer.XferConClient._

import reflect.runtime.universe.TypeTag

class XferQClient[T: TypeTag](val queue: ArrayBlockingQueue[T],
  val controllers: XferControllers) {

  def offer(t: T) =  queue.offer(t)

  val qclient = new Thread() {

    var canceled = false

    override def run(): Unit = {
      while (!canceled) {
        if (queue.peek() == null) {
          debug("XferQClient:  QReader is waiting ..")
        } else {
          debug("XferQClient:  QReader found an entry immediately")
        }
        val payload = queue.take
        debug(s"XferQClient: payload is $payload")
        val wparams = XferWriteParams(controllers.xferConf,
          TcpCommon.serialize(payload))
        val wres = controllers.client.write(controllers.xferConf, wparams)
        info(s"Xferqclient: QReader completed with resp=$wres")
      }
    }
  }
  qclient.start
}

case class Howdy(name: String, metric: Int)

object XferQClient {
  import org.openchai.tcp.xfer.XferConCommon._
  def main(args: Array[String]): Unit = {
    val nEntries = 20
    val q = new ArrayBlockingQueue[AnyQEntry](5)
    val controllers = makeXferControllers(TestControllers)
    val client = new XferQClient[AnyQEntry](q,controllers)
    val entries = for (i <- 1 until nEntries) yield {
      (Array("a", "b", "c"), TcpCommon.serialize(Howdy(s"Hi${i}!", i*i)))
    }

  }
}


