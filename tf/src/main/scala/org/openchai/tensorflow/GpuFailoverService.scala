package org.openchai.tensorflow

import java.io.File

import org.openchai.tcp.util.Logger.{debug, error, warn}
import org.openchai.tcp.util.{FileUtils, Logger}
import org.openchai.tensorflow.GpuClient.{GpuAlternate, GpuClientInfo, GpuInfo, GpusInfo}
import GpuLogger._
import org.openchai.tcp.rpc.TcpParams
import org.openchai.util.TfAppConfig

import scala.collection.mutable.{ArrayBuffer => AB}

case class FailoverInfo(appConfig: TfAppConfig, gpuInfos: GpusInfo)

class GpuFailoverService(failoverInfo: FailoverInfo, tfClientFailoverCallback: TfClientFailoverCallback) extends GpuRegistrationService {
  val appConfig = failoverInfo.appConfig
  val gpuInfos = failoverInfo.gpuInfos
  val alternates = AB[GpuAlternate](gpuInfos.alternates: _*)
  var alternatesUnderflow = alternates.isEmpty
  private val _broken = AB[GpuInfo]()
  private val newGpus = AB[GpuInfo]()
  private val _gpus = AB[GpuInfo](gpuInfos.gpus:_*)

  override def gpus = Seq(_gpus:_*)

  override def broken = Seq(_broken:_*)

  override def registerAlternate(host: String, port: Int): Option[TfClient] = {
    val hostPort = s"$host:$port"
    val optTfClient = if (_gpus.map(_.tfServerHostAndPort).contains(hostPort)) {
      warn(s"RegisterAlternate: we already have an active gpu with $hostPort")
      None
    } else if (alternates.map(_.tfServerHostAndPort).contains(hostPort)) {
      warn(s"RegisterAlternate: we already have an alternate gpu with $hostPort")
      None
    } else {
      val alternate = GpuAlternate(hostPort)
      if (_broken.nonEmpty) {
        val toRecover = _broken.remove(0)
        alternates += alternate
        val gpuInfo = activate(alternate, toRecover)
        val tfClient = TfClient(appConfig, gpuInfo.parseTfServerHostAndPort._1, gpuInfo.parseTfServerHostAndPort._2)
        tfClientFailoverCallback.failoverClient(gpuInfo.gpuNum, tfClient)
        warn(s"Successfully replaced broken GPU $toRecover with alternate GPU $hostPort")
        Some(tfClient)
      }
      else {
        alternates += alternate
        warn(s"Successfully added alternate GPU $hostPort")
        None
      }
    }
    optTfClient
  }

  def activate(gpuAlternate: GpuAlternate, baseGpu: GpuInfo) = {
    assert(alternates.contains(gpuAlternate),s"Alternates did not contain $gpuAlternate ?")
    alternates -= gpuAlternate
    val gpuInfo = GpuInfo(gpuAlternate, baseGpu)
    _gpus += gpuInfo
    gpuInfo
  }

  def fail(gci: GpuClientInfo, msg: String, e: Option[Exception] = None) = this.synchronized {
    val gi = gci.gpuInfo
    if (broken.map(_.gpuNum).contains(gi.gpuNum)) {
      warn(s"Got another FAIL on the BROKEN GPU $gi - $msg ${e.map(Logger.toString(_))}")
      (gci, false)
    } else {
      error(s"Got a BROKEN GPU $gi - $msg ${e.map(Logger.toString(_))}")
      val brokenGpu = _gpus.find { g => g.gpuNum == gi.gpuNum }.head
      assert(brokenGpu != null, s"Why did we not find the brokenGpu for ${gi.gpuNum} in ${
        _gpus.map {
          _.gpuNum
        }.mkString(",")
      } ?")
      _gpus -= brokenGpu
      if (alternates.isEmpty) {
        _broken += brokenGpu
        throw new IllegalStateException(s"No more alternates available. We're cooked.")
      } else {
        val alt = alternates.head
        val newGpu = brokenGpu.copy(tfServerHostAndPort = alt.tfServerHostAndPort)
        newGpus += newGpu
        alternates -= alt
        //      alternates -= alternates.filter(_.tfServerHostAndPort != alt.tfServerHostAndPort).head
        warn(s"Successfully failed over from $brokenGpu to $newGpu")
        _gpus += newGpu
        debug(s"gpus=${gpus.mkString(",")} alternates=${alternates.mkString(",")}")
        val newClientInfo = gci.copy(gpuInfo = newGpu)
        resetFiles(newGpu.gpuNum, newClientInfo)
        (newClientInfo,true)
      }
    }
  }

  def resetFiles(gpuNum: Int, c: GpuClientInfo) = {
    val inDirProcessing = new File(s"${c.gpuInfo.dir}/processing")
    if (inDirProcessing.exists && inDirProcessing.list.nonEmpty) {
      val startDir = c.gpuInfo.dir
      txError(gpuNum, s"Found non-empty processing dir $inDirProcessing: will move entries to $startDir..")
      inDirProcessing.list.foreach(f => FileUtils.mv(s"$inDirProcessing/$f", s"$startDir/${FileUtils.fileName(f)}"))
    }
  }

  def checkProcessingDir(gpuNum: Int, c: GpuClient) = {
    val inDirProcessing = new File(s"${c.gci.gpuInfo.dir}/processing")
    if (inDirProcessing.exists && inDirProcessing.list.nonEmpty) {
      //          val outDir = s"/tmp/imageProcessing/$gpuNum" // /processing"
      val outDir = c.gci.gpuInfo.dir // /processing"
      FileUtils.mkdirs(outDir)
      txError(gpuNum, s"Found non-empty processing dir $inDirProcessing: will move entries to $outDir..")
      inDirProcessing.list.foreach(f => FileUtils.mv(s"$inDirProcessing/$f", s"$outDir/${FileUtils.fileName(f)}"))
    }
  }
}

trait TfClientFailoverCallback {
  def failoverClient(gpuNum: Int, newTfClient: TfClient)
}
