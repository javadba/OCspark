package org.openchai.tensorflow

case class GpuException(emsg: String, gpuNum: Option[Int] = None, tfServerAndHost: Option[String] = None, e: Option[Exception] = None)
  extends Exception(emsg) {

  override def getMessage = s"$emsg"
}
