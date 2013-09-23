package com.google.dart.tools.ui.watchdog;

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.watchdog.SwtEventMonitorThread.StackTraceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An internal data holder for event data, including the timestamp and sample stack traces.
 */
class Event {
  /** Time stamp of when the Event started in milliseconds. */
  private long startTimeMillis;

  /** Duration of the Event, in milliseconds, or 0 if duration is unknown */
  private long durationMillis;

  /** The list of individual traces created when sample is called. */
  private List<Trace> traces;

  /** True if this event shouldn't be reported to the logging server. */
  private boolean excludeEvent;

  /** The name of the user's operation that is blocked */
  private final String blockedOperation;

  /** The reason why the user's operation is blocked */
  private final String blockedReason;

  private List<String> filterOutTraces;

  private long stopTimeMillis;

  /** Create a new Event which started at the timestamp in milliseconds */
  public Event(long timestamp) {
    this(timestamp, null, null);
  }

  /** Create a new Event which started at timestamp and lasted for duration microseconds. */
  public Event(long timestamp, long durationMicros) {
    this.startTimeMillis = timestamp;
    this.durationMillis = durationMicros;
    this.blockedOperation = null;
    this.blockedReason = null;
    traces = new ArrayList<Trace>();
  }

  /** Create a new Event which started at the timestamp in milliseconds */
  public Event(long timestamp, String operation, String reason) {
    this.startTimeMillis = timestamp;
    this.durationMillis = 0;
    this.blockedOperation = operation == null ? "" : operation;
    this.blockedReason = reason == null ? "" : reason;
    traces = new ArrayList<Trace>();
  }

  public void addSamples(StackTraceInfo[] stackTraces, int numStacks, Thread displayThread) {
    long lastCaptureTime = 0;
    for (int i = 0; i < numStacks && !excludeEvent; ++i) {
      StackTraceElement[] stack = stackTraces[i].stacks.get(displayThread);
      if (stack != null) {
        long captureTime = stackTraces[i].captureTime.getTime();
        long duration = lastCaptureTime != 0 ? 1000 * (captureTime - lastCaptureTime) : 0;
        lastCaptureTime = captureTime;
        excludeEvent = shouldExclude4xEvent(stack);
        traces.add(new Trace(this, captureTime, duration, stack));
      }
    }
  }

  public String getBlockedOperation() {
    return blockedOperation;
  }

  public String getBlockedReason() {
    return blockedReason;
  }

  public long getDurationMillis() {
    return durationMillis;
  }

  public long getStartTimeMillis() {
    return startTimeMillis;
  }

  public Trace[] getTraces() {
    return traces.toArray(new Trace[traces.size()]);
  }

  public int getTracesCount() {
    return traces.size();
  }

  public boolean isEventExcluded() {
    return excludeEvent;
  }

  /**
   * Records a stack trace with the current event if it matches our criteria, e.g. not a modal
   * dialog.
   */
  public void sample(long traceTime, StackTraceElement[] trace) {
    if (trace != null) {
      excludeEvent = shouldExclude3xEvent(trace);
      traces.add(new Trace(this, traceTime, durationMillis, trace));
    }
  }

  /**
   * Captures a stack trace from the main loop and records it with the current event if it matches
   * our criteria, e.g. not a modal dialog.
   */
  public void sample(Thread eventThread) {
    Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
    StackTraceElement[] trace = allStackTraces.get(eventThread);
    long traceTime = System.currentTimeMillis();
    sample(traceTime, trace);
  }

  public void setDurationMillis(long durationMillis) {
    this.durationMillis = durationMillis;
  }

  public void setStartTimeMillis(long startTimeMillis) {
    this.startTimeMillis = startTimeMillis;
  }

  public void setStopTimeMillis(long stopTimeMillis) {
    this.stopTimeMillis = stopTimeMillis;
    setDurationMillis(stopTimeMillis - startTimeMillis);
  }

  /**
   * Shrink traces size to half by dropping traces at odd positions.
   * <p>
   * Dropping the traces at odd positions [1,3,5,...] and retaining the ones at even positions
   * [0,2,4,...] to always keep the first trace.
   */
  public void shrinkTraces() {
    List<Trace> evenTraces = new ArrayList<Trace>(traces.size());
    for (int i = 0; i < traces.size(); i += 2) {
      evenTraces.add(traces.get(i));
    }
    traces = evenTraces;
  }

  public String toLogMessage(UIInstrumentationBuilder instrumentation) {
    @SuppressWarnings("resource")
    PrintStringWriter msg = new PrintStringWriter();
    msg.println("Watchdog Event:");

    instrumentation.data("BlockedOperation", blockedOperation);
    msg.println(blockedOperation != null ? blockedOperation : "unknown");

    instrumentation.data("BlockedReason", blockedReason);
    msg.println(blockedReason != null ? blockedReason : "unknown");

    instrumentation.metric("Started", startTimeMillis);
    msg.println("Started : " + startTimeMillis);

    instrumentation.metric("Stopped", stopTimeMillis);
    msg.println("Stopped : " + stopTimeMillis);

    instrumentation.metric("Duration", durationMillis);
    msg.println("Duration: " + durationMillis);

    int traceCount = 0;
    for (Trace trace : traces) {
      msg.println("Trace " + ++traceCount);

      instrumentation.metric("TraceTime[" + traceCount + "]", trace.getTimestampMillis());
      msg.println("Time:     " + trace.getTimestampMillis());

      instrumentation.metric("TraceDuration[" + traceCount + "]", trace.getDurationMicros());
      msg.println("Duration: " + trace.getDurationMicros());

      @SuppressWarnings("resource")
      PrintStringWriter writer = new PrintStringWriter();
      for (StackTraceElement stackElem : trace.getStackTrace()) {
        writer.println(stackElem.toString());
        msg.println("  " + stackElem);
      }
      instrumentation.data("Trace[" + traceCount + "]", writer.toString());
    }
    return msg.toString();
  }

  List<String> getFilters() {
    if (filterOutTraces == null) {
      filterOutTraces = new ArrayList<String>();
      filterOutTraces.add("org.eclipse.swt.internal.gtk.OS.gtk_dialog_run");
      filterOutTraces.add("org.eclipse.e4.ui.workbench.addons.dndaddon.DnDManager.startDrag");
    }
    return filterOutTraces;
  }

  /**
   * Returns true if the stack trace contains a modal dialog invocation (Window.runEventLoop).
   */
  private boolean isModalDialog(StackTraceElement[] stackTrace) {
    for (StackTraceElement s : stackTrace) {
      if (s.getClassName().equals("org.eclipse.jface.window.Window")
          && s.getMethodName().equals("runEventLoop")) {
        return true;
      }
    }
    return false;
  }

  private boolean shouldExclude3xEvent(StackTraceElement[] stackTrace) {
    if (isModalDialog(stackTrace)) {
      return true;
    }
    if (stackTrace.length > 0) {
      String fullyQualifiedMethodName = stackTrace[0].getClassName() + "."
          + stackTrace[0].getMethodName();
      // We see events with this at the top of the stack a lot when Eclipse is idle
      // and the screensaver is on.
      if (fullyQualifiedMethodName.startsWith("org.eclipse.swt.internal.gtk.OS._g_main_context_")) {
        return true;
      }
    }
    return false;
  }

  private boolean shouldExclude4xEvent(StackTraceElement[] stackTrace) {
    for (StackTraceElement element : stackTrace) {
      String fullyQualifiedMethodName = element.getClassName() + "." + element.getMethodName();
      for (String filter : getFilters()) {
        if (fullyQualifiedMethodName.equals(filter)) {
          return true;
        }
      }
    }
    return false;
  }
}
