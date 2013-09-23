package com.google.dart.tools.ui.watchdog;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.swt.widgets.Display;

/**
 * Watchdog timer thread which periodically checks if the Workbench main event loop is currently
 * processing an event and how long it has been processing. If the event and duration meets our
 * critera, we begin sampling it more often to capture stack traces of what it's doing.
 * <p>
 * To keep the total number of collected traces with in a maximum limit, we shrink the traces size
 * to half by dropping traces at odd positions and double the sampling interval when we reach the
 * max trace count.
 */
public class WatchdogThread extends Thread {

  /** Indication of when to stop the thread from executing */
  private volatile boolean stop = false;

  public WatchdogThread() {
    super("WatchdogThread");
    setDaemon(true);
    setPriority(NORM_PRIORITY + 1);
  }

  /**
   * Main run loop for this thread which periodically checks with the Workbench to see if an event
   * is current being processed and captures details about it if it meets our criteria.
   */
  @Override
  public void run() {
    DartCore.logInformation("WatchdogThread running");

    // Capture traces if an event takes longer than X milliseconds
    int monitoringEventTimeMillis = 500;

    int idleSampleRateMillis = monitoringEventTimeMillis / 2;

    // Sample interval to capture the traces of an unresponsive event
    int activeSampleRateMillis = 100;

    // Maximum number of traces to keep
    int maxTraceCount = 8;

    Thread mainEventThread = Display.getDefault().getThread();
    Event event = null;
    Event blockedJobsEvent = null;

    while (!stop) {
      // Log all occurrences of the Blocked Jobs dialog.
      BlockedTaskInfo blockedTaskInfo = MonitoringUtil.getBlockedTaskInfo();
      long blockedJobstStartTimeMilliseconds = (blockedTaskInfo == null) ? 0
          : blockedTaskInfo.getStartTimeInMilliseconds();
      if (blockedJobsEvent != null
          && blockedJobstStartTimeMilliseconds != blockedJobsEvent.getStartTimeMillis()) {
        eventCompleted(blockedJobsEvent, blockedJobstStartTimeMilliseconds);
        blockedJobsEvent = null;
      }
      if (blockedJobstStartTimeMilliseconds > 0 && blockedJobsEvent == null
          && blockedTaskInfo != null) {
        // TODO(thirumala): Report the thread that is blocking the UI thread.
        blockedJobsEvent = new Event(
            blockedJobstStartTimeMilliseconds,
            blockedTaskInfo.getTaskName(),
            blockedTaskInfo.getReason());
        // Avoid logging multiple messages for this pause.
        event = null;
      }

      // Log when the UI thread is frozen for more than WARN_TIME_MILLIS
      long nextEventStartTimeMilliseconds = MonitoringUtil.getCurrentUIEventStartTimeInMilliseconds();
      if (event != null && nextEventStartTimeMilliseconds != event.getStartTimeMillis()) {
        eventCompleted(event, nextEventStartTimeMilliseconds);
        event = null;
      }
      try {
        if (nextEventStartTimeMilliseconds > 0) { // If an event is being handled, start sampling it
          // How long has it been running?
          long duration = System.currentTimeMillis() - nextEventStartTimeMilliseconds;
          if (duration > monitoringEventTimeMillis) {
            if (event == null) {
              event = new Event(nextEventStartTimeMilliseconds);
            }
            if (event.getTracesCount() >= maxTraceCount) {
              // Shrink traces size to half and double the sampling interval to keep the total
              // number of traces with in the max trace count limit.
              event.shrinkTraces();
              activeSampleRateMillis *= 2;
            }
            event.sample(mainEventThread);
          }
          Thread.sleep(activeSampleRateMillis);
        } else {
          Thread.sleep(idleSampleRateMillis);
        }
      } catch (InterruptedException e) {
        DartCore.logInformation("Thread sleep interrupted", e);
      }
    }
  }

  public void shutdown() {
    stop = true;
  }

  /**
   * Log an event that has completed.
   */
  private void eventCompleted(Event event, long nextEventStartTime) {
    if (event != null && !event.isEventExcluded()) {
      // Set the stop time to either the next event's start time or the current time.
      long stopTimeMillis = nextEventStartTime > 0 ? nextEventStartTime
          : System.currentTimeMillis();
      event.setStopTimeMillis(stopTimeMillis);
      UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());
      try {
        DartCore.logInformation(event.toLogMessage(instrumentation));
      } catch (RuntimeException e) {
        instrumentation.record(e);
        DartCore.logInformation("Failed to log Watchdog Event", e);
      } finally {
        instrumentation.log();
      }
    }
  }
}
