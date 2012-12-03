package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.analysis.ScanCallback;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.pub.PubBuildParticipantTest;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.CHANGED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.io.File;
import java.util.ArrayList;

public class AnalysisServerParticipantTest extends TestCase {

  /**
   * Specialized {@link AnalysisServer} that asserts specific methods are called
   */
  private final class MockServer extends AnalysisServer {

    private final ArrayList<Object[]> expected = new ArrayList<Object[]>();
    private int index = 0;

    MockServer() {
      super(PackageLibraryManagerProvider.getAnyLibraryManager());
    }

    @Override
    public void analyze(File file) {
      validateCall("analyze", file);
    }

    @Override
    public void changed(File file) {
      validateCall("changed", file);
    }

    @Override
    public void discard(File file) {
      validateCall("discard", file);
    }

    @Override
    public void scan(File file, ScanCallback callback) {
      validateCall("scan", file);
    }

    @Override
    public void start() {
      throw new RuntimeException();
    }

    void assertComplete() {
      int delta = expected.size() - index;
      if (delta == 0) {
        return;
      }
      PrintStringWriter msg = new PrintStringWriter();
      msg.print("Expected ");
      msg.print(delta);
      msg.print(" additional call(s):");
      for (int i = index; i < expected.size(); i++) {
        msg.println();
        msg.print("  ");
        Object[] details = expected.get(i);
        for (Object each : details) {
          msg.print(each);
          msg.print(", ");
        }
      }
      fail(msg.toString());
    }

    void expect(String methodName, File file) {
      expected.add(new Object[] {methodName, file});
    }

    private void validateCall(String mthName, File file) {
      if (index >= expected.size()) {
        fail("Unexpected call to " + mthName);
      }
      Object[] details = expected.get(index++);
      if (!details[0].equals(mthName)) {
        fail("Expected call to " + details[0] + " but was " + mthName);
      }
      if (!details[1].equals(file)) {
        fail("Expected different file argument\n  expected: " + details[1] + "\n  actual: " + file);
      }
    }
  }

  private static final MockProject PROJECT = new MockProject(
      PubBuildParticipantTest.class.getSimpleName());
  private static final MockFile BUILDER0 = PROJECT.addFile(DartCore.BUILD_DART_FILE_NAME);
  private static final MockFile DART0 = PROJECT.addFile("some.dart");
  private static final MockFile DART01 = PROJECT.addFile("some1.dart");
  private static final MockFolder MYAPP = PROJECT.addFolder("myapp");
  private static final MockFile BUILDER1 = MYAPP.addFile(DartCore.BUILD_DART_FILE_NAME);
  private static final MockFile DART1 = MYAPP.addFile("other.dart");
  private static final MockFolder SVN = PROJECT.addFolder(".svn");
  private static final MockFile DART2 = SVN.addFile("foo.dart");
  private static final MockFolder PACKAGES = PROJECT.addFolder(DartCore.PACKAGES_DIRECTORY_NAME);
  private static final MockFolder SOME_PACKAGE = PACKAGES.addFolder("pkg1");
  private static final MockFile DART3 = SOME_PACKAGE.addFile("bar.dart");
  private static final MockFolder SOME_FOLDER = PROJECT.addFolder("some_folder");
  private static final MockFile DART4 = SOME_FOLDER.addFile("bar.dart");
  static {
    MYAPP.addFile(DartCore.PUBSPEC_FILE_NAME);
    SOME_FOLDER.addFile(DartCore.BUILD_DART_FILE_NAME);
    SVN.addFile(DartCore.PUBSPEC_FILE_NAME);
    SVN.addFile(DartCore.BUILD_DART_FILE_NAME);
    SOME_PACKAGE.addFile(DartCore.PUBSPEC_FILE_NAME);
    SOME_PACKAGE.addFile(DartCore.BUILD_DART_FILE_NAME);
  }

  private static final NullProgressMonitor MONITOR = new NullProgressMonitor();
  private MockServer server;

  public void test_build_added_folder() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SOME_FOLDER, ADDED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("scan", SOME_FOLDER.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_added_package() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, CHANGED).add(SOME_PACKAGE, ADDED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("discard", PROJECT.getLocation().toFile());
    server.expect("scan", PROJECT.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_added_packages() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, ADDED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("discard", PROJECT.getLocation().toFile());
    server.expect("scan", PROJECT.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_added_project() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, ADDED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("scan", PROJECT.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_changed_dart() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SOME_FOLDER, CHANGED).add(DART4, CHANGED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("changed", DART4.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_changed_dart_in_package() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, CHANGED).add(SOME_PACKAGE, CHANGED).add(DART3, CHANGED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("changed", DART3.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_full() throws Exception {
    MockDelta delta = null;
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("scan", PROJECT.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_removed_dart() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SOME_FOLDER, CHANGED).add(DART4, REMOVED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("discard", DART4.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void test_build_removed_directory() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(SOME_FOLDER, REMOVED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("discard", SOME_FOLDER.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  // If anything in the "packages" directory is removed, then rescan the application
  public void test_build_removed_package() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, CHANGED).add(SOME_PACKAGE, REMOVED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("discard", SOME_PACKAGE.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  // If the "packages" directory is removed, then rescan the application
  public void test_build_removed_packages() throws Exception {
    MockDelta delta = new MockDelta(PROJECT, CHANGED);
    delta.add(PACKAGES, REMOVED);
    BuildEvent event = new BuildEvent(PROJECT, delta, MONITOR);
    server.expect("discard", PACKAGES.getLocation().toFile());
    new AnalysisServerParticipant(server, true).build(event, MONITOR);
  }

  public void testClean() throws Exception {
    CleanEvent event = new CleanEvent(PROJECT, MONITOR);
    server.expect("discard", PROJECT.getLocation().toFile());
    new AnalysisServerParticipant(server, true).clean(event, MONITOR);
  }

  @Override
  protected void setUp() {
    server = new MockServer();
  }

  @Override
  protected void tearDown() {
    server.assertComplete();
  }
}
