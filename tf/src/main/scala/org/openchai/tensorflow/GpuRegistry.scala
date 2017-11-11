package org.openchai.tensorflow

import org.openchai.tcp.rpc
import org.openchai.tcp.rpc._
import org.openchai.tcp.util.Logger._

object GpuRegistry {

  case class RegisterGpuStruct(host: String, port: Int)

  case class RegisterGpuReq(value: RegisterGpuStruct) extends P2pReq[RegisterGpuStruct]

  case class RegisterGpuRespStruct(registerGpuStruct: RegisterGpuStruct, numGpus: Int)

  case class RegisterGpuResp(val value: RegisterGpuRespStruct) extends P2pResp[RegisterGpuRespStruct]

  class RegistryIf extends ServiceIf("GpuRegistry") {
      def register(gpuHost: String, gpuPort: Int) = {
//      try {
        val resp = getRpc().request(RegisterGpuReq(RegisterGpuStruct(gpuHost, gpuPort)))
        info(s"RegisterGpu response: $resp")
        resp.asInstanceOf[RegisterGpuResp]
//      } catch {
//        case se: SocketException =>
//            error(s"SocketException on RegisterGpu", se)
//            throw se
//        case e: Exception => throw e
//      }
      }
    }

  def registerGpuAlternate(registryHost: String, registryPort: Int, gpuHost: String, gpuPort: Int) = {
    info(s"Registering gpu alternate $gpuHost:$gpuPort with  $registryHost:$registryPort ..")
    val tcpClient = new TcpClient(TcpParams(registryHost, registryPort), new RegistryIf())
    tcpClient.serviceIf.asInstanceOf[RegistryIf].register(gpuHost, gpuPort)
  }
}

case class GpuRegistry(host: String, port: Int, gpuFailoverService: GpuFailoverService) extends Thread {

  import GpuRegistry._

  val server = TcpServer(host, port, new ServerIf() {
    override def service(req: rpc.P2pReq[_]) = {
      req match {
        case o: RegisterGpuReq =>
          val struct = o.value
          val tfClientOpt = gpuFailoverService.registerAlternate(struct.host, struct.port)
          info(s"GpuRegistry: registered $struct: we got new config: " +
            s"${tfClientOpt.flatMap(t => Some(t.config)).getOrElse(" (none) ")}. Now we have ${gpuFailoverService.alternates.length} alternates")
          RegisterGpuResp(RegisterGpuRespStruct(struct, gpuFailoverService.gpus.length + gpuFailoverService.alternates.length))
        case _ =>
          throw new IllegalArgumentException(s"GpuRegsitry Unknown service type ${req.getClass.getName} on port ${port}")
      }
    }
  })
  server.start
}
