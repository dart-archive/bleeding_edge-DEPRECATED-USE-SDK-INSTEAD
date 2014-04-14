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

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServerListener;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

public class LocalAnalysisServerImplTest extends TestCase {
  private LocalAnalysisServerImpl server = new LocalAnalysisServerImpl();
  private TestAnalysisServerListener serverListener = new TestAnalysisServerListener();

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
    // ignored
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
    // ignored
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

  public void test_setOptions() throws Exception {
    String id = createContext("test");
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    server.setOptions(id, options);
    server.test_waitForWorkerComplete();
  }

  public void test_setOptions_noContext() throws Exception {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    server.setOptions("no-such-context", options);
    // ignored
  }

  public void test_setPrioritySources() throws Exception {
    String id = createContext("test");
    // no priority sources
    server.setPrioritySources(id, Source.EMPTY_ARRAY);
    // set one priority source
    Source source = addSource(id, "/test.dart", "");
    server.setPrioritySources(id, new Source[] {source});
    server.test_waitForWorkerComplete();
  }

  public void test_setPrioritySources_noContext() throws Exception {
    server.setPrioritySources("no-such-context", Source.EMPTY_ARRAY);
    // ignored
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server.addAnalysisServerListener(serverListener);
  }

  @Override
  protected void tearDown() throws Exception {
    server.shutdown();
    server = null;
    super.tearDown();
  }

  private Source addSource(String contextId, String fileName, String contents) {
    Source source = new FileBasedSource(createFile(fileName));
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    server.applyChanges(contextId, changeSet);
    server.setContents(contextId, source, contents);
    return source;
  }

  /**
   * Creates some test context and returns its identifier.
   */
  private String createContext(String name) {
    String sdkPath = DirectoryBasedDartSdk.getDefaultSdkDirectory().getAbsolutePath();
    Map<String, String> packagesMap = ImmutableMap.of("analyzer", "some/path");
    return server.createContext(name, sdkPath, packagesMap);
  }
}
