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

import com.google.common.base.Joiner;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AnalysisServerTest extends TestCase {

  private static final String EMPTY_CACHE_CONTENT = "v3\n</end-libraries>\n</end-cache>\n</end-queue>\n";

  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final String TEST_CLASS_SIMPLE_NAME = AnalysisServerTest.class.getSimpleName();

  private static final long FIVE_MINUTES_MS = 300000;

  private AnalysisServer server;
  private AnalysisServer defaultServer;

  private Listener listener;

  public void test_AnalysisServer_analyzeLibrary() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_analyzeLibrary(tempDir);
      }
    });
  }

  public File test_AnalysisServer_analyzeLibrary(File tempDir) throws Exception,
      InterruptedException {
    File libFile = setupMoneyLibrary(tempDir);
    setupServer();
    assertTrackedLibraryFiles();

    server.analyze(libFile);
    waitForIdle();
    if (!listener.getResolved().contains(libFile.getPath())) {
      fail("Expected resolved library " + libFile + " but found " + listener.getResolved());
    }
    assertEquals(3, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    assertTrackedLibraryFiles(libFile);
    assertTrue(isLibraryResolved(libFile));
    return libFile;
  }

  public void test_AnalysisServer_changed() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = test_AnalysisServer_analyzeLibrary(tempDir);
        listener.reset();
        server.changed(libFile);
        waitForIdle();
        if (!listener.getResolved().contains(libFile.getPath())) {
          fail("Expected resolved library " + libFile + " but found " + listener.getResolved());
        }
        assertEquals(1, listener.getResolved().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_AnalysisServer_discardDirectory() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_discardLib(tempDir, true);
      }
    });
  }

  public void test_AnalysisServer_discardFile() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_discardLib(tempDir, false);
      }
    });
  }

  public File test_AnalysisServer_discardLib(File tempDir, boolean discardParent) throws Exception {
    File libFile = test_AnalysisServer_analyzeLibrary(tempDir);
    assertTrackedLibraryFiles(libFile);

    listener.reset();
    server.discard(discardParent ? libFile.getParentFile() : libFile);
    waitForIdle();
    assertEquals(0, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertWasDiscarded(libFile);
    assertTrackedLibraryFiles();
    assertFalse(isLibraryCached(libFile));

    listener.reset();
    synchronized (getServerQueue()) {
      // Queue a task that should never run
      server.analyze(libFile);
      server.discard(discardParent ? libFile.getParentFile() : libFile);
    }
    waitForIdle();
    assertEquals(0, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    assertTrackedLibraryFiles();
    assertFalse(isLibraryCached(libFile));

    return libFile;
  }

  public void test_AnalysisServer_idleEvent() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        final int[] count = new int[] {0};
        final File libFile = setupMoneyLibrary(tempDir);
        setupServer();
        server.addIdleListener(new IdleListener() {
          @Override
          public void idle(boolean idle) {
            if (idle) {
              count[0]++;
              if (count[0] < 3) {
                server.changed(libFile);
              }
            }
          }
        });
        server.analyze(libFile);
        while (count[0] < 3) {
          waitForIdle();
        }
      }
    });
  }

  public void test_AnalysisServer_parse() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        File currencyFile = new File(libFile.getParent(), "currency.dart");
        File fileDoesNotExist = new File(libFile.getParent(), "doesNotExist.dart");
        setupServer();
        SavedContext savedContext = server.getSavedContext();

        listener.reset();
        DartUnit unit = savedContext.parse(libFile, libFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Money");
        listener.assertWasParsed(libFile, libFile);
        assertEquals(1, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(libFile, currencyFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Currency");
        listener.assertWasParsed(libFile, currencyFile);
        assertEquals(1, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(libFile, fileDoesNotExist, FIVE_MINUTES_MS);
        assertNoTopDeclaration(unit);
        listener.assertWasParsed(libFile, fileDoesNotExist);
        assertEquals(1, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(1, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(libFile, libFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Money");
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(libFile, currencyFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Currency");
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(libFile, fileDoesNotExist, FIVE_MINUTES_MS);
        assertNoTopDeclaration(unit);
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_AnalysisServer_parse2() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        File currencyFile = new File(libFile.getParent(), "currency.dart");
        setupServer();
        SavedContext savedContext = server.getSavedContext();

        listener.reset();
        DartUnit unit = savedContext.parse(libFile, currencyFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Currency");
        listener.assertWasParsed(libFile, currencyFile);
        listener.assertWasParsed(libFile, libFile);
        assertEquals(2, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(libFile, currencyFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Currency");
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_AnalysisServer_parseDoesNotExist() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        File currencyFile = new File(libFile.getParent(), "currency.dart");
        File fileDoesNotExist = new File(libFile.getParent(), "doesNotExist.dart");
        setupServer();
        SavedContext savedContext = server.getSavedContext();

        listener.reset();
        DartUnit unit = savedContext.parse(fileDoesNotExist, fileDoesNotExist, FIVE_MINUTES_MS);
        assertNoTopDeclaration(unit);
        listener.assertWasParsed(fileDoesNotExist, fileDoesNotExist);
        assertEquals(1, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(1, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(fileDoesNotExist, libFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Money");
        listener.assertWasParsed(fileDoesNotExist, libFile);
        assertEquals(1, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(fileDoesNotExist, currencyFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Currency");
        listener.assertWasParsed(fileDoesNotExist, currencyFile);
        assertEquals(1, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(fileDoesNotExist, fileDoesNotExist, FIVE_MINUTES_MS);
        assertNoTopDeclaration(unit);
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(fileDoesNotExist, libFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "Money");
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_AnalysisServer_parseResolutionErrors() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File aFile = new File(tempDir, "a.dart");
        FileUtilities.setContents(aFile, "#source('b.dart');" + LINE_SEPARATOR
            + "class A extends C { foo() { x } }");
        File bFile = new File(tempDir, "b.dart");
        FileUtilities.setContents(bFile, "class B { D f1; E f2; F f3; G f4; }");
        setupServer();
        SavedContext savedContext = server.getSavedContext();

        listener.reset();
        server.getSavedContext().resolve(aFile, FIVE_MINUTES_MS);
        assertEquals(7, listener.getErrors().size());

        listener.reset();
        DartUnit unit = savedContext.parse(aFile, aFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "A");
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        unit = savedContext.parse(aFile, bFile, FIVE_MINUTES_MS);
        assertTopDeclarationExists(unit, "B");
        assertEquals(0, listener.getParsedCount());
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_AnalysisServer_read_analyzeContext() throws Exception {
    initServer(new StringReader(
        "v3\none\n</end-libraries>\n</end-cache>\nAnalyzeContextTask\n</end-queue>"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles();
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeContextTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_analyzeLibrary() throws Exception {
    initServer(new StringReader("v3\none\n</end-libraries>\n</end-cache>\none\n</end-queue>"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles();
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeLibraryTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_empty() throws Exception {
    initServer(new StringReader(EMPTY_CACHE_CONTENT));
    assertEquals(0, getTrackedLibraryFiles().length);
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeLibraryTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_empty_v1() throws Exception {
    initServer(new StringReader("v1\n</end-libraries>\nsome-stuff"));
    assertEquals(0, getTrackedLibraryFiles().length);
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeContextTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_empty_v2() throws Exception {
    initServer(new StringReader("v2\n</end-libraries>\n</end-cache>"));
    assertEquals(0, getTrackedLibraryFiles().length);
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeContextTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_one() throws Exception {
    initServer(new StringReader("v3\none\n</end-libraries>\n</end-cache>\n</end-queue>"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles();
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeLibraryTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_one_v1() throws Exception {
    initServer(new StringReader("v1\none\n</end-libraries>\nsome-stuff"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles();
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeContextTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_one_v2() throws Exception {
    initServer(new StringReader("v2\none\n</end-libraries>\n</end-cache>"));
    File[] trackedLibraryFiles = getTrackedLibraryFiles();
    assertEquals(1, trackedLibraryFiles.length);
    assertEquals("one", trackedLibraryFiles[0].getName());
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeContextTask", getServerQueue().get(0).getClass().getSimpleName());
  }

  public void test_AnalysisServer_read_version_invalid() throws Exception {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    try {
      readCache(new StringReader("vOther"));
      fail("Expected IOException because of invalid version number");
    } catch (Exception e) {
      //$FALL-THROUGH$
    }
  }

  public void test_AnalysisServer_read_version_missing() throws Exception {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    try {
      readCache(new StringReader(""));
      fail("Expected IOException because of missing version number");
    } catch (Exception e) {
      //$FALL-THROUGH$
    }
  }

  public void test_AnalysisServer_resolve() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();

        listener.reset();
        LibraryUnit libUnit = server.getSavedContext().resolve(libFile, FIVE_MINUTES_MS);
        assertNotNull(libUnit);
        listener.assertWasParsed(libFile, libFile);
        listener.assertWasResolved(libFile);
        assertTrue(listener.getParsedCount() > 10);
        assertEquals(3, listener.getResolved().size());
        //assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        // assert that resolved unit has been cached
        listener.reset();
        LibraryUnit libUnit2 = server.getSavedContext().resolve(libFile, FIVE_MINUTES_MS);
        assertTrue(libUnit == libUnit2);
        assertEquals(0, listener.getResolved().size());
        assertEquals(0, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        File fileDoesNotExist = new File(libFile.getParent(), "doesNotExist.dart");
        libUnit = server.getSavedContext().resolve(fileDoesNotExist, FIVE_MINUTES_MS);
        assertNotNull(libUnit);
        listener.assertWasParsed(fileDoesNotExist, fileDoesNotExist);
        listener.assertWasResolved(fileDoesNotExist);
        assertEquals(1, listener.getParsedCount());
        assertEquals(1, listener.getResolved().size());
        assertEquals(2, listener.getErrors().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_AnalysisServer_resolveWhenBusy() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();

        DartUnit unit = server.getSavedContext().resolve(libFile, FIVE_MINUTES_MS).getSelfDartUnit();
        assertEquals("Money", unit.getTopDeclarationNames().iterator().next());

        // Simulate a busy system
        ResolveCallback.Sync callback = new ResolveCallback.Sync();
        synchronized (getServerQueue()) {
          FileUtilities.setContents(libFile, "class A { }");
          server.changed(libFile);
          server.getSavedContext().resolve(libFile, callback);
        }
        unit = callback.waitForResolve(FIVE_MINUTES_MS).getSelfDartUnit();
        assertEquals("A", unit.getTopDeclarationNames().iterator().next());
      }
    });
  }

  // commented out due to timing issues now that builder is driving the 
  // analysis server
  // TODO(danrubel): fix test
//  public void test_AnalysisServer_resolveWhenBusy2() throws Exception {
//    if (!DartCoreDebug.ANALYSIS_SERVER) {
//      return;
//    }
//    setupDefaultServer();
//
//    String projName = getClass().getSimpleName() + "_createResolveDispose";
//    String libFileName = "mylib.dart";
//
//    TestProject proj = new TestProject(projName);
//    File libFile = proj.setFileContent(libFileName, "class A { }").getLocation().toFile();
//    DartUnit unit = server.getSavedContext().resolve(libFile, FIVE_MINUTES_MS).getSelfDartUnit();
//    assertEquals("A", unit.getTopDeclarationNames().iterator().next());
//
//    // Simulate a very busy system
//    ResolveCallback.Sync callback = new ResolveCallback.Sync();
//    synchronized (getServerQueue()) {
//      proj.dispose();
//      proj = new TestProject(projName);
//      libFile = proj.setFileContentWithoutWaitingForAnalysis(libFileName, "class B { }").getLocation().toFile();
//      server.getSavedContext().resolve(libFile, callback);
//    }
//    unit = callback.waitForResolve(FIVE_MINUTES_MS).getSelfDartUnit();
//    assertEquals("B", unit.getTopDeclarationNames().iterator().next());
//    proj.dispose();
//  }

  public void test_AnalysisServer_scan() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        File sourcedFile = new File(libFile.getParent(), "currency.dart");
        setupServer();
        assertTrackedLibraryFiles();

        listener.reset();
        server.scan(sourcedFile, true);
        waitForIdle();
        assertTrackedLibraryFiles(sourcedFile);
        assertEquals(3, listener.getResolved().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        synchronized (getServerQueue()) {
          server.scan(libFile, true);
          server.discard(libFile);
        }
        waitForIdle();
        assertTrackedLibraryFiles(libFile);
        assertEquals(1, listener.getResolved().size());
        listener.assertNoDuplicates();
        listener.assertWasDiscarded(sourcedFile);
      }
    });
  }

  public void test_AnalysisServer_scanAndIndex() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        File sourcedFile = new File(libFile.getParent(), "currency.dart");
        setupServer();
        assertTrackedLibraryFiles();

        //    final InMemoryIndex index = InMemoryIndex.newInstanceForTesting();
        //    new Thread(getClass().getSimpleName() + " scanAndIndex") {
        //      @Override
        //      public void run() {
        //        index.getOperationProcessor().run();
        //      };
        //    }.start();
        //    server.addAnalysisListener(new AnalysisIndexManager(index));
        //    assertEquals(0, index.getRelationshipCount());
        //
        //    Resource libResource = new Resource(composeResourceId(
        //        libFile.toURI().toString(),
        //        libFile.toURI().toString()));
        //    libElement = new Element(libResource, "#library");

        listener.reset();
        server.scan(tempDir, true);
        waitForIdle();
        assertTrackedLibraryFiles(libFile);
        assertEquals(3, listener.getResolved().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        listener.reset();
        server.scan(sourcedFile, true);
        waitForIdle();
        assertTrackedLibraryFiles(libFile);
        assertEquals(0, listener.getResolved().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();
      }
    });
  }

  public void test_AnalysisServer_stop() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();

        final boolean[] latch = new boolean[1];
        server.getSavedContext().resolve(libFile, new ResolveCallback() {
          @Override
          public void resolved(LibraryUnit libraryUnit) {
            try {
              synchronized (latch) {
                latch[0] = true;
                latch.notifyAll();
              }
              Thread.sleep(100);
            } catch (InterruptedException e) {
              //$FALL-THROUGH$
            }
          }
        });
        synchronized (latch) {
          if (!latch[0]) {
            latch.wait(FIVE_MINUTES_MS);
          }
        }

        server.stop();
        assertTrue(server.isIdle());
      }
    });
  }

  public void test_AnalysisServer_write_1() throws Exception {
    initServer(null);
    String libFileName = "myLibrary.dart";
    server.analyze(new File(libFileName).getAbsoluteFile());
    assertEquals(1, getServerQueue().size());
    assertEquals("AnalyzeContextTask", getServerQueue().get(0).getClass().getSimpleName());
    StringWriter writer = new StringWriter(5000);
    writeCache(writer);
    assertTrue(writer.toString().indexOf(libFileName) > 0);
    assertTrue(writer.toString().indexOf("AnalyzeContext") > 0);
  }

  public void test_AnalysisServer_write_empty() throws Exception {
    initServer(null);
    assertEquals(0, getServerQueue().size());
    StringWriter writer = new StringWriter(5000);
    writeCache(writer);
    Assert.assertEquals(EMPTY_CACHE_CONTENT, writer.toString());
  }

  public void test_AnalysisServer_write_read_1() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = test_AnalysisServer_analyzeLibrary(tempDir);

        assertTrackedLibraryFiles(libFile);
        assertTrue(isLibraryResolved(libFile));
        assertEquals(0, getServerQueue().size());

        StringWriter writer = new StringWriter(5000);
        server.stop();
        writeCache(writer);
        setupServer(new StringReader(writer.toString()));

        assertTrackedLibraryFiles(libFile);
        assertTrue(isLibraryCached(libFile));
        assertFalse(isLibraryResolved(libFile));
        assertEquals(0, getServerQueue().size());
      }
    });
  }

  public void test_AnalysisServer_write_read_2() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();
        assertTrackedLibraryFiles();

        String contents = FileUtilities.getContents(libFile);
        String libraryDirective = "#library(\"Money\");";
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
        waitForIdle();
        assertTrackedLibraryFiles(libFile);
        assertTrue(isLibraryResolved(libFile));

        StringWriter writer = new StringWriter(5000);
        server.stop();
        writeCache(writer);
        setupServer(new StringReader(writer.toString()));
        assertTrackedLibraryFiles(libFile);
        assertTrue(isLibraryCached(libFile));
        assertFalse(isLibraryResolved(libFile));
      }
    });
  }

  public void test_AnalysisServer_write_read_bad() throws Exception {
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

  @Override
  protected void tearDown() throws Exception {
    if (server != null && server != defaultServer) {
      server.stop();
    }
  }

  private void assertNoTopDeclaration(DartUnit unit) {
    assertNotNull(unit);
    Set<String> actualNames = unit.getTopDeclarationNames();
    if (actualNames.size() != 0) {
      fail("Expected no top level declarations, but found " + actualNames);
    }
  }

  private void assertTopDeclarationExists(DartUnit unit, String expectedName) {
    assertNotNull(unit);
    Set<String> actualNames = unit.getTopDeclarationNames();
    if (!actualNames.contains(expectedName)) {
      fail("Expected " + expectedName + " but found " + actualNames);
    }
  }

  private void assertTrackedLibraryFiles(File... expected) throws Exception {
    File[] actual = getTrackedLibraryFiles();
    if (actual.length == expected.length) {
      HashSet<File> files = new HashSet<File>();
      files.addAll(Arrays.asList(actual));
      if (actual.length == files.size()) {
        for (File file : expected) {
          if (!files.remove(file)) {
            break;
          }
        }
      }
      if (files.size() == 0) {
        return;
      }
    }
    String msg = "Expected:";
    for (File file : expected) {
      msg += "\n   " + file;
    }
    msg += "\nbut found:";
    for (File file : actual) {
      msg += "\n   " + file;
    }
    fail(msg);
  }

  @SuppressWarnings("unchecked")
  private ArrayList<Task> getServerQueue() throws Exception {
    Field field = server.getClass().getDeclaredField("queue");
    field.setAccessible(true);
    return (ArrayList<Task>) field.get(server);
  }

  private File[] getTrackedLibraryFiles() throws Exception {
    Method method = server.getClass().getDeclaredMethod("getTrackedLibraryFiles");
    method.setAccessible(true);
    Object result = method.invoke(server);
    return (File[]) result;
  }

  private void initServer(Reader reader) throws Exception {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    if (reader != null) {
      readCache(reader);
    }
    listener = new Listener(server);
  }

  private boolean isLibraryCached(File libFile) throws Exception {
    Method method = server.getClass().getDeclaredMethod("isLibraryCached", File.class);
    method.setAccessible(true);
    Object result = method.invoke(server, libFile);
    return result instanceof Boolean && ((Boolean) result).booleanValue();
  }

  private boolean isLibraryResolved(File libFile) throws Exception {
    Method method = server.getClass().getDeclaredMethod("isLibraryResolved", File.class);
    method.setAccessible(true);
    Object result = method.invoke(server, libFile);
    return result instanceof Boolean && ((Boolean) result).booleanValue();
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

  private void setupDefaultServer() throws Exception {
    if (!DartCoreDebug.ANALYSIS_SERVER) {
      fail("Do not call this method when analysis server is not enabled");
    }
    defaultServer = SystemLibraryManagerProvider.getDefaultAnalysisServer();
    server = defaultServer;
    listener = new Listener(server);
    waitForIdle();
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
