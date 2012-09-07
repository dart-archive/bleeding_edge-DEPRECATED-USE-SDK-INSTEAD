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
import com.google.dart.tools.core.DartCore;
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
  private static File moneyDir;
  private static File moneyLibFile;
  private static File simpleMoneySrcFile;
  private static File bankDir;
  private static File bankLibFile;
  private static File packagesDir;
  private static File pubspecFile;
  private static File nestedAppFile;
  private static File nestedLibFile;

  /**
   * Called once prior to executing the first test in this class
   */
  public static void setUpOnce() throws Exception {
    tempDir = TestUtilities.createTempDirectory();

    moneyDir = new File(tempDir, "Money");
    TestUtilities.copyPluginRelativeContent("Money", moneyDir);
    moneyLibFile = new File(moneyDir, "money.dart");
    assertTrue(moneyLibFile.exists());
    simpleMoneySrcFile = new File(moneyDir, "simple_money.dart");
    assertTrue(simpleMoneySrcFile.exists());

    bankDir = new File(tempDir, "Bank");
    TestUtilities.copyPluginRelativeContent("Bank", bankDir);
    bankLibFile = new File(bankDir, "bank.dart");
    assertTrue(bankLibFile.exists());
    packagesDir = new File(bankDir, DartCore.PACKAGES_DIRECTORY_NAME);
    assertTrue(packagesDir.exists());
    pubspecFile = new File(bankDir, DartCore.PUBSPEC_FILE_NAME);
    assertTrue(pubspecFile.exists());

    nestedAppFile = new File(new File(bankDir, "nested"), "nestedApp.dart");
    assertTrue(nestedAppFile.exists());
    nestedLibFile = new File(new File(bankDir, "nested"), "nestedLib.dart");
    assertTrue(nestedLibFile.exists());
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
    server.analyze(moneyLibFile);
    assertTrackedLibraryFiles(server, moneyLibFile);
    server.assertAnalyzeContext(true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(moneyLibFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    task.assertResolved(true);
    assertTrackedLibraryFiles(server, moneyLibFile);
  }

  public void test_analyze_libraryThenSource() throws Exception {
    test_analyze_library();
    server.analyze(simpleMoneySrcFile);
    assertTrackedLibraryFiles(server, moneyLibFile, simpleMoneySrcFile);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(simpleMoneySrcFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, moneyLibFile);
    task.assertResolved(false);
  }

  public void test_analyze_source() throws Exception {
    assertTrackedLibraryFiles(server);
    server.assertAnalyzeContext(false);
    server.analyze(simpleMoneySrcFile);
    assertTrackedLibraryFiles(server, simpleMoneySrcFile);
    server.assertAnalyzeContext(true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(simpleMoneySrcFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, simpleMoneySrcFile);
    task.assertResolved(true);
  }

  public void test_analyze_sourceThenLibrary() throws Exception {
    test_analyze_source();
    server.analyze(moneyLibFile);
    assertTrackedLibraryFiles(server, simpleMoneySrcFile, moneyLibFile);
    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(moneyLibFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    task.assertResolved(true);
    assertTrackedLibraryFiles(server, moneyLibFile);
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
