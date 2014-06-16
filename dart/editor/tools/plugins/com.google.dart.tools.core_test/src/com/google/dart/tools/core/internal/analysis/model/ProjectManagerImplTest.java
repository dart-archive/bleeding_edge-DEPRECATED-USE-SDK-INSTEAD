/*
 * Copyright 2013 Dart project authors.
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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.internal.index.file.MemoryNodeManager;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectEvent;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.PubFolder;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl.AnalysisContextFactory;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import static com.google.dart.engine.element.ElementFactory.library;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectManagerImplTest extends ContextManagerImplTest {

  private final class MockContextForTest extends MockContext {

    private List<Source> sources = new ArrayList<Source>();

    @Override
    public LibraryElement computeLibraryElement(Source source) {
      if (source.getShortName().equals("libraryA.dart")) {
        return library(this, "libraryA");
      }
      return null;
    }

    @Override
    public Source[] getLibrariesContaining(Source source) {
      if (source.getShortName().equals("libraryA.dart")) {
        sources.add(source);
        return new Source[] {source};
      }
      return Source.EMPTY_ARRAY;
    }

    @Override
    public Source[] getLibrarySources() {
      return sources.toArray(new Source[sources.size()]);
    }

    @Override
    public boolean isClientLibrary(Source librarySource) {
      if (librarySource.getShortName().equals("libraryA.dart")) {
        return true;
      }
      return false;
    }

    @Override
    public boolean isServerLibrary(Source librarySource) {
      if (librarySource.getShortName().equals("libraryB.dart")) {
        return true;
      }
      return false;
    }

  }

  private final class MockProjectListener implements ProjectListener {
    private final ArrayList<Project> analyzed = new ArrayList<Project>();

    public void assertNoProjectsAnalyzed() {
      assertEquals(0, analyzed.size());
    }

    public void assertProjectAnalyzed(Project project) {
      assertEquals(1, analyzed.size());
      assertSame(project, analyzed.get(0));
    }

    @Override
    public void projectAnalyzed(ProjectEvent event) {
      analyzed.add(event.getProject());
    }
  }

  private final class MockProjectManagerImpl extends ProjectManagerImpl {

    private Project projectUnderTest;

    public MockProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk, String sdkContextId,
        DartIgnoreManager ignoreManager) {
      super(
          resource,
          sdk,
          sdkContextId,
          IndexFactory.newIndex(IndexFactory.newSplitIndexStore(new MemoryNodeManager())),
          ignoreManager);
    }

    @Override
    public Project getProject(IProject project) {
      if (project == projectContainer) {
        if (projectUnderTest == null) {
          projectUnderTest = new ProjectImpl(
              projectContainer,
              sdk,
              sdkContextId,
              getIndex(),
              new AnalysisContextFactory() {
                @Override
                public AnalysisContext createContext() {
                  return context;
                }
              });
        }
        return projectUnderTest;
      }
      return super.getProject(project);
    }
  }

  private MockWorkspaceRoot rootContainer;
  private MockProject projectContainer;
  private DartIgnoreManager ignoreManager = new DartIgnoreManager();
  private MockContext context;

  public void test_getContext() {
    MockProjectManagerImpl manager = newTarget();
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    Project project = manager.getProject(projectContainer);
    AnalysisContext expected = project.getContext(resource);
    AnalysisContext actual = manager.getContext(resource);
    assertNotNull(actual);
    assertSame(expected, actual);
  }

  public void test_getHtmlFileForLibrary() {
//    MockProjectManagerImpl manager = newTarget();
    //TODO(keertip): finish when context api has been implemented
    MockFolder mockFolder = projectContainer.getMockFolder("web");
    MockFile file = new MockFile(mockFolder, "libraryA.dart", "library libraryA;\n\n main(){}");
    mockFolder.add(file);
    MockFile htmlfile = new MockFile(mockFolder, "<!DOCTYPE html>\n<html><body> +"
        + " <script type=\"application/dart\" src=\"libraryA.dart\"></script>\n" + "</body></html>");
    mockFolder.add(htmlfile);
    MockFile libFile = new MockFile(mockFolder, "libraryB.dart", "library libraryB;\n\n main(){}");
    mockFolder.add(libFile);
//    IResource resource = manager.getHtmlFileForLibrary(manager.getSource(file));
//    assertNotNull(resource);
//    assertEquals(htmlfile, resource);
//    assertNull(manager.getHtmlFileForLibrary(manager.getSource(libFile)));

  }

  public void test_getIgnoreManager() throws Exception {
    MockProjectManagerImpl manager = newTarget();
    assertSame(ignoreManager, manager.getIgnoreManager());
  }

  public void test_getIndex() throws Exception {
    MockProjectManagerImpl manager = newTarget();
    Index index = manager.getIndex();
    assertNotNull(index);
    assertSame(index, manager.getIndex());
  }

  public void test_getLibrarySources() {
    MockProjectManagerImpl manager = newTarget();
    MockFolder mockFolder = projectContainer.getMockFolder("web");
    MockFile file = new MockFile(mockFolder, "libraryA.dart", "library libraryA;\n\n main(){}");
    mockFolder.add(file);
    Source[] libraries = manager.getLibrarySources(file);
    Source[] sources = manager.getLibrarySources(mockFolder.getProject());
    assertEquals(1, sources.length);
    assertEquals(sources.length, libraries.length);
    assertEquals(sources[0].getShortName(), libraries[0].getShortName());
  }

  public void test_getProject() {
    MockProjectManagerImpl manager = newTarget();
    Project actual = manager.getProject(projectContainer);
    assertNotNull(actual);
    assertSame(projectContainer, actual.getResource());
  }

  public void test_getProjects() {
    MockProjectManagerImpl manager = newTarget();
    Project[] actual = manager.getProjects();
    assertNotNull(actual);
    assertEquals(1, actual.length);
    assertNotNull(actual[0]);
    assertSame(manager.getProject(projectContainer), actual[0]);
  }

  public void test_getPubFolder() {
    MockProjectManagerImpl manager = newTarget();
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    Project project = manager.getProject(projectContainer);
    PubFolder expected = project.getPubFolder(resource);
    PubFolder actual = manager.getPubFolder(resource);
    assertNotNull(actual);
    assertSame(expected, actual);
  }

  public void test_getPubFolder_project() {
    MockProjectManagerImpl manager = newTarget();
    IResource resource = projectContainer;
    Project project = manager.getProject(projectContainer);
    PubFolder expected = project.getPubFolder(resource);
    PubFolder actual = manager.getPubFolder(resource);
    assertNotNull(actual);
    assertSame(expected, actual);
  }

  public void test_getResource() {
    MockProjectManagerImpl manager = newTarget();
    assertSame(rootContainer, manager.getResource());
  }

  public void test_getResource_Source() {
    MockProjectManagerImpl manager = newTarget();
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    File file = resource.getLocation().toFile();
    manager.getProject(projectContainer);
    Source source = new FileBasedSource(file);
    assertSame(resource, manager.getResource(source));
  }

  public void test_getResource_Source_null() {
    MockProjectManagerImpl manager = newTarget();
    assertNull(manager.getResource(null));
  }

  public void test_getResource_Source_outside() {
    MockProjectManagerImpl manager = newTarget();
    File file = new File("/does/not/exist.dart");
    manager.getProject(projectContainer);
    Source source = new FileBasedSource(file);
    assertNull(manager.getResource(source));
  }

  @Override
  public void test_getSdkContext() throws Exception {
    MockProjectManagerImpl manager = newTarget();
    AnalysisContext sdkContext = manager.getSdkContext();
    assertNotNull(sdkContext);
    SourceFactory factory = sdkContext.getSourceFactory();
    assertNotNull(factory);
    Source source = factory.forUri("dart:core");
    assertNotNull(source);
    source = factory.forUri("package:foo/bar.dart");
    assertNull(source);
    source = factory.forUri("file:/does/not/exist.dart");
    assertNull(source);
  }

  public void test_isClientLibrary() {
    MockProjectManagerImpl manager = newTarget();
    MockFolder mockFolder = projectContainer.getMockFolder("web");
    MockFile file = new MockFile(
        mockFolder,
        "libraryA.dart",
        "library libraryA;\nimport 'dart:html';\n main(){}");
    mockFolder.add(file);
    manager.getProject(projectContainer);
    Source source = new FileBasedSource(file.toFile());
    boolean result = manager.isClientLibrary(source);
    assertTrue(result);
    result = manager.isServerLibrary(source);
    assertFalse(result);

  }

  public void test_isServerLibrary() {
    MockProjectManagerImpl manager = newTarget();
    MockFolder mockFolder = projectContainer.getMockFolder("web");
    MockFile serverFile = new MockFile(
        mockFolder,
        "libraryB.dart",
        "library libraryB;\nimport 'dart:io';\n main(){}");
    mockFolder.add(serverFile);
    manager.getProject(projectContainer);
    Source source = new FileBasedSource(serverFile.toFile());
    boolean result = manager.isClientLibrary(source);
    assertFalse(result);
    result = manager.isServerLibrary(source);
    assertTrue(result);

  }

  public void test_listener() throws Exception {
    MockProjectManagerImpl manager = newTarget();
    Project project = manager.getProject(projectContainer);
    MockProjectListener listener = new MockProjectListener();
    manager.addProjectListener(listener);
    listener.assertNoProjectsAnalyzed();
    manager.projectAnalyzed(project);
    listener.assertProjectAnalyzed(project);
  }

  public void test_newSearchEngine() throws Exception {
    MockProjectManagerImpl manager = newTarget();
    assertNotNull(manager.newSearchEngine());
  }

  public void test_resolveUriToFileInfo() {
    MockProjectManagerImpl manager = newTarget();
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    IFileInfo info = manager.resolveUriToFileInfo(
        projectContainer,
        resource.getLocation().toFile().toURI().toString());
    assertNotNull(info);
    assertNotNull(info.getResource());

    info = manager.resolveUriToFileInfo(projectContainer, "package:pkg1/build.dart");
    assertNotNull(info);
    assertNotNull(info.getResource());

    info = manager.resolveUriToFileInfo(projectContainer, "package:/doesnotexist/nofile.dart");
    assertNull(info);
  }

  @Override
  protected MockProjectManagerImpl newTarget() {
    return new MockProjectManagerImpl(rootContainer, sdk, sdkContextId, ignoreManager);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rootContainer = new MockWorkspaceRoot();
    projectContainer = TestProjects.newPubProject3(rootContainer);
    rootContainer.add(projectContainer);
    context = new MockContextForTest();
  }
}
