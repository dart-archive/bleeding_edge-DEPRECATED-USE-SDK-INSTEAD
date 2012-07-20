/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;

import java.io.File;

public class LibraryScanTaskTest extends AbstractDartCoreTest {

  private class AnalysisServerAdapter extends AnalysisServer {
    private boolean analyzeContext = false;

    public AnalysisServerAdapter(EditorLibraryManager libraryManager) {
      super(libraryManager);
    }

    public void assertAnalyzeContext(boolean expectedState) {
      if (analyzeContext != expectedState) {
        fail("Expected background analysis " + expectedState + " but found " + analyzeContext);
      }
    }

    public void resetAnalyzeContext() {
      analyzeContext = false;
    }

    @Override
    protected void queueAnalyzeContext() {
      analyzeContext = true;
    }
  }

  private static final long FIVE_MINUTES_MS = 300000;

  private static File tempDir;
  private static File libraryFile;
  private static File dartFile;
  private static File doesNotExist;

  /**
   * Called once prior to executing the first test in this class
   */
  public static void setUpOnce() throws Exception {
    tempDir = TestUtilities.createTempDirectory();
    TestUtilities.copyPluginRelativeContent("Money", tempDir);
    libraryFile = new File(tempDir, "money.dart");
    assertTrue(libraryFile.exists());
    dartFile = new File(tempDir, "simple_money.dart");
    assertTrue(dartFile.exists());
    doesNotExist = new File(tempDir, "doesNotExist.dart");
  }

  /**
   * Called once after executing the last test in this class
   */
  public static void tearDownOnce() {
    FileUtilities.delete(tempDir);
    tempDir = null;
  }

  private AnalysisServerAdapter server;
  private Listener listener;

  public void test_scan_directory() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(tempDir, true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, libraryFile);
    server.assertAnalyzeContext(true);
  }

  public void test_scan_doesNotExist() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(doesNotExist, true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server);
    server.assertAnalyzeContext(false);
  }

  public void test_scan_library() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(libraryFile, true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, libraryFile);
    server.assertAnalyzeContext(true);
  }

  public void test_scan_libraryThenSource() throws Exception {
    test_scan_library();
    server.resetAnalyzeContext();
    server.scan(dartFile, true);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, libraryFile);
    server.assertAnalyzeContext(false);
  }

  public void test_scan_source() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(dartFile, true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, dartFile);
    server.assertAnalyzeContext(true);
  }

  public void test_scan_sourceThenLibrary() throws Exception {
    test_scan_source();
    server.resetAnalyzeContext();
    server.scan(libraryFile, true);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, libraryFile);
    server.assertAnalyzeContext(true);
  }

  @Override
  protected void setUp() throws Exception {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServerAdapter(libraryManager);
    listener = new Listener(server);
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
  }
}
