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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A queue of pending analysis tasks. Tasks are prioritized such that updates take priority over
 * requests, which take priority over background tasks.
 */
public class TaskQueue {

  /**
   * A queue of tasks to be performed. Synchronize against this object before accessing it.
   */
  private final ArrayList<Task> queue = new ArrayList<Task>();

  /**
   * The index at which new tasks are inserted into the queue. Synchronize against {@link #queue}
   * before accessing this field.
   */
  private int newTaskIndex = 0;

  /**
   * The index at which the task being performed can insert new tasks. Tracking this allows new
   * tasks to take priority and be first in the queue. Synchronize against {@link #queue} before
   * accessing this field.
   */
  private int subTaskIndex = 0;

  /**
   * <code>true</code> if the background thread should continue executing analysis tasks.
   * Synchronize against {@link #queue} before accessing this field.
   */
  private boolean analyzing;

  /**
   * Add a new {@link AnalyzeContextTask} to the end of the queue. If this task has already been
   * added to the end of the queue, don't add it again.
   * 
   * @param task the task (not <code>null</code>)
   */
  public void addLastTask(Task task) {
    synchronized (queue) {
      int index = queue.size();
      if (index > 0 && queue.get(index - 1) == task) {
        return;
      }
      queue.add(index, task);
      // notify any threads blocked in waitForTask()
      queue.notifyAll();
    }
  }

  /**
   * Insert a new task into the queue. Priority tasks are queued in the order they are received
   * ahead of other tasks. Other tasks are queued in reverse order after priority tasks so that more
   * recently added tasks will be performed first.
   * 
   * @param task the tasks (not <code>null</code>)
   */
  public void addNewTask(Task task) {
    synchronized (queue) {
      queue.add(newTaskIndex, task);
      if (task.isPriority()) {
        newTaskIndex++;
      }
      subTaskIndex++;
      // notify any threads blocked in waitForTask()
      queue.notifyAll();
    }
  }

  /**
   * Add a task representing a portion of a larger task. Subtasks are queued in the order received
   * relative to the last task returned by {@link #removeNextTask()} behind priority tasks. This
   * method should only be called on the background thread.
   * 
   * @param task the sub task (not <code>null</code>)
   */
  public void addSubTask(Task task) {
    synchronized (queue) {
      queue.add(subTaskIndex, task);
      subTaskIndex++;
    }
  }

  /**
   * Answer an array of the tasks currently in the queue in the order in which they appear.
   * Modifying this array will not modify the state of the queue.
   */
  public Task[] getTasks() {
    synchronized (queue) {
      return queue.toArray(new Task[queue.size()]);
    }
  }

  /**
   * Answer <code>true</code> if the receiver's {@link #removeNextTask()} will return the next task
   * on the queue or <code>false</code> if the that method will always return <code>null</code>
   * regardless of how many tasks are queued.
   */
  public boolean isAnalyzing() {
    synchronized (queue) {
      return analyzing;
    }
  }

  public boolean isEmpty() {
    synchronized (queue) {
      return queue.isEmpty();
    }
  }

  /**
   * Remove all background tasks
   */
  public void removeBackgroundTasks() {
    synchronized (queue) {
      Iterator<Task> iter = queue.iterator();
      while (iter.hasNext()) {
        if (iter.next().isBackgroundAnalysis()) {
          iter.remove();
        }
      }
    }
  }

  /**
   * Remove the next task to be performed from the queue and answer that task. If the queue is empty
   * or {@link #isAnalyzing()} is false, then return <code>null</code>
   */
  public Task removeNextTask() {
    synchronized (queue) {
      if (!analyzing || queue.isEmpty()) {
        return null;
      }
      if (newTaskIndex > 0) {
        newTaskIndex--;
      }
      subTaskIndex = newTaskIndex;
      return queue.remove(0);
    }
  }

  /**
   * Set the receiver's analyzing state. If <code>true</code> then receiver's
   * {@link #removeNextTask()} will return the next task on the queue otherwise that method will
   * always return <code>null</code> regardless of how many tasks are queued.
   * 
   * @return the analyzing state of the receiver prior to calling this method
   */
  public boolean setAnalyzing(boolean analyzing) {
    synchronized (queue) {
      if (this.analyzing == analyzing) {
        return analyzing;
      }
      this.analyzing = analyzing;
      // If no longer analyzing, notify any threads blocked in waitForTask()
      if (!analyzing) {
        queue.notifyAll();
      }
      return !analyzing;
    }
  }

  public int size() {
    synchronized (queue) {
      return queue.size();
    }
  }

  /**
   * Wait for a task to be added to the queue
   * 
   * @return <code>true</code> if the queue has tasks, or <code>false</code> if analysis was
   *         canceled.
   */
  public boolean waitForTask() {
    synchronized (queue) {
      while (true) {
        if (!analyzing) {
          return false;
        }
        if (!queue.isEmpty()) {
          return true;
        }
        try {
          queue.wait();
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
    }
  }
}
