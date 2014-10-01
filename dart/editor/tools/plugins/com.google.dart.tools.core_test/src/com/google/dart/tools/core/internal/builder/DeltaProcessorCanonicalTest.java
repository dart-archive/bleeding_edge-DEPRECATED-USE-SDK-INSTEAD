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

import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.internal.analysis.model.WorkspaceDeltaProcessor;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;

import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.runtime.Path;
import org.mockito.Mockito;

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.io.File;
import java.io.IOException;

// Similar to DeltaProcessorTest but focusing on canonical processing of package changes
public class DeltaProcessorCanonicalTest extends AbstractDartCoreTest {

  private MockProject projectContainer;
  private DeltaProcessorMockProject project;

  private File projDir;
  private File packagesDir;
  private File pkg1Dir;

  /**
   * Test adding and removing a symlinked package in the "packages" directory
   */
  public void test_traverse_package_added_removed() throws Exception {

    if (!symlinkPackage(projDir)) {
      return;
    }

    // add package
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    // Canonical locations
    File pkg1SomeDir = new File(pkg1Dir, "some_folder");
    File[] added = new File[] {
        new File(pkg1Dir, "build.dart"), new File(pkg1Dir, "bar.dart"),
        new File(pkg1SomeDir, "build.dart"), new File(pkg1SomeDir, "bar.dart")};

    project.assertChanged(projectContainer, added, null, null, null);
    project.assertNoCalls();

    // remove package
    FileUtilities2.deleteSymLink(new File(packagesDir, "pkg1"));
    delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1", REMOVED);

    processor = new DeltaProcessor(project);
    AnalysisMarkerManager markerManager = Mockito.mock(AnalysisMarkerManager.class);
    IgnoreResourceFilter filter = new IgnoreResourceFilter(new DartIgnoreManager(), markerManager);
    updater = new ProjectUpdater();
    filter.addDeltaListener(updater);
    processor.addDeltaListener(filter);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertPackagesRemoved(projectContainer);
    project.assertNoCalls();
  }

  /**
   * Test removing a directory contained in a symlinked package
   */
  public void test_traverse_package_directory_removed() throws Exception {
    if (!symlinkPackage(projDir)) {
      return;
    }

    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg = packages.getMockFolder("pkg1");
    MockFolder folder = (MockFolder) pkg.remove("some_folder");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add(folder, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    AnalysisMarkerManager markerManager = Mockito.mock(AnalysisMarkerManager.class);
    IgnoreResourceFilter filter = new IgnoreResourceFilter(new DartIgnoreManager(), markerManager);
    ProjectUpdater updater = new ProjectUpdater();
    filter.addDeltaListener(updater);
    processor.addDeltaListener(filter);
    processor.traverse(delta);
    updater.applyChanges();

    File removedDir = new File(pkg1Dir, folder.getName());
    project.assertChanged(projectContainer, null, null, null, new File[] {removedDir});
    project.assertNoCalls();
  }

  /**
   * Test adding a file in a symlinked package
   */
  public void test_traverse_package_file_added() throws Exception {
    if (!symlinkPackage(projDir)) {
      return;
    }

    // Create delta with a added file in pkg1
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFile file = pkg1.getMockFile("bar.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    AnalysisMarkerManager markerManager = Mockito.mock(AnalysisMarkerManager.class);
    IgnoreResourceFilter filter = new IgnoreResourceFilter(new DartIgnoreManager(), markerManager);
    ProjectUpdater updater = new ProjectUpdater();
    filter.addDeltaListener(updater);
    processor.addDeltaListener(filter);
    processor.traverse(delta);
    updater.applyChanges();

    File addedFile = new File(pkg1Dir, file.getName());
    project.assertChanged(projectContainer, new File[] {addedFile}, null, null, null);
    project.assertNoCalls();
  }

  /**
   * Test adding a file in a symlinked package where the "packages" directory is in a pub folder
   * that is not the project folder
   */
  public void test_traverse_package_file_added2() throws Exception {

    projectContainer.remove(PUBSPEC_FILE_NAME);
    File myAppDir = new File(projDir, "myapp");

    if (!symlinkPackage(myAppDir)) {
      return;
    }

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

    File addedFile = new File(pkg1Dir, file.getName());
    project.assertChanged(myApp, new File[] {addedFile}, null, null, null);
  }

  /**
   * Test changing a file in a symlinked package.
   * <p>
   * In addition, test that the delta processor correctly traverses starting with the "packages"
   * directory rather than the project director, similar to what will be sent to the
   * {@link DeltaProcessor} by the {@link WorkspaceDeltaProcessor}.
   */
  public void test_traverse_package_file_changed() throws Exception {
    if (!symlinkPackage(projDir)) {
      return;
    }

    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFile file = pkg1.getMockFile("bar.dart");
    MockDelta delta = new MockDelta(packages);
    delta.add("pkg1").add("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    AnalysisMarkerManager markerManager = Mockito.mock(AnalysisMarkerManager.class);
    IgnoreResourceFilter filter = new IgnoreResourceFilter(new DartIgnoreManager(), markerManager);
    ProjectUpdater updater = new ProjectUpdater();
    filter.addDeltaListener(updater);
    processor.addDeltaListener(filter);
    processor.traverse(delta);
    updater.applyChanges();

    File changedFile = new File(pkg1Dir, file.getName());
    project.assertChanged(projectContainer, null, new File[] {changedFile}, null, null);
    project.assertNoCalls();
  }

  /**
   * Test changing a file in a symlinked package in the web/packages directory and assert that no
   * calls are made because it is not a top level or pub directory.
   */
  public void test_traverse_package_file_changed_inWebDir() throws Exception {
    if (!symlinkPackage(projDir)) {
      return;
    }

    MockFolder packages = projectContainer.getMockFolder("web").getMockFolder(
        PACKAGES_DIRECTORY_NAME);
    MockDelta delta = new MockDelta(packages);
    delta.add("pkg1").add("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    AnalysisMarkerManager markerManager = Mockito.mock(AnalysisMarkerManager.class);
    IgnoreResourceFilter filter = new IgnoreResourceFilter(new DartIgnoreManager(), markerManager);
    ProjectUpdater updater = new ProjectUpdater();
    filter.addDeltaListener(updater);
    processor.addDeltaListener(filter);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertNoCalls();
  }

  /**
   * Test removing a file from a symlinked package
   */
  public void test_traverse_package_file_removed() throws Exception {
    if (!symlinkPackage(projDir)) {
      return;
    }

    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg = packages.getMockFolder("pkg1");
    MockFile file = (MockFile) pkg.remove("bar.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add(file, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    AnalysisMarkerManager markerManager = Mockito.mock(AnalysisMarkerManager.class);
    IgnoreResourceFilter filter = new IgnoreResourceFilter(new DartIgnoreManager(), markerManager);
    ProjectUpdater updater = new ProjectUpdater();
    filter.addDeltaListener(updater);
    processor.addDeltaListener(filter);
    processor.traverse(delta);
    updater.applyChanges();

    File removedFile = new File(pkg1Dir, file.getName());
    project.assertChanged(projectContainer, null, null, new File[] {removedFile}, null);
    project.assertNoCalls();
  }

  @Override
  protected void setUp() throws Exception {
    projectContainer = TestProjects.newPubProject3();
    project = new DeltaProcessorMockProject(projectContainer);

    projDir = FileUtilities2.createTempDir(projectContainer.getName());
    pkg1Dir = FileUtilities2.createTempDir("pkg1").getCanonicalFile();
    projectContainer.setLocation(new Path(projDir.getAbsolutePath()));
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }

  /**
   * Create a symlink from the pubDir/packages/pkg1 directory to the pkg1 directory.
   * 
   * @param pubDir the pub directory (not {@code null})
   * @return {@code true} if symlinks can be created on this platform, else {@code false}
   */
  private boolean symlinkPackage(File pubDir) throws IOException {
    if (!FileUtilities2.isSymLinkSupported()) {
      System.out.println("Skipping test in " + getClass().getSimpleName());
      return false;
    }
    packagesDir = new File(pubDir, "packages");
    assertTrue(packagesDir.mkdirs());
    FileUtilities2.createSymLink(pkg1Dir, new File(packagesDir, "pkg1"));
    return true;
  }
}
