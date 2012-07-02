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
   * A queue of tasks to be performed. Lock against this object before accessing it.
   */
  private final ArrayList<Task> queue = new ArrayList<Task>();

  /**
   * The index at which new tasks are inserted into the queue. Lock against {@link #queue} before
   * accessing this field.
   */
  private int newTaskIndex = 0;

  /**
   * The index at which the task being performed can insert new tasks. Tracking this allows new
   * tasks to take priority and be first in the queue. Lock against {@link #queue} before accessing
   * this field.
   */
  private int subTaskIndex = 0;

  /**
   * Add a new {@link AnalyzeContextTask} to the end of the queue
   * 
   * @param task the task (not <code>null</code>)
   */
  public void addLastTask(Task task) {
    synchronized (queue) {
      queue.add(queue.size(), task);
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
   * Answer the last task in the queue or <code>null</code> if none
   */
  public Task getLastTask() {
    synchronized (queue) {
      if (queue.isEmpty()) {
        return null;
      }
      return queue.get(queue.size() - 1);
    }
  }

  /**
   * Answer the object used to synchronize and notify
   * 
   * @return the object (not <code>null</code>)
   */
  public Object getSharedLock() {
    return queue;
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
   * Remove the next task to be performed from the queue and answer that task. If the queue is
   * empty, then return <code>null</code>
   */
  public Task removeNextTask() {
    synchronized (queue) {
      if (queue.isEmpty()) {
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
   * Wait for a task to be added to the queue
   * 
   * @return <code>true</code> if the queue has tasks, or <code>false</code> if the wait was
   *         interrupted before a new task was added
   */
  public boolean waitForTask() {
    synchronized (queue) {
      if (!queue.isEmpty()) {
        return true;
      }
      try {
        queue.wait();
      } catch (InterruptedException e) {
        //$FALL-THROUGH$
      }
      return !queue.isEmpty();
    }
  }
}
