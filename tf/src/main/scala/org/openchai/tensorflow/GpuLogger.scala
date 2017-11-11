package org.openchai.tensorflow

import org.openchai.tcp.util.Logger

object GpuLogger {

  def bashColors = Array(92, 91, 94, 95, 96, 32, 33, 35, 36, 97, 37, 93)

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
    def error(msg: Any, t: Throwable) = if (LogLevel >= 0) println(msg + (if (t != null) s"\n${Logger.toString(t)}" else ""))

    error(("\n" + s"ERROR: ${f2(msg)}" + "\033[0m").replace("\n", s"\n\033[${bashColors(tx1 - 1)}m TX$tx1 >> "), t)
  }


}
