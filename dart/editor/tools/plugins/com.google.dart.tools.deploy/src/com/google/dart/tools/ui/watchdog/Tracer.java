package com.google.dart.tools.ui.watchdog;

import org.eclipse.core.runtime.Platform;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple helper class for Eclipse debug tracing.
 */
public class Tracer {
  private static SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS");

  /**
   * Returns a tracer object if the given debug option is enabled, or {@code null} if it is not.
   */
  public static Tracer create(String prefix, String debugOption) {
    if (isTracingEnabled(debugOption)) {
      return new Tracer(prefix);
    }
    return null;
  }

  /**
   * Returns {@code true} if the debug option is set, but only if the platform is running in debug
   * mode (e.g., not in a unit test.)
   */
  public static boolean isTracingEnabled(String debugOption) {
    return Platform.isRunning() && "true".equalsIgnoreCase(Platform.getDebugOption(debugOption));
  }

  private static String getTimestamp() {
    return timeFormatter.format(new Date());
  }

  private final String prefix;
  private final PrintStream out = System.out;

  /**
   * Creates a tracer object that assists in debug tracing.
   * 
   * @param prefix a string to be prefixed to every trace line (may be {@code null})
   */
  private Tracer(String prefix) {
    this.prefix = prefix;
  }

  public void trace(Object o) {
    out.printf("%s %s: %s\n", getTimestamp(), prefix, o);
  }

  public void trace(String msg, Object... params) {
    trace(String.format(msg, params));
  }

  public void traceStackTrace(Throwable t) {
    if (t == null) {
      // TODO(tparker): Either disallow null here, or do something more sensible with the null
      // argument
      trace(t);
    } else {
      StringWriter writer = new StringWriter();
      PrintWriter printer = new PrintWriter(writer);
      t.printStackTrace(printer);
      printer.flush();
      trace(writer.toString());
    }
  }
}
