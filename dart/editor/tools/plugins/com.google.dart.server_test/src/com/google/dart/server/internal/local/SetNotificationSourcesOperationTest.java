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
import com.google.dart.server.NotificationKind;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SetNotificationSourcesOperationTest extends TestCase {
  private LocalAnalysisServerImpl server = mock(LocalAnalysisServerImpl.class);
  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);

  public void test_mergeWith_false_differentContext() throws Exception {
    Source[] sources = new Source[] {sourceA};
    SetNotificationSourcesOperation operationA = new SetNotificationSourcesOperation(
        "id-A",
        NotificationKind.NAVIGATION,
        sources);
    SetNotificationSourcesOperation operationB = new SetNotificationSourcesOperation(
        "id-B",
        NotificationKind.NAVIGATION,
        sources);
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_false_differentKind() throws Exception {
    Source[] sources = new Source[] {sourceA};
    SetNotificationSourcesOperation operationA = new SetNotificationSourcesOperation(
        "id",
        NotificationKind.NAVIGATION,
        sources);
    SetNotificationSourcesOperation operationB = new SetNotificationSourcesOperation(
        "id",
        NotificationKind.OUTLINE,
        sources);
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_false_notSetNotificationSources() throws Exception {
    Source[] sources = new Source[] {sourceA};
    SetNotificationSourcesOperation operationA = new SetNotificationSourcesOperation(
        "id",
        NotificationKind.NAVIGATION,
        sources);
    ServerOperation operationB = new PerformAnalysisOperation("id");
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_true() throws Exception {
    Source[] sourcesA = new Source[] {sourceA};
    Source[] sourcesB = new Source[] {sourceB};
    SetNotificationSourcesOperation operationA = new SetNotificationSourcesOperation(
        "id",
        NotificationKind.NAVIGATION,
        sourcesA);
    SetNotificationSourcesOperation operationB = new SetNotificationSourcesOperation(
        "id",
        NotificationKind.NAVIGATION,
        sourcesB);
    assertTrue(operationA.mergeWith(operationB));
    // perform
    operationA.performOperation(server);
    verify(server, times(1)).internalSetNotificationSources(
        "id",
        NotificationKind.NAVIGATION,
        sourcesB);
  }

  public void test_perform() throws Exception {
    Source[] sources = new Source[] {sourceA, sourceB};
    SetNotificationSourcesOperation operation = new SetNotificationSourcesOperation(
        "id",
        NotificationKind.NAVIGATION,
        sources);
    assertSame(ServerOperationPriority.CONTEXT_NOTIFICATION, operation.getPriority());
    assertEquals("id", operation.getContextId());
    // perform
    operation.performOperation(server);
    verify(server, times(1)).internalSetNotificationSources(
        "id",
        NotificationKind.NAVIGATION,
        sources);
  }
}
