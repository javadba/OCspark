package org.openchai.tensorflow

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import java.util.{Comparator, PriorityQueue}

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.FileUtils.{fileExt, fileName, writeBytes}
import org.openchai.tcp.util.Logger.{debug, error, info}
import org.openchai.tcp.util.{FileUtils, TcpUtils}
import org.openchai.tensorflow.GpuClient.GpuClientInfo
import org.openchai.tensorflow.SparkSubmitter.ThreadResult
import org.openchai.util.TfAppConfig

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}

object GpuClient {

  case class ImgInfo(tag: String, path: String)

  case class GpuInfo(gpuNum: Int, tfServerHostAndPort: String, imgApp: String, dir: String, outDir: String)

  case class GpuAlternate(tfServerHostAndPort: String)

  case class GpuClientInfo(gpuInfo: GpuInfo, conf: TfAppConfig, master: String, batchSize: Int, inQ: LinkedBlockingQueue[ImgInfo], outQ: LinkedBlockingQueue[ThreadResult])

  case class GpuStatus(gpuClientInfo: GpuClientInfo, status: String)

  case class ImgPartition(gpuNum: Int, imgs: AB[ImgInfo])

  class QReader(q: LinkedBlockingQueue[ThreadResult]) extends Thread {
    override def run() = {
      while (true) {
        val tr = q.poll
        println(tr)
      }
    }
  }

  val NumExt = "^[\\d+]$".r

  @inline def isDigitsExt(fname: String) = NumExt.findAllIn(fileExt(fname)).nonEmpty

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
    info(s"Slaves from $slavesPath: ${slaves.mkString(",")}")
    gpus = GpusInfo(slaves, alternates)
    gpus
  }

  def gpuClientService(gpusInfo: GpusInfo, conf: TfAppConfig, master: String, batchSize: Int) = {

    type InDirGroups = Map[String, Map[Int, GpuClient]]

    val gpuClientInfos = gpusInfo.gpus.map { gi =>
      GpuClientInfo(gi, conf, master, batchSize, new LinkedBlockingQueue[ImgInfo](), new LinkedBlockingQueue[ThreadResult]())
    }
    val clients /*: InDirGroups */ = gpuClientInfos.map {
      gpu => (gpu.gpuInfo.gpuNum, GpuClient(gpu))
    }.toMap

    clients.values.foreach{ _.start }

    val inDirGroups: InDirGroups = clients.groupBy { case (g, gc) => gc.gi.gpuInfo.dir }

    case class ImageWriterThread(inDirGroups: InDirGroups, outDir: String) extends Thread {
      val nGpus = gpus.gpus.length

      override def run() = {
        println(s"Starting  ImageWriterThread with ${inDirGroups.size} groups and writing to $outDir..")
        while (true) {
          for (dirGroup <- inDirGroups) {
            val partitions = getImageLists(gpus.gpus.filter { g => dirGroup._2.values.map(_.gi.gpuInfo).toSeq.contains(g) }, dirGroup._1, batchSize)
            partitions.foreach { p =>
              /* val images = */ prepImages(p, outDir)
              p.imgs.foreach {
                i => clients(p.gpuNum).gi.inQ.offer(i)
              }
            }
          }
          Thread.sleep(100)
        }
      }

      this.start
    }
    //    val inDirThreads = inDirGroups.toSeq.map { case (dir, gseq) =>
    //      ImageWriterThread(dir, gseq)
    //    }
    val inDirThread = ImageWriterThread(inDirGroups, conf.outDir)
    inDirThread.join
  }


  private val ilSet = mutable.Set[String]()

  def getImageLists(gpus: Seq[GpuInfo], dir: String, batchSize: Int): Seq[ImgPartition] = {
    if (!ilSet.contains(dir)) {
      println(s"Checking images dir $dir ..")
      ilSet += dir
    }
    var files = AB[File](
      new File(dir).listFiles
        .filter(f => f.isFile
          && f.getName.contains(".")
          && (ImagesMeta.allowsExt(fileExt(f.getName))
          || isDigitsExt(f.getName))
        ).map { f => (f, f.length) }
        .sortBy(_._1)
        .map(_._1)
        .toSeq: _*)
    getImageLists(gpus, files, batchSize)
  }

  def getImageLists(inGpus: Seq[GpuInfo], files: Seq[File], batchSize: Int): Seq[ImgPartition] = {
    val (fnamesWithAffinity, otherFnames) = files.partition(f => isDigitsExt(f.getName))

    import org.openchai.tcp.util.FileUtils._
    val gpuNums = inGpus.map(_.gpuNum)

    fnamesWithAffinity.foreach { f =>
      assert(gpuNums.contains(fileExt(f.getName).toInt), s"Affinity extension not within range of available tx1's: ${f}")
    }
    val groups = fnamesWithAffinity
      .groupBy(f => fileExt(f.getName))
      .mapValues { files => AB(files.map(f => ImgInfo(fileName(f.getAbsolutePath), f.getAbsolutePath)): _*) }
    val allg = for (g <- gpuNums) yield {
      ImgPartition(g, groups.getOrElse(g.toString, AB[ImgInfo]()))
    }

    val pq = new PriorityQueue[ImgPartition](new Comparator[ImgPartition]() {
      override def compare(o1: ImgPartition, o2: ImgPartition) = o1.imgs.length - o2.imgs.length
    })

    import collection.JavaConverters._
    pq.addAll(allg.asJava)
    otherFnames.foreach { fn =>
      val lowest = pq.poll()
      lowest.imgs += ImgInfo(fileName(fn.getAbsolutePath), fn.getAbsolutePath);
      pq.offer(lowest)
    }
    pq.iterator.asScala.toList
  }

  def prepImages(imgPart: ImgPartition, outDir: String) = {
    val ndir = s"$outDir/${imgPart.gpuNum}"
//    debug(s"Worker dir=$ndir")
    for (path <- imgPart.imgs.map(_.path)) yield {
      new File(ndir).mkdirs
      val link = s"$ndir/${new File(path).getName}"
      val olink = if (isDigitsExt(link)) FileUtils.removeExt(link) else link
      if (!new File(olink).exists) {
        Files.createSymbolicLink(Paths.get(olink), Paths.get(path))
      }
    }
    (ndir, new File(ndir).listFiles)
  }
}

case class GpuClient(gi: GpuClientInfo) extends Thread {
  val QTimeOut = 600 // 200
  import GpuClient._

  override def run() = {
    info(s"Starting GpuClient ${gi.gpuInfo}..")
    val inQ = gi.inQ
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
      val res = runSparkJob(gi, batch)
      gi.outQ.put(res)
    }
  }

  def bashColors = Array(92, 91, 93, 94, 95, 96, 32, 33, 35, 36, 97, 37)

  def txDebug(tx1: Int, msg: String) = {
    debug(("\n" + msg + "\033[0m").replace("\n", s"\n\033[${bashColors(tx1 - 1)}m TX$tx1 >> "))
  }

  def txInfo(tx1: Int, msg: String) = {
    info(("\n" + msg + "\033[0m").replace("\n", s"\n\033[${bashColors(tx1 - 1)}m TX$tx1 >> "))
  }

  def runSparkJob(gci: GpuClientInfo, batch: AB[ImgInfo]) = {

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
                val res = TfSubmitter.labelImg(tfClient, imgLabel)
                txInfo(gi.gpuNum, s"LabelImage result: $res")
                Some(res)
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
        writeBytes(s"${
          li.value.outDir
        }/$fname.result", (li.value.cmdResult.stdout + li.value.cmdResult.stderr).getBytes("ISO-8859-1"))
      //      debug(s"${li.value.fpath}.result", li.value.cmdResult.stdout.getBytes("ISO-8859-1"))
    }
    val processed = c.map {
      _.value.nImagesProcessed
    }.sum
    info(s"Finished runSparkJob for gi.gpuNum=$gi.gpuNum processed=$processed")
    ThreadResult(processed, c.mkString("\n"))

  }

}
