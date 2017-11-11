package org.openchai.tensorflow

import org.openchai.tensorflow.GpuClient.GpuInfo


trait GpuRegistrationService {
  def registerAlternate(host: String, port: Int): Option[TfClient]
  def gpus: Seq[GpuInfo]
  def broken: Seq[GpuInfo]
}
