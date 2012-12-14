package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;

import static com.google.dart.tools.core.DartCore.BUILD_DART_FILE_NAME;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Utility methods for creating various {@link MockProject}s.
 */
public class TestProjects {

  public static final NullProgressMonitor MONITOR = new NullProgressMonitor();

  /**
   * Answer a new empty mock project
   * 
   * @return a project (not {@code null})
   */
  public static MockProject newEmptyProject() {
    return new MockProject("empty");
  }

  /**
   * Answer a new mock project with a pubspec file in the root and a packages directory containing
   * one package and a hidden ".svn" directory, with *.dart files sprinkled throughout
   * 
   * @return the mock project (not {@code null})
   */
  public static MockProject newPubProject1() {
    MockProject project = new MockProject("pub1");
    project.addFile(PUBSPEC_FILE_NAME);
    project.addFile("some.dart");

    MockFolder web = project.addFolder("web");
    web.addFile(BUILD_DART_FILE_NAME);
    web.addFile("other.dart");

    MockFolder packages = project.addFolder(PACKAGES_DIRECTORY_NAME);

    MockFolder pkgFoo = packages.addFolder("foo");
    pkgFoo.addFile(PUBSPEC_FILE_NAME);
    pkgFoo.addFile("bar.dart");

    MockFolder svn = project.addFolder(".svn");
    svn.addFile(PUBSPEC_FILE_NAME);
    svn.addFile("blat.dart");

    return project;
  }

  /**
   * Answer a new simple non-pub project
   * 
   * @return a project (not {@code null})
   */
  public static MockProject newPubProject2() {
    MockProject project = new MockProject("pub2");
    project.addFile(PUBSPEC_FILE_NAME);
    project.addFile(BUILD_DART_FILE_NAME);
    project.addFile("some.dart");
    project.addFile("some1.dart");

    MockFolder myApp = project.addFolder("myapp");
    myApp.addFile(PUBSPEC_FILE_NAME);
    myApp.addFile(BUILD_DART_FILE_NAME);
    myApp.addFile("other.dart");

    MockFolder svn = project.addFolder(".svn");
    svn.addFile(PUBSPEC_FILE_NAME);
    svn.addFile(BUILD_DART_FILE_NAME);
    svn.addFile("foo.dart");

    MockFolder packages = project.addFolder(PACKAGES_DIRECTORY_NAME);

    MockFolder pkg1 = packages.addFolder("pkg1");
    pkg1.addFile(PUBSPEC_FILE_NAME);
    pkg1.addFile(BUILD_DART_FILE_NAME);
    pkg1.addFile("bar.dart");

    MockFolder pkg1Folder = pkg1.addFolder("some_folder");
    pkg1Folder.addFile(BUILD_DART_FILE_NAME);
    pkg1Folder.addFile("bar.dart");

    return project;
  }

  /**
   * Answer a new simple non-pub project
   * 
   * @return a project (not {@code null})
   */
  public static MockProject newSimpleProject() {
    MockProject project = new MockProject("simple");
    project.addFile(BUILD_DART_FILE_NAME);
    project.addFile("some.dart");
    project.addFile("some1.dart");

    MockFolder folder = project.addFolder("web");
    folder.addFile(BUILD_DART_FILE_NAME);
    folder.addFile("other.dart");

    MockFolder packages = folder.addFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder somePackage = packages.addFolder("pkg1");
    somePackage.addFile("bar.dart");

    return project;
  }

  // no instances
  private TestProjects() {
  }
}
