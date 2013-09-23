package com.google.dart.tools.ui.watchdog;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.deploy.ApplicationWorkbenchAdvisor;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.ui.IWindowListener;
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.IWorkbenchWindow;
//import org.eclipse.ui.PlatformUI;

public class MonitoringUtil {

  /**
   * The time when the current event started to be processed or zero if no current event.
   */
  private static volatile long currentEventTime = 0;

  /** The event listener used to cache the current event */
  private static Listener eventListener = new Listener() {
    @Override
    public void handleEvent(Event event) {
      currentEventTime = System.currentTimeMillis();
    }
  };

  public static BlockedTaskInfo getBlockedTaskInfo() {
    return null;
  }

  /** Answer the current event start time or zero if no event is being processed */
  public static long getCurrentUIEventStartTimeInMilliseconds() {
    return currentEventTime;
  }

  /** Called by {@link ApplicationWorkbenchAdvisor} when no events are being processed */
  public static void idle() {
    currentEventTime = 0;
  }

  /**
   * Called on the UI thread after the display has been created to track the current event and
   * monitor the responsiveness of the UI thread.
   */
  public static void start() {
    DartCore.logInformation("UI Monitoring started");
    hookDisplayEvents();
    new WatchdogThread().start();
  }

  private static void hookDisplayEvents() {
    // Listen for all types of events
    // See org.eclipse.swt.SWT for event types
    for (int eventType = 1; eventType < 50; eventType++) {
      Display.getDefault().addFilter(eventType, eventListener);
    }
  }
}
