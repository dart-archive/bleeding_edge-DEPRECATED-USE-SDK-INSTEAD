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

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.test.util.FileUtilities;

import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertCachedLibraries;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertPackageContexts;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.getCachedLibrary;

public class FileChangedTaskTest extends AbstractDartAnalysisTest {

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
  private SavedContext savedContext;
  private Listener listener;

  /**
   * Assert cache discarded only if file has changed on disk
   */
  public void test_changed() {
    server.start();
    ParseResult parse1 = savedContext.parse(moneyLibFile, moneyLibFile, FIVE_MINUTES_MS);
    assertNotNull(parse1.getDartUnit());
    ParseResult parse2 = savedContext.parse(moneyLibFile, moneyLibFile, FIVE_MINUTES_MS);
    assertSame(parse1.getDartUnit(), parse2.getDartUnit());

    listener.reset();
    server.changed(moneyLibFile);
    ParseResult parse3 = savedContext.parse(moneyLibFile, moneyLibFile, FIVE_MINUTES_MS);
    assertSame(parse1.getDartUnit(), parse3.getDartUnit());
    server.assertAnalyzeContext(false);
    listener.assertDiscarded();

    listener.reset();
    moneyLibFile.setLastModified(System.currentTimeMillis() + 1000);
    server.changed(moneyLibFile);
    ParseResult parse4 = savedContext.parse(moneyLibFile, moneyLibFile, FIVE_MINUTES_MS);
    assertNotSame(parse1.getDartUnit(), parse4.getDartUnit());
    server.assertAnalyzeContext(true);
    listener.assertDiscarded(moneyLibFile);
  }

  /**
   * Assert adding and removing import in application does not change context of imported nested
   * library because that library is in the application directory hierarchy.
   */
  public void test_changed_application_importNested() throws Exception {
    final String directive = "#import('nested/nestedLib.dart');";
    final String oldContent = FileUtilities.getContents(bankLibFile);
    int index = oldContent.indexOf(directive);
    assertTrue(index > 0);
    final String newContent = oldContent.substring(0, index)
        + oldContent.substring(index + directive.length());

    server.scan(bankDir, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(server, bankDir, bankLibFile, nestedLibFile, nestedAppFile);
    ParseResult parse1 = savedContext.parse(bankLibFile, bankLibFile, FIVE_MINUTES_MS);
    assertNotNull(parse1.getDartUnit());
    listener.waitForIdle(2, FIVE_MINUTES_MS);

    final long oldLastModified = bankLibFile.lastModified();
    FileUtilities.setContents(bankLibFile, newContent);
    // Ensure marked as modified... lastModified is only accurate to the second
    bankLibFile.setLastModified(oldLastModified + 1000);
    ParseResult parse2;
    try {
      server.changed(bankLibFile);
      listener.waitForIdle(3, FIVE_MINUTES_MS);

      assertPackageContexts(server, bankDir);
      assertCachedLibraries(server, null);
      assertCachedLibraries(server, bankDir, bankLibFile, nestedLibFile, nestedAppFile);
      parse2 = savedContext.parse(bankLibFile, bankLibFile, FIVE_MINUTES_MS);
      listener.waitForIdle(4, FIVE_MINUTES_MS);

      assertNotNull(parse2.getDartUnit());
      assertNotSame(parse2.getDartUnit(), parse1.getDartUnit());
    } finally {
      FileUtilities.setContents(bankLibFile, oldContent);
      bankLibFile.setLastModified(oldLastModified);
    }
    server.changed(bankLibFile);
    listener.waitForIdle(5, FIVE_MINUTES_MS);
    assertPackageContexts(server, bankDir);
    assertCachedLibraries(server, null);
    assertCachedLibraries(server, bankDir, bankLibFile, nestedLibFile, nestedAppFile);
    ParseResult parse3 = savedContext.parse(bankLibFile, bankLibFile, FIVE_MINUTES_MS);
    assertNotNull(parse3.getDartUnit());
    assertNotSame(parse3.getDartUnit(), parse1.getDartUnit());
    assertNotSame(parse3.getDartUnit(), parse2.getDartUnit());
  }

  /**
   * Assert adding and removing import in application changes context of imported outside library
   * because that library is outside the application directory hierarchy.
   */
  // TODO (danrubel): Add support for this in subsequent CL
//  public void test_changed_application_importOutside() throws Exception {
//    final String directive = "#import('../Money/money.dart');";
//    final String oldContent = FileUtilities.getContents(bankLibFile);
//    int index = oldContent.indexOf(directive);
//    assertTrue(index > 0);
//    final String newContent = oldContent.substring(0, index)
//        + oldContent.substring(index + directive.length());
//
//    server.scan(bankDir, null);
//    server.scan(moneyDir, null);
//    server.start();
//    listener.waitForIdle(1, FIVE_MINUTES_MS);
//    assertPackageContexts(server, bankDir);
//    assertCachedLibraries(server, null, moneyLibFile);
//    assertCachedLibraries(server, bankDir, bankLibFile, nestedLibFile, nestedAppFile);
//    ParseResult parse1 = savedContext.parse(bankLibFile, bankLibFile, FIVE_MINUTES_MS);
//    assertNotNull(parse1.getDartUnit());
//
//    final long oldLastModified = bankLibFile.lastModified();
//    FileUtilities.setContents(bankLibFile, newContent);
//    // Ensure marked as modified... lastModified is only accurate to the second
//    bankLibFile.setLastModified(oldLastModified + 1000);
//    ParseResult parse2;
//    try {
//      server.changed(bankLibFile);
//      listener.waitForIdle(2, FIVE_MINUTES_MS);
//      assertPackageContexts(server, bankDir);
//      assertCachedLibraries(server, null);
//      assertCachedLibraries(server, bankDir, nestedLibFile, nestedAppFile);
//      parse2 = savedContext.parse(bankLibFile, bankLibFile, FIVE_MINUTES_MS);
//      assertNotNull(parse2.getDartUnit());
//      assertNotSame(parse2.getDartUnit(), parse1.getDartUnit());
//    } finally {
//      FileUtilities.setContents(bankLibFile, oldContent);
//      bankLibFile.setLastModified(oldLastModified);
//    }
//    server.changed(bankLibFile);
//    listener.waitForIdle(3, FIVE_MINUTES_MS);
//    assertPackageContexts(server, bankDir);
//    assertCachedLibraries(server, null);
//    assertCachedLibraries(server, bankDir, nestedLibFile, nestedAppFile);
//    ParseResult parse3 = savedContext.parse(bankLibFile, bankLibFile, FIVE_MINUTES_MS);
//    assertNotNull(parse3.getDartUnit());
//    assertNotSame(parse3.getDartUnit(), parse1.getDartUnit());
//    assertNotSame(parse3.getDartUnit(), parse2.getDartUnit());
//  }

  /**
   * Assert removing #source directive causes sourced file to become analyzed as library
   */
  public void test_changed_library() throws Exception {
    final String directive = "#source(\"simple_money.dart\");";
    final String oldContent = FileUtilities.getContents(moneyLibFile);
    int index = oldContent.indexOf(directive);
    assertTrue(index > 0);
    final String newContent = oldContent.substring(0, index)
        + oldContent.substring(index + directive.length());

    server.scan(moneyLibFile, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, moneyLibFile);
    Object lib1 = getCachedLibrary(savedContext, moneyLibFile);
    assertNotNull(lib1);
    assertNull(getCachedLibrary(savedContext, simpleMoneySrcFile));

    server.resetAnalyzeContext();
    final long oldLastModified = moneyLibFile.lastModified();
    FileUtilities.setContents(moneyLibFile, newContent);
    // Ensure marked as modified... lastModified is only accurate to the second
    moneyLibFile.setLastModified(oldLastModified + 1000);
    try {
      server.changed(moneyLibFile);
      listener.waitForIdle(2, FIVE_MINUTES_MS);
      assertTrackedLibraryFiles(server, moneyLibFile, simpleMoneySrcFile);
      server.assertAnalyzeContext(true);
      Object lib2 = getCachedLibrary(savedContext, moneyLibFile);
      assertNotNull(lib2);
      assertNotSame(lib1, lib2);
      lib1 = lib2;
      assertNotNull(getCachedLibrary(savedContext, simpleMoneySrcFile));
    } finally {
      FileUtilities.setContents(moneyLibFile, oldContent);
      moneyLibFile.setLastModified(oldLastModified);
    }

    server.resetAnalyzeContext();
    server.changed(moneyLibFile);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, moneyLibFile);
    server.assertAnalyzeContext(true);
    Object lib2 = getCachedLibrary(savedContext, moneyLibFile);
    assertNotNull(lib2);
    assertNotSame(lib1, lib2);
    assertNull(getCachedLibrary(savedContext, simpleMoneySrcFile));
  }

  /**
   * Assert adding #library directive causes sourced file to become analyzed as library
   */
  public void test_changed_source() throws Exception {
    final String oldContent = FileUtilities.getContents(simpleMoneySrcFile);
    PrintStringWriter writer = new PrintStringWriter();
    writer.println("#library(\"foobar\");");
    writer.append(oldContent);
    final String newContent = writer.toString();

    server.scan(moneyLibFile, null);
    server.start();
    listener.waitForIdle(1, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, moneyLibFile);
    Object lib1 = getCachedLibrary(savedContext, moneyLibFile);
    assertNotNull(lib1);
    assertNull(getCachedLibrary(savedContext, simpleMoneySrcFile));

    server.resetAnalyzeContext();
    final long oldLastModified = simpleMoneySrcFile.lastModified();
    FileUtilities.setContents(simpleMoneySrcFile, newContent);
    // Ensure marked as modified... lastModified is only accurate to the second
    simpleMoneySrcFile.setLastModified(oldLastModified + 1000);
    try {
      server.changed(simpleMoneySrcFile);
      listener.waitForIdle(2, FIVE_MINUTES_MS);
      assertTrackedLibraryFiles(server, moneyLibFile, simpleMoneySrcFile);
      server.assertAnalyzeContext(true);
      Object lib2 = getCachedLibrary(savedContext, moneyLibFile);
      assertNotNull(lib2);
      assertNotSame(lib1, lib2);
      lib1 = lib2;
      assertNotNull(getCachedLibrary(savedContext, simpleMoneySrcFile));
    } finally {
      FileUtilities.setContents(simpleMoneySrcFile, oldContent);
      simpleMoneySrcFile.setLastModified(oldLastModified);
    }

    server.resetAnalyzeContext();
    server.changed(simpleMoneySrcFile);
    listener.waitForIdle(3, FIVE_MINUTES_MS);
    assertTrackedLibraryFiles(server, moneyLibFile);
    server.assertAnalyzeContext(true);
    Object lib2 = getCachedLibrary(savedContext, moneyLibFile);
    assertNotNull(lib2);
    assertNotSame(lib1, lib2);
    assertNull(getCachedLibrary(savedContext, simpleMoneySrcFile));
  }

  @Override
  protected void setUp() throws Exception {
    server = new AnalysisServerAdapter();
    savedContext = server.getSavedContext();
    listener = new Listener(server);
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
  }
}
