/*
 * Copyright 2012 Dart project authors.
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
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertCachedLibraries;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertPackageContexts;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;

import java.io.File;

public class DiscardTaskTest extends AbstractDartCoreTest {

  private static final long FIVE_MINUTES_MS = 300000;

  private static File tempDir;
  private static File moneyDir;
  private static File moneyLibFile;
  private static File simpleMoneySrcFile;
  private static File bankDir;
  private static File bankLibFile;
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

  public void test_discard_application_directory() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(bankDir, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    server.assertAnalyzeContext(true);
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile, nestedLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null, bankLibFile, nestedAppFile, nestedLibFile);

    server.discard(bankDir);
    listener.waitForDiscarded(FIVE_MINUTES_MS, bankLibFile, nestedAppFile, nestedLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
  }

  public void test_discard_application_nestedLib() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(bankDir, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    server.assertAnalyzeContext(true);
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile, nestedLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null, bankLibFile, nestedAppFile, nestedLibFile);

    // Discarding nested app should discard bank app because bank app imports nested app
    server.discard(nestedLibFile);
    listener.waitForDiscarded(FIVE_MINUTES_MS, bankLibFile, nestedLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null, nestedAppFile);
  }

  public void test_discard_libraryDirectory() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(moneyDir, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    server.assertAnalyzeContext(true);
    assertTrackedLibraryFiles(server, moneyLibFile);
    assertPackageContexts(server);
    assertCachedLibraries(server, null, moneyLibFile);

    server.discard(moneyDir);
    listener.waitForDiscarded(FIVE_MINUTES_MS, moneyLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
  }

  public void test_discard_libraryFile() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(moneyLibFile, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    server.assertAnalyzeContext(true);
    assertTrackedLibraryFiles(server, moneyLibFile);
    assertPackageContexts(server);
    assertCachedLibraries(server, null, moneyLibFile);

    server.discard(moneyLibFile);
    listener.waitForDiscarded(FIVE_MINUTES_MS, moneyLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
  }

  // assert that discard takes priority over analysis
  public void test_discard_priority() throws Exception {
    AnalysisServer server = new AnalysisServer(PackageLibraryManagerProvider.getAnyLibraryManager());
    listener = new Listener(server);

    server.analyze(moneyLibFile);
    server.discard(moneyDir);
    server.start();
    listener.waitForIdle(FIVE_MINUTES_MS);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
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
