package org.openchai.tensorflow

import java.util.concurrent.{Callable, Executors, TimeUnit, TimeoutException}

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.FileUtils.{fileExt, fileName, writeBytes}
import org.openchai.tcp.util.Logger.{error, info}
import org.openchai.tcp.util.{ExecParams, ExecResult, TcpUtils}
import org.openchai.tensorflow.GpuClient.{GpuClientInfo, ImgInfo}
import org.openchai.tensorflow.SparkSubmitter.ThreadResult
import GpuClient.{mvBatch, txDebug, txInfo}

import scala.collection.mutable.{ArrayBuffer => AB}

object GpuSparkJob {
  val DefaultLabelImgTimeoutSecs = 40

  def GpuNumFromTag(tag: String) = {
    // TODO: implement this!
    error(s"GpuNumFromTag not yet implemented, returning 2 as a hack..")
    3
  }

  def runSparkJob(gci: GpuClientInfo, batch: AB[ImgInfo], labelImgTimeoutSecs: Int = DefaultLabelImgTimeoutSecs) = {

    val gi = gci.gpuInfo
    txDebug(gi.gpuNum, s"Connecting to spark master ${gci.master} ..")
    val spark = SparkSession.builder.master(gci.master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val rdds = batch.map { b =>
      sc.binaryFiles(b.path, 1)
    }
    val irdd = sc.union(rdds)
    val bdirs = batch.map(_.path).mkString(",")
    txDebug(gi.gpuNum, s"runsparkJob: binary files rdd on $bdirs ready")

    val bcGci = sc.broadcast(gci)
    val out = irdd.mapPartitionsWithIndex {
      case (np, part) =>
        val gci = bcGci.value
        val gi = gci.gpuInfo
        val (tx1Host, tx1Port) = (gi.tfServerHostAndPort.split(":")(0), gi.tfServerHostAndPort.split(":")(1).toInt)
        val tfClientOpt = try {
          Option(TfClient(gci.conf, tx1Host, tx1Port))
        } catch {
          case e: Exception =>
            error(s"Unable to connect to $tx1Host:$tx1Port", e)
            None
        }

        if (tfClientOpt.nonEmpty) {
          val tfClient = tfClientOpt.get
          part.map {
            case (ipath, contents) =>
              val path = if (fileExt(ipath) == gi.gpuNum.toString) ipath.substring(0, ipath.lastIndexOf(".")) else ipath
              if (path.endsWith("result.result")) {
                error(s"Got a double result path $path")
              }
              if (!ImagesMeta.allowsExt(fileExt(path))) {
                if (fileExt(path) != "result") {
                  info(s"Skipping non image file $path")
                }
                None
              } else {
                val outputTag = s"${
                  TcpUtils.getLocalHostname
                }-Part$np-$path"
                val imgLabel = LabelImgRest(None, gci.master, gi.tfServerHostAndPort, s"SparkPartition-${
                  gi.gpuNum
                }-$np", gi.imgApp, path, gi.outDir, outputTag, contents.toArray)
                txDebug(gi.gpuNum, s"Running labelImage for $imgLabel")

                val execSvc = Executors.newSingleThreadExecutor
                val future = execSvc.submit(new Callable[LabelImgResp]() {
                  override def call() = {
                    TfSubmitter.labelImg(tfClient, imgLabel)
                  }
                })
                val res = try {
                  val res = future.get(labelImgTimeoutSecs,TimeUnit.SECONDS)
                  if (res.value.cmdResult.isFatalError) {
                    error(s"Got a FATAL error from gpu ${gi}: res=$res")
                  }
                  Some(res)
                } catch {
                  case t: TimeoutException =>
                    Some(LabelImgResp(LabelImgRespStruct("TIMEOUT",path, gi.outDir,ExecResult(null,99999,999,"","",true))))
                } finally {
                  execSvc.shutdownNow
                }

                txInfo(gi.gpuNum, s"LabelImage result: $res")
                res
              }
          }
        } else {
          part.map {
            _ =>
              None
          }
        }
    }

    val c = out.collect.flatten
    var fatalGpu = false
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
            (li.value.cmdResult.stdout + li.value.cmdResult.stderr)
          }).getBytes("ISO-8859-1"))
    }
    val processed = c.map {
      _.value.nImagesProcessed
    }.sum
    info(s"Finished runSparkJob for gi.gpuNum=${gi.gpuNum} processed=$processed")
    (ThreadResult(processed, c.mkString("\n")), fatalGpu)

  }

}
