/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.ui.internal;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import java.util.concurrent.TimeUnit;

/**
 * Tracks heap size and disables some features if JVM is about to run out of memory.
 */
public class HeapTracker {
  /**
   * Sleep 1000 milliseconds.
   */
  private static final long ONE_SECOND_MILLIS = 1000;

  /**
   * The percentage of total heap size relative to the max heap size, starting from which we begin
   * to check free memory.
   */
  private static final long MAX_TOTAL_PERCENT = 80;

  /**
   * The required minimum number of MB of free memory.
   */
  private static final long MIN_FREE_MB = 100;

  /**
   * If free memory is low this number of seconds, run GC.
   */
  private static final int NUM_NO_FREE_MEMORY_SAMPLES_1 = 5;

  /**
   * If free memory is low this number of seconds, treat it a low memory condition.
   */
  private static final int NUM_NO_FREE_MEMORY_SAMPLES_2 = 10;

  /**
   * The number of milliseconds to wait after disabling index.
   */
  private static final int WAIT_AFTER_INDEX_MILLIS = 10 * 1000;

  /**
   * Number of bytes in 1 megabyte.
   */
  private static final long MB = 1024 * 1024;

  /**
   * The time when index was disabled.
   */
  private static long indexDisabledMillis = -1;

  /**
   * The current number of low free memory observations in a row.
   */
  private static int noFreeMemoryCounter = 0;

  public static void start() {
    Thread thread = new Thread() {
      @Override
      public void run() {
        doTrack();
      }
    };
    thread.setName("Editor heap tracking thread");
    thread.setDaemon(true);
    thread.start();
  }

  private static void doTrack() {
    while (true) {
      Uninterruptibles.sleepUninterruptibly(ONE_SECOND_MILLIS, TimeUnit.MILLISECONDS);
      if (isLowMemory()) {
        // if index was cleared recently, ignore OOM
        if (System.currentTimeMillis() - indexDisabledMillis < WAIT_AFTER_INDEX_MILLIS) {
          continue;
        }
        // first solution - disable index
        if (indexDisabledMillis == -1) {
          DartCore.getProjectManager().disableIndex();
          Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
              MessageDialog.openWarning(
                  DartToolsPlugin.getActiveWorkbenchShell(),
                  "Almost out of memory",
                  "Editor is almost out of memory.\n"
                      + "To keep it responsive search and refactoring features are disabled now.\n\n"
                      + "It is recommended to close some projects or give Editor more memory and restart it.");
            }
          });
          indexDisabledMillis = System.currentTimeMillis();
          continue;
        }
        // final solution - inform user that Editor is again about to run out of memory
        Display.getDefault().syncExec(new Runnable() {
          @Override
          public void run() {
            MessageDialog.openWarning(
                DartToolsPlugin.getActiveWorkbenchShell(),
                "Almost out of memory",
                "Search and refactoring features are disabled, but Editor is again almost out of memory.\n\n"
                    + "It is strongly recommended to give Editor more memory and restart it.");
          }
        });
        // stop tracking heap, we cannot do anything
        return;
      }
    }
  }

  private static boolean isLowFreeMemory() {
    Runtime runtime = Runtime.getRuntime();
    long freeMemory = runtime.freeMemory();
    return freeMemory / MB < MIN_FREE_MB;
  }

  private static boolean isLowMemory() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
//    long freeMemory = runtime.freeMemory();
//    System.out.println("max=" + maxMemory / MB + "  total=" + totalMemory / MB + "  free="
//        + freeMemory / MB);
    // check if heap can grow
    long allocatedPercent = (totalMemory * 100) / maxMemory;
    if (allocatedPercent < MAX_TOTAL_PERCENT) {
      return false;
    }
    // check if almost OOM
    if (!isLowFreeMemory()) {
      noFreeMemoryCounter = 0;
      return false;
    }
    noFreeMemoryCounter++;
//    System.out.println("noFreeMemoryCounter: " + noFreeMemoryCounter);
    // if almost OOM long enough, try to run GC
    if (noFreeMemoryCounter == NUM_NO_FREE_MEMORY_SAMPLES_1) {
      System.gc();
      return false;
    }
    // give GC some time
    if (noFreeMemoryCounter < NUM_NO_FREE_MEMORY_SAMPLES_2) {
      return false;
    }
    // still almost OOM, report it
    noFreeMemoryCounter = 0;
    return true;
  }
}
