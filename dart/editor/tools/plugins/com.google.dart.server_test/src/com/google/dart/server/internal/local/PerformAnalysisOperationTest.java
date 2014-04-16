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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PerformAnalysisOperationTest extends TestCase {
  private LocalAnalysisServerImpl server = mock(LocalAnalysisServerImpl.class);

  public void test_getPriority() throws Exception {
    ServerOperation operation;
    {
      operation = new PerformAnalysisOperation("id", false, false);
      assertSame(ServerOperationPriority.CONTEXT_ANALYSIS, operation.getPriority());
    }
    {
      operation = new PerformAnalysisOperation("id", false, true);
      assertSame(ServerOperationPriority.CONTEXT_ANALYSIS_CONTINUE, operation.getPriority());
    }
    {
      operation = new PerformAnalysisOperation("id", true, false);
      assertSame(ServerOperationPriority.CONTEXT_ANALYSIS_PRIORITY, operation.getPriority());
    }
    {
      operation = new PerformAnalysisOperation("id", true, true);
      assertSame(
          ServerOperationPriority.CONTEXT_ANALYSIS_PRIORITY_CONTINUE,
          operation.getPriority());
    }
  }

  public void test_mergeWith_false_differentContext() throws Exception {
    PerformAnalysisOperation operationA = new PerformAnalysisOperation("id-A");
    PerformAnalysisOperation operationB = new PerformAnalysisOperation("id-B");
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_false_notPerformAnalysis() throws Exception {
    PerformAnalysisOperation operationA = new PerformAnalysisOperation("id");
    MergeableOperation operationB = new SetOptionsOperation("id", null);
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_true() throws Exception {
    PerformAnalysisOperation operationA = new PerformAnalysisOperation("id");
    PerformAnalysisOperation operationB = new PerformAnalysisOperation("id");
    assertTrue(operationA.mergeWith(operationB));
  }

  public void test_perform() throws Exception {
    PerformAnalysisOperation operation = new PerformAnalysisOperation("id");
    assertSame(ServerOperationPriority.CONTEXT_ANALYSIS, operation.getPriority());
    assertEquals("id", operation.getContextId());
    // perform
    operation.performOperation(server);
    verify(server, times(1)).internalPerformAnalysis("id");
  }
}
