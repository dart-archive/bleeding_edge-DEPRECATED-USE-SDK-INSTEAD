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

import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertCachedLibraries;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertPackageContexts;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.getServerTaskQueue;

import java.io.File;

public class AnalyzeLibraryTaskTest extends AbstractDartAnalysisTest {

  private class AnalyzeLibraryTaskAdapter extends AnalyzeLibraryTask {
    private boolean resolved;

    public AnalyzeLibraryTaskAdapter(File libraryFile) {
      super(server, libraryFile, null);
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

  /**
   * Called once prior to executing the first test in this class
   */
  public static void setUpOnce() throws Exception {
    setUpBankExample();
  }

  /**
   * Called once after executing the last test in this class
   */
  public static void tearDownOnce() {
    tearDownBankExample();
  }

  private AnalysisServerAdapter server;
  private Listener listener;

  /**
   * Assert analyzing an application only analyzes the libraries in that application.
   */
  public void test_analyze_application() throws Exception {
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
    server.assertAnalyzeContext(false);

    server.analyze(bankLibFile);
    assertTrackedLibraryFiles(server, bankLibFile);
    server.assertAnalyzeContext(true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);

    assertTrackedLibraryFiles(server, bankLibFile);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);

    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(bankLibFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    task.assertResolved(true);

    assertTrackedLibraryFiles(server, bankLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(
        server,
        bankDir,
        bankLibFile,
        nestedLibFile,
        moneyLibFile,
        customerLibFile);
  }

  /**
   * Assert analyzing a library only analyzes that library, then assert analyzing an application
   * switches the context for that library.
   */
  public void test_analyze_library() throws Exception {
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
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
    assertPackageContexts(server);
    assertCachedLibraries(server, null, moneyLibFile);

    // Assert analyzing an application changes the library's context

    server.analyze(bankLibFile);
    assertTrackedLibraryFiles(server, moneyLibFile, bankLibFile);

    task = new AnalyzeLibraryTaskAdapter(bankLibFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);

    task.assertResolved(true);
    assertTrackedLibraryFiles(server, moneyLibFile, bankLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(
        server,
        bankDir,
        bankLibFile,
        nestedLibFile,
        moneyLibFile,
        customerLibFile);
  }

  /**
   * Assert that analyzing a source file and then analyzing the application containing that source
   * file discards the source file
   */
  public void test_analyze_sourceThenApplication() throws Exception {
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
    server.assertAnalyzeContext(false);

    server.analyze(localBankFile);
    assertTrackedLibraryFiles(server, localBankFile);
    server.assertAnalyzeContext(true);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);

    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(localBankFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    task.assertResolved(true);

    assertTrackedLibraryFiles(server, localBankFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(server, bankDir, localBankFile);

    server.analyze(bankLibFile);
    assertTrackedLibraryFiles(server, localBankFile, bankLibFile);
    server.assertAnalyzeContext(true);

    task = new AnalyzeLibraryTaskAdapter(bankLibFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    task.assertResolved(true);

    assertTrackedLibraryFiles(server, bankLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(
        server,
        bankDir,
        bankLibFile,
        nestedLibFile,
        moneyLibFile,
        customerLibFile);
  }

  /**
   * Assert that analyzing a source file and then analyzing the library containing that source file
   * discards the source file
   */
  public void test_analyze_sourceThenLibrary() throws Exception {
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
    assertPackageContexts(server);
    assertCachedLibraries(server, null, simpleMoneySrcFile);
    task.assertResolved(true);

    server.analyze(moneyLibFile);
    assertTrackedLibraryFiles(server, simpleMoneySrcFile, moneyLibFile);
    task = new AnalyzeLibraryTaskAdapter(moneyLibFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);

    task.assertResolved(true);
    assertTrackedLibraryFiles(server, moneyLibFile);
    assertPackageContexts(server);
    assertCachedLibraries(server, null, moneyLibFile);
  }

  /**
   * Assert that scan will place an application in its suggested context but then subsequent
   * analysis of that application moves it to the correct context
   */
  public void test_scan_analyze_application() throws Exception {
    assertTrackedLibraryFiles(server);
    server.assertAnalyzeContext(false);
    server.scan(bankDir, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile, nestedLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(server, bankDir, bankLibFile, nestedLibFile, nestedAppFile);
    server.assertAnalyze(false, bankLibFile, nestedLibFile, nestedAppFile);

    AnalyzeLibraryTaskAdapter task = new AnalyzeLibraryTaskAdapter(bankLibFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    task.assertResolved(true);
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile, nestedLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(
        server,
        bankDir,
        bankLibFile,
        nestedLibFile,
        moneyLibFile,
        nestedAppFile,
        customerLibFile);

    task = new AnalyzeLibraryTaskAdapter(nestedAppFile);
    getServerTaskQueue(server).addNewTask(task);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    task.assertResolved(true);
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile, nestedLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(
        server,
        bankDir,
        bankLibFile,
        nestedLibFile,
        moneyLibFile,
        nestedAppFile,
        customerLibFile);
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
