package org.openchai.tensorflow

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import org.openchai.tcp.util.Logger.{debug, error, info, warn}
import org.openchai.tcp.util.{FileUtils, Logger}
import org.openchai.tensorflow.GpuClient.GpuClientInfo
import org.openchai.tensorflow.DirectSubmitter.ThreadResult
import org.openchai.util.TfAppConfig

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}
import GpuLogger._

object GpuClient {

  val DirReaderSleep = 100 // 250 // 5000  // TODO: set to 100 after debugging completed
  case class ImgInfo(tag: String, path: String)

  case class GpuInfo(gpuNum: Int, tfServerHostAndPort: String, imgApp: String, dir: String, outDir: String) {

    def parseTfServerHostAndPort = (tfServerHostAndPort.split(":")(0), tfServerHostAndPort.split(":")(1).toInt)

  }

  object GpuInfo {
    def apply(gpuAlternate: GpuAlternate, baseGpu: GpuInfo) = baseGpu.copy(tfServerHostAndPort = gpuAlternate.tfServerHostAndPort)
  }

  case class GpuAlternate(tfServerHostAndPort: String)

  case class GpuClientInfo(gpuInfo: GpuInfo, conf: TfAppConfig, batchSize: Int, inQ: LinkedBlockingQueue[ImgInfo], outQ: LinkedBlockingQueue[ThreadResult])

  case class GpuStatus(gpuClientInfo: GpuClientInfo, status: String)

  case class ImgPartition(gpuNum: Int, imgs: AB[ImgInfo])

  var gpus: GpusInfo = _

  case class GpusInfo(gpus: Seq[GpuInfo], alternates: Seq[GpuAlternate])

  val DefaultSlavesPath = "/shared/gpu-slaves.txt"
  var slavesPath = DefaultSlavesPath

  def readGpus(slavesPath: String /*, inDir: String, outDir: String*/) = {
    val (slavesIter, alternatesIter) = scala.io.Source.fromFile(slavesPath)
      .getLines
      .map(_.trim)
      .filter(l => l.length > 0 && !l.startsWith("#"))
      .map { l =>
        l.split("\\s+")
      }.partition(_.length > 1)
    val slaves = slavesIter.map { arr => GpuInfo(arr(0).toInt, arr(1), arr(2), if (arr.length >= 4) arr(3) else "", if (arr.length >= 5) arr(4) else "") }
      .toSeq
    val alternates = alternatesIter.map(s => GpuAlternate(s(0))).toSeq
    info(s"Slaves from $slavesPath: ${slaves.mkString(",")} alternates=${alternates.mkString(",")}")
    gpus = GpusInfo(slaves, alternates)
    gpus
  }

  type InDirGroups = Map[String, Map[Int, GpuClient]]

  private val processCtrMap = mutable.Map[Int, AtomicInteger]()

  def processImages(gpus: Seq[GpuInfo], clients: Map[Int, GpuClient], batchSize: Int) = {
    val inDirGroups: InDirGroups = clients.groupBy { case (g, gc) => gc.gci.gpuInfo.dir }.filter(entry => entry._1 != null && entry._1.length > 0)
    for (dirGroup <- inDirGroups) {
      val inGpus = gpus.filter { g => dirGroup._2.values.map(_.gci.gpuInfo.gpuNum).toSeq.contains(g.gpuNum) }
      val partitions = ImageHandler.getImageLists(inGpus, dirGroup._1, batchSize)
      partitions.foreach { p =>
        if (p.imgs.isEmpty && processCtrMap.getOrElseUpdate(p.gpuNum, new AtomicInteger(0)).getAndIncrement % 100 == 0) {
          txDebug(p.gpuNum, s"No images found ${processCtrMap(p.gpuNum).get}")
        } else if (p.imgs.nonEmpty && processCtrMap.contains(p.gpuNum)) {
          processCtrMap(p.gpuNum).set(0)
        }
        p.imgs.foreach {
          i =>
            txDebug(p.gpuNum, s"Enqueueing image ${i.path} ..")
            val newName = s"${new File(i.path).getParent}/processing/${FileUtils.fileName(i.path)}"
            FileUtils.mkdirs(new File(newName).getParent)
            FileUtils.mv(i.path, newName)
            if (!gpus.map(_.gpuNum).contains(p.gpuNum)) {
              txDebug(p.gpuNum, s"NOT enqueuing image ${i.path} since gpu is DEAD")
            } else {
              clients(p.gpuNum).gci.inQ.offer(i.copy(path = newName))
            }
        }
      }
    }


  }

  //  def connectClient(tfClient: TfClient, inGci: GpuClientInfo, failoverService: GpuFailoverService, tx1Host: String, tx1Port: Int)
  //  : (TfClient, GpuClientInfo) =
  //    try {
  //      (TfClient(inGci.conf, tx1Host, tx1Port), inGci)
  //
  //    } catch {
  //      case e: Exception =>
  //        val (gci, failedOver) = failoverService.fail(inGci, s"Unable to connect to $tx1Host:$tx1Port", Some(e))
  //        val newTfClient = TfClient(gci.conf, gci.gpuInfo.parseTfServerHostAndPort._1, gci.gpuInfo.parseTfServerHostAndPort._2)
  //        (tfClient, gci)
  //    }

  def gpuClientService(failoverInfo: FailoverInfo, batchSize: Int) = {

    val conf = failoverInfo.appConfig
    val gpusInfo = failoverInfo.gpuInfos

    val gpuClientInfos = gpusInfo.gpus.map { gi =>
      GpuClientInfo(gi, conf, batchSize, new LinkedBlockingQueue[ImgInfo](), new LinkedBlockingQueue[ThreadResult]())
    }

    var clients: Map[Int, GpuClient] = null

    val tfClientFailoverCallback = new TfClientFailoverCallback {

      override def failoverClient(gpuNum: Int, newTfClient: TfClient) = {
        val optTfClient = clients.find(_._2.gpuNum == gpuNum)
        assert(optTfClient.nonEmpty, s"Why were we unable to find tfClient with gpuNum=$gpuNum ?")
        optTfClient.get._2.tfClient = newTfClient
      }
    }

    val gpuFailoverService = new GpuFailoverService(new FailoverInfo(conf, gpus), tfClientFailoverCallback)

    clients = gpuClientInfos.map {
      gpu => (gpu.gpuInfo.gpuNum, GpuClient(gpu, gpuFailoverService))
    }.toMap


    val gpuRegistry = GpuRegistry(conf.gpuRegistryHost, conf.gpuRegistryPort, gpuFailoverService)

    clients.values.foreach {
      _.start
    }

    case class ImageWriterThread() extends Thread {
      val nGpus = gpus.gpus.length
      val errorCtr = new AtomicInteger(0)

      override def run() = {
        info(s"Starting  ImageWriterThread with ${clients.size} gpu clients ..")

        clients.foreach { case (gpuNum, c) =>
          gpuFailoverService.checkProcessingDir(gpuNum, c)
        }
        val reporterCounter = new AtomicInteger(0)
        while (true) {
          try {
            processImages(gpuFailoverService.gpus, clients, batchSize)
            if (gpuFailoverService.alternatesUnderflow /* && !underflowReported */ ) {
              if (reporterCounter.getAndIncrement % 1 /* 10 */ == 0) {
                throw GpuException(s"GPU Failure Alert on ${gpuFailoverService.broken.mkString(",")}: insufficient alternates available!")
              }
            }
          } catch {
            case e: Exception =>
              if (errorCtr.incrementAndGet % 100 == 1) {
                error(s"ProcessImages failure ${errorCtr.get}", e)
              }
          } finally {
            Thread.sleep(DirReaderSleep)
          }
        }
      }
    }
    val inDirThread = ImageWriterThread()
    inDirThread.start
  }


  import reflect.runtime.universe.TypeTag

  trait ProcessorIf {
    def process[T: TypeTag, U: TypeTag](t: T): U
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

  def mvBatch(gpuNum: Int, batch: AB[ImgInfo], toDir: String, findGrandparent: Boolean = false) = {
    for (img <- batch) {
      try {
        val destDir = (if (findGrandparent) {
          new File(img.path).getParentFile.getParent
        } else new File(img.path).getParent) + toDir
        FileUtils.mkdirs(destDir)
        FileUtils.mv(img.path, destDir + "/" + FileUtils.fileName(img.path))
      } catch {
        case e: Exception =>
          txError(gpuNum, s"Unable to move image $img in batch=${batch.mkString(",")}", e)
      }
    }

  }

}

case class GpuClient(inGci: GpuClientInfo, failoverService: GpuFailoverService) extends Thread {
  val QTimeOut = 200

  import GpuClient._

  var gci = inGci
  val gpuNum = gci.gpuInfo.gpuNum
  var tfClient: TfClient = null

  override def run() = {
    info(s"Starting GpuClient ${gci.gpuInfo}..")
    val empty = new java.util.concurrent.atomic.AtomicInteger
    val inQ = gci.inQ
    val (tx1Host, tx1Port) = gci.gpuInfo.parseTfServerHostAndPort


    //    val failoverService = new GpuFailoverService(failoverInfo, new TfClientFailoverCallback {
    //      override def failoverClient(gpuNum: Int, newTfClient: TfClient) = {
    //        tfClient = newTfClient
    //      }
    //    })

    var initTfClient = try {
      TfClient(gci.conf, gci.gpuInfo.parseTfServerHostAndPort._1, gci.gpuInfo.parseTfServerHostAndPort._2)
    } catch {
      case e: Exception =>
        val (newGci, failedOver) = failoverService.fail(inGci, s"Unable to connect to $tx1Host:$tx1Port", Some(e))
        if (failedOver) {
          gci = newGci
          TfClient(gci.conf, gci.gpuInfo.parseTfServerHostAndPort._1, gci.gpuInfo.parseTfServerHostAndPort._2)
        } else {
          throw new GpuException(s"Unable to failover for $gci")
        }
    }
    tfClient = initTfClient

    while (true) {
      var cntr = 0
      val batch = AB[ImgInfo]()
      var timeout = false
      while (!timeout && cntr < gci.batchSize) {
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
          val (res, fatalGpu) = GpuStreaming.doBatch(tfClient, gci, batch)
          if (fatalGpu) {
            val (newGci, failedOver) = failoverService.fail(gci, "Failed on doBatch")
            if (failedOver) {
              gci = newGci
              try {
                tfClient = TfClient(gci.conf, gci.gpuInfo.parseTfServerHostAndPort._1, gci.gpuInfo.parseTfServerHostAndPort._2)
              } catch {
                case e: Exception =>
                  throw new GpuException(s"Unable to connect to failedOver specs ${gci.gpuInfo.tfServerHostAndPort}",
                    Some(gpuNum), Some(gci.gpuInfo.tfServerHostAndPort), Some(e))
              }

            } else {
              throw new IllegalStateException(s"Failover failed for batch ${batch.mkString(",")} on $gci. Can not proceed")
            }
            mvBatch(gpuNum, batch, "", true)
          } else {
            gci.outQ.put(res)
            mvBatch(gpuNum, batch, "/completed", true)
            ImageHandler.prepImages(ImgPartition(gpuNum, batch), gci.gpuInfo.outDir)
          }
        } catch {
          case e: Exception =>
            txError(gpuNum, s"GpuClient.run failed for batch=${batch.mkString(",")}", e)
            mvBatch(gpuNum, batch, "", true)
        }
      } else {
        if (empty.getAndIncrement % 100 == 0) {
          txDebug(gpuNum, s"Empty batch ${empty.get + 1}")
        }
      }
    }
  }

}
