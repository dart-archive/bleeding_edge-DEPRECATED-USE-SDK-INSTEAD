/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import static junit.framework.Assert.fail;

public class AnalysisTestUtilities {

  /**
   * Wait for any background analysis to be complete
   */
  public static void waitForAnalysis() {
    if (DartCoreDebug.ANALYSIS_SERVER) {
      waitForIdle(60000);
    }
  }

  /**
   * Wait up to the specified amount of time for the specified analysis server to be idle. If the
   * specified number is less than or equal to zero, then this method returns immediately.
   * 
   * @param server the analysis server to be tested (not <code>null</code>)
   * @param milliseconds the maximum number of milliseconds to wait
   */
  public static void waitForIdle(AnalysisServer server, long milliseconds) {
    final Object waitForIdleLock = new Object();

    AnalysisListener listener = new AnalysisListener() {

      @Override
      public void discarded(AnalysisEvent event) {
        // ignored
      }

      @Override
      public void idle(boolean idle) {
        if (idle) {
          synchronized (waitForIdleLock) {
            waitForIdleLock.notifyAll();
          }
        }
      }

      @Override
      public void parsed(AnalysisEvent event) {
        // ignored
      }

      @Override
      public void resolved(AnalysisEvent event) {
        // ignored
      }
    };

    server.addAnalysisListener(listener);
    try {
      long endTime = System.currentTimeMillis() + milliseconds;
      while (true) {
        synchronized (waitForIdleLock) {
          while (!server.isIdle()) {
            long delta = endTime - System.currentTimeMillis();
            if (delta <= 0) {
              fail("AnalysisServer not idle");
            }
            try {
              waitForIdleLock.wait(delta);
            } catch (InterruptedException e) {
              //$FALL-THROUGH$
            }
          }
        }
        if (!ResourceChangeListener.isScanning()) {
          return;
        }
        if (System.currentTimeMillis() >= endTime) {
          fail("AnalysisServer not idle");
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    } finally {
      server.removeAnalysisListener(listener);
    }
  }

  /**
   * Wait up to the specified amount of time for the default analysis server to be idle. If the
   * specified number is less than or equal to zero, then this method returns immediately.
   * 
   * @param milliseconds the maximum number of milliseconds to wait
   * @return <code>true</code> if the server is idle
   */
  public static void waitForIdle(long milliseconds) {
    waitForIdle(SystemLibraryManagerProvider.getDefaultAnalysisServer(), milliseconds);
  }
}
