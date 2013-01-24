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
package com.google.dart.engine.internal.index.operation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.logging.Logger;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class OperationProcessorTest extends EngineTestCase {

  /**
   * Runs given {@link OperationProcessor} in thread.
   */
  private static Source[] runOperationProcessor(IndexOperation beforeStopOperations[],
      boolean waitStop, IndexOperation afterStopOperations[]) throws Exception {
    final CountDownLatch stopLatch = new CountDownLatch(1);
    // prepare operations
    final LinkedList<IndexOperation> operations;
    {
      operations = Lists.newLinkedList();
      Collections.addAll(operations, beforeStopOperations);
      //
      IndexOperation stopOperation = mock(IndexOperation.class);
      doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          stopLatch.countDown();
          return null;
        }
      }).when(stopOperation).performOperation();
      operations.add(stopOperation);
    }
    // prepare OperationQueue to return elements from "operations"
    OperationQueue queue = mock(OperationQueue.class);
    when(queue.dequeue(anyInt())).then(new Answer<IndexOperation>() {
      @Override
      public IndexOperation answer(InvocationOnMock invocation) throws Throwable {
        if (!operations.isEmpty()) {
          return operations.removeFirst();
        }
        return null;
      }
    });
    when(queue.getOperations()).thenReturn(Arrays.asList(afterStopOperations));
    // run OperationProcessor
    final OperationProcessor processor = new OperationProcessor(queue);
    new Thread() {
      @Override
      public void run() {
        processor.run();
      }
    }.start();
    // wait for stop
    stopLatch.await();
    return processor.stop(waitStop);
  }

  public void test_performOperation() throws Exception {
    IndexOperation operation = mock(IndexOperation.class);
    runOperationProcessor(new IndexOperation[] {operation}, false, new IndexOperation[] {});
    verify(operation).performOperation();
  }

  public void test_performOperation_throwException() throws Exception {
    Logger oldLogger = AnalysisEngine.getInstance().getLogger();
    try {
      NullPointerException myException = new NullPointerException();
      IndexOperation operation = mock(IndexOperation.class);
      doThrow(myException).when(operation).performOperation();
      // set mock Logger
      Logger logger = mock(Logger.class);
      AnalysisEngine.getInstance().setLogger(logger);
      // run processor
      runOperationProcessor(new IndexOperation[] {operation}, false, new IndexOperation[] {});
      // verify that "myException" was logged
      verify(logger).logError(anyString(), same(myException));
    } finally {
      AnalysisEngine.getInstance().setLogger(oldLogger);
    }
  }

  public void test_run_afterStop() throws Exception {
    OperationQueue queue = mock(OperationQueue.class);
    OperationProcessor processor = new OperationProcessor(queue);
    processor.stop(false);
    try {
      processor.run();
      fail();
    } catch (IllegalStateException e) {
    }
  }

  public void test_run_InterruptedException() throws Exception {
    final IndexOperation operation = mock(IndexOperation.class);
    final OperationQueue queue = mock(OperationQueue.class);
    final OperationProcessor processor = new OperationProcessor(queue);
    when(queue.dequeue(anyInt())).then(new Answer<IndexOperation>() {
      int num = 0;

      @Override
      public IndexOperation answer(InvocationOnMock invocation) throws Throwable {
        num++;
        if (num == 1) {
          throw new InterruptedException();
        }
        if (num == 2) {
          return operation;
        }
        processor.stop(false);
        return null;
      }
    });
    // run processor
    Thread thread = new Thread() {
      @Override
      public void run() {
        processor.run();
      }
    };
    thread.start();
    thread.join();
    // operation should be performed
    verify(operation).performOperation();
  }

  public void test_stop_returnsNotIndexed_wasReady() throws Exception {
    Source source = mock(Source.class);
    IndexUnitOperation operation = mock(IndexUnitOperation.class);
    OperationQueue queue = mock(OperationQueue.class);
    when(operation.getSource()).thenReturn(source);
    when(queue.getOperations()).thenReturn(ImmutableList.<IndexOperation> of(operation));
    // stop processor
    OperationProcessor processor = new OperationProcessor(queue);
    Source[] sources = processor.stop(false);
    assertExactElements(sources, new Object[] {source});
  }

  public void test_stop_returnsNotIndexed_wasRunning() throws Exception {
    Source source1 = mock(Source.class);
    Source source2 = mock(Source.class);
    IndexUnitOperation operation1 = mock(IndexUnitOperation.class);
    IndexUnitOperation operation2 = mock(IndexUnitOperation.class);
    when(operation1.getSource()).thenReturn(source1);
    when(operation2.getSource()).thenReturn(source2);
    // run processor
    Source[] sources = runOperationProcessor(new IndexOperation[] {}, true, new IndexOperation[] {
        operation1, operation2});
    Set<Source> sourceSet = ImmutableSet.copyOf(sources);
    assertExactElements(sourceSet, new Object[] {source1, source2});
  }

  public void test_stop_returnsNotIndexed_wasStopped() throws Exception {
    Source source = mock(Source.class);
    IndexUnitOperation operation = mock(IndexUnitOperation.class);
    OperationQueue queue = mock(OperationQueue.class);
    when(operation.getSource()).thenReturn(source);
    when(queue.getOperations()).thenReturn(ImmutableList.<IndexOperation> of(operation));
    // stop processor
    OperationProcessor processor = new OperationProcessor(queue);
    processor.stop(false);
    Source[] sources = processor.stop(false);
    assertExactElements(sources, new Object[] {source});
  }

  public void test_waitForRunning() throws Exception {
    OperationQueue queue = mock(OperationQueue.class);
    when(queue.getOperations()).thenReturn(ImmutableList.<IndexOperation> of());
    // start processor
    final OperationProcessor processor = new OperationProcessor(queue);
    new Thread() {
      @Override
      public void run() {
        processor.run();
      }
    }.start();
    assertEquals(true, processor.waitForRunning());
    // stop
    processor.stop(true);
    // cannot wait for "running" again
    assertEquals(false, processor.waitForRunning());
  }
}
