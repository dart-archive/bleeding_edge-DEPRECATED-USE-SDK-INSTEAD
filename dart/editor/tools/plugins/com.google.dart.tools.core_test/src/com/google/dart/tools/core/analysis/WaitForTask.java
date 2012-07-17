/*
 * Copyright (c) 2012, the Dart project authors.
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

public class WaitForTask {

  private final Object lock = new Object();
  private final TaskQueue queue;
  private boolean waiting;
  private boolean result;
  private Thread thread;

  public WaitForTask(TaskQueue queue) {
    this.queue = queue;
  }

  public boolean getResult() {
    synchronized (lock) {
      return result;
    }
  }

  public void interrupt() {
    thread.interrupt();
  }

  public boolean isWaiting() {
    synchronized (lock) {
      return waiting;
    }
  }

  public WaitForTask start() {
    synchronized (lock) {
      waiting = true;
      thread = new Thread(getClass().getSimpleName()) {
        @Override
        public void run() {
          result = queue.waitForTask();
          synchronized (lock) {
            waiting = false;
            lock.notify();
          }
        }
      };
      thread.start();
    }
    return this;
  }

  /**
   * Wait for the receiver's thread to complete
   * 
   * @param milliseconds wait upto the specified number of millisecond
   */
  public void waitForResult(long milliseconds) {
    synchronized (lock) {
      if (waiting) {
        try {
          lock.wait(milliseconds);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
  }

}
