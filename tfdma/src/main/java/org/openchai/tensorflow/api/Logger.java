package org.openchai.tensorflow.api;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {
  public static void info(String msg) {
    System.out.println(msg);
  }

  public static String eToString(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return String.format("%s - %s", e.getMessage(), sw.toString());
  }
  public static void error(String msg, Exception e) {
    System.out.println(msg);
    System.out.println(String.format("%s: %s", msg, eToString(e)));
  }

  public static void p(String msg, Object arg1, Object ... moreArgs) {

    info(f(msg,arg1, moreArgs));
  }

  public static void p(String msg) {
    info(msg);
  }

  public static String f(String msg, Object ... v) {
    return String.format(msg, v);
  }

}
