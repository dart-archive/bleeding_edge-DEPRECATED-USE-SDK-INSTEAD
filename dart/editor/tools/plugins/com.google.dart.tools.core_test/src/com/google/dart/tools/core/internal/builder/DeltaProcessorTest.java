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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockResource;

import static com.google.dart.tools.core.DartCore.BUILD_DART_FILE_NAME;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IResource;

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

public class DeltaProcessorTest extends AbstractDartCoreTest {

  private MockProject projectContainer;
  private MockFolder appContainer;
  private MockFolder subAppContainer;
  private DeltaProcessorMockProject project;

  public void test_traverse_defaultContext_file_changed() throws Exception {
    projectContainer.remove(PUBSPEC_FILE_NAME);
    ((MockContext) project.getDefaultContext()).assertExtracted(appContainer);

    MockDelta delta = new MockDelta(projectContainer);
    MockFile file = projectContainer.getMockFile("some.dart");
    delta.add(file);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    ((MockContext) project.getPubFolder(appContainer).getContext()).assertNoCalls();
    project.assertChanged(null, null, new IResource[] {file}, null);
    project.assertNoCalls();
  }

  public void test_traverse_folder2Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(appContainer, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    MockFolder libContainer = appContainer.getMockFolder("lib");
    IResource[] resources1 = {
        appContainer.getFile(BUILD_DART_FILE_NAME), appContainer.getFile("other.dart"),
        libContainer.getFile("stuff.dart"), subAppContainer.getFile("sub_stuff.dart")};

    project.assertChanged(projectContainer, resources1, null, null);
    project.assertPubspecAdded(appContainer, subAppContainer);
    project.assertPubspecRemoved();
    project.assertDiscardContextsIn();
    project.assertNoCalls();
  }

  public void test_traverse_folder2Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(appContainer, REMOVED);
    projectContainer.remove(appContainer.getName());

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, null, new IResource[] {appContainer});
    project.assertPubspecAdded();
    project.assertPubspecRemoved();
    project.assertDiscardContextsIn(appContainer);
    project.assertNoCalls();
  }

  public void test_traverse_html_file_added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    MockFile file = projectContainer.getMockFile("some.html");
    delta.add(file, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, new IResource[] {file}, null, null);
    project.assertNoCalls();
  }

  public void test_traverse_html_file_changed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    MockFile file = projectContainer.getMockFile("some.html");
    delta.add(file);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, new IResource[] {file}, null);
    project.assertNoCalls();
  }

  public void test_traverse_html_file_removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    MockResource file = projectContainer.remove("some.html");
    delta.add(file, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, null, new IResource[] {file});
    project.assertNoCalls();
  }

  public void test_traverse_ignored_file_change() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(".svn").add("foo.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertNoCalls();
  }

  public void test_traverse_ignored_folder() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(".svn", REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertNoCalls();
  }

  public void test_traverse_package_added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFolder pkgFolder = pkg1.getMockFolder("some_folder");
    IResource[] added = new IResource[] {
        pkg1.getMockFile("bar.dart"), pkg1.getMockFile("build.dart"),
        pkgFolder.getMockFile("bar.dart"), pkgFolder.getMockFile("build.dart")};

    project.assertChanged(projectContainer, added, null, null);
    project.assertNoCalls();
  }

  public void test_traverse_package_file_added() throws Exception {
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFile file = pkg1.getMockFile("bar.dart");

    // Also test that delta processor handles deltas starting at the "packages" directory
    // similar to what WorkspaceDeltaProcessor will pass to the DeltaProcessor
    MockDelta delta = new MockDelta(packages);
    delta.add("pkg1").add("bar.dart", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, new IResource[] {file}, null, null);
    project.assertNoCalls();
  }

  public void test_traverse_package_file_added2() throws Exception {
    projectContainer.remove(PUBSPEC_FILE_NAME);

    // Create delta with a added file in pkg1
    MockFolder myApp = projectContainer.getMockFolder("myapp");
    MockFolder packages = myApp.addFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.addFolder("pkg1");
    MockFile file = pkg1.addFile("bar.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(myApp).add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(myApp, new IResource[] {file}, null, null);
  }

  public void test_traverse_package_file_changed() throws Exception {
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFile file = pkg1.getMockFile("bar.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, new IResource[] {file}, null);
    project.assertNoCalls();
  }

  public void test_traverse_package_file_removed() throws Exception {
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg = packages.getMockFolder("pkg1");
    MockFile file = (MockFile) pkg.remove("bar.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add(file, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, null, new IResource[] {file});
    project.assertNoCalls();
  }

  public void test_traverse_package_removed() throws Exception {
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = (MockFolder) packages.remove("pkg1");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add(pkg1, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertPackagesRemoved(projectContainer);
    project.assertNoCalls();
  }

  public void test_traverse_packages_added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFolder pkgFolder = pkg1.getMockFolder("some_folder");
    IResource[] added = new IResource[] {
        pkg1.getMockFile("bar.dart"), pkg1.getMockFile("build.dart"),
        pkgFolder.getMockFile("bar.dart"), pkgFolder.getMockFile("build.dart")};

    project.assertChanged(projectContainer, added, null, null);
    project.assertNoCalls();
  }

  public void test_traverse_packages_removed() throws Exception {
    MockFolder packages = (MockFolder) projectContainer.remove(PACKAGES_DIRECTORY_NAME);
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(packages, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertPackagesRemoved(projectContainer);
    project.assertNoCalls();
  }

  public void test_traverse_packages_removed_inSimpleProject() throws Exception {
    projectContainer = TestProjects.newSimpleProjectWithPackages();
    project = new DeltaProcessorMockProject(projectContainer);

    MockFolder packages = (MockFolder) projectContainer.remove(PACKAGES_DIRECTORY_NAME);
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(packages, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertDiscardContextsIn(packages);
    project.assertChanged(null, null, null, new IResource[] {packages});
    project.assertNoCalls();
  }

  public void test_traverse_project_file_added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    MockFile file = projectContainer.getMockFile("some.dart");
    delta.add(file, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, new IResource[] {file}, null, null);
    project.assertNoCalls();
  }

  public void test_traverse_project_file_changed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    MockFile file = projectContainer.getMockFile("some.dart");
    delta.add(file);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, new IResource[] {file}, null);
    project.assertNoCalls();
  }

  public void test_traverse_project_file_removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    MockResource file = projectContainer.remove("some.dart");
    delta.add(file, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, null, new IResource[] {file});
    project.assertNoCalls();
  }

  public void test_traverse_project_removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertDiscardContextsIn(projectContainer);
    project.assertNoCalls();
  }

  public void test_traverse_pubspecAdded() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PUBSPEC_FILE_NAME, ADDED);
    project.assertPubspecAdded();

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertPubspecAdded(projectContainer);
    project.assertNoCalls();
  }

  public void test_traverse_pubspecRemoved() throws Exception {
    project.getDefaultContext(); // force project initialization

    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PUBSPEC_FILE_NAME, REMOVED);
    projectContainer.remove(PUBSPEC_FILE_NAME);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertPubspecRemoved(projectContainer);
    project.assertNoCalls();
  }

  public void test_traverse_subpackage_ignored_file() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertNoCalls();
  }

  public void test_traverse_subpackage_ignored_folder() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME).add("pkg1", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertNoCalls();
  }

  public void test_traverse_web() throws Exception {
    MockFolder web = projectContainer.getMockFolder("web");

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(web);
    updater.applyChanges();

    IResource[] added = new IResource[] {
        web.getFile("other.dart"), web.getMockFolder("sub").getFile("cool.dart")};

    project.assertChanged(projectContainer, added, null, null);
    project.assertNoCalls();
  }

  public void test_traverse_web_added() throws Exception {
    MockFolder web = projectContainer.getMockFolder("web");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(web, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    IResource[] added = new IResource[] {
        web.getFile("other.dart"), web.getMockFolder("sub").getFile("cool.dart")};

    project.assertChanged(projectContainer, added, null, null);
    project.assertNoCalls();
  }

  public void test_traverse_web_removed() throws Exception {
    project.getDefaultContext(); // force project initialization

    MockContainer web = (MockContainer) projectContainer.remove("web");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(web, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, null, null, new IResource[] {web});
    project.assertDiscardContextsIn(web);
    project.assertNoCalls();
  }

  @Override
  protected void setUp() throws Exception {
    projectContainer = TestProjects.newPubProject3();
    appContainer = projectContainer.getMockFolder("myapp");
    subAppContainer = appContainer.getMockFolder("subApp");
    project = new DeltaProcessorMockProject(projectContainer);
  }
}
