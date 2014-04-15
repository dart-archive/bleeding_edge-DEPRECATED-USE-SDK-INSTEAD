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

import com.google.common.collect.Lists;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServerErrorCode;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.NotificationKind;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

public class LocalAnalysisServerImplTest extends AbstractLocalServerTest {
  public void test_addAnalysisServerListener() throws Exception {
    AnalysisServerListener listener = mock(AnalysisServerListener.class);
    server.addAnalysisServerListener(listener);
    // ping listener
    server.test_pingListeners();
    verify(listener, times(1)).computedErrors(null, null, null);
    reset(listener);
    // add second time, still one time
    server.addAnalysisServerListener(listener);
    server.test_pingListeners();
    verify(listener, times(1)).computedErrors(null, null, null);
    reset(listener);
  }

  public void test_applyChanges_errors() throws Exception {
    String contextId = createContext("test");
    Source sourceA = addSource(contextId, "/testA.dart", "library testA");
    Source sourceB = addSource(contextId, "/testB.dart", "class {}");
    server.test_waitForWorkerComplete();
    serverListener.assertErrorsWithCodes(sourceA, ParserErrorCode.EXPECTED_TOKEN);
    serverListener.assertErrorsWithCodes(sourceB, ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_applyChanges_noContext() throws Exception {
    addSource("no-such-context", "/test.dart", "");
    server.test_waitForWorkerComplete();
    serverListener.assertServerErrorsWithCodes(
        AnalysisServerErrorCode.INVALID_CONTEXT_ID,
        AnalysisServerErrorCode.INVALID_CONTEXT_ID);
  }

  public void test_createContext() throws Exception {
    String idA = createContext("test");
    assertEquals("test-0", idA);
    // a new context with the same name gets a unique identifier
    String idB = createContext("test");
    assertEquals("test-1", idB);
  }

  public void test_deleteContext_noContext() throws Exception {
    server.deleteContext("no-such-context");
    server.test_waitForWorkerComplete();
    serverListener.assertServerErrorsWithCodes(AnalysisServerErrorCode.INVALID_CONTEXT_ID);
  }

  public void test_deleteContext_stopAnalysis() throws Exception {
    String contextId = createContext("test");
    server.test_waitForWorkerComplete();
    // add source, while worker is paused
    server.test_setPaused(true);
    Source source = addSource(contextId, "/test.dart", "library test");
    // delete context, so it won't be analyzed
    server.deleteContext(contextId);
    // resume worker, no errors reported, because context has been deleted
    server.test_setPaused(false);
    server.test_waitForWorkerComplete();
    serverListener.assertErrorsWithCodes(source);
  }

  public void test_exceptionInOperation() throws Exception {
    ServerOperation operation = mock(ServerOperation.class);
    when(operation.getPriority()).thenReturn(ServerOperationPriority.CONTEXT_CHANGE);
    doThrow(new Error()).when(operation).performOperation(server);
    server.test_addOperation(operation);
    server.test_waitForWorkerComplete();
    serverListener.assertServerErrorsWithCodes(AnalysisServerErrorCode.EXCEPTION);
  }

  public void test_getVersion() throws Exception {
    assertEquals("0.0.1", server.version());
  }

  public void test_removeAnalysisServerListener() throws Exception {
    AnalysisServerListener listener = mock(AnalysisServerListener.class);
    server.addAnalysisServerListener(listener);
    // ping listener
    server.test_pingListeners();
    verify(listener, times(1)).computedErrors(null, null, null);
    reset(listener);
    // remove listener
    server.removeAnalysisServerListener(listener);
    server.test_pingListeners();
    verify(listener, times(0)).computedErrors(null, null, null);
    reset(listener);
  }

  public void test_setNotificationSources_afterPerformAnalysis() throws Exception {
    String contextId = createContext("test");
    Source source = addSource(contextId, "/test.dart", makeSource(//
        "main() {",
        "  int vvv = 123;",
        "  print(vvv);",
        "}"));
    // for analysis complete
    server.test_waitForWorkerComplete();
    // no navigation yet
    serverListener.assertNavigationRegions(contextId, source).isEmpty();
    // request navigation
    server.setNotificationSources(contextId, NotificationKind.NAVIGATION, new Source[] {source});
    server.test_waitForWorkerComplete();
    // validate that there are results
    serverListener.assertNavigationRegions(contextId, source).isNotEmpty();
  }

  public void test_setNotificationSources_afterPerformAnalysis_changeSourceList() throws Exception {
    String contextId = createContext("test");
    Source sourceA = addSource(contextId, "/testA.dart", makeSource(//
        "main() {",
        "  int aaa = 111;",
        "  print(aaa);",
        "}"));
    Source sourceB = addSource(contextId, "/testB.dart", makeSource(//
        "main() {",
        "  int bbb = 222;",
        "  print(bbb);",
        "}"));
    // for analysis complete
    server.test_waitForWorkerComplete();
    // request regions only for "A"
    server.setNotificationSources(contextId, NotificationKind.NAVIGATION, new Source[] {sourceA});
    server.test_waitForWorkerComplete();
    serverListener.assertNavigationRegions(contextId, sourceA).isNotEmpty();
    // request regions only for "B"
    serverListener.clearNavigationRegions();
    server.setNotificationSources(contextId, NotificationKind.NAVIGATION, new Source[] {sourceB});
    server.test_waitForWorkerComplete();
    serverListener.assertNavigationRegions(contextId, sourceA).isEmpty();
    serverListener.assertNavigationRegions(contextId, sourceB).isNotEmpty();
  }

  public void test_setNotificationSources_beforePerformAnalysis() throws Exception {
    String contextId = createContext("test");
    server.test_setPaused(true);
    Source source = addSource(contextId, "/test.dart", makeSource(//
        "main() {",
        "  int vvv = 123;",
        "  print(vvv);",
        "}"));
    server.setNotificationSources(contextId, NotificationKind.NAVIGATION, new Source[] {source});
    server.test_setPaused(false);
    server.test_waitForWorkerComplete();
    // validate that there are results
    serverListener.assertNavigationRegions(contextId, source).isNotEmpty();
  }

  public void test_setNotificationSources_noContext() throws Exception {
    Source source = mock(Source.class);
    server.setNotificationSources(
        "no-such-context",
        NotificationKind.NAVIGATION,
        new Source[] {source});
    server.test_waitForWorkerComplete();
    serverListener.assertServerErrorsWithCodes(AnalysisServerErrorCode.INVALID_CONTEXT_ID);
  }

  public void test_setOptions() throws Exception {
    String id = createContext("test");
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    server.setOptions(id, options);
    server.test_waitForWorkerComplete();
  }

  public void test_setOptions_noContext() throws Exception {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    server.setOptions("no-such-context", options);
    server.test_waitForWorkerComplete();
    serverListener.assertServerErrorsWithCodes(AnalysisServerErrorCode.INVALID_CONTEXT_ID);
  }

  public void test_setPrioritySources() throws Exception {
    String contextA = createContext("testA");
    String contextB = createContext("testB");
    String contextC = createContext("testC");
    server.test_waitForWorkerComplete();
    server.test_setPaused(true);
    // add sources
    addSource(contextA, "/testA.dart", "");
    Source sourceB = addSource(contextB, "/testB.dart", "");
    addSource(contextC, "/testC.dart", "");
    server.setPrioritySources(contextB, new Source[] {sourceB});
    // resume
    List<String> analyzedContexts = Lists.newArrayList();
    server.test_setAnalyzedContexts(analyzedContexts);
    server.test_setPaused(false);
    server.test_waitForWorkerComplete();
    // check that "B" was analyzed first
    int aIndex = analyzedContexts.indexOf(contextA);
    int bIndex = analyzedContexts.indexOf(contextB);
    int cIndex = analyzedContexts.indexOf(contextC);
    assertThat(bIndex).isLessThan(aIndex);
    assertThat(bIndex).isLessThan(cIndex);
  }

  public void test_setPrioritySources_noContext() throws Exception {
    server.setPrioritySources("no-such-context", Source.EMPTY_ARRAY);
    server.test_waitForWorkerComplete();
    serverListener.assertServerErrorsWithCodes(AnalysisServerErrorCode.INVALID_CONTEXT_ID);
  }
}
