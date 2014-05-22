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

package com.google.dart.server.internal.shared;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.server.AnalysisServer;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Abstract base for any {@link AnalysisServer} implementation tests.
 */
public abstract class AbstractServerTest extends TestCase {
  protected static String makeSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  protected AnalysisServer server;
  protected TestAnalysisServerListener serverListener = new TestAnalysisServerListener();

  protected final void addSource(String contextId, Source source) {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    server.applyChanges(contextId, changeSet);
  }

  protected final Source addSource(String contextId, String fileName, String contents) {
    Source source = new TestSource(createFile(fileName), contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    changeSet.changedContent(source, contents);
    server.applyChanges(contextId, changeSet);
    return source;
  }

  /**
   * Creates some test context and returns its identifier.
   */
  protected final String createContext(String name) {
    String sdkPath = DirectoryBasedDartSdk.getDefaultSdkDirectory().getAbsolutePath();
    Map<String, String> packagesMap = ImmutableMap.of("analyzer", "some/path");
    return server.createContext(name, sdkPath, packagesMap);
  }

  /**
   * Creates a concrete {@link AnalysisServer} instance.
   */
  protected abstract AnalysisServer createServer();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server = createServer();
    server.addAnalysisServerListener(serverListener);
  }

  @Override
  protected void tearDown() throws Exception {
    server.shutdown();
    server = null;
    serverListener = null;
    super.tearDown();
  }
}
