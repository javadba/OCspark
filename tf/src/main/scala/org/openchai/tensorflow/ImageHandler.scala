package org.openchai.tensorflow

import java.io.{File, FileNotFoundException}
import java.nio.file.{FileAlreadyExistsException, Files, Paths}
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Comparator, PriorityQueue}

import org.openchai.tcp.util.FileUtils
import org.openchai.tcp.util.FileUtils.fileExt
import org.openchai.tensorflow.GpuClient.{GpuInfo, ImgInfo, ImgPartition}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}
import org.openchai.tcp.util.Logger._

import scala.collection.generic.AtomicIndexFlag

object ImageHandler {

  val NumExt = "^[\\d+]$".r

  @inline def isDigitsExt(fname: String) = NumExt.findAllIn(fileExt(fname)).nonEmpty

  private val ilPrinted = mutable.Set[String]()

  def getImageLists(gpus: Seq[GpuInfo], dir: String, batchSize: Int): Seq[ImgPartition] = {
    if (!ilPrinted.contains(dir)) {
      info(s"Checking images dir $dir ..")
      ilPrinted += dir
    }
    if (!new File(dir).exists) {
      throw new FileNotFoundException(s"getImageLists for nonexistent directory $dir")
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

  val msgCtr = new AtomicInteger(0)
  val msgCtr1 = new AtomicInteger(0)
  def getImageLists(inGpus: Seq[GpuInfo], files: Seq[File], batchSize: Int): Seq[ImgPartition] = {
    val (fnamesWithAffinity, otherFnames) = files.partition(f => isDigitsExt(f.getName))

    import org.openchai.tcp.util.FileUtils._
    val gpuNums = inGpus.map(_.gpuNum)
    if (gpuNums.isEmpty) {
      if (msgCtr.incrementAndGet % 20 == 1) {
        error("getImageLists: we do not have any gpus")
      }
      Seq.empty[ImgPartition]
    } else {

      fnamesWithAffinity.foreach { f =>
        if (!gpuNums.contains(fileExt(f.getName).toInt) && msgCtr1.incrementAndGet % 20 == 1) {
          error(s"Affinity extension not within range of available tx1's: $f . GpuNums are ${gpuNums.mkString(",")}")
        }
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
      val paths = pq.iterator.asScala.toList
      paths
    }
  }

  import FileUtils._
  def prepImages(imgPart: ImgPartition, outDir: String) = {
    val ndir = s"$outDir/${imgPart.gpuNum}"
    //    debug(s"Worker dir=$ndir")
    for (path <- imgPart.imgs.map(p => filePath(p.path) + "/completed/" + fileName(p.path))) yield {
      new File(ndir).mkdirs
      val link = s"$ndir/${new File(path).getName}"
      val olink = if (isDigitsExt(link)) FileUtils.removeExt(link) else link
      if (!Paths.get(olink).toFile.exists) {
        try {
          Files.createSymbolicLink(Paths.get(olink), Paths.get(path))
        } catch {
          case fae: FileAlreadyExistsException =>
//            GpuClient.txDebug(imgPart.gpuNum, s"Got spurious FileAlreadyExistsException on $olink")
          case e: Exception => throw e
        }
      }
    }
    (ndir, new File(ndir).listFiles)
  }
}
