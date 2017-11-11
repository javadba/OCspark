package org.openchai.tensorflow

import java.util.concurrent.atomic.AtomicInteger

import org.openchai.util.TfConfig

object DirectSubmitter {

  import GpuClient._

  case class ThreadResult(nImagesProcessed: Int, output: String)

  val nImagesProcessed = new AtomicInteger(0)


  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      System.err.println("Usage: DirectSubmitter <submitter.yml file>")
      System.exit(-1)
    }
    val conf = TfConfig.getAppConfig(args(0))
    ImagesMeta.setExtWhiteList(if (conf.imageExtensions.isEmpty) None else Option(conf.imageExtensions))

    val gpuInfos = readGpus(DefaultSlavesPath)
    if (conf.isDirect ) {
      val svc = gpuClientService(FailoverInfo(conf, gpuInfos), conf.batchSize)
      Thread.currentThread.join
    } else {
//      val res = if (conf.isRest) {
//        labelImgViaRest(conf, LabelImgRest(Option(conf.restHostAndPort), conf.tfServerAndPort, conf.master, "TFViaRest", conf.imgApp,
//          conf.inDir, conf.outDir, conf.outTag, null))
//      } else {
//        labelImg(TfClient(conf), LabelImgRest(None, conf.master, conf.tfServerAndPort, "TFCommandLine", conf.imgApp, conf.inDir, conf.outDir, conf.outTag, readFileBytes(conf.inDir)))
//      }
//      info(res.toString)
    }
  }
}
