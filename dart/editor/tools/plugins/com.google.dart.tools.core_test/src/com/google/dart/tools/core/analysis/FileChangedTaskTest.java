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
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.getCachedLibrary;

import java.io.File;

public class FileChangedTaskTest extends AbstractDartCoreTest {

  private static final long FIVE_MINUTES_MS = 300000;

  private static File tempDir;
  private static File libraryFile;
  private static File dartFile;

  /**
   * Called once prior to executing the first test in this class
   */
  public static void setUpOnce() throws Exception {
    tempDir = TestUtilities.createTempDirectory();
    File moneyDir = new File(tempDir, "money");
    TestUtilities.copyPluginRelativeContent("Money", moneyDir);
    libraryFile = new File(moneyDir, "money.dart");
    assertTrue(libraryFile.exists());
    dartFile = new File(moneyDir, "simple_money.dart");
    assertTrue(dartFile.exists());
  }

  /**
   * Called once after executing the last test in this class
   */
  public static void tearDownOnce() {
    FileUtilities.delete(tempDir);
    tempDir = null;
  }

  private AnalysisServerAdapter server;
  private Context savedContext;

  /**
   * Assert cache discarded only if file has changed on disk
   */
  public void test_changed() {
    ParseResult result1 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertNotNull(result1.getDartUnit());
    ParseResult result2 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertSame(result1.getDartUnit(), result2.getDartUnit());

    server.changed(libraryFile);
    ParseResult result3 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertSame(result1.getDartUnit(), result3.getDartUnit());
    server.assertAnalyzeContext(false);

    libraryFile.setLastModified(System.currentTimeMillis() + 1000);
    server.changed(libraryFile);
    ParseResult result4 = savedContext.parse(libraryFile, libraryFile, FIVE_MINUTES_MS);
    assertNotSame(result1.getDartUnit(), result4.getDartUnit());
    server.assertAnalyzeContext(true);
  }

  /**
   * Assert removing #source directive causes sourced file to become analyzed as library
   */
  public void test_changed_library() throws Exception {
    final String directive = "#source(\"simple_money.dart\");";
    final String oldContent = FileUtilities.getContents(libraryFile);
    int index = oldContent.indexOf(directive);
    assertTrue(index > 0);
    final String newContent = oldContent.substring(0, index)
        + oldContent.substring(index + directive.length());

    server.scan(libraryFile, true);
    assertTrue(server.waitForIdle(FIVE_MINUTES_MS));
    assertTrackedLibraryFiles(server, libraryFile);
    Object lib1 = getCachedLibrary(savedContext, libraryFile);
    assertNotNull(lib1);
    assertNull(getCachedLibrary(savedContext, dartFile));

    server.resetAnalyzeContext();
    long oldLastModified = libraryFile.lastModified();
    FileUtilities.setContents(libraryFile, newContent);
    // Ensure marked as modified... lastModified is only accurate to the second
    libraryFile.setLastModified(oldLastModified + 1000);
    try {
      server.changed(libraryFile);
      assertTrue(server.waitForIdle(FIVE_MINUTES_MS));
      assertTrackedLibraryFiles(server, libraryFile, dartFile);
      server.assertAnalyzeContext(true);
      Object lib2 = getCachedLibrary(savedContext, libraryFile);
      assertNotNull(lib2);
      assertNotSame(lib1, lib2);
      lib1 = lib2;
      assertNotNull(getCachedLibrary(savedContext, dartFile));
    } finally {
      FileUtilities.setContents(libraryFile, oldContent);
      libraryFile.setLastModified(oldLastModified);
    }

    server.resetAnalyzeContext();
    server.changed(libraryFile);
    assertTrue(server.waitForIdle(FIVE_MINUTES_MS));
    assertTrackedLibraryFiles(server, libraryFile);
    server.assertAnalyzeContext(true);
    Object lib2 = getCachedLibrary(savedContext, libraryFile);
    assertNotNull(lib2);
    assertNotSame(lib1, lib2);
    assertNull(getCachedLibrary(savedContext, dartFile));
  }

  /**
   * Assert adding #library directive causes sourced file to become analyzed as library
   */
  public void test_changed_source() throws Exception {
    final String oldContent = FileUtilities.getContents(dartFile);
    PrintStringWriter writer = new PrintStringWriter();
    writer.println("#library(\"foobar\");");
    writer.append(oldContent);
    final String newContent = writer.toString();

    server.scan(libraryFile, true);
    assertTrue(server.waitForIdle(FIVE_MINUTES_MS));
    assertTrackedLibraryFiles(server, libraryFile);
    Object lib1 = getCachedLibrary(savedContext, libraryFile);
    assertNotNull(lib1);
    assertNull(getCachedLibrary(savedContext, dartFile));

    server.resetAnalyzeContext();
    long oldLastModified = dartFile.lastModified();
    FileUtilities.setContents(dartFile, newContent);
    // Ensure marked as modified... lastModified is only accurate to the second
    dartFile.setLastModified(oldLastModified + 1000);
    try {
      server.changed(dartFile);
      assertTrue(server.waitForIdle(FIVE_MINUTES_MS));
      assertTrackedLibraryFiles(server, libraryFile, dartFile);
      server.assertAnalyzeContext(true);
      Object lib2 = getCachedLibrary(savedContext, libraryFile);
      assertNotNull(lib2);
      assertNotSame(lib1, lib2);
      lib1 = lib2;
      assertNotNull(getCachedLibrary(savedContext, dartFile));
    } finally {
      FileUtilities.setContents(dartFile, oldContent);
      dartFile.setLastModified(oldLastModified);
    }

    server.resetAnalyzeContext();
    server.changed(dartFile);
    assertTrue(server.waitForIdle(FIVE_MINUTES_MS));
    assertTrackedLibraryFiles(server, libraryFile);
    server.assertAnalyzeContext(true);
    Object lib2 = getCachedLibrary(savedContext, libraryFile);
    assertNotNull(lib2);
    assertNotSame(lib1, lib2);
    assertNull(getCachedLibrary(savedContext, dartFile));
  }

  @Override
  protected void setUp() throws Exception {
    server = new AnalysisServerAdapter();
    savedContext = server.getSavedContext();
    server.start();
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
  }
}
