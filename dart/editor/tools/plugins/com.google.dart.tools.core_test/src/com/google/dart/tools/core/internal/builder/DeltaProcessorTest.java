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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.CallList;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockResource;

import static com.google.dart.tools.core.DartCore.BUILD_DART_FILE_NAME;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;
import static org.mockito.Mockito.mock;

import java.io.File;

public class DeltaProcessorTest extends AbstractDartCoreTest {

  /**
   * Specialized {@link ProjectImpl} that returns a mock context for recording what analysis is
   * requested rather than a context that would actually analyze the source.
   */
  private final class MockProjectImpl extends ProjectImpl {

    private static final String DISCARD_CONTEXTS_IN = "discardContextsIn";
    private static final String PUBSPEC_ADDED = "pubspecAdded";
    private static final String PUBSPEC_REMOVED = "pubspecRemoved";

    private final CallList calls = new CallList();

    MockProjectImpl(IProject resource) {
      super(resource, mock(DartSdk.class), mock(Index.class), new AnalysisContextFactory() {
        @Override
        public AnalysisContext createContext() {
          return new MockContext();
        }
      });
    }

    @Override
    public void discardContextsIn(IContainer container) {
      calls.add(this, DISCARD_CONTEXTS_IN, container);
    }

    @Override
    public void pubspecAdded(IContainer container) {
      calls.add(this, PUBSPEC_ADDED, container);
    }

    @Override
    public void pubspecRemoved(IContainer container) {
      calls.add(this, PUBSPEC_REMOVED, container);
    }

    void assertChanged(IContainer pubFolderResource, File[] added, File[] changed,
        File[] removedFiles, File[] removedDirs) {
      MockContext context;
      if (pubFolderResource != null) {
        PubFolder pubFolder = getPubFolder(pubFolderResource);
        assertNotNull(pubFolder);
        assertSame(pubFolderResource, pubFolder.getResource());
        context = (MockContext) pubFolder.getContext();
      } else {
        PubFolder pubFolder = getPubFolder(getResource());
        assertNull(pubFolder);
        context = (MockContext) getDefaultContext();
      }
      context.assertChanged(added, changed, removedFiles, removedDirs);
    }

    void assertChanged(IContainer pubFolderResource, IResource[] added, IResource[] changed,
        IResource[] removed) {
      MockContext context;
      if (pubFolderResource != null) {
        PubFolder pubFolder = getPubFolder(pubFolderResource);
        assertNotNull(pubFolder);
        assertSame(pubFolderResource, pubFolder.getResource());
        context = (MockContext) pubFolder.getContext();
      } else {
        PubFolder pubFolder = getPubFolder(getResource());
        assertNull(pubFolder);
        context = (MockContext) getDefaultContext();
      }
      context.assertChanged(added, changed, removed);
    }

    void assertDiscardContextsIn(IContainer... expected) {
      for (IContainer container : expected) {
        calls.assertCall(this, DISCARD_CONTEXTS_IN, container);
      }
    }

    void assertNoCalls() {
      calls.assertNoCalls();
      ((MockContext) getDefaultContext()).assertNoCalls();
      for (PubFolder pubFolder : getPubFolders()) {
        ((MockContext) pubFolder.getContext()).assertNoCalls();
      }
    }

    void assertPubspecAdded(IContainer... expected) {
      for (IContainer container : expected) {
        calls.assertCall(this, PUBSPEC_ADDED, container);
      }
    }

    void assertPubspecRemoved(IContainer... expected) {
      for (IContainer container : expected) {
        calls.assertCall(this, PUBSPEC_REMOVED, container);
      }
    }
  }

  private MockProject projectContainer;
  private MockFolder appContainer;
  private MockFolder subAppContainer;
  private MockProjectImpl project;

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

  public void test_traverse_package_added_canonical() throws Exception {
    if (!FileUtilities2.isSymLinkSupported()) {
      System.out.println("Skipping " + getClass().getSimpleName()
          + " test_traverse_package_added_canonical");
      return;
    }

    // Create symlink from /project/packages/pkg1 --> /pkg1
    File projDir = FileUtilities2.createTempDir(projectContainer.getName());
    File packagesDir = new File(projDir, "packages");
    assertTrue(packagesDir.mkdir());
    File pkg1Dir = FileUtilities2.createTempDir("pkg1").getCanonicalFile();
    FileUtilities2.createSymLink(pkg1Dir, new File(packagesDir, "pkg1"));

    projectContainer.setLocation(new Path(projDir.getAbsolutePath()));

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
  }

  public void test_traverse_package_file_added() throws Exception {
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFile file = pkg1.getMockFile("bar.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    ProjectUpdater updater = new ProjectUpdater();
    processor.addDeltaListener(updater);
    processor.traverse(delta);
    updater.applyChanges();

    project.assertChanged(projectContainer, new IResource[] {file}, null, null);
    project.assertNoCalls();
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

    project.assertChanged(projectContainer, null, null, new IResource[] {pkg1});
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

    project.assertChanged(projectContainer, null, null, new IResource[] {packages});
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
    project = new MockProjectImpl(projectContainer);
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
