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

import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.AttributeCallback;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AnalysisServerTest extends TestCase {

  private static final String TEST_CLASS_SIMPLE_NAME = AnalysisServerTest.class.getSimpleName();

  private static final int FIVE_MINUTES_MS = 300000;

  private AnalysisServer server;

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
    File targetDir = new File(tempDir, TEST_CLASS_SIMPLE_NAME);
    TestUtilities.copyPluginRelativeContent("Money", targetDir);
    File libFile = new File(targetDir, "money.dart");
    if (!libFile.exists()) {
      fail("Dart file does not exist: " + libFile);
    }
    setupServer();
    server.analyzeLibrary(libFile);
    waitForIdle();
    if (!listener.getResolved().contains(libFile.getPath())) {
      fail("Expected resolved library " + libFile + " but found " + listener.getResolved());
    }
    assertEquals(3, listener.getResolved().size());
    listener.assertNoDuplicates();
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
    return libFile;
  }

  public void test_AnalysisServer_discard() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File tempDir) throws Exception {
        test_AnalysisServer_discard(tempDir);
      }
    });
  }

  public File test_AnalysisServer_discard(File tempDir) throws Exception {
    File libFile = test_AnalysisServer_analyzeLibrary(tempDir);
    listener.reset();
    server.discard(libFile);
    waitForIdle();
    assertEquals(0, listener.getResolved().size());
    listener.assertNoDuplicates();
    return libFile;
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
    File targetDir = new File(tempDir, TEST_CLASS_SIMPLE_NAME);
    TestUtilities.copyPluginRelativeContent("Money", targetDir);
    File libFile = new File(targetDir, "money.dart");
    if (!libFile.exists()) {
      fail("Dart file does not exist: " + libFile);
    }
    setupServer();
    final LibraryUnit[] resolved = new LibraryUnit[1];
    server.resolveLibrary(libFile, new ResolveLibraryCallback() {
      @Override
      public void resolved(LibraryUnit libraryUnit) {
        resolved[0] = libraryUnit;
      }
    });
    waitForIdle();
    assertNotNull(resolved[0]);
    if (!listener.getResolved().contains(libFile.getPath())) {
      fail("Expected resolved library " + libFile + " but found " + listener.getResolved());
    }
    assertEquals(3, listener.getResolved().size());
    listener.assertNoDuplicates();
  }

  public void test_AnalysisServer_server() throws Exception {
    setupServer();
    long delta = resolveBundledLibraries();
    listener.assertBundledLibrariesResolved();
    System.out.println("  " + delta + " ms to resolve all sdk libraries");
    delta = resolveBundledLibraries();
    if (delta > 10) {
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
    if (delta > 10) {
      fail("Expected libraries to be cached, but took " + delta + " ms");
    }
    int resolveCount = listener.getResolved().size();
    if (resolveCount > 0) {
      fail("Expected libraries to be cached, but resolved " + resolveCount + " libraries");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    if (server != null) {
      server.stop();
    }
    if (index != null) {
      index.getOperationProcessor().stop(false);
    }
  }

  private long resolveBundledLibraries() throws URISyntaxException, InterruptedException, Exception {
    listener.reset();
    resolveBundledLibraries(server);
    long delta = waitForIdle();
    listener.assertNoDuplicates();
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

  private void setupServer() throws Exception {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    listener = new Listener(server);
    long start = System.currentTimeMillis();
    listener.waitForIdle(FIVE_MINUTES_MS);
    long delta = System.currentTimeMillis() - start;
    if (delta > 50) {
      System.out.println("  " + delta + " ms waiting for analysis server to initialize");
    }
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
    if (!listener.waitForIdle(FIVE_MINUTES_MS)) {
      fail(server.getClass().getSimpleName() + " not idle");
    }
    // In the current implementation, the index will process all background indexing
    // before answering any search request
    if (index != null) {
      final CountDownLatch latch = new CountDownLatch(1);
      Resource resource = new Resource("resource");
      Element element = new Element(resource, "element");
      Attribute attribute = Attribute.getAttribute("attribute");
      index.getAttribute(element, attribute, new AttributeCallback() {

        @Override
        public void hasValue(Element element, Attribute attribute, String value) {
          latch.countDown();
        }
      });
      if (!latch.await(FIVE_MINUTES_MS, TimeUnit.MILLISECONDS)) {
        fail(index.getClass().getSimpleName() + " not idle");
      }
    }
    return System.currentTimeMillis() - start;
  }
}
