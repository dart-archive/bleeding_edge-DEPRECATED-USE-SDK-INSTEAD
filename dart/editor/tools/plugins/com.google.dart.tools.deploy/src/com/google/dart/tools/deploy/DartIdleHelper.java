/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.deploy;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.Policy;

/**
 * Originally copied over from <code>org.eclipse.ui.ide.application</code>.
 * <p>
 * The idle helper detects when the system is idle in order to perform garbage collection in a way
 * that minimizes impact on responsiveness of the UI. The algorithm for determining when to perform
 * a garbage collection is as follows: - Never gc if there is a test harness present - Don't gc if
 * background jobs are running - Don't gc if the keyboard or mouse have been active within
 * IDLE_INTERVAL - Don't gc if there has been a GC within the minimum gc interval (system property
 * PROP_GC_INTERVAL) - After a gc, don't gc again until (duration * GC_DELAY_MULTIPLIER) has
 * elapsed. For example, if a GC takes 100ms and the multiplier is 60, don't gc for at least five
 * seconds - Never gc again if any single gc takes longer than system property PROP_GC_MAX
 */
@SuppressWarnings("restriction")
class DartIdleHelper {

  /**
   * The default minimum time between garbage collections.
   */
  private static final int DEFAULT_GC_INTERVAL = 60000;

  /**
   * The default maximum duration for a garbage collection, beyond which the explicit gc mechanism
   * is automatically disabled.
   */
  private static final int DEFAULT_GC_MAX = 8000;

  /**
   * The multiple of the last gc duration before we will consider doing another one.
   */
  private static final int GC_DELAY_MULTIPLIER = 60;

  /**
   * The time interval of no keyboard or mouse events after which the system is considered idle.
   */
  private static final int IDLE_INTERVAL = 5000;

  /**
   * The name of the boolean system property that specifies whether explicit garbage collection is
   * enabled.
   */
  private static final String PROP_GC = "ide.gc"; //$NON-NLS-1$

  /**
   * The name of the integer system property that specifies the minimum time interval in
   * milliseconds between garbage collections.
   */
  private static final String PROP_GC_INTERVAL = "ide.gc.interval"; //$NON-NLS-1$

  /**
   * The name of the integer system property that specifies the maximum duration for a garbage
   * collection. If this duration is ever exceeded, the explicit gc mechanism is disabled for the
   * remainder of the session.
   */
  private static final String PROP_GC_MAX = "ide.gc.max"; //$NON-NLS-1$

  protected IWorkbenchConfigurer configurer;

  private Listener idleListener;

  /**
   * The last time we garbage collected.
   */
  private long lastGC = System.currentTimeMillis();

  /**
   * The maximum gc duration. If this value is exceeded, the entire explicit gc mechanism is
   * disabled.
   */
  private int maxGC = DEFAULT_GC_MAX;
  /**
   * The minimum time interval until the next garbage collection
   */
  private int minGCInterval = DEFAULT_GC_INTERVAL;

  /**
   * The time interval until the next garbage collection
   */
  private int nextGCInterval = DEFAULT_GC_INTERVAL;

  private Job gcJob;

  private Runnable handler;

  /**
   * Creates and initializes the idle handler
   * 
   * @param aConfigurer The workbench configurer.
   */
  DartIdleHelper(IWorkbenchConfigurer aConfigurer) {
    this.configurer = aConfigurer;
    //don't gc while running tests because performance tests are sensitive to timing (see bug 121562)
    if (PlatformUI.getTestableObject().getTestHarness() != null) {
      return;
    }
    String enabled = System.getProperty(PROP_GC);
    //gc is turned on by default if property is missing
    if (enabled != null && enabled.equalsIgnoreCase(Boolean.FALSE.toString())) {
      return;
    }
    //init gc interval
    Integer prop = Integer.getInteger(PROP_GC_INTERVAL);
    if (prop != null && prop.intValue() >= 0) {
      minGCInterval = nextGCInterval = prop.intValue();
    }

    //init max gc interval
    prop = Integer.getInteger(PROP_GC_MAX);
    if (prop != null) {
      maxGC = prop.intValue();
    }

    createGarbageCollectionJob();

    //hook idle handler
    final Display display = configurer.getWorkbench().getDisplay();
    handler = new Runnable() {
      @Override
      public void run() {
        if (!display.isDisposed() && !configurer.getWorkbench().isClosing()) {
          int nextInterval;
          final long start = System.currentTimeMillis();
          //don't garbage collect if background jobs are running
          if (!Job.getJobManager().isIdle()) {
            nextInterval = IDLE_INTERVAL;
          } else if ((start - lastGC) < nextGCInterval) {
            //don't garbage collect if we have collected within the specific interval
            nextInterval = nextGCInterval - (int) (start - lastGC);
          } else {
            gcJob.schedule();
            nextInterval = minGCInterval;
          }
          display.timerExec(nextInterval, this);
        }
      }
    };
    idleListener = new Listener() {
      @Override
      public void handleEvent(Event event) {
        display.timerExec(IDLE_INTERVAL, handler);
      }
    };
    display.addFilter(SWT.KeyUp, idleListener);
    display.addFilter(SWT.MouseUp, idleListener);
  }

  /**
   * Shuts down the idle helper, removing any installed listeners, etc.
   */
  void shutdown() {
    if (idleListener == null) {
      return;
    }
    final Display display = configurer.getWorkbench().getDisplay();
    if (display != null && !display.isDisposed()) {
      try {
        display.asyncExec(new Runnable() {
          @Override
          public void run() {
            display.timerExec(-1, handler);
            display.removeFilter(SWT.KeyUp, idleListener);
            display.removeFilter(SWT.MouseUp, idleListener);
          }
        });
      } catch (SWTException ex) {
        // ignore (display might be disposed)
      }
    }
  }

  /**
   * Creates the job that performs garbage collection
   */
  private void createGarbageCollectionJob() {
    gcJob = new Job(IDEWorkbenchMessages.IDEIdleHelper_backgroundGC) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        final Display display = configurer.getWorkbench().getDisplay();
        if (display != null && !display.isDisposed()) {
          final long start = System.currentTimeMillis();
          System.gc();
          System.runFinalization();
          lastGC = start;
          final int duration = (int) (System.currentTimeMillis() - start);
          if (Policy.DEBUG_GC) {
            System.out.println("Explicit GC took: " + duration); //$NON-NLS-1$
          }
          if (duration > maxGC) {
            if (Policy.DEBUG_GC) {
              System.out.println("Further explicit GCs disabled due to long GC"); //$NON-NLS-1$
            }
            shutdown();
          } else {
            //if the gc took a long time, ensure the next gc doesn't happen for awhile
            nextGCInterval = Math.max(minGCInterval, GC_DELAY_MULTIPLIER * duration);
            if (Policy.DEBUG_GC) {
              System.out.println("Next GC to run in: " + nextGCInterval); //$NON-NLS-1$
            }
          }
        }
        return Status.OK_STATUS;
      }
    };
    gcJob.setSystem(true);
  }
}
