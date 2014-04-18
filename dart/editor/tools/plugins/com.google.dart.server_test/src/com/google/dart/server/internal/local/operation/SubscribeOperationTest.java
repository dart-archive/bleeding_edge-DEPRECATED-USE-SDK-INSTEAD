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

package com.google.dart.server.internal.local.operation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.dart.engine.source.Source;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.SourceSet;
import com.google.dart.server.ListSourceSet;
import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

public class SubscribeOperationTest extends TestCase {
  private LocalAnalysisServerImpl server = mock(LocalAnalysisServerImpl.class);
  private Source source = mock(Source.class);
  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);

  public void test_mergeWith_false_differentContext() throws Exception {
    Map<NotificationKind, SourceSet> subscriptions = ImmutableMap.of(
        NotificationKind.NAVIGATION,
        ListSourceSet.create(source));
    SubscribeOperation operationA = new SubscribeOperation("id-A", subscriptions);
    SubscribeOperation operationB = new SubscribeOperation("id-B", subscriptions);
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_false_subscribeOperation() throws Exception {
    Map<NotificationKind, SourceSet> subscriptions = ImmutableMap.of(
        NotificationKind.NAVIGATION,
        ListSourceSet.create(source));
    SubscribeOperation operationA = new SubscribeOperation("id", subscriptions);
    ServerOperation operationB = new PerformAnalysisOperation("id");
    assertFalse(operationA.mergeWith(operationB));
  }

  public void test_mergeWith_true_differentKind() throws Exception {
    Map<NotificationKind, SourceSet> subscriptionsA = ImmutableMap.of(
        NotificationKind.NAVIGATION,
        ListSourceSet.create(sourceA));
    Map<NotificationKind, SourceSet> subscriptionsB = ImmutableMap.of(
        NotificationKind.OUTLINE,
        ListSourceSet.create(sourceB));
    SubscribeOperation operationA = new SubscribeOperation("id", subscriptionsA);
    SubscribeOperation operationB = new SubscribeOperation("id", subscriptionsB);
    assertTrue(operationA.mergeWith(operationB));
    // perform
    operationA.performOperation(server);
    Map<NotificationKind, SourceSet> expectedSubscriptions = Maps.newHashMap(subscriptionsA);
    expectedSubscriptions.putAll(subscriptionsB);
    verify(server, times(1)).internalSubscribe("id", expectedSubscriptions);
  }

  public void test_mergeWith_true_sameKind() throws Exception {
    Map<NotificationKind, SourceSet> subscriptionsA = ImmutableMap.of(
        NotificationKind.NAVIGATION,
        ListSourceSet.create(sourceA));
    Map<NotificationKind, SourceSet> subscriptionsB = ImmutableMap.of(
        NotificationKind.NAVIGATION,
        ListSourceSet.create(sourceB));
    SubscribeOperation operationA = new SubscribeOperation("id", subscriptionsA);
    SubscribeOperation operationB = new SubscribeOperation("id", subscriptionsB);
    assertTrue(operationA.mergeWith(operationB));
    // perform
    operationA.performOperation(server);
    verify(server, times(1)).internalSubscribe("id", subscriptionsB);
  }

  public void test_perform() throws Exception {
    Map<NotificationKind, SourceSet> subscriptions = ImmutableMap.of(
        NotificationKind.NAVIGATION,
        ListSourceSet.create(source));
    SubscribeOperation operation = new SubscribeOperation("id", subscriptions);
    assertSame(ServerOperationPriority.CONTEXT_NOTIFICATION, operation.getPriority());
    assertEquals("id", operation.getContextId());
    // perform
    operation.performOperation(server);
    verify(server, times(1)).internalSubscribe("id", subscriptions);
  }
}
