package org.openchai.tensorflow

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import org.openchai.tcp.util.{FileUtils, Logger}
import org.openchai.tcp.util.Logger.{debug, error, info, warn}
import org.openchai.tensorflow.GpuClient.{GpuClientInfo, GpuFailoverService}
import org.openchai.tensorflow.SparkSubmitter.ThreadResult
import org.openchai.util.TfAppConfig

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}

object GpuClient {

  val DirReaderSleep =  100 // 250 // 5000  // TODO: set to 100 after debugging completed
  case class ImgInfo(tag: String, path: String)

  case class GpuInfo(gpuNum: Int, tfServerHostAndPort: String, imgApp: String, dir: String, outDir: String)

  case class GpuAlternate(tfServerHostAndPort: String)

  case class GpuClientInfo(gpuInfo: GpuInfo, conf: TfAppConfig, master: String, batchSize: Int, inQ: LinkedBlockingQueue[ImgInfo], outQ: LinkedBlockingQueue[ThreadResult])

  case class GpuStatus(gpuClientInfo: GpuClientInfo, status: String)

  case class ImgPartition(gpuNum: Int, imgs: AB[ImgInfo])

  var gpus: GpusInfo = _

  case class GpusInfo(gpus: Seq[GpuInfo], alternates: Seq[GpuAlternate])

  val DefaultSlavesPath = "/shared/gpu-slaves.txt"
  var slavesPath = DefaultSlavesPath

  def readGpus(slavesPath: String, inDir: String, outDir: String) = {
    val (slavesIter, alternatesIter) = scala.io.Source.fromFile(slavesPath)
      .getLines.map { l =>
      l.split("\\s+")
    }.partition(_.length > 1)
    val slaves = slavesIter.map { arr => GpuInfo(arr(0).toInt, arr(1), arr(2), if (arr.length >= 4) arr(3) else inDir, if (arr.length >= 5) arr(4) else outDir) }
      .toSeq
    val alternates = alternatesIter.map(s => GpuAlternate(s(0))).toSeq
    info(s"Slaves from $slavesPath: ${slaves.mkString(",")} alternates=${alternates.mkString(",")}")
    gpus = GpusInfo(slaves, alternates)
    gpus
  }

  type InDirGroups = Map[String, Map[Int, GpuClient]]

  private val processCtrMap = mutable.Map[Int, AtomicInteger]()
  def processImages(gpus: GpusInfo, clients: Map[Int, GpuClient], outDir: String, batchSize: Int) = {
    val inDirGroups: InDirGroups = clients.groupBy { case (g, gc) => gc.gi.gpuInfo.dir }.filter(entry => entry._1 != null && entry._1.length > 0)

    for (dirGroup <- inDirGroups) {
      val partitions = ImageHandler.getImageLists(gpus.gpus.filter { g => dirGroup._2.values.map(_.gi.gpuInfo).toSeq.contains(g) }, dirGroup._1, batchSize)
      partitions.foreach { p =>
        if (p.imgs.isEmpty && processCtrMap.getOrElseUpdate(p.gpuNum,new AtomicInteger(0)).getAndIncrement % 100 == 0) {
          txDebug(p.gpuNum, s"No images found ${processCtrMap(p.gpuNum).get}")
        } else if (p.imgs.nonEmpty && processCtrMap.contains(p.gpuNum)) {
          processCtrMap(p.gpuNum).set(0)
        }
        p.imgs.foreach {
          i =>
            txDebug(p.gpuNum,s"Enqueueing image ${i.path} ..")
            val newName = s"${new File(i.path).getParent}/processing/${FileUtils.fileName(i.path)}"
            FileUtils.mkdirs(new File(newName).getParent)
            FileUtils.mv(i.path, newName)
            clients(p.gpuNum).gi.inQ.offer(i.copy(path=newName))
        }
      }
    }
  }

  def gpuClientService(gpusInfo: GpusInfo, conf: TfAppConfig, master: String, batchSize: Int) = {

    val gpuClientInfos = gpusInfo.gpus.map { gi =>
      GpuClientInfo(gi, conf, master, batchSize, new LinkedBlockingQueue[ImgInfo](), new LinkedBlockingQueue[ThreadResult]())
    }
    val gpuFailoverService = new GpuFailoverService(gpus)
    val clients /*: InDirGroups */ = gpuClientInfos.map {
      gpu => (gpu.gpuInfo.gpuNum, GpuClient(gpu,gpuFailoverService))
    }.toMap

    clients.values.foreach{ _.start }

    case class ImageWriterThread(outDir: String) extends Thread {
      val nGpus = gpus.gpus.length

      override def run() = {
        info(s"Starting  ImageWriterThread with ${clients.size} gpu clients and writing to $outDir..")

        clients.foreach { case (gpuNum, c) =>
          val inDirProcessing = new File(s"${c.gi.gpuInfo.dir}/processing")
          if (inDirProcessing.exists && inDirProcessing.list.nonEmpty) {
            val outDir = s"/tmp/imageProcessing/$gpuNum" // /processing"
            FileUtils.mkdirs(outDir)
            txError(gpuNum, s"Found non-empty processing dir $inDirProcessing: will move entries to $outDir..")
            inDirProcessing.list.foreach( f => FileUtils.mv(f, outDir))
          }
        }
        var underflowReported = false
        val reporterCounter = new AtomicInteger(0)
        while (true) {
          try {
            processImages(gpus, clients, conf.outDir, batchSize)
            if (gpuFailoverService.alternatesUnderflow /* && !underflowReported */) {
              underflowReported = true
              if (reporterCounter.getAndIncrement % 1 /* 10 */ == 0) {
                throw GpuException(s"GPU Failure Alert on ${gpuFailoverService.broken.mkString(",")}: insufficient alternates available!")
              }
            }
          } catch {
            case e: Exception =>
              error(s"processImages unexpected failure", e)
          } finally {
            Thread.sleep(DirReaderSleep)
          }
        }
      }

      this.start
    }
    val inDirThread = ImageWriterThread(conf.outDir)
    inDirThread.join
  }

  class GpuFailoverService(gpuInfos: GpusInfo) {
    val broken = AB[GpuInfo]()
    val alternates = AB[GpuAlternate](gpus.alternates:_*)
    val newGpus = AB[GpuInfo]()
    var alternatesUnderflow = false
    def fail(gci: GpuClientInfo): Option[GpuClientInfo] = this.synchronized{
      val gi = gci.gpuInfo
      error(s"Got a BROKEN GPU $gi")
      if (alternates.isEmpty) {
        alternatesUnderflow = true
        throw new IllegalStateException(s"No more alternates available. We're cooked.")
      } else {
        val brokenGpu = gpus.gpus.find { g => g.gpuNum == gi.gpuNum }.head
        assert(brokenGpu!=null,s"Why did we not find the brokenGpu for ${gi.gpuNum} in ${gpus.gpus.map{_.gpuNum}.mkString(",")} ?")
        broken += brokenGpu
        val alt = alternates.head
        val newGpu = brokenGpu.copy(tfServerHostAndPort = alt.tfServerHostAndPort)
        newGpus += newGpu
        alternates -= alt
        warn(s"Successfully failed over from $brokenGpu to $newGpu")
        gpus = gpus.copy(gpus = gpus.gpus.filter(_.gpuNum != gci.gpuInfo.gpuNum) :+ newGpu, alternates = alternates.filter(_.tfServerHostAndPort!=alt.tfServerHostAndPort))
        Some(gci.copy(gpuInfo = newGpu))
      }
    }

  }

  import reflect.runtime.universe.TypeTag
  trait ProcessorIf {
    def process[T: TypeTag,U: TypeTag](t: T): U
  }

  class QReader(q: LinkedBlockingQueue[ThreadResult], processor: ProcessorIf) extends Thread {
    override def run() = {
      while (true) {
        val tr = q.poll
        debug(tr)
        val result = processor.process(tr.output)
      }
    }
  }

  def bashColors = Array(92, 91, 94, 95, 96, 32, 33, 35, 36, 97, 37,93)

  import org.openchai.tcp.util.Logger._
  def txDebug(tx1: Int, msg: String) = {
    def debug(msg: Any) = if (LogLevel >= 3) println(s"$msg")

    debug(("\n" + s"DEBUG: ${f2(msg)}" + "\033[0m").replace("\n", s"\n\033[${bashColors(tx1 - 1)}m TX$tx1 >> "))
  }

  def txInfo(tx1: Int, msg: String) = {
    def info(msg: Any) = if (LogLevel >= 2) println(s"$msg")
    info(("\n" + s"INFO: ${f2(msg)}" + "\033[0m").replace("\n", s"\n\033[${bashColors(tx1 - 1)}m TX$tx1 >> "))
  }

  def txError(tx1: Int, msg: String, t: Throwable = null) = {
    def error(msg: Any, t: Throwable) = if (LogLevel >= 0) println(msg + (if (t!=null) s"\n${Logger.toString(t)}" else ""))
    error(("\n" + s"ERROR: ${f2(msg)}" + "\033[0m").replace("\n", s"\n\033[${bashColors(tx1 - 1)}m TX$tx1 >> "),t)
  }

  def mvBatch(batch: AB[ImgInfo], toDir: String, findGrandparent: Boolean = false) = {
    for (img <- batch) {
      val destDir = (if (findGrandparent) {new File(img.path).getParentFile.getParent} else new File(img.path).getParent) + toDir
      FileUtils.mkdirs(destDir)
      FileUtils.mv(img.path, destDir + "/" + FileUtils.fileName(img.path))
    }

  }
}

case class GpuClient(inGi: GpuClientInfo, failoverService: GpuFailoverService) extends Thread {
  val QTimeOut = 200
  import GpuClient._
  var gi = inGi
  val gpuNum = gi.gpuInfo.gpuNum
  override def run() = {
    info(s"Starting GpuClient ${gi.gpuInfo}..")
    val empty = new java.util.concurrent.atomic.AtomicInteger
    val inQ = gi.inQ
    val (tx1Host, tx1Port) = (gi.gpuInfo.tfServerHostAndPort.split(":")(0), gi.gpuInfo.tfServerHostAndPort.split(":")(1).toInt)
    val tfClient = try {
      TfClient(inGi.conf, tx1Host, tx1Port)
    } catch {
      case e: Exception =>
        throw new IllegalStateException(s"Unable to connect to $tx1Host:$tx1Port", e)
    }

    while (true) {
      var cntr = 0
      val batch = AB[ImgInfo]()
      var timeout = false
      while (!timeout && cntr < gi.batchSize) {
        val entry = inQ.poll(QTimeOut, TimeUnit.MILLISECONDS)
        if (entry != null) {
          batch += entry
          cntr += 1
        } else {
          timeout = true
        }
      }
      if (batch.nonEmpty) {
        txDebug(gpuNum, s"Found nonempty batch of size ${batch.length}")
        try {
          val (res, fatalGpu) = GpuStreaming.doBatch(tfClient, gi, batch)
          if (fatalGpu) {
            gi = failoverService.fail(gi).getOrElse(
              throw new IllegalStateException(s"Failover failed for batch ${batch.mkString(",")} on $gi. Can not proceed"))
            mvBatch(batch, "", true)
          } else {
            gi.outQ.put(res)
            mvBatch(batch, "/completed", true)
            ImageHandler.prepImages(ImgPartition(gpuNum, batch), gi.gpuInfo.outDir)
          }
        } catch {
          case e: Exception =>
            txError(gpuNum, s"GpuClient.run failed for batch=${batch.mkString(",")}", e)
            mvBatch(batch, "", true)
        }
      } else {
        if (empty.getAndIncrement % 100 == 0) {
          txDebug(gpuNum, s"Empty batch ${empty.get + 1}")
        }
      }
    }
  }

}
