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
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectEvent;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
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

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;

public class ProjectManagerImplTest extends TestCase {

  private final class MockContextForTest extends MockContext {
    @Override
    public Source[] getLibrariesContaining(Source source) {
      if (source.getShortName().equals("libraryA.dart")) {
        return new Source[] {source};
      }

      return new Source[] {};
    }

    @Override
    public LibraryElement getLibraryElement(Source source) {
      if (source.getShortName().equals("libraryA.dart")) {
        return library(this, "libraryA");
      }
      return null;
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

    public MockProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk,
        DartIgnoreManager ignoreManager) {
      super(resource, sdk, ignoreManager);
    }

    @Override
    public Project getProject(IProject project) {
      if (project == projectContainer) {
        Project result = projects.get(project);
        if (result == null) {
          result = new ProjectImpl(projectContainer, expectedSdk, new AnalysisContextFactory() {
            @Override
            public AnalysisContext createContext() {
              return context;
            }
          });
          projects.put(projectContainer, result);
        }
        return result;
      }
      return super.getProject(project);
    }
  }

  private MockWorkspaceRoot rootContainer;
  private MockProject projectContainer;
  private ProjectManager manager;
  private DartSdk expectedSdk;
  private DartIgnoreManager ignoreManager = new DartIgnoreManager();
  private MockContext context;

  public void test_getContext() {
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    Project project = manager.getProject(projectContainer);
    AnalysisContext expected = project.getContext(resource);
    AnalysisContext actual = manager.getContext(resource);
    assertNotNull(actual);
    assertSame(expected, actual);
  }

  public void test_getHtmlFileForLibrary() {
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
    assertSame(ignoreManager, manager.getIgnoreManager());
  }

  public void test_getIndex() throws Exception {
    Index index = manager.getIndex();
    assertNotNull(index);
    assertSame(index, manager.getIndex());
  }

  public void test_getLaunchableClientLibrarySources() {
    // TODO(keertip): complete implementation when API is available 
    Source[] sources = manager.getLaunchableClientLibrarySources();
    assertTrue(sources.length == 0);
    MockFolder folder = projectContainer.getMockFolder("web");
    MockFile clientfile = new MockFile(
        folder,
        "client.dart",
        "library client;\nimport 'dart:html'\n\n main(){}");
    folder.add(clientfile);
    sources = manager.getLaunchableClientLibrarySources();
//    assertTrue(sources.length == 1);
  }

  public void test_getLaunchableServerLibrarySources() {
    // TODO(keertip): complete implementation when API is available
    Source[] sources = manager.getLaunchableServerLibrarySources();
    assertTrue(sources.length == 0);
    MockFolder folder = projectContainer.getMockFolder("web");
    MockFile file = new MockFile(folder, "server.dart", "library server;\n\n main(){}");
    folder.add(file);
    sources = manager.getLaunchableServerLibrarySources();
//    assertTrue(sources.length == 1);
  }

  public void test_getLibraries() {
    Project actual = manager.getProject(projectContainer);
    MockFolder mockFolder = projectContainer.getMockFolder("web");
    MockFile file = new MockFile(mockFolder, "libraryA.dart", "library libraryA;\n\n main(){}");
    mockFolder.add(file);
    LibraryElement[] libraries = manager.getLibraries(mockFolder);
    LibraryElement[] elements = actual.getLibraries(mockFolder);
    assertEquals(elements.length, libraries.length);
    assertEquals(elements[0], libraries[0]);
  }

  public void test_getLibrarySources() {
    MockFolder mockFolder = projectContainer.getMockFolder("web");
    MockFile file = new MockFile(mockFolder, "libraryA.dart", "library libraryA;\n\n main(){}");
    mockFolder.add(file);
    Source[] sources = manager.getLibrarySources(mockFolder);
    Source[] libraries = manager.getLibrarySources(file);
    assertEquals(1, sources.length);
    assertEquals(sources.length, libraries.length);
    assertEquals(sources[0].getShortName(), libraries[0].getShortName());

  }

  public void test_getProject() {
    Project actual = manager.getProject(projectContainer);
    assertNotNull(actual);
    assertSame(projectContainer, actual.getResource());
  }

  public void test_getProjects() {
    Project[] actual = manager.getProjects();
    assertNotNull(actual);
    assertEquals(1, actual.length);
    assertNotNull(actual[0]);
    assertSame(manager.getProject(projectContainer), actual[0]);
  }

  public void test_getPubFolder() {
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    Project project = manager.getProject(projectContainer);
    PubFolder expected = project.getPubFolder(resource);
    PubFolder actual = manager.getPubFolder(resource);
    assertNotNull(actual);
    assertSame(expected, actual);
  }

  public void test_getPubFolder_project() {
    IResource resource = projectContainer;
    Project project = manager.getProject(projectContainer);
    PubFolder expected = project.getPubFolder(resource);
    PubFolder actual = manager.getPubFolder(resource);
    assertNotNull(actual);
    assertSame(expected, actual);
  }

  public void test_getResource() {
    assertSame(rootContainer, manager.getResource());
  }

  public void test_getResource_Source() {
    IResource resource = projectContainer.getFolder("web").getFile("other.dart");
    File file = resource.getLocation().toFile();
    Project project = manager.getProject(projectContainer);
    Source source = new FileBasedSource(project.getDefaultContext().getSourceFactory(), file);
    assertSame(resource, manager.getResource(source));
  }

  public void test_getResource_Source_null() {
    assertNull(manager.getResource(null));
  }

  public void test_getResource_Source_outside() {
    File file = new File("/does/not/exist.dart");
    Project project = manager.getProject(projectContainer);
    Source source = new FileBasedSource(project.getDefaultContext().getSourceFactory(), file);
    assertNull(manager.getResource(source));
  }

  public void test_getSdk() throws Exception {
    final DartSdk sdk = manager.getSdk();
    assertNotNull(sdk);
    assertSame(expectedSdk, sdk);
  }

  public void test_listener() throws Exception {
    Project project = manager.getProject(projectContainer);
    MockProjectListener listener = new MockProjectListener();
    manager.addProjectListener(listener);
    listener.assertNoProjectsAnalyzed();
    manager.projectAnalyzed(project);
    listener.assertProjectAnalyzed(project);
  }

  public void test_newSearchEngine() throws Exception {
    assertNotNull(manager.newSearchEngine());
  }

  @Override
  protected void setUp() throws Exception {
    rootContainer = new MockWorkspaceRoot();
    projectContainer = TestProjects.newPubProject3(rootContainer);
    rootContainer.add(projectContainer);
    expectedSdk = mock(DartSdk.class);
    manager = new MockProjectManagerImpl(rootContainer, expectedSdk, ignoreManager);
    context = new MockContextForTest();
  }
}
