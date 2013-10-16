/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.analysis.model.ContextManager;

import org.eclipse.core.runtime.IStatus;

import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AnalysisManagerTest extends AbstractDartCoreTest {

  /** Specialized worker that does not perform any actual analysis */
  private class MockWorker extends AnalysisWorker {
    private int analysisCount;

    public MockWorker() {
      super(mock(ContextManager.class), null);
    }

    @Override
    public void performAnalysis(AnalysisManager manager) {
      analysisCount++;
    }

    void assertAnalysisCount(int expected) {
      assertEquals(expected, analysisCount);
    }
  }

  /** Specialized subclass that does not automatically start a background job */
  private class Target extends AnalysisManager {
    private int startBackgroundAnalysisCount = 0;

    @Override
    public void startBackgroundAnalysis() {
      startBackgroundAnalysisCount++;
    }

    void assertBackgroundAnalysisCount(int expected) {
      assertEquals(expected, startBackgroundAnalysisCount);
    }

    void superStartBackgroundAnalysis() {
      super.startBackgroundAnalysis();
    }
  }

  private Target target = new Target();

  public void test_addWorker() throws Exception {
    assertEquals(0, target.getQueueWorkers().length);
    MockWorker worker = new MockWorker();
    target.addWorker(worker);
    target.assertBackgroundAnalysisCount(1);
    assertEquals(1, target.getQueueWorkers().length);
    assertSame(worker, target.getQueueWorkers()[0]);
  }

  public void test_getInstance() {
    assertNotNull(AnalysisManager.getInstance());
  }

  public void test_getNextWorker() throws Exception {
    assertNull(target.getNextWorker());
    MockWorker worker = new MockWorker();
    target.addWorker(worker);
    assertEquals(1, target.getQueueWorkers().length);
    assertSame(worker, target.getQueueWorkers()[0]);
    assertSame(worker, target.getNextWorker());
    assertEquals(0, target.getQueueWorkers().length);
    assertNull(target.getNextWorker());
  }

  public void test_getPause() throws Exception {
    assertEquals(0, target.getPauseCount());
  }

  public void test_pause() throws Exception {
    MockWorker worker = new MockWorker();
    target.pauseBackgroundAnalysis();
    assertEquals(1, target.getPauseCount());
    target.addWorker(worker);
    target.performAnalysis(null);
    worker.assertAnalysisCount(0);
    assertEquals(1, target.getQueueWorkers().length);
    assertSame(worker, target.getQueueWorkers()[0]);
  }

  public void test_performAnalysis() throws Exception {
    MockWorker worker = new MockWorker();
    target.performAnalysis(null);
    worker.assertAnalysisCount(0);
    target.addWorker(worker);
    worker.assertAnalysisCount(0);
    target.performAnalysis(null);
    worker.assertAnalysisCount(1);
  }

  public void test_resume() throws Exception {
    MockWorker worker = new MockWorker();
    target.pauseBackgroundAnalysis();
    assertEquals(1, target.getPauseCount());
    target.addWorker(worker);
    target.performAnalysis(null);
    worker.assertAnalysisCount(0);
    assertEquals(1, target.getQueueWorkers().length);
    assertSame(worker, target.getQueueWorkers()[0]);
    target.assertBackgroundAnalysisCount(0);
    target.resumeBackgroundAnalysis();
    target.assertBackgroundAnalysisCount(1);
    assertEquals(0, target.getPauseCount());
    target.performAnalysis(null);
    worker.assertAnalysisCount(1);
  }

  public void test_resumeBeforePause() throws Exception {
    target.resumeBackgroundAnalysis();
    LOG.assertEntries(IStatus.ERROR);
  }

  public void test_startBackgroundAnalysis() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    MockWorker worker = new MockWorker() {
      @Override
      public void performAnalysis(AnalysisManager manager) {
        assertSame(target, manager);
        latch.countDown();
      };
    };
    target.addWorker(worker);
    target.superStartBackgroundAnalysis();
    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  public void test_waitForBackgroundAnalysis() throws Exception {
    assertTrue(target.waitForBackgroundAnalysis(5000));
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch resume = new CountDownLatch(1);
    final boolean[] finished = {false};
    MockWorker worker = new MockWorker() {
      @Override
      public void performAnalysis(AnalysisManager manager) {
        assertSame(target, manager);
        latch.countDown();
        try {
          finished[0] = resume.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          // ignored
        }
      };
    };
    assertNull(target.getActiveWorker());
    target.addWorker(worker);
    assertNull(target.getActiveWorker());
    target.superStartBackgroundAnalysis();
    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertFalse(target.waitForBackgroundAnalysis(10));
    assertFalse(finished[0]);
    assertSame(worker, target.getActiveWorker());
    resume.countDown();
    assertTrue(target.waitForBackgroundAnalysis(5000));
    assertTrue(finished[0]);
    assertNull(target.getActiveWorker());
  }
}
