package org.openchai.tf

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import com.sun.tools.javac.code.TypeTag
import or.openchai.tf.TaggedEntry
import org.openchai.caffeonspark.{LabelImgReq, LabelImgResp, LabelImgRespStruct, LabelImgStruct}
import org.openchai.tcp.rpc._
import org.openchai.tcp.util.{ExecParams, FileUtils, ProcessUtils, TcpCommon}
import org.openchai.tcp.util.ProcessUtils._
import org.openchai.tcp.util.Logger._
import org.openchai.tcp.xfer._


// The main thing we need to override here is using XferQConServerIf inside the server object
class TfServer(val outQ: BlockingQueue[TaggedEntry], val tfTcpParams: TcpParams,
  val tcpParams: TcpParams, val xtcpParams: TcpParams) {

  val xferServer = new XferQServer(outQ.asInstanceOf[BlockingQueue[Any]],
    tcpParams, xtcpParams)
  val tfServer = new TcpServer(tfTcpParams.server, tfTcpParams.port, new TfServerIf /*[TaggedEntry]*/ (outQ))

  def start() = {
    xferServer.start
    tfServer.start
    Thread.sleep(100)
  }

}

object TfServer {
  val tfExec = "/shared/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image"
  val imagesDir = "/tmp/images"
  val f = new java.io.File(imagesDir)
  if (!f.exists()) {
    f.mkdirs
  }
//  val tfExec = "/shared/label_image/label_image"

  def apply(outQ: BlockingQueue[TaggedEntry], tfTcpParams: TcpParams, tcpParams: TcpParams,
    xtcpParams: TcpParams) = {
    val server = new TfServer(outQ, tfTcpParams, tcpParams, xtcpParams)
    server.start
  }

  def main(args: Array[String]): Unit = {

    val q = new ArrayBlockingQueue[TaggedEntry](1000)
    val (host, port, xhost, xport, ahost, aport, configFile) = if (args.length == 0) {
      val cont = XferConCommon.TestControllers
      (cont.conHost, cont.conPort, cont.dataHost, cont.dataPort, cont.appHost, cont.appPort, cont.configFile)
    } else {
      XferConServer.makeXferConnections(args)
    }
    val server = apply(q, TcpParams(ahost, aport), TcpParams(host, port), TcpParams(xhost, xport))
  }

}


class TfServerIf /*[T]*/ (/*tcpParams: TcpParams, xferServerIf: XferServerIf, */ q: BlockingQueue[TaggedEntry]) extends ServerIf("TfServerIf") {
  // class TfServerIf extends ServerIf {

  val pathsMap = new java.util.concurrent.ConcurrentHashMap[String, TcpXferConfig]()

  private val nReqs = new AtomicInteger(0)

  //  def readFile(path: String) = FileUtils.readFileBytes(path)

  def findInQ(/*q: BlockingQueue[TaggedEntry], */ tag: String) = {
    val aq = q.asInstanceOf[ArrayBlockingQueue[TaggedEntry]]
    println(s"FindInQ: looking for $tag: entries=${aq.size}")
    val e = {
      var p: Option[TaggedEntry] = None
        while (aq.iterator.hasNext && !aq.isEmpty) {
          val pv = aq.iterator.next
          println(s"Queue entry: ${pv}")
          if (pv.tag == tag) {
            println(s"Found entry ${pv.tag}")
            p = Option(pv)
            aq.remove(pv)
          } else {
            None
          }
        }
      p
    }
    e.flatMap { ee => println(s"For tag=$tag found q entry $ee"); Some(ee) }.getOrElse("No q entry found for tag=$tag")
    e
  }

  def labelImg(struct: LabelImgStruct): LabelImgRespStruct = {

    println(s"LabelImg: processing $struct ..")
    if (struct.data.isEmpty) {
      throw new IllegalStateException(s"Non empty md5 for empty data on $struct")
    } else {
      FileUtils.checkMd5(struct.fpath, struct.data, struct.md5)
    }

    val e = findInQ(struct.tag)
    println(s"LabelImg: Found entry ${e.getOrElse("[empty]")}")
    FileUtils.writeBytes(s"${TfServer.imagesDir}/${struct.fpath}", struct.data)
    val exeResult = ProcessUtils.exec("LabelImage", s"""${TfServer.tfExec} --image="${struct.fpath}"""")

    LabelImgRespStruct(exeResult)
  }

  override def service(req: P2pReq[_]): P2pResp[_] = {
    req match {
      case o: LabelImgReq =>
        val struct = o.value
        println(s"Invoking LabelImg: struct=$struct")
        val resp = labelImg(struct)
        LabelImgResp(labelImg(struct))
      case _ => throw new IllegalArgumentException(s"Unknown service type ${req.getClass.getName}")
    }
  }

}
