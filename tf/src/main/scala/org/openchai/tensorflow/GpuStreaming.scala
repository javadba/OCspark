package org.openchai.tensorflow

import java.io.EOFException
import java.net.SocketException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Callable, Executors, TimeUnit, TimeoutException}

import org.openchai.tcp.util.FileUtils.{fileExt, fileName, writeBytes}
import org.openchai.tcp.util.Logger.{error, info}
import org.openchai.tcp.util.{ExecResult, FileUtils, TcpUtils}
import org.openchai.tensorflow.GpuClient._
import org.openchai.tensorflow.DirectSubmitter.ThreadResult

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}
import GpuLogger._

object GpuStreaming {

  error("WARN: setting DefaultLabelImgTimeoutSecs to 20: should be changed to 90+ in production")
  val DefaultLabelImgTimeoutSecs = 20  //TODO: change to 90+ when debugging over

  def GpuNumFromTag(tag: String) = {
    // TODO: implement this!
    error(s"GpuNumFromTag not yet implemented, returning 2 as a hack..")
    3
  }

  private val batchCtrMap = mutable.Map[Int,AtomicInteger]()

  def doBatch(tfClient: TfClient, gci: GpuClientInfo, batch: AB[ImgInfo], labelImgTimeoutSecs: Int = DefaultLabelImgTimeoutSecs) = {

    val gi = gci.gpuInfo
    val bdirs = batch.map(_.path).mkString(",")
    txDebug(gi.gpuNum, s"doBatch: files on $bdirs ready")

    val out = batch.map { b =>
      val ipath = b.path
      val contents = FileUtils.readFileBytes(ipath)
      val path = if (fileExt(ipath) == gi.gpuNum.toString) ipath.substring(0, ipath.lastIndexOf(".")) else ipath
      if (!ImagesMeta.allowsExt(fileExt(path))) {
        if (fileExt(path) != "result") {
          info(s"Skipping non image file $path")
        }
        None
      } else {
        val outputTag = s"${ TcpUtils.getLocalHostname}-$path"

        val imgLabel = LabelImgRest(None, gi.tfServerHostAndPort, s"Gpu-${gi.gpuNum}", gi.imgApp, path, gi.outDir, outputTag, contents)
        txDebug(gi.gpuNum, s"Running labelImage for $imgLabel")

        val execSvc = Executors.newSingleThreadExecutor
        val res = try {
          val future = execSvc.submit(new Callable[LabelImgResp]() {
            override def call() = {
              TfSubmitter.labelImg(tfClient, imgLabel)
            }
          })
          val res = future.get(labelImgTimeoutSecs,TimeUnit.SECONDS)
          if (res.value.cmdResult.isFatalError) {
            error(s"Got a FATAL error from gpu ${gi}: res=$res")
          }
          Some(res)
        } catch {
          case e: Exception =>
            txError(gi.gpuNum, s"Error on $imgLabel",e)
            e match {
              case t: TimeoutException =>
                Some(LabelImgResp(LabelImgRespStruct("TIMEOUT", path, gi.outDir, ExecResult(null, 99999, 999, "", "", true))))
              case s: SocketException =>
                Some(LabelImgResp(LabelImgRespStruct("SOCKETERROR", path, gi.outDir, ExecResult(null, 99999, 999, "", "", true))))
              case o: EOFException =>
                Some(LabelImgResp(LabelImgRespStruct("EOFException", path, gi.outDir, ExecResult(null, 99999, 999, "", "", true))))
              case e: Exception =>
                Some(LabelImgResp(LabelImgRespStruct(e.getClass.getSimpleName, path, gi.outDir, ExecResult(null, 99999, 999, "", "", true))))
            }
        } finally {
          execSvc.shutdownNow
        }

        txInfo(gi.gpuNum, s"LabelImage result: $res")
        res
      }
    }
    var fatalGpu = false
    val c = out.flatten
    c.foreach {
      li =>
        val fp = if (li.value.fpath.startsWith("file:")) {
          val f = li.value.fpath.substring("file:".length)
          val e = f.zipWithIndex.find {
            _._1 != '/'
          }.get._2
          '/' + f.substring(e)
        } else {
          li.value.fpath
        }
        val fname = fileName(fp)
        writeBytes(s"${ li.value.outDir}/$fname.result",
          (if (li.value.cmdResult.isFatalError) {
            fatalGpu = true
            "FATAL ERROR"
          } else {
            (li.value.cmdResult.stdout + li.value.cmdResult.stderr)}).getBytes("ISO-8859-1"))
    }
    val processed = c.map {
      _.value.nImagesProcessed
    }.sum
    txInfo(gi.gpuNum, s"Batch ${batchCtrMap.getOrElseUpdate(gi.gpuNum, new AtomicInteger).incrementAndGet} completed: processed=$processed")
    (ThreadResult(processed, out.mkString("\n")), fatalGpu)

  }

}
