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
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertQueuedTasks;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.assertTrackedLibraryFiles;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.getServerTaskQueue;
import static com.google.dart.tools.core.analysis.AnalysisTestUtilities.getTrackedLibraryFiles;

import com.google.common.base.Joiner;
import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class AnalysisServerTest extends TestCase {

  private static final String EMPTY_CACHE_CONTENT = "v4\n</end-libraries>\n0\n</end-cache>\n</end-queue>\n";
  private static final String TEST_CLASS_SIMPLE_NAME = AnalysisServerTest.class.getSimpleName();
  private static final long FIVE_MINUTES_MS = 300000;

  private AnalysisServer server;
  private Listener listener;

  public void test_analyzeLibrary() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_analyzeLibrary(tempDir);
      }
    });
  }

  public File test_analyzeLibrary(File tempDir) throws Exception, InterruptedException {
    File libFile = setupMoneyLibrary(tempDir);
    setupServer();
    assertTrackedLibraryFiles(server);

    server.analyze(libFile);
    listener.waitForResolved(FIVE_MINUTES_MS, libFile);
    listener.assertResolvedCount(3);
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    assertTrackedLibraryFiles(server, libFile);
    assertTrue(server.isLibraryResolved(libFile));
    return libFile;
  }

  public void test_parseResolutionErrors() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File aFile = new File(tempDir, "a.dart");
        PrintStringWriter writer = new PrintStringWriter();
        writer.println("#source('b.dart');");
        writer.println("class A extends C { foo() { x } }");
        FileUtilities.setContents(aFile, writer.toString());
        File bFile = new File(tempDir, "b.dart");
        FileUtilities.setContents(bFile, "class B { D f1; E f2; F f3; G f4; }");
        setupServer();
        SavedContext savedContext = server.getSavedContext();

        listener.reset();
        savedContext.resolve(aFile, FIVE_MINUTES_MS);
        assertTrue(listener.getErrorCount() > 5);

        listener.reset();
        ParseResult result = savedContext.parse(aFile, aFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(result.getDartUnit(), "A");
        assertEquals(1, result.getParseErrors().length);
        listener.assertParsedCount(0);
        listener.assertResolvedCount(0);
        listener.assertNoErrors();
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        result = savedContext.parse(aFile, bFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(result.getDartUnit(), "B");
        assertEquals(0, result.getParseErrors().length);
        listener.assertParsedCount(0);
        listener.assertResolvedCount(0);
        listener.assertNoErrors();
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_read_analyzeContext() throws Exception {
    initServer(new StringReader(
        "v4\none\n</end-libraries>\n0\n</end-cache>\nAnalyzeContextTask\n</end-queue>"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles(server);
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertQueuedTasks(server, "AnalyzeContextTask");
  }

  public void test_read_analyzeLibrary() throws Exception {
    initServer(new StringReader("v4\none\n</end-libraries>\n0\n</end-cache>\none\n</end-queue>"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles(server);
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertQueuedTasks(server, "AnalyzeLibraryTask");
  }

  public void test_read_empty() throws Exception {
    initServer(new StringReader(EMPTY_CACHE_CONTENT));
    assertEquals(0, getTrackedLibraryFiles(server).length);
    assertQueuedTasks(server, "AnalyzeLibraryTask");
  }

  public void test_read_empty_v1() throws Exception {
    initServer(new StringReader("v1\n</end-libraries>\nsome-stuff"));
    assertEquals(0, getTrackedLibraryFiles(server).length);
    assertQueuedTasks(server, "AnalyzeContextTask");
  }

  public void test_read_empty_v2() throws Exception {
    initServer(new StringReader("v2\n</end-libraries>\n</end-cache>"));
    assertEquals(0, getTrackedLibraryFiles(server).length);
    assertQueuedTasks(server, "AnalyzeContextTask");
  }

  public void test_read_empty_v3() throws Exception {
    initServer(new StringReader("v3\n</end-libraries>\n</end-cache>\n</end-queue>"));
    assertEquals(0, getTrackedLibraryFiles(server).length);
    assertQueuedTasks(server, "AnalyzeLibraryTask");
  }

  public void test_read_one() throws Exception {
    PrintStringWriter content = new PrintStringWriter();

    // version
    content.println("v4");

    // tracked libraries
    content.println("one.dart");
    content.println("</end-libraries>");

    // saved context... one library cached
    content.println("0");
    content.println("two.dart");
    content.println("true");
    content.println("three");
    content.println("</end-prefixes>");
    content.println("four.dart");
    content.println("four.dart");
    content.println("</end-imports>");
    content.println("five.dart");
    content.println("five.dart");
    content.println("</end-sources>");
    content.println("</end-cache>");

    // queued tasks
    content.print("</end-queue>");

    initServer(new StringReader(content.toString()));

    File[] trackedLibraryFiles = getTrackedLibraryFiles(server);
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one.dart", trackedLibraryFiles[0].getName());

    assertPackageContexts(server);
    assertCachedLibraries(server, null, new File("two.dart"));

    assertQueuedTasks(server, "AnalyzeLibraryTask");
  }

  public void test_read_one_v1() throws Exception {
    initServer(new StringReader("v1\none\n</end-libraries>\nsome-stuff"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles(server);
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertQueuedTasks(server, "AnalyzeContextTask");
  }

  public void test_read_one_v2() throws Exception {
    initServer(new StringReader("v2\none\n</end-libraries>\n</end-cache>"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles(server);
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertQueuedTasks(server, "AnalyzeContextTask");
  }

  public void test_read_one_v3() throws Exception {
    PrintStringWriter content = new PrintStringWriter();

    // version
    content.println("v3");

    // tracked libraries
    content.println("one.dart");
    content.println("</end-libraries>");

    // saved context... one library cached
    content.println("two.dart");
    content.println("true");
    content.println("three");
    content.println("</end-prefixes>");
    content.println("four.dart");
    content.println("four.dart");
    content.println("</end-imports>");
    content.println("five.dart");
    content.println("five.dart");
    content.println("</end-sources>");
    content.println("</end-cache>");

    // queued tasks
    content.print("</end-queue>");

    initServer(new StringReader(content.toString()));

    File[] trackedLibraryFiles = getTrackedLibraryFiles(server);
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one.dart", trackedLibraryFiles[0].getName());

    assertPackageContexts(server);
    assertCachedLibraries(server, null, new File("two.dart"));

    assertQueuedTasks(server, "AnalyzeLibraryTask");
  }

  public void test_read_two() throws Exception {
    init2Contexts();
    assert2Contexts();
    assertQueuedTasks(server, "AnalyzeLibraryTask");
  }

  public void test_read_version_invalid() throws Exception {
    PackageLibraryManager libraryManager = PackageLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    try {
      readCache(new StringReader("vOther"));
      fail("Expected IOException because of invalid version number");
    } catch (Exception e) {
      //$FALL-THROUGH$
    }
  }

  public void test_read_version_missing() throws Exception {
    PackageLibraryManager libraryManager = PackageLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    try {
      readCache(new StringReader(""));
      fail("Expected IOException because of missing version number");
    } catch (Exception e) {
      //$FALL-THROUGH$
    }
  }

  public void test_write_1() throws Exception {
    final String libraryFileName1 = "myLibrary.dart";
    final String libraryFileName2 = "someOtherAbcLibrary.dart";

    initServer(null);
    server.analyze(new File(libraryFileName1).getAbsoluteFile());
    assertQueuedTasks(server, "AnalyzeContextTask");
    synchronized (getServerTaskQueueLock()) {
      server.start();
      getServerTaskQueue(server).setAnalyzing(false);
    }
    assertTrue(server.waitForIdle(FIVE_MINUTES_MS));
    server.getSavedContext().resolve(new File(libraryFileName2).getAbsoluteFile(), null);

    StringWriter writer = new StringWriter(5000);
    writeCache(writer);
    assertTrue(writer.toString().indexOf(libraryFileName1) > 0);
    assertTrue(writer.toString().indexOf(libraryFileName2) > 0);
    assertTrue(writer.toString().indexOf("AnalyzeContext") > 0);
  }

  public void test_write_empty() throws Exception {
    initServer(null);
    assertQueuedTasks(server);
    StringWriter writer = new StringWriter(5000);
    writeCache(writer);
    Assert.assertEquals(EMPTY_CACHE_CONTENT, writer.toString());
  }

  public void test_write_read_1() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = test_analyzeLibrary(tempDir);

        waitForIdle();
        assertQueuedTasks(server);

        StringWriter writer = new StringWriter(5000);
        server.stop();
        SavedContext savedContext = server.getSavedContext();
        savedContext.resolve(new File("someFile.dart").getAbsoluteFile(), null);
        savedContext.resolve(new File("otherFile.dart").getAbsoluteFile(), null);
        writeCache(writer);

        initServer(new StringReader(writer.toString()));
        assertQueuedTasks(server, "AnalyzeLibraryTask", "AnalyzeLibraryTask"); // someFile, otherFile
        server.start();
        waitForIdle();

        assertTrackedLibraryFiles(server, libFile);
        assertTrue(server.isLibraryCached(libFile));
        assertFalse(server.isLibraryResolved(libFile));
        assertQueuedTasks(server);
      }
    });
  }

  public void test_write_read_2() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();
        assertTrackedLibraryFiles(server);

        String contents = FileUtilities.getContents(libFile);
        String libraryDirective = "library Money;";
        int start = contents.indexOf(libraryDirective);
        assertTrue(start >= 0);
        contents = Joiner.on("\n").join(
            contents.substring(0, start),
            "#library('Bad\\",
            "Na\"me');",
            "#import('Bad\\",
            "Import.dart');",
            "#import('BadPrefix.dart', prefix:'Bad\\",
            "prefix');",
            "#source('Bad\\",
            "Source.dart');",
            contents.substring(start + libraryDirective.length()));
        FileUtilities.setContents(libFile, contents);

        server.analyze(libFile);
        listener.waitForResolved(FIVE_MINUTES_MS, libFile);
        assertTrackedLibraryFiles(server, libFile);
        assertTrue(server.isLibraryResolved(libFile));

        StringWriter writer = new StringWriter(5000);
        server.stop();
        writeCache(writer);

        initServer(new StringReader(writer.toString()));

        assertQueuedTasks(server, "AnalyzeLibraryTask"); // dart:core
        assertTrackedLibraryFiles(server, libFile);
        assertTrue(server.isLibraryCached(libFile));
        assertFalse(server.isLibraryResolved(libFile));
      }
    });
  }

  public void test_write_read_bad() throws Exception {
    setupServer();

    // Cannot write cache while server is still running

    StringWriter writer = new StringWriter(5000);
    try {
      writeCache(writer);
      fail("should not be able to write cache while server is still running");
    } catch (IllegalStateException e) {
      //$FALL-THROUGH$
    }

    // Cannot read cache while server is still running

    try {
      readCache(new StringReader(EMPTY_CACHE_CONTENT));
      fail("should not be able to read cache while server is still running");
    } catch (IllegalStateException e) {
      //$FALL-THROUGH$
    }

    server.stop();

    // Cannot read empty or incomptable version cache file

    try {
      readCache(new StringReader(""));
      fail("should not be able to read cache with missing version number");
    } catch (IOException e) {
      //$FALL-THROUGH$
    }
    try {
      readCache(new StringReader("nothing"));
      fail("should not be able to read cache with invalid version number");
    } catch (IOException e) {
      //$FALL-THROUGH$
    }
  }

  public void test_write_read_two() throws Exception {
    init2Contexts();

    StringWriter writer = new StringWriter(5000);
    writeCache(writer);
    initServer(new StringReader(writer.toString()));

    assert2Contexts();
    assertQueuedTasks(server, "AnalyzeLibraryTask");
  }

  @Override
  protected void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  private void assert2Contexts() throws Exception {
    File[] trackedLibraryFiles = getTrackedLibraryFiles(server);
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one.dart", trackedLibraryFiles[0].getName());

    assertPackageContexts(server, new File("bankAppDir"));
    assertCachedLibraries(server, null, new File("two.dart"));
    assertCachedLibraries(server, new File("bankAppDir"), new File("six.dart"));
  }

  private void assertTopDeclarationExists(DartUnit unit, String expectedName) {
    assertNotNull(unit);
    Set<String> actualNames = unit.getTopDeclarationNames();
    if (!actualNames.contains(expectedName)) {
      fail("Expected " + expectedName + " but found " + actualNames);
    }
  }

  private Object getServerTaskQueueLock() throws Exception {
    TaskQueue queue = getServerTaskQueue(server);
    Field field = queue.getClass().getDeclaredField("queue");
    field.setAccessible(true);
    return field.get(queue);
  }

  private void init2Contexts() throws Exception {
    PrintStringWriter content = new PrintStringWriter();

    // version
    content.println("v4");

    // tracked libraries
    content.println("one.dart");
    content.println("</end-libraries>");

    // saved context... one library cached
    content.println("1");
    content.println("two.dart");
    content.println("true");
    content.println("three");
    content.println("</end-prefixes>");
    content.println("four.dart");
    content.println("four.dart");
    content.println("</end-imports>");
    content.println("five.dart");
    content.println("five.dart");
    content.println("</end-sources>");
    content.println("</end-cache>");

    // bank context... one library cached
    content.println("bankAppDir");
    content.println("six.dart");
    content.println("true");
    content.println("seven");
    content.println("</end-prefixes>");
    content.println("eight.dart");
    content.println("eight.dart");
    content.println("</end-imports>");
    content.println("nine.dart");
    content.println("nine.dart");
    content.println("</end-sources>");
    content.println("</end-cache>");

    // queued tasks
    content.print("</end-queue>");

    initServer(new StringReader(content.toString()));
  }

  private void initServer(Reader reader) throws Exception {
    PackageLibraryManager libraryManager = PackageLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    if (reader != null) {
      readCache(reader);
    }
    listener = new Listener(server);
  }

  private void readCache(Reader reeader) throws Exception {
    // server.readCache(cacheFile)
    Method method = server.getClass().getDeclaredMethod("readCache", Reader.class);
    method.setAccessible(true);
    try {
      method.invoke(server, reeader);
    } catch (InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }

  private File setupMoneyLibrary(File tempDir) throws IOException {
    File targetDir = new File(tempDir, TEST_CLASS_SIMPLE_NAME);
    TestUtilities.copyPluginRelativeContent("Money", targetDir);
    File libFile = new File(targetDir, "money.dart");
    if (!libFile.exists()) {
      fail("Dart file does not exist: " + libFile);
    }
    return libFile;
  }

  private void setupServer() throws Exception {
    setupServer(null);
  }

  private void setupServer(Reader reader) throws Exception {
    initServer(reader);
    server.start();
    waitForIdle();
  }

  /**
   * Wait for the server and index under test to finish background processing
   * 
   * @return the # of milliseconds waited
   */
  private long waitForIdle() throws InterruptedException {
    final long start = System.currentTimeMillis();
    AnalysisTestUtilities.waitForIdle(server, FIVE_MINUTES_MS);
    return System.currentTimeMillis() - start;
  }

  private void writeCache(Writer writer) throws Exception {
    // server.writeCache(cacheFile);
    Method method = server.getClass().getDeclaredMethod("writeCache", Writer.class);
    method.setAccessible(true);
    try {
      method.invoke(server, writer);
    } catch (InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }
}
