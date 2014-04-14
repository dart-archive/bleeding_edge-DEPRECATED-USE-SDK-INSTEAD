/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerOperationQueueTest extends TestCase {
  private static final long TIMEOUT = 10;
  private ServerOperationQueue queue = new ServerOperationQueue();
  private ServerOperation operationA = mock(ServerOperation.class);
  private ServerOperation operationB = mock(ServerOperation.class);

  public void test_add_lowTheHi() throws Exception {
    when(operationA.getPriority()).thenReturn(ServerOperationPriority.CONTEXT_ANALYSIS);
    when(operationB.getPriority()).thenReturn(ServerOperationPriority.CONTEXT_CHANGE);
    queue.add(operationA);
    queue.add(operationB);
    assertSame(operationB, takeSafely());
    assertSame(operationA, takeSafely());
  }

  public void test_add_merge() throws Exception {
    MergeableOperation mergeableOperationA = mock(MergeableOperation.class);
    MergeableOperation mergeableOperationB = mock(MergeableOperation.class);
    when(mergeableOperationA.getPriority()).thenReturn(ServerOperationPriority.CONTEXT_CHANGE);
    when(mergeableOperationB.getPriority()).thenReturn(ServerOperationPriority.CONTEXT_CHANGE);
    when(mergeableOperationA.mergeWith(mergeableOperationB)).thenReturn(true);
    queue.add(mergeableOperationA);
    queue.add(mergeableOperationB);
    assertSame(mergeableOperationA, takeSafely());
    assertTrue(queue.isEmpty());
  }

  public void test_add_samePriority() throws Exception {
    queue.add(operationA);
    queue.add(operationB);
    assertSame(operationA, takeSafely());
    assertSame(operationB, takeSafely());
    assertTrue(queue.isEmpty());
  }

  public void test_isEmpty() throws Exception {
    assertTrue(queue.isEmpty());
    queue.add(operationA);
    assertFalse(queue.isEmpty());
    assertSame(operationA, takeSafely());
    assertTrue(queue.isEmpty());
  }

  public void test_removeWithContextId_notContextServerOperation() throws Exception {
    queue.add(operationA);
    queue.removeWithContextId("id");
    assertFalse(queue.isEmpty());
  }

  public void test_removeWithContextId_notSameContext() throws Exception {
    ContextServerOperation operation = mock(ContextServerOperation.class);
    when(operation.getPriority()).thenReturn(ServerOperationPriority.CONTEXT_CHANGE);
    when(operation.getContextId()).thenReturn("id");
    queue.add(operation);
    assertFalse(queue.isEmpty());
    // removed
    queue.removeWithContextId("other-id");
    assertFalse(queue.isEmpty());
  }

  public void test_removeWithContextId_sameContext() throws Exception {
    ContextServerOperation operation = mock(ContextServerOperation.class);
    when(operation.getPriority()).thenReturn(ServerOperationPriority.CONTEXT_CHANGE);
    when(operation.getContextId()).thenReturn("id");
    queue.add(operation);
    assertFalse(queue.isEmpty());
    // removed
    queue.removeWithContextId("id");
    assertTrue(queue.isEmpty());
  }

  public void test_take_timeout() throws Exception {
    assertSame(null, takeSafely());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(operationA.getPriority()).thenReturn(ServerOperationPriority.SERVER);
    when(operationB.getPriority()).thenReturn(ServerOperationPriority.SERVER);
  }

  private ServerOperation takeSafely() {
    return queue.take(TIMEOUT);
  }
}
