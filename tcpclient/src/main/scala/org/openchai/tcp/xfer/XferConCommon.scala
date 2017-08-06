package org.openchai.tcp.xfer

import org.openchai.tcp.rpc.TcpParams
import org.openchai.tcp.util.TcpUtils

object XferConCommon {

  val LoremIpsum =
    """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur et nibh sagittis, aliquam metus nec, faucibus neque.
      Proin ullamcorper facilisis turpis at sollicitudin. Mauris facilisis, turpis ut facilisis rhoncus, mi erat ultricies ligula,
      id pretium lacus ante id est. Morbi at lorem congue, consectetur nunc vel, facilisis urna. Fusce mollis pulvinar sagittis.
      Aenean eu imperdiet augue. Morbi sit amet enim tristique nisl tristique efficitur vitae eget diam. Vestibulum non metus eros.

Nulla in nunc interdum, pulvinar dolor et, euismod leo. Mauris at enim nec felis hendrerit porttitor nec vel lorem. Duis a euismod
augue. Maecenas scelerisque, ipsum placerat suscipit ultricies, odio nulla laoreet ex, a varius lacus sem quis sapien.
Aliquam condimentum tellus id tempus posuere. Nullam volutpat, tellus in euismod hendrerit, dui lacus fermentum quam,
a pretium nisi eros a nisl. Duis vehicula eros sit amet nunc fermentum, vel faucibus erat ornare. Suspendisse sed
ligula scelerisque, lobortis est sit amet, dignissim leo. Ut laoreet, augue non efficitur egestas, justo lorem faucibus
    """.stripMargin

  case class XferControllerArgs(conHost: String, conPort: Int, dataHost: String, dataPort: Int, appHost:String, appPort: Int,
    configFile: String, data: Array[Byte], outboundDataPaths: (String,String), inboundDataPath: String)


	val server = "*" // server
  val TestControllers = XferControllerArgs(server, 61234,server, 61235, server, 61236,
    "foobar.properties", LoremIpsum.getBytes("ISO-8859-1"), ("/tmp/xferout1", "/tmp/xferout2"), "/tmp/xferin")

  val AppTcpArgs = TcpParams(server, 61236)

  def remoteControllers(server: String, port: Int = 0)  = {
    val base = if (port > 0) port else 61234
    XferControllerArgs(server, base, server, base+1, server, base+2,
      "foobar.properties", LoremIpsum.getBytes("ISO-8859-1"), ("/tmp/xferout1", "/tmp/xferout2"), "/tmp/xferin")
  }

  def remoteTcpArgs(server: String, port:Int=61236) = TcpParams(server, port)

}
