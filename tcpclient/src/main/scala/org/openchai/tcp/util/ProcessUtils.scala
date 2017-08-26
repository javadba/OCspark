package org.openchai.tcp.util
import org.openchai.tcp.util.Logger._

case class ExecResult(params: ExecParams, elapsed: Long, rc: Int, stdout: String, stderr: String)

case class ExecParams(tag: String, process: String, args: Option[Seq[String]] = None,
  env: Option[Seq[String]] = None, dir: String = ".") {

  override def toString: String = process + "ª " + args.flatMap(arr => Some(arr.mkString(" "," ",""))).getOrElse("")

}

case class ProcessResult(rc: Int, stdout: String, stderr: String)


object ProcessUtils {

  def main(args: Array[String]): Unit = {
     val out = ProcessUtils.exec("ls", "ls -lrta /shared")
//     val out = ProcessUtils.runBash("ls -lrta /shared/*.tmp")
    info(s"$out")
  }
  val Blen = 16*1024
  // ProcessUtils.runBas
  // h("ls -lrta /shared/*.tmp")
  def read(is: java.io.InputStream) = {
    val av = Blen
    // Math.max(Blen, is.available)
    val arr = new Array[Byte](av)
    val nread = is.read(arr)
    new String(arr, "ISO-8859-1").substring(0, nread)
  }
  object Timing {
    def time[T](tag: String, block: => T): T = {
      val start = System.currentTimeMillis
      val res = block
      val duration = System.currentTimeMillis - start
      info(s"Timing for $tag: $duration ms")
      res
    }
  }

  def runBash(label: String, cmd: String) = {
    info(s"Running cmd=[$cmd] ..")
    val res = Timing.time(cmd,
      { val p = Runtime.getRuntime.exec(Array("/bin/bash"))
      val os = p.getOutputStream
      val stdout = p.getInputStream

      val stderr = p.getErrorStream
      os.write(s"$cmd\n".getBytes())
  //    os.write(s"$cmd\n".getBytes("ISO-8859-1"))
      os.flush
  //    val b = new Array[Byte](16*1024)
      val stdo = read(stdout)
      stdout.close
      val stde = read(stderr)
      stderr.close
      if (stdo.length >= Blen - 4) {
        warn(s"stdout from command [$cmd] may not fit in buffer")
      }
      if (stdo.length >= Blen - 4) {
        warn(s"stderr from command [$cmd] may not fit in buffer")
      }
      os.write("echo $?\n".getBytes)
  //    val oarr = read(stdout)
      val rc = -1 // oarr.toString.toInt
      os.close
      new ProcessResult(rc, stdo, stde)
    })
    res
  }
  def runScala(cmd: String) = {
    import sys.process._
    val out = new StringBuilder
    val err = new StringBuilder
    val logger = ProcessLogger(
      (o: String) => out.append(o),
      (e: String) => err.append(e))
    val rc = Seq("/bin/bash","-c") ++ cmd.split(" ").toSeq ! logger
    (out,err, rc)
  }

  def exec(tag: String, cmd: String): ExecResult = {
    val toks = cmd.split(" ")
    val (exe, args) = (toks.head, toks.tail)
    exec(ExecParams(tag, exe, Some(args), None,
      if (exe.indexOf("/")>=0) exe.substring(0,exe.lastIndexOf("/")) else System.getProperty("user.dir")))
  }

  def exec(params: ExecParams): ExecResult = {
    val startt = System.currentTimeMillis()

    val procName = params.process

    val pb = new ProcessBuilder((procName +: params.args.getOrElse(Seq.empty[String])):_*)
    pb.directory(new java.io.File(params.dir))
    error(s"Exec: [$procName ${params.args.get.mkString(" ")}] pbDir=${pb.directory.getAbsolutePath}")

    val proc = pb.start()
    proc.waitFor()

    val exit = proc.exitValue()
    val stdout = scala.io.Source.fromInputStream(proc.getInputStream).getLines.toList.mkString("\n")
    val stderr = scala.io.Source.fromInputStream(proc.getErrorStream).getLines.toList.mkString("\n")

    val elapsed = System.currentTimeMillis() - startt

    val res = ExecResult(params, elapsed, exit, stdout, stderr)
    error(s"Process [${params}] completed in $elapsed with rc=$exit stdoutLen=${stdout.length} stderrLen=${stderr.length}")
    res

  }


}
