package org.openchai.tensorflow.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class Logger {

  public static void p(String msg) {
    info(msg);
  }

  public static void p(String msg, Object arg1, Object ... moreArgs) {

    info(f(msg,arg1, moreArgs));
  }

  public static String f(String msg, Object ... v) {
    return String.format(msg, v);
  }

  public static void debug(String msg) {
    System.out.println(iformat("Debug: " + msg));
  }

  public static void info(String msg) {
    System.out.println(iformat("Info: " + msg));
  }

  public static void warn(String msg, Exception e) {
    System.err.println(String.format("WARN: %s: %s", msg, eToString(e)));
  }

  public static void error(String msg, Exception e) {
    System.err.println(String.format("ERROR: %s: %s", msg, eToString(e)));
  }

  static String iformat(String msg) {
    Date d = new java.util.Date();
    String dm = String.format("%02d%02d %02d:%02d:%02d.%03d",
            d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(),
            d.getSeconds(), d.getTime() / new Double(10e10).intValue());
    return String.format("[%s] %s",dm, msg);
  }

  public static String eToString(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return String.format("%s - %s", e.getMessage(), sw.toString());
  }
}
