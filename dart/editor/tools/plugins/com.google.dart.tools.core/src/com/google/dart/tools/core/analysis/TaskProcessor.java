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

import com.google.dart.tools.core.DartCore;

/**
 * Executes analysis {@link Task}s that have been placed on a {@link TaskQueue}
 */
public class TaskProcessor {

  private final Object lock = new Object();

  /**
   * The queue from which tasks to be performed are removed (not <code>null</code>)
   */
  private final TaskQueue queue;

  /**
   * The background thread on which analysis tasks are performed or <code>null</code> if the
   * background process has not been started yet.
   */
  private Thread backgroundThread;

  /**
   * <code>true</code> if there are no tasks queued (or analyzing is <code>false</code>) and no
   * tasks being performed. Synchronize against {@link #lock} before accessing this field.
   */
  private boolean isIdle = false;

  /**
   * An operation to be performed when the queue is empty and listeners have been notified that the
   * receiver is idle. May be {@code null} if there is no idle operation.
   */
  private Runnable idleOperation;

  /**
   * A collection of objects to be notified when the receiver is idle. Synchronize against
   * {@link #lock} before accessing this field.
   */
  IdleListener[] idleListeners = new IdleListener[0];

  /**
   * Construct a new instance that removes tasks to be performed from the specified queue
   * 
   * @param queue the task queue (not <code>null</code>)
   */
  public TaskProcessor(TaskQueue queue) {
    this.queue = queue;
  }

  /**
   * Add an object to be notified when there are no tasks queued (or analyzing is <code>false</code>
   * ) and no tasks being performed.
   * 
   * @param listener the object to be notified
   */
  public void addIdleListener(IdleListener listener) {
    if (listener == null) {
      return;
    }
    synchronized (lock) {
      for (int i = 0; i < idleListeners.length; i++) {
        if (idleListeners[i] == listener) {
          return;
        }
      }
      int oldLen = idleListeners.length;
      IdleListener[] newListeners = new IdleListener[oldLen + 1];
      System.arraycopy(idleListeners, 0, newListeners, 0, oldLen);
      newListeners[oldLen] = listener;
      idleListeners = newListeners;
    }
  }

  /**
   * Add the specified task to the queue and wait for the receiver to be running before returning
   * 
   * @param task the task to be added (not <code>null</code>)
   * @param milliseconds the number of milliseconds to wait
   * @return <code>true</code> if the receiver is running, else <code>false</code>
   */
  public boolean addLastTaskAndWaitUntilRunning(Task task, long milliseconds) {
    synchronized (lock) {
      queue.addLastTask(task);
      return waitUntilRunning(milliseconds);
    }
  }

  /**
   * Add the specified task to the queue and wait for the receiver to be running before returning
   * 
   * @param task the task to be added (not <code>null</code>)
   * @param milliseconds the number of milliseconds to wait
   * @return <code>true</code> if the receiver is running, else <code>false</code>
   */
  public boolean addNewTaskAndWaitUntilRunning(Task task, long milliseconds) {
    synchronized (lock) {
      queue.addNewTask(task);
      return waitUntilRunning(milliseconds);
    }
  }

  /**
   * Answer <code>true</code> if there are no tasks queued (or analyzing is false) and no tasks
   * being performed. The idle state may change the moment this method returns, so clients should
   * not depend upon this result.
   */
  public boolean isIdle() {
    synchronized (lock) {
      return isIdle;
    }
  }

  /**
   * Remove the specified object from the list of objects to be notified
   * 
   * @param listener the object to be removed
   */
  public void removeIdleListener(IdleListener listener) {
    synchronized (lock) {
      int oldLen = idleListeners.length;
      for (int i = 0; i < oldLen; i++) {
        if (idleListeners[i] == listener) {
          IdleListener[] newListeners = new IdleListener[oldLen - 1];
          System.arraycopy(idleListeners, 0, newListeners, 0, i);
          System.arraycopy(idleListeners, i + 1, newListeners, i, oldLen - 1 - i);
          idleListeners = newListeners;
          return;
        }
      }
    }
  }

  /**
   * Set the operation to be executed when the queue is empty and after all have been notified that
   * the processor is idle. This operation should execute quickly or frequently call both
   * {@link TaskQueue#isAnalyzing()} and {@link TaskQueue#isEmpty()} to keep analysis responsive.
   * 
   * @param task the task or {@code null} if none
   */
  public void setIdleOperation(Runnable runnable) {
    idleOperation = runnable;
  }

  /**
   * Start the background analysis process if it has not already been started. This background
   * thread will process tasks from the associated event queue until the
   * {@link TaskQueue#setAnalyzing(boolean)} method is called with a value of <code>false</code>.
   * 
   * @throws IllegalStateException if background thread has already been started
   */
  public void start() {
    synchronized (lock) {
      if (backgroundThread != null) {
        throw new IllegalStateException();
      }
      backgroundThread = new Thread("Analysis Server") {

        @Override
        public void run() {
          try {
            while (true) {

              // Running :: Execute tasks from the queue
              while (true) {
                notifyProgress();
                Task task = queue.removeNextTask();
                // if no longer analyzing or queue is empty, then switch to idle state
                if (task == null) {
                  break;
                }
                try {
                  task.perform();
                } catch (Throwable e) {
                  DartCore.logError("Analysis Task Exception", e);
                }
              }

              // Notify :: Changing state from Running to Idle
              notifyIdle(true);

              // Perform the idle operation if there is one
              Runnable runnable = idleOperation;
              if (runnable != null) {
                try {
                  runnable.run();
                } catch (Throwable e) {
                  idleOperation = null;
                  DartCore.logError("Idle Operation Exception", e);
                }
              }

              // Idle :: Wait for new tasks on the queue... or analysis to be canceled
              if (!queue.waitForTask()) {
                break;
              }

              // Notify :: Changing state from Idle to Running
              notifyIdle(false);

            }
          } catch (Throwable e) {
            DartCore.logError("Analysis Server Exception", e);
          } finally {
            synchronized (lock) {
              backgroundThread = null;
              // Ensure waiting threads are unblocked
              lock.notifyAll();
            }
          }
        }
      };
      backgroundThread.start();
    }
  }

  /**
   * Wait up to the specified number of milliseconds for the receiver to be idle. If the specified
   * number is less than or equal to zero, then this method returns immediately.
   * 
   * @param milliseconds the maximum number of milliseconds to wait
   * @return <code>true</code> if the receiver is idle, else <code>false</code>
   */
  public boolean waitForIdle(long milliseconds) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      while (!isIdle) {
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

  /**
   * Set the idle state and notify any listeners if the state has changed
   * 
   * @param idle <code>true</code> if the receiver is now idle, else <code>false</code>
   */
  private void notifyIdle(boolean idle) {
    IdleListener[] listenersToNotify;
    synchronized (lock) {
      if (isIdle == idle) {
        return;
      }
      listenersToNotify = idleListeners;
    }
    for (IdleListener listener : listenersToNotify) {
      try {
        listener.idle(idle);
      } catch (Throwable e) {
        DartCore.logError("Exception during idle notification", e);
      }
    }
    /*
     * Set idle state and notify threads waiting for state change *after* notifying listeners
     * so that #add[New/Last]TaskAndWaitUntilRunning will return after listeners have been notified
     */
    synchronized (lock) {
      isIdle = idle;
      lock.notifyAll();
    }
  }

  /**
   * Notify any listeners about the number of tasks remaining.
   */
  private void notifyProgress() {
    IdleListener[] listenersToNotify;
    synchronized (lock) {
      listenersToNotify = idleListeners;
    }
    for (IdleListener listener : listenersToNotify) {
      if (listener instanceof TaskListener) {
        try {
          ((TaskListener) listener).processing(queue.size());
        } catch (Throwable e) {
          DartCore.logError("Exception during processing notification", e);
        }
      }
    }
  }

  /**
   * Wait up to the specified number of milliseconds for the receiver to start (or finish)
   * processing tasks or analysis to be turned off. If the specified number is less than or equal to
   * zero, then this method returns immediately.
   * 
   * @param milliseconds the maximum number of milliseconds to wait
   * @return <code>true</code> if the receiver is in the specified state, else <code>false</code>
   */
  private boolean waitUntilRunning(long milliseconds) {
    synchronized (lock) {
      long end = System.currentTimeMillis() + milliseconds;
      // if receiver is idle, but queue is empty, then receiver has finished processing tasks
      while (isIdle && !queue.isEmpty() && queue.isAnalyzing()) {
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
