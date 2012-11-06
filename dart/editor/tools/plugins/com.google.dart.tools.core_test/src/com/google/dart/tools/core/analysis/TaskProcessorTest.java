package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.AbstractDartCoreTest;

public class TaskProcessorTest extends AbstractDartCoreTest {

  private class AddTaskThread extends Thread {
    private Object lock = new Object();
    private final Task task;
    private boolean complete = false;
    private boolean result;

    public AddTaskThread(Task task) {
      super("AddTaskThread");
      this.task = task;
    }

    public void assertResult(boolean expectedResult) {
      assertEquals(expectedResult, result);
    }

    @Override
    public void run() {
      result = processor.addNewTaskAndWaitUntilRunning(task, FIVE_MINUTES_MS);
      synchronized (lock) {
        complete = true;
        lock.notifyAll();
      }
    }

    boolean waitForComplete(long milliseconds) {
      synchronized (lock) {
        long end = System.currentTimeMillis() + milliseconds;
        while (!complete) {
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
      }
      return true;
    }
  }

  private static class BlockingIdleListener implements TaskListener {
    private final Object lock = new Object();
    private boolean blocked = false;

    @Override
    public void idle(boolean idle) {
      synchronized (lock) {
        blocked = true;
        lock.notifyAll();
        while (blocked) {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
      }
    }

    @Override
    public void processing(int toBeProcessed) {
      // ignored
    }

    void unblock() {
      synchronized (lock) {
        blocked = false;
        lock.notifyAll();
      }
    }

    boolean waitUntilBlocked(long milliseconds) {
      synchronized (lock) {
        long end = System.currentTimeMillis() + milliseconds;
        while (!blocked) {
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
      }
      return true;
    }
  }

  private static final long FIVE_MINUTES_MS = 300000;

  private TaskQueue queue;
  private TaskProcessor processor;
  private WaitForIdle listener;

  /**
   * Assert addNewAndWaitForRunning returns true when analysis is stopped
   */
  public void test_addTaskWhenStopped() throws Exception {
    queue.setAnalyzing(false);
    assertTrue(processor.waitForIdle(FIVE_MINUTES_MS));
    BlockingTask task = new BlockingTask();
    assertTrue(processor.addNewTaskAndWaitUntilRunning(task, FIVE_MINUTES_MS));
  }

  /**
   * Assert that notification will always happen before addNewAndWaitForRunning returns
   */
  public void test_addTaskWhileRunning() throws Exception {
    final BlockingIdleListener blockingListener = new BlockingIdleListener();
    waitAndAssertIdle(1);
    assertTrue(listener.isIdle());

    processor.addIdleListener(blockingListener);
    final BlockingTask task = new BlockingTask();
    final AddTaskThread thread = new AddTaskThread(task);
    thread.start();
    assertTrue(blockingListener.waitUntilBlocked(FIVE_MINUTES_MS));
    assertFalse(thread.waitForComplete(10));
    processor.removeIdleListener(blockingListener);
    blockingListener.unblock();

    assertTrue(thread.waitForComplete(FIVE_MINUTES_MS));
    thread.assertResult(true);
  }

  /**
   * Assert processor with no tasks moves to idle state
   */
  public void test_empty() throws Exception {
    waitAndAssertIdle(1);
  }

  /**
   * Assert that the idle operation gets executed after all tasks and after listeners have been
   * notified that the processor is idle.
   */
  public void test_idleTask() throws Exception {
    BlockingTask blockingTask = new BlockingTask();
    queue.addNewTask(blockingTask);
    final BlockingIdleListener blockingListener = new BlockingIdleListener();
    processor.addIdleListener(blockingListener);

    final boolean[] complete = {false};
    processor.setIdleOperation(new Runnable() {
      @Override
      public void run() {
        synchronized (complete) {
          complete[0] = true;
          complete.notifyAll();
        }
      }
    });

    assertFalse(complete[0]);
    assertTrue(blockingTask.waitUntilStarted(FIVE_MINUTES_MS));
    assertFalse(complete[0]);
    blockingTask.unblock();
    assertTrue(blockingListener.waitUntilBlocked(FIVE_MINUTES_MS));
    assertFalse(complete[0]);
    processor.removeIdleListener(blockingListener);
    blockingListener.unblock();
    synchronized (complete) {
      if (!complete[0]) {
        complete.wait(100);
      }
    }
    assertTrue(complete[0]);
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
    listener.waitForRun(50);
    assertRunning(expectedIdleCount);
  }
}
