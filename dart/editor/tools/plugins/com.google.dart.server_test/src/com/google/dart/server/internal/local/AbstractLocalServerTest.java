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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.util.Map;

public class AbstractLocalServerTest extends TestCase {
  protected static String makeSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  protected LocalAnalysisServerImpl server;
  protected TestAnalysisServerListener serverListener = new TestAnalysisServerListener();

  protected final Source addSource(String contextId, String fileName, String contents) {
    Source source = new TestSource(createFile(fileName), contents);
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    server.applyChanges(contextId, changeSet);
    server.setContents(contextId, source, contents);
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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server = new LocalAnalysisServerImpl();
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
