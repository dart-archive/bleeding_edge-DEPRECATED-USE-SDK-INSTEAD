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

final class WaitForIdle implements TaskListener {
  private final Object lock = new Object();
  private final TaskQueue queue;
  private int idleCount;
  private boolean idle;
  private Task task;

  public WaitForIdle(TaskQueue queue, TaskProcessor processor) {
    this.queue = queue;
    processor.addIdleListener(this);
  }

  public void addNewTaskOnIdle(Task task) {
    this.task = task;
  }

  public int getIdleCount() {
    synchronized (lock) {
      return idleCount;
    }
  }

  @Override
  public void idle(boolean idle) {
    synchronized (lock) {
      this.idle = idle;
      if (idle) {
        idleCount++;
        // unblock threads in waitForIdle() and waitForRun()
        lock.notify();
        if (task != null) {
          queue.addNewTask(task);
        }
      }
    }
  }

  public boolean isIdle() {
    synchronized (lock) {
      return idle;
    }
  }

  @Override
  public void processing(int toBeProcessed) {
    // ignored
  }

  /**
   * Wait up to the specified number of milliseconds for the receiver to have the specified idle
   * count. If the specified number is less than or equal to zero, then this method returns
   * immediately.
   * 
   * @param expectedIdleCount the expected idle count
   * @param milliseconds the maximum number of milliseconds to wait
   * @return <code>true</code> if the receiver has the specified idle count, else <code>false</code>
   */
  public boolean waitForIdle(int expectedIdleCount, long milliseconds) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      while (expectedIdleCount > idleCount) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          lock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      return expectedIdleCount == idleCount;
    }
  }

  /**
   * Wait up to the specified number of milliseconds for the receiver to be notified that the
   * background process is not idle. If the specified number is less than or equal to zero, then
   * this method returns immediately.
   * 
   * @param milliseconds the maximum number of milliseconds to wait
   * @return <code>true</code> if the receiver has been notified, else <code>false</code>
   */
  public boolean waitForRun(int milliseconds) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      while (idle) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          lock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      return true;
    }

  }
}
