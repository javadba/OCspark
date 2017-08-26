package org.openchai.tcp.util

import java.io.{PrintWriter, StringWriter}

trait Logger {
  def f(msg: String) = {
    val d = new java.util.Date
//    "%02d%02d-%02d:%02d:%02d.%03d".format(d.getMonth, d.getDate, d.getHours, d.getMinutes, d.getSeconds, d.getTime / 10e10.toInt)
    "%02d:%02d.%03d".format(d.getHours, d.getMinutes, d.getSeconds, d.getTime / 10e10.toInt)
  }

  def debug(msg: String) = Logger.debug(msg)
  def info(msg: String) = Logger.info(msg)
  def warn(msg: String) = Logger.warn(msg)
  def error(msg: String) = Logger.error(msg)
}
object Logger {

  val LogLevel = Option(System.getProperty("logger.level")).getOrElse("2").toInt
  def f(msg: String) = {
    val d = new java.util.Date
    "%02d%02d-%02d:%02d:%02d.%03d".format(d.getMonth, d.getDate, d.getHours, d.getMinutes, d.getSeconds, d.getTime / 10e10.toInt)
  }

  def debug(msg: Any) = if (LogLevel >= 3) println(s"Debug: $msg")
  def info(msg: Any) = if (LogLevel >= 2) println(s"Info: $msg")
  def warn(msg: Any) = if (LogLevel >= 1) println(s"WARN: $msg")
  def error(msg: Any) = if (LogLevel >= 0) println(s"ERROR: $msg")
  def error(msg: Any, t: Throwable) = if (LogLevel >= 0) println(s"ERROR: $msg\n${toString(t)}")

  def toString(t: Throwable) = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    t.printStackTrace(pw)
    String.format("%s - %s", t.getMessage, sw.toString)
  }

}
