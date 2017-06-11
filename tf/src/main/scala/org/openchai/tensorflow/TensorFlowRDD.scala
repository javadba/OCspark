package org.openchai.tensorflow

import org.apache.spark.sql.SparkSession
import org.openchai.tcp.util.{FileUtils, TcpUtils}

object TensorFlowRDD {
}

case class TFSubmitter() {
}
object TFSubmitter {

  def runSparkJob(master: String, tfServer: String, imgApp: String, dir: String, nPartitions: Int = 10) = {
    val spark = SparkSession.builder.master(master).appName("TFSubmitter").getOrCreate
    val sc = spark.sparkContext
    val irdd = sc.binaryFiles(dir,nPartitions)
    val out = irdd.mapPartitionsWithIndex{ case (np, part) =>
      val tfClient = TfClient(tfServer)
      part.map { case (path,contents) =>
      val label = s"${TcpUtils.getLocalHostname}-Part$np-$path"
      val bytes = contents.toArray
      val md5 = FileUtils.md5(bytes)
      val res = tfClient.labelImg(LabelImgStruct(label,path,
        bytes, md5, Option(imgApp)))
        println(s"Received label result: $res")
      res
      }
    }
    val c = out.collect

  }


  def main(args: Array[String]): Unit = {
    val Array(master,tfServer, imgApp, dir,nPartitions) = args
    val dir2 = s"${System.getProperty("user.dir")}/tf/src/main/resources/images/"
    runSparkJob(master,tfServer, imgApp, s"file:///$dir2",nPartitions.toString.toInt)
  }
}
