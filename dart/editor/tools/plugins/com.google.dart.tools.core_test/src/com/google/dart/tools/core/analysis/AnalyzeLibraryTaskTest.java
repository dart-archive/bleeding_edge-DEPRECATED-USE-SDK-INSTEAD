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
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.getServerTaskQueue;

import java.io.File;

public class AnalyzeLibraryTaskTest extends AbstractDartCoreTest {

  private class AnalyzeLibraryTaskAdapter extends AnalyzeLibraryTask {
    private boolean resolved;

    public AnalyzeLibraryTaskAdapter(File libraryFile) {
      super(server, server.getSavedContext(), libraryFile, null);
    }

    @Override
    protected boolean resolve(Library library) {
      resolved = true;
      return false;
    }

    void assertResolved(boolean wasResolved) {
      assertEquals(wasResolved, resolved);
    }
  }

  private static final long FIVE_MINUTES_MS = 300000;

  private static File tempDir;
  private static File libraryFile;
  private static File dartFile;

//  private static File doesNotExist;

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
//    doesNotExist = new File(tempDir, "doesNotExist.dart");
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

  public void test_analyze_library() throws Exception {
    assertTrackedLibraryFiles(server);
    server.assertAnalyzeContext(false);
    server.analyze(libraryFile);
    assertTrackedLibraryFiles(server, libraryFile);
    server.assertAnalyzeContext(true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(libraryFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    task.assertResolved(true);
    assertTrackedLibraryFiles(server, libraryFile);
  }

  public void test_analyze_libraryThenSource() throws Exception {
    test_analyze_library();
    server.analyze(dartFile);
    assertTrackedLibraryFiles(server, libraryFile, dartFile);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(dartFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, libraryFile);
    task.assertResolved(false);
  }

  public void test_analyze_source() throws Exception {
    assertTrackedLibraryFiles(server);
    server.assertAnalyzeContext(false);
    server.analyze(dartFile);
    assertTrackedLibraryFiles(server, dartFile);
    server.assertAnalyzeContext(true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(dartFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, dartFile);
    task.assertResolved(true);
  }

  public void test_analyze_sourceThenLibrary() throws Exception {
    test_analyze_source();
    server.analyze(libraryFile);
    assertTrackedLibraryFiles(server, dartFile, libraryFile);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(libraryFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    task.assertResolved(true);
    assertTrackedLibraryFiles(server, libraryFile);
  }

  @Override
  protected void setUp() throws Exception {
    server = new AnalysisServerAdapter();
    listener = new Listener(server);
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
  }

}
