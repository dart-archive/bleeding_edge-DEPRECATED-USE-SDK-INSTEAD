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

import com.google.dart.server.AnalysisServerListener;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BroadcastAnalysisServerListenerTest extends TestCase {
  private BroadcastAnalysisServerListener broadcast = new BroadcastAnalysisServerListener();
  private AnalysisServerListener listenerA = mock(AnalysisServerListener.class);
  private AnalysisServerListener listenerB = mock(AnalysisServerListener.class);

  public void test_addAnalysisServerListener() throws Exception {
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.addAnalysisServerListener(listenerB);
    broadcast.computedErrors(null, null, null);
    verify(listenerA, times(1)).computedErrors(null, null, null);
    verify(listenerB, times(1)).computedErrors(null, null, null);
  }

  public void test_addAnalysisServerListener_ignoreDuplicate() throws Exception {
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.computedErrors(null, null, null);
    verify(listenerA, times(1)).computedErrors(null, null, null);
  }

  public void test_computedErrors() throws Exception {
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.addAnalysisServerListener(listenerB);
    broadcast.computedErrors(null, null, null);
    verify(listenerA, times(1)).computedErrors(null, null, null);
    verify(listenerB, times(1)).computedErrors(null, null, null);
  }

  public void test_computedHighlights() throws Exception {
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.addAnalysisServerListener(listenerB);
    broadcast.computedHighlights(null, null, null);
    verify(listenerA, times(1)).computedHighlights(null, null, null);
    verify(listenerB, times(1)).computedHighlights(null, null, null);
  }

  public void test_computedNavigation() throws Exception {
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.addAnalysisServerListener(listenerB);
    broadcast.computedNavigation(null, null, null);
    verify(listenerA, times(1)).computedNavigation(null, null, null);
    verify(listenerB, times(1)).computedNavigation(null, null, null);
  }

  public void test_computedOutline() throws Exception {
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.addAnalysisServerListener(listenerB);
    broadcast.computedOutline(null, null, null);
    verify(listenerA, times(1)).computedOutline(null, null, null);
    verify(listenerB, times(1)).computedOutline(null, null, null);
  }

  public void test_removeAnalysisServerListener() throws Exception {
    broadcast.addAnalysisServerListener(listenerA);
    broadcast.removeAnalysisServerListener(listenerA);
    broadcast.computedErrors(null, null, null);
    verify(listenerA, times(0)).computedErrors(null, null, null);
  }
}
