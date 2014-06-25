package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import static com.google.dart.tools.core.DartCore.BUILD_DART_FILE_NAME;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
    return newEmptyProject(new MockWorkspaceRoot());
  }

  /**
   * Answer a new empty mock project
   * 
   * @return a project (not {@code null})
   */
  public static MockProject newEmptyProject(MockWorkspaceRoot rootContainer) {
    return rootContainer.addProject("testproj");
  }

  /**
   * Answer a new mock project with a pubspec file in the root and a packages directory containing
   * one package and a hidden ".svn" directory, with *.dart files sprinkled throughout
   * 
   * @return the mock project (not {@code null})
   */
  public static MockProject newPubProject1() {
    MockProject project = newEmptyProject();
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
   * Answer a new mock project with a pubspec file in the root, a nested folder containing another
   * pubspec file, and a packages directory containing one package and a hidden ".svn" directory,
   * with *.dart files sprinkled throughout.
   * 
   * @return the mock project (not {@code null})
   */
  public static MockProject newPubProject2() {
    return newPubProject2(new MockWorkspaceRoot());
  }

  /**
   * Answer a new mock project with a pubspec file in the root, a nested folder containing another
   * pubspec file, and a packages directory containing one package and a hidden ".svn" directory,
   * with *.dart files sprinkled throughout.
   * 
   * @return the mock project (not {@code null})
   */
  public static MockProject newPubProject2(MockWorkspaceRoot rootContainer) {
    MockProject project = newEmptyProject(rootContainer);
    project.addFile(PUBSPEC_FILE_NAME, "name:  myapp");
    project.addFile(BUILD_DART_FILE_NAME);
    project.addFile("some.dart");
    project.addFile("some1.dart");
    project.addFile("some.html");

    MockFolder myApp = project.addFolder("myapp");
    myApp.addFile(PUBSPEC_FILE_NAME);
    myApp.addFile(BUILD_DART_FILE_NAME);
    myApp.addFile("other.dart");

    MockFolder svn = project.addFolder(".svn");
    svn.addFile(PUBSPEC_FILE_NAME);
    svn.addFile(BUILD_DART_FILE_NAME);
    svn.addFile("foo.dart");

    addPackages(project);

    return project;
  }

  /**
   * Answer a new mock project with a pubspec file in the root, a nested folder containing another
   * pubspec file, and a packages directory containing one package and a hidden ".svn" directory,
   * with *.dart files sprinkled throughout.
   * 
   * @return a project (not {@code null})
   */
  public static MockProject newPubProject3() {
    return newPubProject3(new MockWorkspaceRoot());
  }

  /**
   * Answer a new mock project with a pubspec file in the root, a nested folder containing another
   * pubspec file, and a packages directory containing one package and a hidden ".svn" directory,
   * with *.dart files sprinkled throughout.
   * 
   * @return a project (not {@code null})
   */
  public static MockProject newPubProject3(MockWorkspaceRoot rootContainer) {
    MockProject project = newPubProject2(rootContainer);
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProjectDescription description = workspace.newProjectDescription(project.getName());
    description.setNatureIds(new String[] {DartCore.DART_PROJECT_NATURE});
    try {
      project.setDescription(description, null);
    } catch (CoreException e) {

    }

    MockFolder app = project.getMockFolder("myapp");

    MockFolder appLib = app.addFolder("lib");
    appLib.addFile("stuff.dart");

    app.addFolder("mylib");

    MockFolder subApp = app.addFolder("subApp");
    subApp.addFile(PUBSPEC_FILE_NAME);
    subApp.addFile("sub_stuff.dart");

    MockFolder web = project.addFolder("web");
    web.addFile("other.dart");

    addPackages(web);

    MockFolder sub = web.addFolder("sub");
    sub.addFile("cool.dart");

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

  /**
   * Answer a new simple non-pub project, but with "packages" folder.
   * 
   * @return a project (not {@code null})
   */
  public static MockProject newSimpleProjectWithPackages() {
    MockProject project = newSimpleProject();
    addPackages(project);
    return project;
  }

  private static void addPackages(MockContainer container) {
    MockFolder packages = container.addFolder(PACKAGES_DIRECTORY_NAME);

    MockFolder pkg1 = packages.addFolder("pkg1");
    pkg1.addFile(PUBSPEC_FILE_NAME);
    pkg1.addFile(BUILD_DART_FILE_NAME);
    pkg1.addFile("bar.dart");

    MockFolder pkg1Folder = pkg1.addFolder("some_folder");
    pkg1Folder.addFile(BUILD_DART_FILE_NAME);
    pkg1Folder.addFile("bar.dart");

  }

  // no instances
  private TestProjects() {
  }
}
