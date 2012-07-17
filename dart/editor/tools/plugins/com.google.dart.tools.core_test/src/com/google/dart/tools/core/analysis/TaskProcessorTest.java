package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.AbstractDartCoreTest;

public class TaskProcessorTest extends AbstractDartCoreTest {

  private TaskQueue queue;
  private TaskProcessor processor;
  private WaitForIdle listener;

  /**
   * Assert processor with no tasks moves to idle state
   */
  public void test_empty() throws Exception {
    waitAndAssertIdle(1);
  }

  /**
   * Assert single idle -> run -> idle sequence
   */
  public void test_perform() throws Exception {
    waitAndAssertIdle(1);
    BlockingTask task = new BlockingTask();
    queue.addNewTask(task);
    waitAndAssertRuning(1);
    task.unblock();
    waitAndAssertIdle(2);
    assertTrue(task.wasPerformed());
  }

  /**
   * Assert single idle -> run -> idle sequence for two tasks
   */
  public void test_perform2() throws Exception {
    waitAndAssertIdle(1);
    BlockingTask task1 = new BlockingTask();
    BlockingTask task2 = new BlockingTask();
    queue.addNewTask(task1);
    waitAndAssertRuning(1);
    queue.addNewTask(task2);
    assertRunning(1);
    task1.unblock();
    assertRunning(1);
    task2.unblock();
    waitAndAssertIdle(2);
    assertTrue(task1.wasPerformed());
    assertTrue(task2.wasPerformed());
  }

  /**
   * Assert single idle -> run -> idle sequence for four tasks
   */
  public void test_perform4() throws Exception {
    waitAndAssertIdle(1);
    BlockingTask task1 = new BlockingTask();
    BlockingTask task2 = new BlockingTask();
    queue.addNewTask(task1);
    queue.addNewTask(NullTask.newRequestTask());
    queue.addNewTask(task2);
    queue.addNewTask(NullTask.newRequestTask());
    task1.unblock();
    task2.unblock();
    waitAndAssertIdle(2);
    assertTrue(task1.wasPerformed());
    assertTrue(task2.wasPerformed());
  }

  /**
   * Assert processor with no tasks stays in idle state when stopped
   */
  public void test_stop() throws Exception {
    waitAndAssertIdle(1);
    queue.setAnalyzing(false);
    assertFalse(listener.waitForRun(10));
    waitAndAssertIdle(1);
  }

  /**
   * Assert stopped processor does not process new tasks
   */
  public void test_stop1() throws Exception {
    waitAndAssertIdle(1);
    queue.setAnalyzing(false);
    BlockingTask task = new BlockingTask();
    queue.addNewTask(task);
    task.unblock();
    assertFalse(listener.waitForRun(10));
    waitAndAssertIdle(1);
    assertEquals(false, task.wasPerformed());
  }

  /**
   * Assert that stopped processor finishes current task but does not process any more
   */
  public void test_stop4() throws Exception {
    waitAndAssertIdle(1);
    BlockingTask task1 = new BlockingTask();
    BlockingTask task2 = new BlockingTask();
    BlockingTask task3 = new BlockingTask();
    queue.addNewTask(task1);
    waitAndAssertRuning(1);
    queue.addNewTask(task2);
    queue.addNewTask(task3);
    queue.setAnalyzing(false);
    task1.unblock();
    task2.unblock();
    task3.unblock();
    waitAndAssertIdle(2);
    assertEquals(true, task1.wasPerformed());
    assertEquals(false, task2.wasPerformed());
    assertEquals(false, task3.wasPerformed());
  }

  @Override
  protected void setUp() {
    queue = new TaskQueue();
    processor = new TaskProcessor(queue);
    listener = new WaitForIdle(queue, processor);
    queue.setAnalyzing(true);
    processor.start();
  }

  @Override
  protected void tearDown() throws Exception {
    queue.setAnalyzing(false);
  }

  private void assertIdle(int expectedIdleCount) {
    assertTrue(processor.waitForIdle(10));
    assertEquals(true, processor.isIdle());
    assertEquals(true, listener.isIdle());
    assertEquals(expectedIdleCount, listener.getIdleCount());
    assertNull(queue.removeNextTask());
  }

  private void assertRunning(int expectedIdleCount) {
    assertEquals(false, processor.isIdle());
    assertEquals(false, listener.isIdle());
    assertEquals(expectedIdleCount, listener.getIdleCount());
  }

  private void waitAndAssertIdle(int expectedIdleCount) {
    listener.waitForIdle(expectedIdleCount, 50);
    assertIdle(expectedIdleCount);
  }

  private void waitAndAssertRuning(int expectedIdleCount) {
    assertTrue(listener.waitForRun(10));
    assertRunning(expectedIdleCount);
  }
}
