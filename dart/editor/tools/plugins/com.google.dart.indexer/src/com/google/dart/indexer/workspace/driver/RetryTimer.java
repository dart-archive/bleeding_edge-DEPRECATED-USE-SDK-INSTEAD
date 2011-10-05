/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.workspace.driver;

/**
 * Thread safety: <code>isActive</code> is safe for calls from any thread; all other methods require
 * external synchronization for multithreaded usage.
 */
public class RetryTimer {
  private volatile long nextAttempTime;

  private static final int delays[] = new int[] {1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377};

  private int nextDelayIndex;

  public RetryTimer() {
    successfulAttemp();
  }

  public long delayUntilNextAttemp() {
    if (!isActive()) {
      throw new IllegalStateException(getClass().getSimpleName()
          + ": delayUntilNextAttemp can only be called when isActive is true");
    }
    return nextAttempTime - System.currentTimeMillis();
  }

  public void failedAttemp() {
    nextAttempTime = System.currentTimeMillis() + 1000 * delays[nextDelayIndex];
    if (nextDelayIndex + 1 < delays.length) {
      nextDelayIndex += 1;
    }
  }

  public boolean isActive() {
    return nextAttempTime != -1;
  }

  public boolean shouldMakeNextAttemp() {
    return delayUntilNextAttemp() <= 0;
  }

  public void successfulAttemp() {
    nextAttempTime = -1;
    nextDelayIndex = 0;
  }
}
