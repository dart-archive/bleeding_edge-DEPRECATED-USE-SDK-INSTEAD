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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.source.Source;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

public class OperationQueueTest extends EngineTestCase {
  public void test_dequeue() throws Exception {
    IndexOperation notQueryOperation = mock(IndexOperation.class);
    IndexOperation isQueryOperation = mock(IndexOperation.class);
    when(isQueryOperation.isQuery()).thenReturn(true);
    // enqueue 2 operations
    OperationQueue queue = new OperationQueue();
    queue.enqueue(isQueryOperation);
    queue.enqueue(notQueryOperation);
    // do dequeue, first "notQuery"
    assertSame(notQueryOperation, queue.dequeue(0));
    assertSame(isQueryOperation, queue.dequeue(0));
    assertSame(null, queue.dequeue(0));
  }

  public void test_dequeue_empty_hasTime() throws Exception {
    OperationQueue queue = new OperationQueue();
    assertSame(null, queue.dequeue(1));
  }

  public void test_dequeue_empty_noTime() throws Exception {
    OperationQueue queue = new OperationQueue();
    assertSame(null, queue.dequeue(0));
    assertSame(null, queue.dequeue(-1));
  }

  public void test_enqueue_isQuery() throws Exception {
    IndexOperation notQueryOperation = mock(IndexOperation.class);
    IndexOperation isQueryOperation = mock(IndexOperation.class);
    when(isQueryOperation.isQuery()).thenReturn(true);
    // enqueue 2 operations
    OperationQueue queue = new OperationQueue();
    queue.enqueue(isQueryOperation);
    queue.enqueue(notQueryOperation);
    // test operations - first notQuery
    List<IndexOperation> operations = queue.getOperations();
    assertExactElements(operations, notQueryOperation, isQueryOperation);
  }

  public void test_enqueue_removeSource() throws Exception {
    Source source = mock(Source.class);
    // prepare "notQuery" operations
    IndexOperation notQueryOperation = mock(IndexOperation.class);
    IndexOperation notQueryOperation_toRemove = mock(IndexOperation.class);
    when(notQueryOperation_toRemove.removeWhenSourceRemoved(source)).thenReturn(true);
    // prepare "isQuery" operations
    IndexOperation isQueryOperation = mock(IndexOperation.class);
    IndexOperation isQueryOperation_toRemove = mock(IndexOperation.class);
    when(isQueryOperation.isQuery()).thenReturn(true);
    when(isQueryOperation_toRemove.isQuery()).thenReturn(true);
    when(isQueryOperation_toRemove.removeWhenSourceRemoved(source)).thenReturn(true);
    // enqueue operations
    OperationQueue queue = new OperationQueue();
    queue.enqueue(notQueryOperation);
    queue.enqueue(notQueryOperation_toRemove);
    queue.enqueue(isQueryOperation);
    queue.enqueue(isQueryOperation_toRemove);
    // test operations
    {
      List<IndexOperation> operations = queue.getOperations();
      assertExactElements(
          operations,
          notQueryOperation,
          notQueryOperation_toRemove,
          isQueryOperation,
          isQueryOperation_toRemove);
    }
    // enqueue "remove"
    RemoveSourceOperation removeOperation = mock(RemoveSourceOperation.class);
    when(removeOperation.getSource()).thenReturn(source);
    queue.enqueue(removeOperation);
    // test operations
    {
      List<IndexOperation> operations = queue.getOperations();
      assertExactElements(operations, notQueryOperation, removeOperation, isQueryOperation);
    }
  }

  public void test_new() throws Exception {
    OperationQueue queue = new OperationQueue();
    assertEquals(0, queue.size());
    assertSize(0, queue.getOperations());
  }

  public void test_setProcessQueries() throws Exception {
    IndexOperation notQueryOperation = mock(IndexOperation.class);
    IndexOperation isQueryOperation = mock(IndexOperation.class);
    when(isQueryOperation.isQuery()).thenReturn(true);
    // enqueue 2 operations
    OperationQueue queue = new OperationQueue();
    queue.enqueue(isQueryOperation);
    queue.enqueue(notQueryOperation);
    // no testing
    queue.setProcessQueries(false);
    queue.setProcessQueries(true);
    // disable queries, so no "isQueryOperation"
    queue.setProcessQueries(false);
    assertSame(notQueryOperation, queue.dequeue(0));
    assertSame(null, queue.dequeue(0));
  }
}
