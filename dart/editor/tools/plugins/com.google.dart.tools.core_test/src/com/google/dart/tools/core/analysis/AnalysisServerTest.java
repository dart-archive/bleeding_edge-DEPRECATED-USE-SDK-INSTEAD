package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.DartCoreDebug;
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
import java.util.HashSet;

public class AnalysisServerTest extends TestCase {

  private static final class Listener implements AnalysisListener {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final HashSet<String> resolved = new HashSet<String>();
    private final StringBuilder duplicates = new StringBuilder();

    @Override
    public void parsed(AnalysisEvent event) {
    }

    @Override
    public void resolved(AnalysisEvent event) {
      String libPath = event.getLibraryFile().getPath();
      if (!resolved.add(libPath)) {
        if (duplicates.length() == 0) {
          duplicates.append("Duplicate library resolutions:");
        }
        duplicates.append(LINE_SEPARATOR);
        duplicates.append(libPath);
      }
    }

    void assertBundledLibrariesResolved() throws Exception {
      ArrayList<String> notResolved = new ArrayList<String>();
      EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
      ArrayList<String> librarySpecs = new ArrayList<String>(libraryManager.getAllLibrarySpecs());
      for (String urlSpec : librarySpecs) {
        URI libraryUri = new URI(urlSpec);
        File libraryFile = new File(libraryManager.resolveDartUri(libraryUri));
        String libraryPath = libraryFile.getPath();
        if (!resolved.contains(libraryPath)) {
          notResolved.add(libraryPath);
        }
      }
      if (notResolved.size() > 0) {
        fail("Expected these libraries to be resolved: " + notResolved);
      }
    }

    void assertNoDuplicates() {
      if (duplicates.length() > 0) {
        fail(duplicates.toString());
      }
    }

    HashSet<String> getResolved() {
      return resolved;
    }

    void reset() {
      resolved.clear();
      duplicates.setLength(0);
    }
  }

  private static final String TEST_CLASS_SIMPLE_NAME = AnalysisServerTest.class.getSimpleName();

  private static final int FIVE_MINUTES_MS = 300000;

  /**
   * Indicated whether the test has waited for both the default analysis server and the indexer to
   * finish processing background tasks.
   */
  private static boolean waitedForIdle = false;

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
    waitForIdle(server, index);
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
    waitForIdle(server, index);
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
    waitForIdle(server, index);
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
    server.resolveLibrary(libFile, new ResolveLibraryListener() {
      @Override
      public void resolved(LibraryUnit libraryUnit) {
        resolved[0] = libraryUnit;
      }
    });
    waitForIdle(server, index);
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

  /**
   * Wait for both the system analysis server and the indexer to be idle so that they do not
   * interfere with timings
   */
  @Override
  protected void setUp() throws Exception {
    if (!waitedForIdle) {
      waitedForIdle = true;
      System.out.println(TEST_CLASS_SIMPLE_NAME);
      if (DartCoreDebug.ANALYSIS_SERVER) {
        System.out.println("  Waiting for analysis server and indexer to be idle...");
        AnalysisServer defaultServer = SystemLibraryManagerProvider.getDefaultAnalysisServer();
        InMemoryIndex defaultIndex = InMemoryIndex.getInstance();
        defaultIndex.initializeIndex();
        long delta = waitForIdle(defaultServer, defaultIndex);
        System.out.println("  Waited " + delta + " ms for idle");
      } else {
        System.out.println("  Default analysis server not enabled");
      }
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
    long delta = waitForIdle(server, index);
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

  private void setupServer() {
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    server = new AnalysisServer(libraryManager);
    listener = new Listener();
    server.addAnalysisListener(listener);
    assertTrue(server.waitForIdle(10));
  }

  private void setupServerAndIndex() {
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
   * Wait for the specified server and index to finish background processing
   * 
   * @param server the server (not <code>null</code>)
   * @param index the index or <code>null</code>
   * @return the # of milliseconds waited
   */
  private long waitForIdle(AnalysisServer server, InMemoryIndex index) throws InterruptedException {
    final long start = System.currentTimeMillis();
    if (!server.waitForIdle(FIVE_MINUTES_MS)) {
      fail(server.getClass().getSimpleName() + " not idle");
    }
    if (index != null && !index.waitForIdle(FIVE_MINUTES_MS)) {
      fail(index.getClass().getSimpleName() + " not idle");
    }
    return System.currentTimeMillis() - start;
  }
}
