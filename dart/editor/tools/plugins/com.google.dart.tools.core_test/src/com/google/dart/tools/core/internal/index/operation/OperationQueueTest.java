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
package com.google.dart.tools.core.internal.index.operation;

import junit.framework.TestCase;

public class OperationQueueTest extends TestCase {
  public void test_OperationQueue_create() {
    assertNotNull(new OperationQueue());
  }

  public void test_OperationQueue_dequeue_empty() throws InterruptedException {
    OperationQueue queue = new OperationQueue();
    assertNull(queue.dequeue(0));
  }

  public void test_OperationQueue_dequeue_multiple() throws InterruptedException {
    OperationQueue queue = new OperationQueue();
    IndexOperation operation1 = new NullOperation();
    IndexOperation operation2 = new NullOperation();
    IndexOperation operation3 = new NullOperation();

    queue.enqueue(operation1);
    queue.enqueue(operation2);
    queue.enqueue(operation3);

    assertEquals(operation1, queue.dequeue(0));
    assertEquals(operation2, queue.dequeue(0));
    assertEquals(operation3, queue.dequeue(0));
    assertNull(queue.dequeue(0));
  }

  public void test_OperationQueue_dequeue_nonEmpty() throws InterruptedException {
    OperationQueue queue = new OperationQueue();
    IndexOperation operation = new NullOperation();
    queue.enqueue(operation);
    assertEquals(operation, queue.dequeue(0));
  }

  public void test_OperationQueue_dequeue_queries() throws InterruptedException {
    OperationQueue queue = new OperationQueue();
    IndexOperation operation1 = new NullOperation();
    IndexOperation operation2 = new NullOperation();
    IndexOperation query1 = new NullOperation(true);
    IndexOperation query2 = new NullOperation(true);

    queue.enqueue(operation1);
    queue.enqueue(query1);
    queue.enqueue(query2);
    queue.enqueue(operation2);

    assertEquals(operation1, queue.dequeue(0));
    assertEquals(operation2, queue.dequeue(0));
    assertEquals(query1, queue.dequeue(0));
    assertEquals(query2, queue.dequeue(0));
    assertNull(queue.dequeue(0));
  }

  public void test_OperationQueue_dequeue_queries_pending() throws InterruptedException {
    OperationQueue queue = new OperationQueue();
    IndexOperation operation1 = new NullOperation();
    IndexOperation operation2 = new NullOperation();
    IndexOperation query1 = new NullOperation(true);
    IndexOperation query2 = new NullOperation(true);

    queue.enqueue(operation1);
    queue.enqueue(query1);
    queue.enqueue(query2);
    queue.enqueue(operation2);

    queue.setProcessQueries(false);

    assertEquals(operation1, queue.dequeue(0));
    assertEquals(operation2, queue.dequeue(0));
    assertNull(queue.dequeue(0));

    queue.setProcessQueries(true);

    assertEquals(query1, queue.dequeue(0));
    assertEquals(query2, queue.dequeue(0));
    assertNull(queue.dequeue(0));
  }
}
