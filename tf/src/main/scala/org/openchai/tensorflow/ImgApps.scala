package org.openchai.tensorflow

sealed trait ImgApps {
  val name: String
}

case object TensorflowApp extends ImgApps {
  override val name = "tensorflow-labelImage"
}

case object DarknetApp extends ImgApps {
  override val name = "darknet-labelImage"
}