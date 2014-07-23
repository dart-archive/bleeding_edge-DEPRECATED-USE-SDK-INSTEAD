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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.tools.core.CmdLineOptions;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl.AnalysisContextFactory;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.utilities.io.FileUtilities;

import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.tools.core.DartCore.PACKAGES_DIRECTORY_NAME;
import static com.google.dart.tools.core.DartCore.PUBSPEC_FILE_NAME;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

public class ProjectImplTest extends ContextManagerImplTest {

  private final class MockContextForTest extends MockContext {

    @Override
    public LibraryElement computeLibraryElement(Source source) {
      if (source.getShortName().equals("libraryA.dart")) {
        return library(this, "libraryA");
      }
      return null;
    }

    @Override
    public Source[] getLibrarySources() {
      return Source.EMPTY_ARRAY;
    }
  }

  private MockProject projectContainer;
  private MockFolder webContainer;
  private MockFolder subContainer;
  private MockFolder appContainer;
  private MockFolder subAppContainer;
  private File[] packageRoots = new File[0];

  private Index index;
  private TestProject testProject;
  private boolean useInstrumentedContexts;

  public void assertUriResolvedToPackageRoot(Project project, IPath expectedPackageRoot) {
    IPath expected = expectedPackageRoot != null ? expectedPackageRoot.append("foo").append(
        "foo.dart") : null;

    SourceFactory factory = project.getDefaultContext().getSourceFactory();
    Source source = factory.forUri("package:foo/foo.dart");
    IPath actual = new Path(source.getFullName());

    assertEquals(expected.setDevice(null), actual.setDevice(null));
  }

  public void test_AnalysisContextFactory() throws Exception {
    AnalysisContextFactory contextFactory = new AnalysisContextFactory();
    AnalysisContext context = contextFactory.createContext();
    assertNotNull(context);
    File[] roots = contextFactory.getPackageRoots(projectContainer);
    assertNotNull(roots);
    assertEquals(0, roots.length);
  }

  /**
   * Verify context removed from index when folder containing pubspec is discarded
   */
  public void test_discardContextsIn_project() {
    ProjectImpl project = newTarget();
    assertEquals(1, project.getPubFolders().length);
    PubFolder pubFolder = project.getPubFolder(projectContainer);
    assertNotNull(pubFolder);

    projectContainer.remove(PUBSPEC_FILE_NAME);
    MockFolder myApp = projectContainer.getMockFolder("myapp");
    myApp.remove(PUBSPEC_FILE_NAME);
    myApp.getMockFolder("subApp").remove(PUBSPEC_FILE_NAME);
    project.discardContextsIn(projectContainer);

    verify(index, times(2)).removeContext(pubFolder.getContext());
    assertEquals(0, project.getPubFolders().length);
  }

  /**
   * Verify that when "web" folder is removed, index is not modified because "web" is a child folder
   * and does not contains a pubspec
   */
  public void test_discardContextsIn_project_web() {
    ProjectImpl project = newTarget();
    assertEquals(1, project.getPubFolders().length);
    PubFolder pubFolder = project.getPubFolder(projectContainer);
    assertNotNull(pubFolder);

    projectContainer.remove("web");
    project.discardContextsIn(webContainer);

    verifyNoMoreInteractions(index);
    assertEquals(1, project.getPubFolders().length);
    assertSame(pubFolder, project.getPubFolder(projectContainer));
  }

  public void test_getContext_folder() {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);

    MockContext context1 = (MockContext) project.getContext(projectContainer);
    assertNotNull(context1);
    assertSame(context1, project.getDefaultContext());
    assertSame(context1, project.getContext(webContainer));
    assertSame(context1, project.getContext(subContainer));

    MockContext context2 = (MockContext) project.getContext(appContainer);
    assertNotNull(context2);
    assertNotSame(context1, context2);

    context1.assertExtracted(appContainer);
    context2.assertExtracted(null);

    assertDartSdkFactoryInitialized(projectContainer, context1);
    assertFactoryInitialized(appContainer, context2);
  }

  public void test_getContext_project() {
    ProjectImpl project = newTarget();
    MockContext context1 = (MockContext) project.getContext(projectContainer);
    assertNotNull(context1);
    assertSame(context1, project.getDefaultContext());
    assertSame(context1, project.getContext(webContainer));
    assertSame(context1, project.getContext(subContainer));
    assertSame(context1, project.getContext(appContainer));

    context1.assertExtracted(null);

    assertFactoryInitialized(projectContainer, context1);
  }

  public void test_getLibrarySources() {
    ProjectImpl project = newTarget();
    // TODO(keertip): make this more meaningful
    MockFolder folder = projectContainer.getMockFolder("web");
    MockFile file = new MockFile(folder, "libraryA.dart", "library libraryA;\n\n main(){}");
    folder.add(file);
    Source[] sources = project.getLibrarySources();
    assertNotNull(sources);
  }

  public void test_getMultiplePackageRoots_project() throws Exception {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {});
    final IEclipsePreferences prefs = mock(IEclipsePreferences.class);
    when(prefs.get(DartCore.PROJECT_PREF_PACKAGE_ROOT, "")).thenReturn(
        "bar" + File.pathSeparator + "foo");
    final DartCore core = mock(DartCore.class);
    when(core.getProjectPreferences(projectContainer)).thenReturn(prefs);

    File[] roots = ProjectImpl.getPackageRoots(core, options, projectContainer);
    assertNotNull(roots);
    assertEquals(2, roots.length);
    assertEquals("bar", roots[0].getName());
    assertEquals("foo", roots[1].getName());
  }

  public void test_getPackageRoots() throws Exception {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {});
    DartCore core = DartCore.getPlugin();

    File[] roots = ProjectImpl.getPackageRoots(core, options, projectContainer);
    assertNotNull(roots);
    assertEquals(0, roots.length);
  }

  public void test_getPackageRoots_global() throws Exception {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {
        "--package-root", "foo", "bar"});
    DartCore core = DartCore.getPlugin();

    File[] roots = ProjectImpl.getPackageRoots(core, options, projectContainer);
    assertNotNull(roots);
    assertEquals(2, roots.length);
    assertEquals("foo", roots[0].getName());
    assertEquals("bar", roots[1].getName());
  }

  public void test_getPackageRoots_project() throws Exception {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {});
    final IEclipsePreferences prefs = mock(IEclipsePreferences.class);
    when(prefs.get(DartCore.PROJECT_PREF_PACKAGE_ROOT, "")).thenReturn("bar");
    final DartCore core = mock(DartCore.class);
    when(core.getProjectPreferences(projectContainer)).thenReturn(prefs);

    File[] roots = ProjectImpl.getPackageRoots(core, options, projectContainer);
    assertNotNull(roots);
    assertEquals(1, roots.length);
    assertEquals("bar", roots[0].getName());
  }

  public void test_getResource() {
    ProjectImpl project = newTarget();
    assertSame(projectContainer, project.getResource());
  }

  public void test_getResource_Source() {
    ProjectImpl project = newTarget();
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    File file = resource.getLocation().toFile();
    Source source = new FileBasedSource(file);
    assertSame(resource, project.getResource(source));
  }

  public void test_getResource_Source_null() {
    ProjectImpl project = newTarget();
    assertNull(project.getResource(null));
  }

  public void test_getResource_Source_outside() {
    ProjectImpl project = newTarget();
    File file = new File("/does/not/exist.dart");
    Source source = new FileBasedSource(file);
    assertNull(project.getResource(source));
  }

  public void test_getResourceMap_forContext() {
    projectContainer.remove(DartCore.PUBSPEC_FILE_NAME);

    // Initialize the project without instrumented contexts
    ProjectImpl project = newTarget();
    useInstrumentedContexts = false;
    assertFalse(project.getDefaultContext() instanceof InstrumentedAnalysisContextImpl);

    AnalysisContext defaultContext = project.getDefaultContext();
    assertFalse(defaultContext instanceof InstrumentedAnalysisContextImpl);
    AnalysisContext pubContext = project.getContext(appContainer);
    assertFalse(pubContext instanceof InstrumentedAnalysisContextImpl);
    assertNotSame(defaultContext, pubContext);

    ResourceMap defaultResourceMap = project.getResourceMap(projectContainer);
    assertNotNull(defaultResourceMap);
    ResourceMap pubResourceMap = project.getResourceMap(appContainer);
    assertNotNull(pubResourceMap);
    assertNotSame(defaultResourceMap, pubResourceMap);

    assertNull(project.getResourceMap((AnalysisContext) null));
    assertNull(project.getResourceMap(AnalysisEngine.getInstance().createAnalysisContext()));

    assertSame(defaultResourceMap, project.getResourceMap(defaultContext));
    assertSame(pubResourceMap, project.getResourceMap(pubContext));
  }

  public void test_getResourceMap_forInstrumentedContext() {
    projectContainer.remove(DartCore.PUBSPEC_FILE_NAME);

    // Initialize the project with instrumented contexts
    ProjectImpl project = newTarget();
    useInstrumentedContexts = true;
    assertTrue(project.getDefaultContext() instanceof InstrumentedAnalysisContextImpl);

    AnalysisContext defaultContext = project.getDefaultContext();
    assertTrue(defaultContext instanceof InstrumentedAnalysisContextImpl);
    AnalysisContext pubContext = project.getContext(appContainer);
    assertTrue(pubContext instanceof InstrumentedAnalysisContextImpl);
    assertNotSame(defaultContext, pubContext);

    ResourceMap defaultResourceMap = project.getResourceMap(projectContainer);
    assertNotNull(defaultResourceMap);
    ResourceMap pubResourceMap = project.getResourceMap(appContainer);
    assertNotNull(pubResourceMap);
    assertNotSame(defaultResourceMap, pubResourceMap);

    assertNull(project.getResourceMap((AnalysisContext) null));
    assertNull(project.getResourceMap(AnalysisEngine.getInstance().createAnalysisContext()));

    AnalysisContext uninstrumentedDefaultContext = ((InstrumentedAnalysisContextImpl) defaultContext).getBasis();
    AnalysisContext uninstrumentedPubContext = ((InstrumentedAnalysisContextImpl) pubContext).getBasis();

    assertSame(defaultResourceMap, project.getResourceMap(defaultContext));
    assertSame(pubResourceMap, project.getResourceMap(pubContext));
    assertSame(defaultResourceMap, project.getResourceMap(uninstrumentedDefaultContext));
    assertSame(pubResourceMap, project.getResourceMap(uninstrumentedPubContext));
  }

  public void test_packageRoots_ignored() throws Exception {
    ProjectImpl project = newTarget();
    packageRoots = new File[] {new Path("/does/not/exist").toFile()};

    assertUriResolvedToPackageRoot(project, new Path("/does/not/exist"));
  }

  public void test_packageRoots_not_set() throws Exception {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);
    appContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);
    packageRoots = new File[] {};

    assertUriDoesNotResolve(project);
  }

  public void test_packageRoots_set() throws Exception {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);
    appContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);
    packageRoots = new File[] {new Path("/does/not/exist").toFile()};

    assertUriResolvedToPackageRoot(project, new Path(packageRoots[0].getPath()));
  }

  public void test_pubFolder_folder() {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);

    assertNull(project.getPubFolder(projectContainer));
    assertNull(project.getPubFolder(webContainer));
    assertNull(project.getPubFolder(subContainer));

    PubFolder pubFolder = project.getPubFolder(appContainer);
    assertNotNull(pubFolder);

    assertEquals(1, project.getPubFolders().length);
    assertSame(pubFolder, project.getPubFolders()[0]);

    assertNotNull(project.getDefaultContext());
    assertNotSame(project.getDefaultContext(), pubFolder.getContext());

    assertDartSdkFactoryInitialized(projectContainer, project.getDefaultContext());
    assertFactoryInitialized(appContainer, pubFolder.getContext());
  }

  public void test_pubFolder_none() {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);
    appContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);

    assertNull(project.getPubFolder(projectContainer));
    assertNull(project.getPubFolder(webContainer));
    assertNull(project.getPubFolder(subContainer));
    assertNull(project.getPubFolder(appContainer));

    assertEquals(0, project.getPubFolders().length);

    assertNotNull(project.getDefaultContext());

    assertDartSdkFactoryInitialized(projectContainer, project.getDefaultContext());
  }

  public void test_pubFolder_project() {
    ProjectImpl project = newTarget();
    // Simulate traversal in which the top level pubspec is visited last
    projectContainer.add(projectContainer.remove(PUBSPEC_FILE_NAME));

    PubFolder pubFolder = project.getPubFolder(projectContainer);
    assertNotNull(pubFolder);

    assertSame(pubFolder, project.getPubFolder(webContainer));
    assertSame(pubFolder, project.getPubFolder(subContainer));
    assertSame(pubFolder, project.getPubFolder(appContainer));

    assertEquals(1, project.getPubFolders().length);
    assertSame(pubFolder, project.getPubFolders()[0]);

    assertNotNull(project.getDefaultContext());
    assertSame(project.getDefaultContext(), pubFolder.getContext());

    assertFactoryInitialized(projectContainer, project.getDefaultContext());
  }

  public void test_pubspecAdded_folder() {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);
    appContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);

    assertEquals(0, project.getPubFolders().length);
    MockContext defaultContext = (MockContext) project.getDefaultContext();
    defaultContext.assertExtracted(null);
    defaultContext.assertMergedContext(null);
    assertDartSdkFactoryInitialized(projectContainer, defaultContext);

    appContainer.addFile(PUBSPEC_FILE_NAME);
    project.pubspecAdded(appContainer);

    assertEquals(1, project.getPubFolders().length);
    assertNull(project.getPubFolder(projectContainer));
    PubFolder pubFolder = project.getPubFolder(appContainer);
    assertNotNull(pubFolder);
    assertSame(defaultContext, project.getDefaultContext());
    assertNotSame(defaultContext, pubFolder.getContext());
    assertSame(appContainer, pubFolder.getResource());
    defaultContext.assertExtracted(appContainer);
    defaultContext.assertMergedContext(null);
    assertDartSdkFactoryInitialized(projectContainer, defaultContext);
  }

  public void test_pubspecAdded_folder_ignored() {
    ProjectImpl project = newTarget();
    appContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);

    assertEquals(1, project.getPubFolders().length);
    PubFolder pubFolder = project.getPubFolder(projectContainer);
    assertNotNull(pubFolder);

    appContainer.addFile(PUBSPEC_FILE_NAME);
    project.pubspecAdded(appContainer);

    assertEquals(1, project.getPubFolders().length);
    assertSame(pubFolder, project.getPubFolder(projectContainer));
  }

  public void test_pubspecAdded_project() {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);
    appContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);
    packageRoots = new File[] {new Path("/does/not/exist").toFile()};

    assertEquals(0, project.getPubFolders().length);
    MockContext defaultContext = (MockContext) project.getDefaultContext();
    defaultContext.assertExtracted(null);
    defaultContext.assertMergedContext(null);
    assertFactoryInitialized(projectContainer, defaultContext);
    assertUriResolvedToPackageRoot(project, new Path(packageRoots[0].getPath()));

    packageRoots = new File[] {};
    projectContainer.addFile(PUBSPEC_FILE_NAME);
    project.pubspecAdded(projectContainer);

    assertEquals(1, project.getPubFolders().length);
    PubFolder pubFolder = project.getPubFolder(projectContainer);
    assertNotNull(pubFolder);
    assertSame(defaultContext, project.getDefaultContext());
    assertSame(defaultContext, pubFolder.getContext());
    assertSame(projectContainer, pubFolder.getResource());
    defaultContext.assertExtracted(null);
    defaultContext.assertMergedContext(null);
    assertFactoryInitialized(projectContainer, defaultContext);
    assertUriResolvedToPackageRoot(
        project,
        projectContainer.getLocation().append(PACKAGES_DIRECTORY_NAME));
  }

  public void test_pubspecAdded_project_replacing_folder() {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);

    assertEquals(1, project.getPubFolders().length);
    assertNull(project.getPubFolder(projectContainer));
    PubFolder oldPubFolder = project.getPubFolder(appContainer);
    assertNotNull(oldPubFolder);
    assertSame(appContainer, oldPubFolder.getResource());
    MockContext defaultContext = (MockContext) project.getDefaultContext();
    defaultContext.assertExtracted(appContainer);
    defaultContext.assertMergedContext(null);
    assertDartSdkFactoryInitialized(projectContainer, defaultContext);

    projectContainer.addFile(PUBSPEC_FILE_NAME);
    project.pubspecAdded(projectContainer);

    assertEquals(1, project.getPubFolders().length);
    PubFolder newPubFolder = project.getPubFolder(projectContainer);
    assertNotNull(newPubFolder);
    assertNotSame(oldPubFolder, newPubFolder);
    assertSame(defaultContext, project.getDefaultContext());
    assertSame(defaultContext, newPubFolder.getContext());
    assertSame(projectContainer, newPubFolder.getResource());
    defaultContext.assertExtracted(null);
    defaultContext.assertMergedContext(oldPubFolder.getContext());
    assertFactoryInitialized(projectContainer, defaultContext);
  }

  /**
   * Verify index is updated when pubspec is removed
   */
  public void test_pubspecRemoved_folder() {
    ProjectImpl project = newTarget();
    projectContainer.remove(PUBSPEC_FILE_NAME);

    assertEquals(1, project.getPubFolders().length);
    PubFolder pubFolder = project.getPubFolder(appContainer);
    assertNotNull(pubFolder);

    appContainer.remove(PUBSPEC_FILE_NAME);
    project.pubspecRemoved(appContainer);

    verify(index).removeContext(pubFolder.getContext());
    assertEquals(1, project.getPubFolders().length);
    assertSame(subAppContainer, project.getPubFolder(subAppContainer).getResource());
    SourceContainer directory = new DirectoryBasedSourceContainer(
        subAppContainer.getLocation().toFile());
    ((MockContext) project.getDefaultContext()).extractContext(directory);
  }

  /**
   * Verify nothing removed from index when pubspec removed from project container because default
   * context does not change
   */
  public void test_pubspecRemoved_project_to_folder() {
    ProjectImpl project = newTarget();
    subAppContainer.remove(PUBSPEC_FILE_NAME);

    assertEquals(1, project.getPubFolders().length);
    PubFolder pubFolder = project.getPubFolder(projectContainer);
    assertNotNull(pubFolder);

    projectContainer.remove(PUBSPEC_FILE_NAME);
    project.pubspecRemoved(projectContainer);

    verifyNoMoreInteractions(index);
    assertEquals(1, project.getPubFolders().length);
    assertSame(appContainer, project.getPubFolder(appContainer).getResource());
    SourceContainer directory = new DirectoryBasedSourceContainer(
        appContainer.getLocation().toFile());
    ((MockContext) project.getDefaultContext()).extractContext(directory);
  }

  public void test_pubspecRemoved_project_to_none() {
    ProjectImpl project = newTarget();
    appContainer.remove(PUBSPEC_FILE_NAME);
    subAppContainer.remove(PUBSPEC_FILE_NAME);

    assertEquals(1, project.getPubFolders().length);
    PubFolder pubFolder = project.getPubFolder(projectContainer);
    assertNotNull(pubFolder);
    assertUriResolvedToPackageRoot(
        project,
        projectContainer.getLocation().append(PACKAGES_DIRECTORY_NAME));

    projectContainer.remove(PUBSPEC_FILE_NAME);
    project.pubspecRemoved(projectContainer);

    assertEquals(0, project.getPubFolders().length);
  }

  public void test_resolvePathToPackage() {
    //TODO(keertip): implement project with ExplicitUriResolver and package map
    // use that to verify path to package
    // ProjectImpl project = newTarget();
  }

  public void test_resolveUriTofileInfoFile() throws IOException {
    projectContainer.remove(PUBSPEC_FILE_NAME);
    File parent = projectContainer.getLocation().toFile().getParentFile();
    File packages = new File(parent, "packagesRoot");
    File dartFile = new File(packages, "bar/foo.dart");
    FileUtilities.create(dartFile);

    final IEclipsePreferences prefs = mock(IEclipsePreferences.class);
    when(prefs.get(DartCore.PROJECT_PREF_PACKAGE_ROOT, "")).thenReturn(packages.getAbsolutePath());
    final DartCore core = mock(DartCore.class);
    when(core.getProjectPreferences(projectContainer)).thenReturn(prefs);

    ProjectImpl project = new ProjectImpl(
        projectContainer,
        sdk,
        sdkContextId,
        index,
        new AnalysisContextFactory() {
          @Override
          public AnalysisContext createContext() {
            return new MockContextForTest();
          }

          @Override
          public File[] getPackageRoots(IContainer container) {
            CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {});
            return ProjectImpl.getPackageRoots(core, options, projectContainer);
          }
        });
    IFileInfo info = project.resolveUriToFileInfo(webContainer, "package:bar/foo.dart");
    assertNotNull(info);
    assertNotNull(info.getFile());
    assertNull(info.getResource());
  }

  public void test_resolveUriToFileInfoResource() {
    ProjectImpl project = newTarget();
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    IFileInfo info = project.resolveUriToFileInfo(
        webContainer,
        resource.getLocation().toFile().toURI().toString());
    assertNotNull(info);
    assertNotNull(info.getResource());

    info = project.resolveUriToFileInfo(webContainer, "package:pkg1/build.dart");
    assertNotNull(info);
    assertNotNull(info.getResource());

  }

  public void test_resolveUriToFileInfoResourceInLib() throws IOException {
    if (!DartCore.isWindows()) {
      ProjectImpl project = newTarget();
      File packages = projectContainer.getFolder("packages").getLocation().toFile();
      File lib = projectContainer.getFolder("lib").getLocation().toFile();

      FileUtilities.createDirectory(packages);
      FileUtilities.createDirectory(lib);
      FileUtilities2.createSymLink(lib, new File(packages, "myapp"));

      IFileInfo info = project.resolveUriToFileInfo(webContainer, "package:myapp/stuff.dart");
      assertNotNull(info);
      assertNotNull(info.getResource());
      assertEquals(
          info.getResource().getFullPath(),
          projectContainer.getFile("lib/stuff.dart").getFullPath());
    }
  }

  public void test_resolveUriToFileInfoResourceInLib_Windows() throws Exception {
    if (DartCore.isWindows()) {
      testProject = new TestProject();
      if (testProject.getProject().exists()) {
        testProject.createFolder("lib");
        testProject.setFileContent("lib/stuff.dart", "library stuff;");
        testProject.setFileContent(PUBSPEC_FILE_NAME, "name:  myapp");
        testProject.createFolder(PACKAGES_DIRECTORY_NAME + "/myapp");
        testProject.setFileContent(PACKAGES_DIRECTORY_NAME + "/myapp/stuff.dart", "library stuff;");
        ProjectImpl projectimpl = new ProjectImpl(testProject.getProject(), sdk, sdkContextId);
        IFileInfo info = projectimpl.resolveUriToFileInfo(
            testProject.getProject(),
            "package:myapp/stuff.dart");
        assertNotNull(info);
        assertNotNull(info.getResource());
        assertTrue(info.getResource().getFullPath().toString().contains("/lib/"));
      }
    }
  }

  @Override
  protected ProjectImpl newTarget() {
    return new ProjectImpl(
        projectContainer,
        sdk,
        sdkContextId,
        index,
        new AnalysisContextFactory() {
          @Override
          public AnalysisContext createContext() {
            InternalAnalysisContext context = new MockContextForTest();
            if (useInstrumentedContexts) {
              context = new InstrumentedAnalysisContextImpl(context);
            }
            return context;
          }

          @Override
          public File[] getPackageRoots(IContainer container) {
            return packageRoots;
          }
        });
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    projectContainer = TestProjects.newPubProject3();
    webContainer = projectContainer.getMockFolder("web");
    subContainer = webContainer.getMockFolder("sub");
    appContainer = projectContainer.getMockFolder("myapp");
    subAppContainer = appContainer.getMockFolder("subApp");
    index = mock(Index.class);
  }

  @Override
  protected void tearDown() throws Exception {
    File packages = projectContainer.getFolder("packages").getLocation().toFile();
    File lib = projectContainer.getFolder("lib").getLocation().toFile();

    if (packages.exists()) {
      FileUtilities.delete(packages);
    }
    if (lib.exists()) {
      FileUtilities.delete(lib);
    }

    if (testProject != null && testProject.getProject().exists()) {
      testProject.dispose();
    }
  }

  private void assertDartSdkFactoryInitialized(MockContainer container, AnalysisContext context) {
    SourceFactory factory = context.getSourceFactory();
    File file1 = container.getFile(new Path("doesNotExist1.dart")).getLocation().toFile();
    Source source1 = new FileBasedSource(file1);

    Source source2 = factory.resolveUri(source1, "doesNotExist2.dart");
    File file2 = new File(source2.getFullName());
    assertEquals(file1.getParent(), file2.getParent());

    Source source3 = factory.resolveUri(source1, "package:doesNotExist3/doesNotExist4.dart");
    assertNotNull(source3);
    assertFalse(new File(source3.getFullName()).exists());
  }

  private void assertFactoryInitialized(MockContainer container, AnalysisContext context) {
    SourceFactory factory = context.getSourceFactory();
    File file1 = container.getFile(new Path("doesNotExist1.dart")).getLocation().toFile();
    Source source1 = new FileBasedSource(file1);

    Source source2 = factory.resolveUri(source1, "doesNotExist2.dart");
    File file2 = new File(source2.getFullName());
    assertEquals(file1.getParent(), file2.getParent());

    Source source3 = factory.resolveUri(source1, "package:doesNotExist3/doesNotExist4.dart");
    File file3 = new File(source3.getFullName());
    assertEquals("doesNotExist4.dart", file3.getName());
    File parent3 = file3.getParentFile();
    assertEquals("doesNotExist3", parent3.getName());
  }

  private void assertUriDoesNotResolve(Project project) {
    SourceFactory factory = project.getDefaultContext().getSourceFactory();
    Source source = factory.forUri("package:foo/foo.dart");
    assertNotNull(source);
    assertFalse(new File(source.getFullName()).exists());
  }
}
