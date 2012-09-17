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

import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;

import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertCachedLibraries;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertPackageContexts;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;

public class DiscardTaskTest extends AbstractDartAnalysisTest {

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
   * Assert discarding the application only discards that library
   */
  public void test_discard_application() throws Exception {
    scanBankDir();
    server.discard(bankLibFile);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    listener.assertDiscarded(bankLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(server, bankDir, nestedLibFile, nestedAppFile);
  }

  /**
   * Assert discarding the application directory discards every library in that directory tree
   */
  public void test_discard_application_directory() throws Exception {
    scanBankDir();
    server.discard(bankDir);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    listener.assertDiscarded(bankLibFile, nestedAppFile, nestedLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
  }

  /**
   * Assert discarding the nested library discards the libraries importing it
   */
  public void test_discard_application_nestedLib() throws Exception {
    scanBankDir();
    server.discard(nestedLibFile);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    listener.assertDiscarded(bankLibFile, nestedLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(server, bankDir, nestedAppFile);
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
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    listener.assertDiscarded(moneyLibFile);
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
    listener.waitForIdle(2, FIVE_MINUTES_MS);
    listener.assertDiscarded(moneyLibFile);
    listener.assertResolvedCount(0);
    listener.assertNoDuplicates();
    assertTrackedLibraryFiles(server);
    assertPackageContexts(server);
    assertCachedLibraries(server, null);
  }

  /**
   * Assert adding and removing "packages" directory changes the context
   */
  public void test_discard_packages() throws Exception {
    scanBankDir();
    server.discard(packagesDir);
    listener.waitForIdle(2, FIVE_MINUTES_MS);
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

  private void scanBankDir() throws Exception {
    assertTrackedLibraryFiles(server);
    server.scan(bankDir, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    server.assertAnalyzeContext(true);
    assertTrackedLibraryFiles(server, bankLibFile, nestedAppFile, nestedLibFile);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(server, bankDir, bankLibFile, nestedLibFile, nestedAppFile);
  }
}
