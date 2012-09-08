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

import com.google.dart.tools.core.AbstractDartCoreTest;

import java.io.File;

public class TaskQueueTest extends AbstractDartCoreTest {

  private TaskQueue queue;

  public void test_addBackgroundTask() throws Exception {
    NullTask backgroundTask = NullTask.newBackgroundTask();

    queue.addNewTask(backgroundTask);

    assertTrue(backgroundTask == queue.removeNextTask());
    assertNull(queue.removeNextTask());
  }

  /**
   * Assert addLastTask adds to the end of the queue only once
   */
  public void test_addLastTask() throws Exception {
    NullTask backgroundTask = NullTask.newBackgroundTask();
    NullTask requestTask = NullTask.newRequestTask();
    NullTask subTask = NullTask.newUpdateTask();
    NullTask lastTask = NullTask.newUpdateTask();

    queue.addNewTask(backgroundTask);
    queue.addLastTask(lastTask);
    queue.addSubTask(subTask);
    queue.addLastTask(lastTask);
    queue.addNewTask(requestTask);

    Task[] tasks = queue.getTasks();

    assertTrue(requestTask == queue.removeNextTask());
    assertTrue(backgroundTask == queue.removeNextTask());
    assertTrue(subTask == queue.removeNextTask());
    assertTrue(lastTask == queue.removeNextTask());
    assertNull(queue.removeNextTask());

    assertEquals(4, tasks.length);
    assertTrue(requestTask == tasks[0]);
    assertTrue(backgroundTask == tasks[1]);
    assertTrue(subTask == tasks[2]);
    assertTrue(lastTask == tasks[3]);
  }

  /**
   * Assert that requests and background tasks are queued in the reverse order received
   */
  public void test_addNewTask_requests() throws Exception {
    NullTask backgroundTask1 = NullTask.newBackgroundTask();
    NullTask backgroundTask2 = NullTask.newBackgroundTask();
    NullTask requestTask1 = NullTask.newRequestTask();
    NullTask requestTask2 = NullTask.newRequestTask();

    queue.addNewTask(backgroundTask1);
    queue.addNewTask(requestTask1);
    queue.addNewTask(backgroundTask2);
    queue.addNewTask(requestTask2);

    assertTrue(requestTask2 == queue.removeNextTask());
    assertTrue(backgroundTask2 == queue.removeNextTask());
    assertTrue(requestTask1 == queue.removeNextTask());
    assertTrue(backgroundTask1 == queue.removeNextTask());
    assertNull(queue.removeNextTask());
  }

  /**
   * Assert that updates are queued in the order received and before background tasks
   */
  public void test_addNewTask_updatesBeforeBackgroundTasks() throws Exception {
    NullTask backgroundTask = NullTask.newBackgroundTask();
    NullTask updateTask1 = NullTask.newUpdateTask();
    NullTask updateTask2 = NullTask.newUpdateTask();

    queue.addNewTask(updateTask1);
    queue.addNewTask(backgroundTask);
    queue.addNewTask(updateTask2);

    assertTrue(updateTask1 == queue.removeNextTask());
    assertTrue(updateTask2 == queue.removeNextTask());
    assertTrue(backgroundTask == queue.removeNextTask());
    assertNull(queue.removeNextTask());
  }

  /**
   * Assert that updates are queued in the order received and before requests
   */
  public void test_addNewTask_updatesBeforeRequests() throws Exception {
    NullTask requestTask = NullTask.newRequestTask();
    NullTask updateTask1 = NullTask.newUpdateTask();
    NullTask updateTask2 = NullTask.newUpdateTask();

    queue.addNewTask(updateTask1);
    queue.addNewTask(requestTask);
    queue.addNewTask(updateTask2);

    assertTrue(updateTask1 == queue.removeNextTask());
    assertTrue(updateTask2 == queue.removeNextTask());
    assertTrue(requestTask == queue.removeNextTask());
    assertNull(queue.removeNextTask());
  }

  /**
   * Assert subtasks are queued in the order received
   */
  public void test_addSubTask_order() throws Exception {
    NullTask subTask1 = NullTask.newUpdateTask();
    NullTask subTask2 = NullTask.newUpdateTask();

    queue.addSubTask(subTask1);
    queue.addSubTask(subTask2);

    assertTrue(subTask1 == queue.removeNextTask());
    assertTrue(subTask2 == queue.removeNextTask());
    assertNull(queue.removeNextTask());
  }

  /**
   * Assert subtasks are queued relative to the last task removed after priority tasks
   */
  public void test_addSubTask_relative() throws Exception {
    NullTask updateTask1 = NullTask.newUpdateTask();
    NullTask updateTask2 = NullTask.newUpdateTask();
    NullTask requestTask1 = NullTask.newRequestTask();
    NullTask requestTask2 = NullTask.newRequestTask();
    NullTask requestTask3 = NullTask.newRequestTask();
    NullTask subTask1 = NullTask.newUpdateTask();
    NullTask subTask2 = NullTask.newUpdateTask();
    NullTask subTask3 = NullTask.newUpdateTask();

    queue.addNewTask(requestTask1);
    queue.addSubTask(subTask1);
    queue.addNewTask(updateTask1);
    queue.addNewTask(requestTask2);

    assertTrue(updateTask1 == queue.removeNextTask());

    queue.addSubTask(subTask2);
    queue.addSubTask(subTask3);
    queue.addNewTask(updateTask2);
    queue.addNewTask(requestTask3);

    assertTrue(updateTask2 == queue.removeNextTask());
    assertTrue(requestTask3 == queue.removeNextTask());
    assertTrue(subTask2 == queue.removeNextTask());
    assertTrue(subTask3 == queue.removeNextTask());
    assertTrue(requestTask2 == queue.removeNextTask());
    assertTrue(requestTask1 == queue.removeNextTask());
    assertTrue(subTask1 == queue.removeNextTask());
    assertNull(queue.removeNextTask());
  }

  /**
   * Assert that analyzing determines whether or not removeNextTask removes tasks from the queue but
   * that you can always add tasks to the queue
   */
  public void test_analyzing() throws Exception {
    NullTask backgroundTask = NullTask.newBackgroundTask();
    NullTask requestTask1 = NullTask.newRequestTask();
    NullTask requestTask2 = NullTask.newRequestTask();
    NullTask subTask1 = NullTask.newUpdateTask();
    NullTask subTask2 = NullTask.newUpdateTask();
    NullTask lastTask1 = NullTask.newUpdateTask();
    NullTask lastTask2 = NullTask.newUpdateTask();

    queue.addNewTask(backgroundTask);
    queue.addLastTask(lastTask1);
    queue.addSubTask(subTask1);
    queue.addNewTask(requestTask1);

    assertTrue(queue.isAnalyzing());
    assertTrue(requestTask1 == queue.removeNextTask());
    assertTrue(queue.setAnalyzing(false));
    assertFalse(queue.isAnalyzing());
    assertNull(queue.removeNextTask());

    queue.addLastTask(lastTask2);
    queue.addSubTask(subTask2);
    queue.addNewTask(requestTask2);

    assertFalse(queue.isAnalyzing());
    assertNull(queue.removeNextTask());
    assertFalse(queue.setAnalyzing(true));
    assertTrue(queue.isAnalyzing());
    assertTrue(requestTask2 == queue.removeNextTask());
    assertTrue(subTask2 == queue.removeNextTask());
    assertTrue(backgroundTask == queue.removeNextTask());
    assertTrue(queue.setAnalyzing(false));
    assertFalse(queue.isAnalyzing());
    assertNull(queue.removeNextTask());
    assertFalse(queue.setAnalyzing(true));
    assertTrue(queue.isAnalyzing());
    assertTrue(subTask1 == queue.removeNextTask());
    assertTrue(lastTask1 == queue.removeNextTask());
    assertTrue(lastTask2 == queue.removeNextTask());
    assertNull(queue.removeNextTask());
  }

  public void test_empty() throws Exception {
    assertNull(queue.removeNextTask());
  }

  public void test_removeBackgroundTasks() throws Exception {
    NullTask backgroundTask1 = NullTask.newBackgroundTask(new File("foo"));
    NullTask backgroundTask2 = NullTask.newBackgroundTask();
    NullTask requestTask1 = NullTask.newRequestTask();
    NullTask requestTask2 = NullTask.newRequestTask();
    NullTask updateTask1 = NullTask.newUpdateTask();
    NullTask updateTask2 = NullTask.newUpdateTask();

    queue.addNewTask(backgroundTask1);
    queue.addNewTask(requestTask1);
    queue.addNewTask(updateTask1);
    queue.addNewTask(backgroundTask2);
    queue.addNewTask(updateTask2);
    queue.addNewTask(requestTask2);

    Task[] tasks = queue.getTasks();
    assertEquals(6, tasks.length);
    assertTrue(updateTask1 == tasks[0]);
    assertTrue(updateTask2 == tasks[1]);
    assertTrue(requestTask2 == tasks[2]);
    assertTrue(backgroundTask2 == tasks[3]);
    assertTrue(requestTask1 == tasks[4]);
    assertTrue(backgroundTask1 == tasks[5]);

    queue.removeBackgroundTasks(new File("boo"));

    tasks = queue.getTasks();
    assertEquals(5, tasks.length);
    assertTrue(updateTask1 == tasks[0]);
    assertTrue(updateTask2 == tasks[1]);
    assertTrue(requestTask2 == tasks[2]);
    assertTrue(requestTask1 == tasks[3]);
    assertTrue(backgroundTask1 == tasks[4]);
  }

  /**
   * Assert that waitForTask returns <code>false</code> when not analyzing
   */
  public void test_waitForTask_analyzing() throws Exception {
    queue.setAnalyzing(false);
    assertFalse(queue.waitForTask());
    queue.setAnalyzing(true);

    WaitForTask wait = new WaitForTask(queue).start();
    assertTrue(wait.isWaiting());
    wait.waitForResult(10);
    assertTrue(wait.isWaiting());
    wait.interrupt();
    wait.waitForResult(10);
    assertTrue(wait.isWaiting());
    queue.setAnalyzing(false);
    wait.waitForResult(10);
    assertFalse(wait.isWaiting());
    assertFalse(wait.getResult());
  }

  /**
   * Assert that addLastTask notifies waitForTask
   */
  public void test_waitForTask_last() throws Exception {
    NullTask task = NullTask.newRequestTask();
    queue.addLastTask(task);
    assertTrue(queue.waitForTask());
    assertTrue(task == queue.removeNextTask());

    WaitForTask wait = new WaitForTask(queue).start();
    assertTrue(wait.isWaiting());
    task = NullTask.newRequestTask();
    queue.addLastTask(task);
    wait.waitForResult(5000);
    assertFalse(wait.isWaiting());
    assertTrue(wait.getResult());
    assertTrue(task == queue.removeNextTask());
  }

  /**
   * Assert that addNewTask notifies waitForTask
   */
  public void test_waitForTask_new() throws Exception {
    NullTask task = NullTask.newBackgroundTask();
    queue.addNewTask(task);
    assertTrue(queue.waitForTask());
    assertTrue(task == queue.removeNextTask());

    WaitForTask wait = new WaitForTask(queue).start();
    assertTrue(wait.isWaiting());
    task = NullTask.newBackgroundTask();
    queue.addNewTask(task);
    wait.waitForResult(5000);
    assertFalse(wait.isWaiting());
    assertTrue(wait.getResult());
    assertTrue(task == queue.removeNextTask());
  }

  @Override
  protected void setUp() {
    queue = new TaskQueue();
    queue.setAnalyzing(true);
  }
}
