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

public class SetPrioritySourcesOperationTest extends TestCase {
  private LocalAnalysisServerImpl server = mock(LocalAnalysisServerImpl.class);
  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);

  public void test_mergeWith_false_differentContext() throws Exception {
    Source[] sources = new Source[] {sourceA};
    SetPrioritySourcesOperation operationA = new SetPrioritySourcesOperation("id-A", sources);
    SetPrioritySourcesOperation operationB = new SetPrioritySourcesOperation("id-B", sources);
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_false_notSetContents() throws Exception {
    Source[] sources = new Source[] {sourceA};
    SetPrioritySourcesOperation operationA = new SetPrioritySourcesOperation("id", sources);
    ServerOperation operationB = new PerformAnalysisOperation("id");
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_true() throws Exception {
    Source[] sourcesA = new Source[] {sourceA};
    Source[] sourcesB = new Source[] {sourceB};
    SetPrioritySourcesOperation operationA = new SetPrioritySourcesOperation("id", sourcesA);
    SetPrioritySourcesOperation operationB = new SetPrioritySourcesOperation("id", sourcesB);
    assertTrue(operationA.mergeWith(operationB));
    // perform
    operationA.performOperation(server);
    verify(server, times(1)).internalSetPrioritySources("id", sourcesB);
  }

  public void test_perform() throws Exception {
    Source[] sources = new Source[] {sourceA, sourceB};
    SetPrioritySourcesOperation operation = new SetPrioritySourcesOperation("id", sources);
    assertSame(ServerOperationPriority.CONTEXT_CHANGE, operation.getPriority());
    assertEquals("id", operation.getContextId());
    // perform
    operation.performOperation(server);
    verify(server, times(1)).internalSetPrioritySources("id", sources);
  }
}
