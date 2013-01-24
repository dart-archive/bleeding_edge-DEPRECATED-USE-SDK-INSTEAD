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
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.AbstractDartCoreTest;
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

import static org.eclipse.core.resources.IResourceDelta.ADDED;
import static org.eclipse.core.resources.IResourceDelta.REMOVED;

import java.util.ArrayList;
import java.util.Collections;

public class DeltaProcessorTest extends AbstractDartCoreTest {

  /**
   * Specialized {@link ProjectImpl} that returns a mock context for recording what analysis is
   * requested rather than a context that would actually analyze the source.
   */
  private final class MockProjectImpl extends ProjectImpl {

    private final ArrayList<IContainer> containersDeleted = new ArrayList<IContainer>();
    private final ArrayList<IContainer> pubspecsAdded = new ArrayList<IContainer>();
    private final ArrayList<IContainer> pubspecsRemoved = new ArrayList<IContainer>();

    MockProjectImpl(IProject resource) {
      super(resource);
    }

    @Override
    public void discardContextsIn(IContainer container) {
      containersDeleted.add(container);
    }

    @Override
    public void pubspecAdded(IContainer container) {
      pubspecsAdded.add(container);
    }

    @Override
    public void pubspecRemoved(IContainer container) {
      pubspecsRemoved.add(container);
    }

    @Override
    protected AnalysisContext createDefaultContext() {
      return new MockContext();
    }

    void assertContainerDeleted(IContainer... expected) {
      assertEqualContents(expected, containersDeleted);
    }

    void assertPubspecAdded(IContainer... expected) {
      assertEqualContents(expected, pubspecsAdded);
    }

    void assertPubspecRemoved(IContainer... expected) {
      assertEqualContents(expected, pubspecsRemoved);
    }

    private void assertEqualContents(IContainer[] expected, ArrayList<IContainer> actual) {
      if (actual.size() == expected.length) {
        boolean success = true;
        for (int index = 0; index < expected.length; index++) {
          if (!actual.contains(expected[index])) {
            success = false;
            break;
          }
        }
        if (success) {
          return;
        }
      }
      PrintStringWriter msg = new PrintStringWriter();
      msg.println("Expected:");
      for (String string : sort(getPaths(expected))) {
        msg.println(string);
      }
      msg.println("Actual:");
      for (String string : sort(getPaths(actual))) {
        msg.println(string);
      }
      fail(msg.toString().trim());
    }

    private ArrayList<String> getPaths(ArrayList<IContainer> resources) {
      return getPaths(resources.toArray(new IResource[resources.size()]));
    }

    private ArrayList<String> getPaths(IResource[] resources) {
      ArrayList<String> result = new ArrayList<String>();
      for (IResource resource : resources) {
        result.add(resource.getLocation().toOSString());
      }
      return result;
    }

    private ArrayList<String> sort(ArrayList<String> paths) {
      Collections.sort(paths);
      return paths;
    }
  }

  private MockProject projectContainer;
  private MockFolder appContainer;
  private MockFolder subAppContainer;
  private MockProjectImpl project;

  public void test_traverse_file1Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("some.dart", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertSourcesChanged(projectContainer, true, projectContainer.getMockFile("some.dart"));
  }

  public void test_traverse_file1Changed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("some.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertSourcesChanged(projectContainer, true, projectContainer.getMockFile("some.dart"));
  }

  public void test_traverse_file1Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("some.dart", REMOVED);
    MockResource removedFile = projectContainer.remove("some.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertSourcesDeleted(projectContainer, removedFile);
  }

  public void test_traverse_file2Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("myapp").add("other.dart", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertSourcesChanged(appContainer, true, appContainer.getMockFile("other.dart"));
  }

  public void test_traverse_file2Changed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("myapp").add("other.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertSourcesChanged(appContainer, true, appContainer.getMockFile("other.dart"));
  }

  public void test_traverse_file3Changed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(".svn").add("foo.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_folder1() throws Exception {
    MockFolder web = projectContainer.getMockFolder("web");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(web);

    assertSourcesChanged(
        projectContainer,
        true,
        web.getFile("other.dart"),
        web.getMockFolder("sub").getFile("cool.dart"));
  }

  public void test_traverse_folder1a() throws Exception {
    MockFolder web = projectContainer.getMockFolder("web");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(false));
    processor.traverse(web);

    assertSourcesChanged(
        projectContainer,
        false,
        web.getFile("other.dart"),
        web.getMockFolder("sub").getFile("cool.dart"));
  }

  public void test_traverse_folder1Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    MockFolder web = projectContainer.getMockFolder("web");
    assertSourcesChanged(
        projectContainer,
        true,
        web.getFile("other.dart"),
        web.getMockFolder("sub").getFile("cool.dart"));
  }

  public void test_traverse_folder1Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web", REMOVED);
    MockContainer removedFolder = (MockContainer) projectContainer.remove("web");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    MockContext context = (MockContext) project.getContext(projectContainer);
    context.assertSourcesChanged();
    context.assertSourcesDeleted(removedFolder);
    assertNoContextChanges((MockContext) project.getContext(appContainer));
    assertNoContextChanges((MockContext) project.getContext(subAppContainer));
    project.assertPubspecAdded();
    project.assertPubspecRemoved();
    project.assertContainerDeleted(removedFolder);
  }

  public void test_traverse_folder2Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(appContainer, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoContextChanges((MockContext) project.getContext(projectContainer));

    MockFolder libContainer = appContainer.getMockFolder("lib");
    IResource[] resources1 = {
        appContainer.getFile(BUILD_DART_FILE_NAME), appContainer.getFile("other.dart"),
        libContainer.getFile("stuff.dart")};
    MockContext context = (MockContext) project.getContext(appContainer);
    context.assertSourcesChanged(resources1);
    context.assertSourcesDeleted();

    IResource[] resources2 = {subAppContainer.getFile("sub_stuff.dart")};
    context = (MockContext) project.getContext(subAppContainer);
    context.assertSourcesChanged(resources2);
    context.assertSourcesDeleted();

    project.assertPubspecAdded(appContainer, subAppContainer);
    project.assertPubspecRemoved();
    project.assertContainerDeleted();
  }

  public void test_traverse_folder2Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(appContainer, REMOVED);
    projectContainer.remove(appContainer.getName());

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoContextChanges((MockContext) project.getContext(projectContainer));
    assertNoContextChanges((MockContext) project.getContext(appContainer));
    assertNoContextChanges((MockContext) project.getContext(subAppContainer));
    project.assertPubspecAdded();
    project.assertPubspecRemoved();
    project.assertContainerDeleted(appContainer);
  }

  public void test_traverse_folder3Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(".svn", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_folder3Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(".svn", REMOVED);
    projectContainer.remove(".svn");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_folder4Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoContextChanges((MockContext) project.getContext(projectContainer));
    assertNoContextChanges((MockContext) project.getContext(appContainer));
    assertNoContextChanges((MockContext) project.getContext(subAppContainer));
    project.assertPubspecAdded();
    project.assertPubspecRemoved();
    project.assertContainerDeleted(projectContainer);
  }

  public void test_traverse_package1Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertPackagesDartSourcesChanged();
  }

  public void test_traverse_package1FileAdded() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    MockFolder pkg1 = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME).getMockFolder("pkg1");
    assertSourcesChanged(projectContainer, true, pkg1.getMockFile("bar.dart"));
  }

  public void test_traverse_package1FileChanged() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    MockFolder pkg1 = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME).getMockFolder("pkg1");
    assertSourcesChanged(projectContainer, true, pkg1.getMockFile("bar.dart"));
  }

  public void test_traverse_package1FileRemoved() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart", REMOVED);
    MockFolder pkg = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME).getMockFolder("pkg1");
    MockFile removedFile = (MockFile) pkg.remove("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertSourcesDeleted(projectContainer, removedFile);
  }

  public void test_traverse_package1Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME).add("pkg1", REMOVED);
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = (MockFolder) packages.remove("pkg1");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertSourcesDeleted(projectContainer, pkg1);
  }

  public void test_traverse_package2Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME).add("pkg1", ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_package2FileChanged() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_package2FileRemoved() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME).add("pkg1").add("bar.dart", REMOVED);
    MockFolder web = projectContainer.getMockFolder("web");
    web.getMockFolder(PACKAGES_DIRECTORY_NAME).getMockFolder("pkg1").remove("bar.dart");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_package2Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME).add("pkg1", REMOVED);
    MockFolder web = projectContainer.getMockFolder("web");
    web.getMockFolder(PACKAGES_DIRECTORY_NAME).remove("pkg1");

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_packages1Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertPackagesDartSourcesChanged();
  }

  public void test_traverse_packages1Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PACKAGES_DIRECTORY_NAME, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    assertSourcesDeleted(projectContainer, packages);
  }

  public void test_traverse_packages2Added() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME, ADDED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_packages2Removed() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add("web").add(PACKAGES_DIRECTORY_NAME, REMOVED);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoChanges();
  }

  public void test_traverse_pubspecAdded() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PUBSPEC_FILE_NAME, ADDED);
    project.assertPubspecAdded();

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoContextChanges((MockContext) project.getContext(projectContainer));
    assertNoContextChanges((MockContext) project.getContext(appContainer));
    assertNoContextChanges((MockContext) project.getContext(subAppContainer));
    project.assertPubspecAdded(projectContainer);
    project.assertPubspecRemoved();
    project.assertContainerDeleted();
  }

  public void test_traverse_pubspecRemoved() throws Exception {
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(PUBSPEC_FILE_NAME, REMOVED);
    projectContainer.remove(PUBSPEC_FILE_NAME);

    DeltaProcessor processor = new DeltaProcessor(project);
    processor.addDeltaListener(new ProjectUpdater(true));
    processor.traverse(delta);

    assertNoContextChanges((MockContext) project.getContext(projectContainer));
    assertNoContextChanges((MockContext) project.getContext(appContainer));
    assertNoContextChanges((MockContext) project.getContext(subAppContainer));
    project.assertPubspecAdded();
    project.assertPubspecRemoved(projectContainer);
    project.assertContainerDeleted();
  }

  @Override
  protected void setUp() throws Exception {
    projectContainer = TestProjects.newPubProject3();
    appContainer = projectContainer.getMockFolder("myapp");
    subAppContainer = appContainer.getMockFolder("subApp");
    project = new MockProjectImpl(projectContainer);
  }

  /**
   * Assert no changes in project or contexts
   */
  private void assertNoChanges() {
    assertNoContextChanges((MockContext) project.getContext(projectContainer));
    assertNoContextChanges((MockContext) project.getContext(appContainer));
    assertNoContextChanges((MockContext) project.getContext(subAppContainer));

    project.assertPubspecAdded();
    project.assertPubspecRemoved();
    project.assertContainerDeleted();
  }

  /**
   * Assert no changes in the specified context
   */
  private void assertNoContextChanges(MockContext context) {
    context.assertSourcesChanged();
    context.assertSourcesDeleted();
  }

  /**
   * Assert the project "packages" sources changed, but nothing else
   */
  private void assertPackagesDartSourcesChanged() {
    MockFolder packages = projectContainer.getMockFolder(PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFolder pkgFolder = pkg1.getMockFolder("some_folder");

    assertSourcesChanged(
        projectContainer,
        true,
        pkg1.getMockFile("bar.dart"),
        pkg1.getMockFile("build.dart"),
        pkgFolder.getMockFile("bar.dart"),
        pkgFolder.getMockFile("build.dart"));
  }

  /**
   * Assert that sources were changed in the specified container and no other changes
   */
  private void assertSourcesChanged(MockContainer container, boolean changed,
      IResource... resources) {

    MockContext context = (MockContext) project.getContext(container);
    if (changed) {
      context.assertSourcesChanged(resources);
    } else {
      context.assertSourcesChanged();
    }
    context.assertSourcesDeleted();

    if (container != projectContainer) {
      assertNoContextChanges((MockContext) project.getContext(projectContainer));
    }
    if (container != appContainer) {
      assertNoContextChanges((MockContext) project.getContext(appContainer));
    }
    if (container != subAppContainer) {
      assertNoContextChanges((MockContext) project.getContext(subAppContainer));
    }

    project.assertPubspecAdded();
    project.assertPubspecRemoved();
    project.assertContainerDeleted();
  }

  /**
   * Assert that sources were deleted from the specified container and no other changes
   */
  private void assertSourcesDeleted(MockContainer container, IResource... resources) {
    MockContext context = (MockContext) project.getContext(container);
    context.assertSourcesChanged();
    context.assertSourcesDeleted(resources);

    if (container != projectContainer) {
      assertNoContextChanges((MockContext) project.getContext(projectContainer));
    }
    if (container != appContainer) {
      assertNoContextChanges((MockContext) project.getContext(appContainer));
    }
    if (container != subAppContainer) {
      assertNoContextChanges((MockContext) project.getContext(subAppContainer));
    }

    project.assertPubspecAdded();
    project.assertPubspecRemoved();
    project.assertContainerDeleted();
  }
}
