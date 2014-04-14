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

import com.google.dart.engine.source.Source;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SetContentsOperationTest extends TestCase {
  private LocalAnalysisServerImpl server = mock(LocalAnalysisServerImpl.class);
  private Source source = mock(Source.class);

  public void test_mergeWith_false_differentContext() throws Exception {
    SetContentsOperation operationA = new SetContentsOperation("id-A", source, "contents A");
    SetContentsOperation operationB = new SetContentsOperation("id-B", source, "contents B");
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_false_notSetContents() throws Exception {
    SetContentsOperation operationA = new SetContentsOperation("id", source, "new contents");
    MergeableOperation operationB = new PerformAnalysisOperation("id");
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_true() throws Exception {
    SetContentsOperation operationA = new SetContentsOperation("id", source, "contents A");
    SetContentsOperation operationB = new SetContentsOperation("id", source, "contents B");
    assertTrue(operationA.mergeWith(operationB));
    // perform
    operationA.performOperation(server);
    verify(server, times(1)).internalSetContents("id", source, "contents B");
  }

  public void test_perform() throws Exception {
    SetContentsOperation operation = new SetContentsOperation("id", source, "new contents");
    assertSame(ServerOperationPriority.CONTEXT_CHANGE, operation.getPriority());
    assertEquals("id", operation.getContextId());
    // perform
    operation.performOperation(server);
    verify(server, times(1)).internalSetContents("id", source, "new contents");
  }
}
