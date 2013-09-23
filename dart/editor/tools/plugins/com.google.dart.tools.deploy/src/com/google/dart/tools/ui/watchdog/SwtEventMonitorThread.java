package com.google.dart.tools.ui.watchdog;

import com.google.common.collect.ImmutableMap;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SWT long event monitoring thread.
 */
class SwtEventMonitorThread extends Thread implements Listener {
  /**
   * Information from a single stack trace.
   */
  static class StackTraceInfo {
    public final Date captureTime = new Date();
    public final Map<Thread, StackTraceElement[]> stacks;

    public StackTraceInfo(Map<Thread, StackTraceElement[]> stacks) {
      this.stacks = stacks;
    }
  }
  /**
   * A helper class to track and report potential deadlocks.
   */
  private class DeadlockTracker {
    private final long deadlockDelta_ns;
    private long lastCompletedEvent_ns = getTimeMarker_ns();
    private boolean haveAlreadyLoggedPossibleDeadlock = false;

    public DeadlockTracker() {
      // Sample interval to capture the traces of an unresponsive event
      deadlockDelta_ns = 10 * 60 * 1000000000L; // == 10 min
    }

    /**
     * Logs a possible deadlock to the remote log.
     * 
     * @param currTime_ns the current time
     * @param seqNo the current event number
     * @param stackTraces stack traces for the currently stalled event
     * @param numStacks the number of valid traces for the currently stalled event
     */
    public void logPossibleDeadlock(long currTime_ns, int seqNo, StackTraceInfo[] stackTraces,
        int numStacks) {
      if (!haveAlreadyLoggedPossibleDeadlock
          && currTime_ns - lastCompletedEvent_ns > deadlockDelta_ns) {
        if (localTraceLog != null) {
          StringBuilder str = new StringBuilder(1024 * numStacks + 128);

          String msg = String.format(
              FMT_FORCED,
              seqNo,
              deadlockDelta_ns,
              DATE_FMT.format(new Date(lastCompletedEvent_ns)));
          str.append(msg);

          stackTracesToString(str, stackTraces, numStacks);

          localTraceLog.trace(str.toString());
        }
        //logEventRemotely(new LongEventInfo(lastCompletedEvent_ns, 0), stackTraces, numStacks);
        // Does local logging make sense in a deadlock situation?
        logEventLocally(new LongEventInfo(lastCompletedEvent_ns, 0), stackTraces, numStacks);
        haveAlreadyLoggedPossibleDeadlock = true;
      }
    }

    /**
     * Resets the deadlock tracker's state.
     */
    public void reset(long lastCompletedEvent_ns) {
      this.lastCompletedEvent_ns = lastCompletedEvent_ns;
      haveAlreadyLoggedPossibleDeadlock = false;
    }
  }

  // TODO(foremans): Replace these with the SWT constants once the monitoring patch is finalized
  // and we are no longer deploying for Eclipse 3.8.
  public static final int BeginEvent = 50;
  public static final int EndEvent = 51;

  public static final int BeginSleep = 52;

  public static final int EndSleep = 53;
  /*
   * Tracks when the current event was started, or if the event has nested {@link
   * Event#sendEvent} calls, then the time when the most recent nested call returns and the
   * current event is resumed. ONLY ACCESSS FROM THE UI THREAD.
   */
  private long eventStartOrResumeTime_ms = getTimestamp_ms();
  // Accessed by both the UI and monitoring threads.
  private final AtomicBoolean cancelled = new AtomicBoolean(true);
  private final AtomicBoolean sleeping = new AtomicBoolean(false);

  private final AtomicLong grabStackTraceAt_ns = new AtomicLong();

  private final AtomicReference<LongEventInfo> publishEvent = new AtomicReference<LongEventInfo>(
      null);
  // Accessed by both the UI and monitoring threads. Changing the interval counter in the UI thread
  // causes the background thread to reset its stalled event state.
  private volatile int intervalCounter = 1;
  // Not accessed by the UI thread.
  private final Tracer localTraceLog;
  private final Display display;
  private final long stackPollingDelay_ns;
  private final int maxTraceCount;
  private final int minTraceCount;

  private final int threshold_ms;

  //private final boolean enableRemoteLogging;

  private static final String FMT = "Event: %1$dns from %2$s%n";

  private static final String FMT_FORCED = "Event #%1$d: %2$dns from %3$s [still running]%n";

  private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("HH:mm:ss.SSS");

  private static <T> void decimate(T[] list, int fromSize, int toSize) {
    for (int i = 1; i < toSize; ++i) {
      int j = (i * fromSize + toSize / 2) / toSize; //== floor(i*(from/to)+0.5) == round(i*from/to)
      list[i] = list[j];
    }
  }

  public SwtEventMonitorThread(Display display, int threshold_ms, int pollingRate_ms,
      int minTraceCount, int maxTraceCount, Tracer localTraceLog, boolean enableRemoteLogging) {
    super("[DEBUG] SWT Event watchdog, analysis, and logging thread");

    assert (display != null);
    assert (0 <= minTraceCount);
    assert (minTraceCount <= maxTraceCount);
    assert (threshold_ms > 0);

    setDaemon(true);
    setPriority(NORM_PRIORITY + 1);

    this.display = display;
    this.stackPollingDelay_ns = 1000000L * (pollingRate_ms > 0 ? pollingRate_ms
        : Long.MAX_VALUE / 1000000L);
    this.minTraceCount = Math.max(1, minTraceCount); // Prevent div-by-0 gracefully
    this.maxTraceCount = maxTraceCount;
    this.threshold_ms = threshold_ms;
    //this.enableRemoteLogging = enableRemoteLogging;
    this.localTraceLog = localTraceLog;
  }

  // Called on the UI thread!
  public void beginEvent() {
    handleEventTransition();
  }

  // Called on the UI thread!
  public void beginSleep() {
    handleEventTransition();
    sleeping.set(true);
  }

  // Called on the UI thread!
  public void endEvent() {
    handleEventTransition();
  }

  // Called on the UI thread!
  public void endSleep() {
    // Display.sleep() just ended, so update the event start time and polling interval and reset the
    // stalled event state.
    eventStartOrResumeTime_ms = getTimestamp_ms();
    intervalCounter++; // resets the stalled event state in the background thread

    grabStackTraceAt_ns.set(getTimeMarker_ns() + stackPollingDelay_ns);
    sleeping.set(false);
  }

  @Override
  public void handleEvent(org.eclipse.swt.widgets.Event event) {
    /*
     * Freeze monitoring involves seeing long intervals between BeginEvent/EndEvent messages,
     * regardless of the level of event nesting. For example:
     * 1) Log if a top-level or nested dispatch takes too long (interval is between BeginEvent and
     *    EndEvent).
     * 2) Log if preparation before popping up a dialog takes too long (interval is between two
     *    BeginEvent messages).
     * 3) Log if processing after dismissing a dialog takes too long (interval is between two
     *    EndEvent messages).
     * 4) Log if there is a long delay between nested calls (interval is between EndEvent and
     *    BeginEvent). This could happen after a dialog is dismissed, does too much processing on
     *    the UI thread, and then pops up a notification dialog.
     * 5) Don't log for long delays between top-level events (interval is between EndEvent and
     *    BeginEvent at the top level), which should only occur if the application is idle and
     *    the event loop is spending all of its time sleeping.
     *
     * Calls to Display.sleep() make the UI responsive, whether or not events are actually
     * dispatched, so items 1-4 above assume that there are no intervening calls to sleep() between
     * the event transitions. Treating the BeginSleep event as an event transition lets us
     * accurately capture true freeze intervals.
     *
     * Correct management of BeginSleep/EndSleep events allow us to handle items 4 and 5 above
     * since we can tell if a long delay between an EndEvent and a BeginEvent are due to an idle
     * state (in Display.sleep()) or a UI freeze.
     */
    if (event.type == BeginEvent) {
      beginEvent();
    }
    if (event.type == EndEvent) {
      endEvent();
    }
    if (event.type == BeginSleep) {
      beginSleep();
    }
    if (event.type == EndSleep) {
      endSleep();
    }
  }

  @Override
  public void run() {
    /*
     * If this event loop starts in the middle of a UI freeze, it will succeed in capturing the
     * portion of that UI freeze that it sees.
     */
    final long pollingNyquistDelay_ns = stackPollingDelay_ns / 2;
    long pollingDelay_ns = stackPollingDelay_ns;
    int lastInterval = intervalCounter;

    StackTraceInfo[] stackTraces = new StackTraceInfo[maxTraceCount];
    int numStacks = 0;

    DeadlockTracker deadlockTracker = new DeadlockTracker();
    boolean resetStalledEventState = true;

    while (!cancelled.get()) {
      long sleepFor;
      int currInterval = intervalCounter;
      long currTime_ns = getTimeMarker_ns();
      if (resetStalledEventState) {
        numStacks = 0;
        pollingDelay_ns = stackPollingDelay_ns;
        sleepFor = pollingNyquistDelay_ns;
        deadlockTracker.reset(currTime_ns);
        resetStalledEventState = false;
      } else {
        sleepFor = Math.min(pollingNyquistDelay_ns, grabStackTraceAt_ns.get() - currTime_ns);
      }

      nanosleep(sleepFor);

      /*
       * If after sleeping we see that a new event has been dispatched, mark that we should update
       * the stalled event state. Otherwise, check if we have surpassed our threshold and collect a
       * stack trace.
       *
       * Note that stack trace accumulation occurs even if no events are dispatched (the nested
       * Event.sendEvent() call depth is zero). If that occurs and the next event's execution time
       * is shorter than the logging threshold, then the stalled event state is discarded without
       * being logged, with no harm done. If the next event's execution time exceeds the logging
       * threshold, then the correct event duration is still logged, but the captured stack traces
       * will be polluted by "empty" traces at the beginning. Logs processing should be aware of
       * this and skip over any initial traces that only contain the main read and dispatch loop.
       * More accurate stack trace information can be guaranteed by only gathering stack traces when
       * an event is dispatched, but that requires additional synchronization.
       */
      if (lastInterval != currInterval) {
        resetStalledEventState = true;
      } else if (!sleeping.get()) {
        deadlockTracker.logPossibleDeadlock(currTime_ns, currInterval, stackTraces, numStacks);

        // Collect additional stack traces if enough time has elapsed.
        if (maxTraceCount > 0) {
          long nextStackAt_ns = grabStackTraceAt_ns.get();

          if (currTime_ns - nextStackAt_ns > 0) {
            if (numStacks == maxTraceCount) {
              decimate(stackTraces, maxTraceCount, minTraceCount);
              numStacks = minTraceCount;
              pollingDelay_ns = (pollingDelay_ns * maxTraceCount + minTraceCount / 2)
                  / minTraceCount;
            }

            try {
              Map<Thread, StackTraceElement[]> stacks;
              Thread watchdogThread = display.getThread();
              stacks = ImmutableMap.of(watchdogThread, watchdogThread.getStackTrace());
              stackTraces[numStacks++] = new StackTraceInfo(stacks);
              grabStackTraceAt_ns.compareAndSet(nextStackAt_ns, nextStackAt_ns + pollingDelay_ns);
            } catch (SWTException e) {
              // Display is disposed so start terminating
              cancelled.set(true);
              resetStalledEventState = true;
            }
          }
        }
      }

      // If a stalled event has finished, publish it and mark that the information should be reset.
      LongEventInfo snap = publishEvent.getAndSet(null);
      if (snap != null) {
        //logEventRemotely(snap, stackTraces, numStacks);
        logEventLocally(snap, stackTraces, numStacks);
        resetStalledEventState = true;
      }

      lastInterval = currInterval;
    }
  }

  public void shutdown() throws SWTException {
    if (!cancelled.getAndSet(true)) {
      display.removeListener(BeginEvent, this);
      display.removeListener(EndEvent, this);
      display.removeListener(BeginSleep, this);
      display.removeListener(EndSleep, this);
    }
    wakeUp();
  }

  @Override
  public synchronized void start() throws SWTException {
    display.addListener(BeginEvent, this);
    display.addListener(EndEvent, this);
    display.addListener(BeginSleep, this);
    display.addListener(EndSleep, this);
    cancelled.set(false);
    // Calling super.start() after setting cancelled.set(false) prevents a race condition in run().
    super.start();
  }

  @Override
  protected void finalize() throws Throwable {
    shutdown();
    super.finalize();
  }

  long getTimeMarker_ns() {
    return System.nanoTime();
  }

  long getTimestamp_ms() {
    return System.currentTimeMillis();
  }

  /**
   * Prints the snapshot and stack captures to the workspace log.
   * <p>
   * This method does not log anything remotely.
   */
  void logEventLocally(LongEventInfo snapshot, StackTraceInfo[] stackTraces, int numStacks) {
    if (localTraceLog != null) {
      StringBuilder str = new StringBuilder(1024 * numStacks + 128);

      String msg = String.format(FMT, snapshot.duration, DATE_FMT.format(new Date(snapshot.start)));

      str.append(msg);
      stackTracesToString(str, stackTraces, numStacks);

      localTraceLog.trace(str.toString());
    }
  }

  void nanosleep(long nanoseconds) {
    long delay_ms = nanoseconds / 1000000L;

    if (delay_ms > 0) {
      try {
        Thread.sleep(delay_ms);
      } catch (InterruptedException e) {
        // wake up
      }
    }
  }

  // Called on the UI thread!
  private void handleEventTransition() {
    // Any event transition or entry into the sleeping state needs to reset the detection of freezes
    // and potentially trigger logging.
    long currTime = getTimestamp_ms();
    int duration = (int) (currTime - eventStartOrResumeTime_ms);
    if (duration >= threshold_ms) {
      LongEventInfo info = new LongEventInfo(eventStartOrResumeTime_ms, duration);
      publishEvent.set(info);
      wakeUp();
    }

    eventStartOrResumeTime_ms = currTime;
    intervalCounter++;

    grabStackTraceAt_ns.set(getTimeMarker_ns() + stackPollingDelay_ns);
  }

  /**
   * Logs the snapshot and stack captures remotely if remote logging is enabled.
   * <p>
   * This method does not log locally.
   */
//  void logEventRemotely(LongEventInfo snapshot, StackTraceInfo[] stackTraces, int numStacks) {
//    if (enableRemoteLogging && CommonCorePlugin.isRemoteLoggingEnabled()) {
//      final Thread displayThread = display.getThread();
//      final Trace[] traces = new Trace[numStacks];
//
//      long startTime = snapshot.start;
//      long endTime = startTime + snapshot.duration;
//
//      Event event = new Event(startTime, snapshot.duration);
//      event.addSamples(stackTraces, numStacks, displayThread);
//
//      if (!event.isEventExcluded()) {
//        CommonCorePlugin.logEntry(startTime, endTime,
//            EventConverter.convertToLogEntry(event, EventType.EVENT_WATCHDOG));
//      }
//    }
//  }

  private void stackTracesToString(StringBuilder str, StackTraceInfo[] stackTraces, int numStacks) {
    StackTraceInfo last = null;
    if (stackTraces != null) {
      for (int i = 0; i < numStacks; ++i) {
        StackTraceInfo trace = stackTraces[i];
        str.append("\tTrace ").append(DATE_FMT.format(trace.captureTime));
        if (last != null) {
          double dt = trace.captureTime.getTime() - last.captureTime.getTime();
          String unit = "ms";
          if (dt > 1000.0) {
            dt /= 1000.0;
            unit = "s";
          }
          dt = Math.round(dt * 10.0) / 10.0;
          str.append(" (+").append(dt).append(unit).append(")");
        }
        last = trace;
        str.append('\n');

        for (Map.Entry<Thread, StackTraceElement[]> e : trace.stacks.entrySet()) {
          Thread key = e.getKey();
          if (key != this) {
            String threadName = key.getName();
            str.append("\t\t").append(threadName);
            str.append(':').append('\n');
            for (StackTraceElement s : e.getValue()) {
              str.append("\t\t\t").append(s.toString()).append('\n');
            }
          }
        }
      }
    }
  }

  private void wakeUp() {
    interrupt();
  }
}
