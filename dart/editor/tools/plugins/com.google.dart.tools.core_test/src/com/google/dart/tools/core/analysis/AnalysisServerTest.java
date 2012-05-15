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
import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.AttributeCallback;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

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

  private InMemoryIndex index;

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
    server.analyzeLibrary(libFile);
    waitForIdle();
    if (!listener.getResolved().contains(libFile.getPath())) {
      fail("Expected resolved library " + libFile + " but found " + listener.getResolved());
    }
    assertEquals(3, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    assertTrue(isLibraryFileTracked(libFile));
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

    listener.reset();
    server.discard(discardParent ? libFile.getParentFile() : libFile);
    waitForIdle();
    assertEquals(0, listener.getResolved().size());
    listener.assertNoDuplicates();
    listener.assertWasDiscarded(libFile);
    assertFalse(isLibraryFileTracked(libFile));
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
    assertFalse(isLibraryFileTracked(libFile));
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
    server.addAnalysisListener(new AnalysisListener() {

      @Override
      public void discarded(AnalysisEvent event) {
      }

      @Override
      public void idle(boolean idle) {
        if (idle) {
          count[0]++;
          if (count[0] < 3) {
            server.changed(libFile);
          }
        }
      }

      @Override
      public void parsed(AnalysisEvent event) {
      }

      @Override
      public void resolved(AnalysisEvent event) {
      }
    });
    server.analyzeLibrary(libFile);
    long end = System.currentTimeMillis() + 30000;
    while (count[0] < 3) {
      AnalysisTestUtilities.waitForIdle(server, end - System.currentTimeMillis());
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
      libFile = proj.setFileContent(libFileName, "class B { }").getLocation().toFile();
      server.resolveLibrary(libFile, callback);
    }
    unit = callback.waitForResolution().getSelfDartUnit();
    assertEquals("B", unit.getTopDeclarationNames().iterator().next());
    proj.dispose();
  }

  public void test_AnalysisServer_server() throws Exception {
    setupServer();
    long delta = resolveBundledLibraries();
    listener.assertBundledLibrariesResolved();
    System.out.println("  " + delta + " ms to resolve all sdk libraries");
    delta = resolveBundledLibraries();
    // Increasing this test from 10 to 100 ms - we can get valid values larger then 10.
    if (delta > 100) {
      fail("Expected libraries to be cached, but took " + delta + " ms");
    }
    int resolveCount = listener.getResolved().size();
    if (resolveCount > 0) {
      fail("Expected libraries to be cached, but resolved " + resolveCount + " libraries");
    }
  }

  public void test_AnalysisServer_serverAndIndex() throws Exception {
    setupServerAndIndex();
    long delta = resolveBundledLibraries();
    listener.assertBundledLibrariesResolved();
    System.out.println("  " + delta + " ms to resolve and index all sdk libraries");
    delta = resolveBundledLibraries();
    if (delta > 100) {
      fail("Expected libraries to be cached, but took " + delta + " ms");
    }
    int resolveCount = listener.getResolved().size();
    if (resolveCount > 0) {
      fail("Expected libraries to be cached, but resolved " + resolveCount + " libraries");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    if (server != null && server != defaultServer) {
      server.stop();
    }
    if (index != null) {
      index.getOperationProcessor().stop(false);
    }
  }

  private Object getServerQueue() throws Exception {
    Field field = server.getClass().getDeclaredField("queue");
    field.setAccessible(true);
    return field.get(server);
  }

  private boolean isLibraryFileCached(File libFile) throws Exception {
    Method method = server.getClass().getDeclaredMethod("isLibraryFileCached", File.class);
    method.setAccessible(true);
    Object result = method.invoke(server, libFile);
    return result instanceof Boolean && ((Boolean) result).booleanValue();
  }

  private boolean isLibraryFileTracked(File libFile) throws Exception {
    Method method = server.getClass().getDeclaredMethod("isLibraryFileTracked", File.class);
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

  private long resolveBundledLibraries() throws URISyntaxException, InterruptedException, Exception {
    listener.reset();
    resolveBundledLibraries(server);
    long delta = waitForIdle();
    listener.assertNoDuplicates();
    listener.assertNoDiscards();
    return delta;
  }

  private void resolveBundledLibraries(AnalysisServer server) throws URISyntaxException {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    ArrayList<String> librarySpecs = new ArrayList<String>(libraryManager.getAllLibrarySpecs());
    for (String urlSpec : librarySpecs) {
      URI libraryUri = new URI(urlSpec);
      File libraryFile = new File(libraryManager.resolveDartUri(libraryUri));
      server.resolveLibrary(libraryFile, null);
    }
  }

  private void setupDefaultServer() throws Exception {
    if (!DartCoreDebug.ANALYSIS_SERVER) {
      fail("Do not call this method when analysis server is not enabled");
    }
    defaultServer = SystemLibraryManagerProvider.getDefaultAnalysisServer();
    server = defaultServer;
    listener = new Listener(server);
    AnalysisTestUtilities.waitForIdle(server, FIVE_MINUTES_MS);
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
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    listener = new Listener(server);
    server.start();
    AnalysisTestUtilities.waitForIdle(server, FIVE_MINUTES_MS);
  }

  private void setupServerAndIndex() throws Exception {
    setupServer();
    index = InMemoryIndex.newInstanceForTesting();
    new Thread(new Runnable() {
      @Override
      public void run() {
        index.getOperationProcessor().run();
      }
    }, "AnalysisServerTest :: Index Operation Processor").start(); //$NON-NLS-0$
    server.addAnalysisListener(new AnalysisIndexManager(index));
  }

  /**
   * Wait for the server and index under test to finish background processing
   * 
   * @return the # of milliseconds waited
   */
  private long waitForIdle() throws InterruptedException {
    final long start = System.currentTimeMillis();
    AnalysisTestUtilities.waitForIdle(server, FIVE_MINUTES_MS);
    // In the current implementation, the index will process all background indexing
    // before answering any search request
    if (index != null) {
      final boolean[] complete = new boolean[1];
      Resource resource = new Resource("resource");
      Element element = new Element(resource, "element");
      Attribute attribute = Attribute.getAttribute("attribute");
      index.getAttribute(element, attribute, new AttributeCallback() {
        @Override
        public void hasValue(Element element, Attribute attribute, String value) {
          synchronized (complete) {
            complete[0] = true;
            complete.notifyAll();
          }
        }
      });
      synchronized (complete) {
        if (!complete[0]) {
          complete.wait(FIVE_MINUTES_MS);
          if (!complete[0]) {
            fail(index.getClass().getSimpleName() + " not idle");
          }
        }
      }
    }
    return System.currentTimeMillis() - start;
  }

  private LibraryUnit waitForResolution(File libFile) {
    Resolution callback = new Resolution();
    server.resolveLibrary(libFile, callback);
    return callback.waitForResolution();
  }
}
