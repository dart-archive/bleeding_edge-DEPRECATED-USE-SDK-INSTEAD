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

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class AnalysisServerTest extends TestCase {

  private static class Resolution implements ResolveLibraryCallback {
    private LibraryUnit result;

    @Override
    public void resolved(LibraryUnit libraryUnit) {
      synchronized (this) {
        result = libraryUnit;
        notifyAll();
      }
    }

    LibraryUnit waitForResolution() {
      synchronized (this) {
        if (result == null) {
          try {
            wait(FIVE_MINUTES_MS);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
          if (result == null) {
            fail("Timed out waiting for resolution");
          }
        }
        LibraryUnit libUnit = result;
        result = null;
        return libUnit;
      }
    }
  }

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

    server.analyzeLibrary(libFile);
    waitForIdle();
    if (!listener.getResolved().contains(libFile.getPath())) {
      fail("Expected resolved library " + libFile + " but found " + listener.getResolved());
    }
    assertEquals(3, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    assertTrackedLibraryFiles(libFile);
    assertTrue(isLibraryFileCached(libFile));
    return libFile;
  }

  public void test_AnalysisServer_changed() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_changed(tempDir);
      }
    });
  }

  public File test_AnalysisServer_changed(File tempDir) throws Exception {
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
    return libFile;
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
    assertFalse(isLibraryFileCached(libFile));

    listener.reset();
    synchronized (getServerQueue()) {
      // Queue a task that should never run
      server.analyzeLibrary(libFile);
      server.discard(discardParent ? libFile.getParentFile() : libFile);
    }
    waitForIdle();
    assertEquals(0, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    assertTrackedLibraryFiles();
    assertFalse(isLibraryFileCached(libFile));

    return libFile;
  }

  public void test_AnalysisServer_idleEvent() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_idleEvent(tempDir);
      }
    });
  }

  public void test_AnalysisServer_idleEvent(File tempDir) throws Exception {
    final int[] count = new int[] {0};
    final File libFile = setupMoneyLibrary(tempDir);
    setupServer();
    server.addAnalysisListener(new AnalysisListener.Empty() {
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
    server.analyzeLibrary(libFile);
    while (count[0] < 3) {
      waitForIdle();
    }
  }

  public void test_AnalysisServer_parseLibraryFile() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_parseLibraryFile(tempDir);
      }
    });
  }

  public void test_AnalysisServer_parseLibraryFile(File tempDir) throws Exception {
    File libFile = setupMoneyLibrary(tempDir);
    setupServer();

    listener.reset();
    ParseLibraryFileEvent result = parseLibraryFile(libFile);
    assertNotNull(result);
    assertNotNull(result.getUnit());
    assertEquals(1, result.getImportedFiles().size());
    assertEquals(4, result.getSourcedFiles().size());
    listener.assertWasParsed(libFile, libFile);
    assertEquals(1, listener.getParsedCount());
    assertEquals(0, listener.getResolved().size());
    assertEquals(0, listener.getErrors().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();

    listener.reset();
    File fileDoesNotExist = new File(libFile.getParent(), "doesNotExist.dart");
    result = parseLibraryFile(fileDoesNotExist);
    assertNotNull(result);
    assertNotNull(result.getUnit());
    assertEquals(1, result.getImportedFiles().size()); // implicit import dart:core
    assertEquals(0, result.getSourcedFiles().size());
    listener.assertWasParsed(fileDoesNotExist, fileDoesNotExist);
    assertEquals(1, listener.getParsedCount());
    assertEquals(0, listener.getResolved().size());
    assertEquals(1, listener.getErrors().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
  }

  public void test_AnalysisServer_resolveLibrary() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_resolveLibrary(tempDir);
      }
    });
  }

  public void test_AnalysisServer_resolveLibrary(File tempDir) throws Exception {
    File libFile = setupMoneyLibrary(tempDir);
    setupServer();

    listener.reset();
    LibraryUnit libUnit = waitForResolution(libFile);
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
    LibraryUnit libUnit2 = waitForResolution(libFile);
    assertTrue(libUnit == libUnit2);
    assertEquals(0, listener.getResolved().size());
    assertEquals(0, listener.getErrors().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();

    listener.reset();
    File fileDoesNotExist = new File(libFile.getParent(), "doesNotExist.dart");
    libUnit = waitForResolution(fileDoesNotExist);
    assertNotNull(libUnit);
    listener.assertWasParsed(fileDoesNotExist, fileDoesNotExist);
    listener.assertWasResolved(fileDoesNotExist);
    assertEquals(1, listener.getParsedCount());
    assertEquals(1, listener.getResolved().size());
    assertEquals(1, listener.getErrors().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
  }

  public void test_AnalysisServer_resolveLibraryWhenBusy() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();

        DartUnit unit = waitForResolution(libFile).getSelfDartUnit();
        assertEquals("Money", unit.getTopDeclarationNames().iterator().next());

        // Simulate a busy system
        Resolution callback = new Resolution();
        synchronized (getServerQueue()) {
          FileUtilities.setContents(libFile, "class A { }");
          server.changed(libFile);
          server.resolveLibrary(libFile, callback);
        }
        unit = callback.waitForResolution().getSelfDartUnit();
        assertEquals("A", unit.getTopDeclarationNames().iterator().next());
      }
    });
  }

  public void test_AnalysisServer_resolveLibraryWhenBusy2() throws Exception {
    if (!DartCoreDebug.ANALYSIS_SERVER) {
      return;
    }
    setupDefaultServer();

    String projName = getClass().getSimpleName() + "_createResolveDispose";
    String libFileName = "mylib.dart";

    TestProject proj = new TestProject(projName);
    File libFile = proj.setFileContent(libFileName, "class A { }").getLocation().toFile();
    DartUnit unit = waitForResolution(libFile).getSelfDartUnit();
    assertEquals("A", unit.getTopDeclarationNames().iterator().next());

    // Simulate a very busy system
    Resolution callback = new Resolution();
    synchronized (getServerQueue()) {
      proj.dispose();
      proj = new TestProject(projName);
      libFile = proj.setFileContentWithoutWaitingForAnalysis(libFileName, "class B { }").getLocation().toFile();
      server.resolveLibrary(libFile, callback);
    }
    unit = callback.waitForResolution().getSelfDartUnit();
    assertEquals("B", unit.getTopDeclarationNames().iterator().next());
    proj.dispose();
  }

  public void test_AnalysisServer_scan() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_scan(tempDir);
      }
    });
  }

  public void test_AnalysisServer_scan(File tempDir) throws Exception {
    File libFile = setupMoneyLibrary(tempDir);
    File sourcedFile = new File(libFile.getParent(), "currency.dart");
    setupServer();
    assertTrackedLibraryFiles();

    listener.reset();
    server.scan(sourcedFile);
    waitForIdle();
    assertTrackedLibraryFiles(sourcedFile);
    assertEquals(3, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();

    listener.reset();
    synchronized (getServerQueue()) {
      server.scan(libFile);
      server.discard(libFile);
    }
    waitForIdle();
    assertTrackedLibraryFiles(libFile);
    assertEquals(1, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertWasDiscarded(sourcedFile);
  }

  public void test_AnalysisServer_scanAndIndex() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_scanAndIndex(tempDir);
      }
    });
  }

  public void test_AnalysisServer_scanAndIndex(File tempDir) throws Exception {
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
    server.scan(tempDir);
    waitForIdle();
    assertTrackedLibraryFiles(libFile);
    assertEquals(3, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();

    listener.reset();
    server.scan(sourcedFile);
    waitForIdle();
    assertTrackedLibraryFiles(libFile);
    assertEquals(0, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();

//    index.shutdown();
  }

  public void test_AnalysisServer_stop() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();

        final boolean[] latch = new boolean[1];
        server.resolveLibrary(libFile, new ResolveLibraryCallback() {
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

  public void test_AnalysisServer_write_read_0() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();
        assertEquals(0, listener.getResolved().size());
        listener.assertNoDuplicates();
        listener.assertNoDiscards();

        assertTrackedLibraryFiles();
        assertFalse(isLibraryFileCached(libFile));

        File cacheFile = new File(libFile.getParentFile(), "analysis.cache");

        server.stop();
        server.writeCache(cacheFile);
        setupServer(cacheFile);

        assertTrackedLibraryFiles();
        assertFalse(isLibraryFileCached(libFile));
      }
    });
  }

  public void test_AnalysisServer_write_read_1() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = test_AnalysisServer_analyzeLibrary(tempDir);

        assertTrackedLibraryFiles(libFile);
        assertTrue(isLibraryFileCached(libFile));

        File cacheFile = new File(libFile.getParentFile(), "analysis.cache");

        server.stop();
        server.writeCache(cacheFile);
        setupServer(cacheFile);

        assertTrackedLibraryFiles(libFile);
        assertTrue(isLibraryFileCached(libFile));
      }
    });
  }

  public void test_AnalysisServer_write_read_bad() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        File libFile = setupMoneyLibrary(tempDir);
        setupServer();

        // Cannot write cache while server is still running

        File cacheFile = new File(libFile.getParentFile(), "analysis.cache");
        try {
          server.writeCache(cacheFile);
          fail("should not be able to write cache while server is still running");
        } catch (IllegalStateException e) {
          //$FALL-THROUGH$
        }

        // Cannot read cache while server is still running

        try {
          server.readCache(cacheFile);
          fail("should not be able to read cache while server is still running");
        } catch (IllegalStateException e) {
          //$FALL-THROUGH$
        }

        server.stop();

        // Cannot read null or non-existant cache file... returns false

        assertFalse(server.readCache(null));
        assertFalse(cacheFile.exists());
        assertFalse(server.readCache(cacheFile));

        // Cannot read empty or incomptable version cache file... returns false

        FileUtilities.setContents(cacheFile, "");
        assertFalse(server.readCache(cacheFile));
        FileUtilities.setContents(cacheFile, "nothing");
        assertFalse(server.readCache(cacheFile));
      }
    });
  }

  @Override
  protected void tearDown() throws Exception {
    if (server != null && server != defaultServer) {
      server.stop();
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

  private boolean isLibraryFileCached(File libFile) throws Exception {
    Method method = server.getClass().getDeclaredMethod("isLibraryFileCached", File.class);
    method.setAccessible(true);
    Object result = method.invoke(server, libFile);
    return result instanceof Boolean && ((Boolean) result).booleanValue();
  }

  private ParseLibraryFileEvent parseLibraryFile(File libFile) throws InterruptedException {
    final ParseLibraryFileEvent[] result = new ParseLibraryFileEvent[1];
    server.parseLibraryFile(libFile, new ParseLibraryFileCallback() {
      @Override
      public void parsed(ParseLibraryFileEvent event) {
        synchronized (result) {
          result[0] = event;
          result.notifyAll();
        }
      }
    });
    synchronized (result) {
      if (result[0] == null) {
        result.wait(FIVE_MINUTES_MS);
      }
    }
    return result[0];
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

  private void setupServer(File cacheFile) throws Exception {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    if (cacheFile != null) {
      if (!server.readCache(cacheFile)) {
        fail("Failed to read cached from " + cacheFile);
      }
    }
    listener = new Listener(server);
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

  private LibraryUnit waitForResolution(File libFile) {
    Resolution callback = new Resolution();
    server.resolveLibrary(libFile, callback);
    return callback.waitForResolution();
  }
}
