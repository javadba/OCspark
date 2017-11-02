package org.openchai.tensorflow

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.{Comparator, PriorityQueue}

import org.openchai.tcp.util.FileUtils
import org.openchai.tcp.util.FileUtils.fileExt
import org.openchai.tensorflow.GpuClient.{GpuInfo, ImgInfo, ImgPartition}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => AB}

object ImageHandler {

  val NumExt = "^[\\d+]$".r

  @inline def isDigitsExt(fname: String) = NumExt.findAllIn(fileExt(fname)).nonEmpty

  private val ilPrinted = mutable.Set[String]()

  def getImageLists(gpus: Seq[GpuInfo], dir: String, batchSize: Int): Seq[ImgPartition] = {
    if (!ilPrinted.contains(dir)) {
      println(s"Checking images dir $dir ..")
      ilPrinted += dir
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
    val paths = pq.iterator.asScala.toList
    paths
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
