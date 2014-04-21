/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl.AnalysisContextFactory;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.internal.model.DartIgnoreFile;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockResource;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ProjectManagerIgnoreListenerTest extends TestCase {

  private final class MockIgnoreFile extends DartIgnoreFile {

    private MockIgnoreFile() {
      super(new File("/tmp/does/not/exist/ignores.txt"));
    }

    @Override
    public void initFile() throws IOException {
      // do not write to disk
    }

    @Override
    public DartIgnoreFile load() throws IOException {
      // do not read from disk
      return this;
    }

    @Override
    public DartIgnoreFile store() throws IOException {
      // do not write to disk
      return this;
    }
  }

  private DartIgnoreManager ignoreManager;
  private MockWorkspaceRoot rootContainer;
  private MockProject projectContainer;
  private MockFolder appFolder;
  private AnalysisManager analysisManager;
  private AnalysisMarkerManager markerManager;
  private ProjectManager projectManager;
  private Project project;
  private ProjectImpl projectImpl;
  private MockContext projectContext;
  private MockContext appContext;
  private ProjectManagerIgnoreListener listener;
  private Index index;

  public void test_ignoreAppWithSingleContexts() throws Exception {
    clearInteractions();
    ignoreManager.addToIgnores(appFolder.getLocation());
    assertIgnored(appFolder, appContext);
    ignoreManager.removeFromIgnores(appFolder.getLocation());
    assertAnalyzed(appFolder, appContext);
  }

  public void test_ignoreFileInApp() throws Exception {
    MockFile file = appFolder.getMockFolder("lib").getMockFile("stuff.dart");
    clearInteractions();
    ignoreManager.addToIgnores(file.getLocation());
    assertIgnored(file, appContext);
    ignoreManager.removeFromIgnores(file.getLocation());
    assertAnalyzed(file, appContext);
  }

  public void test_ignoreFileInProject() throws Exception {
    MockFile file = projectContainer.getMockFile("some.dart");
    clearInteractions();
    ignoreManager.addToIgnores(file.getLocation());
    assertIgnored(file, projectContext);
    ignoreManager.removeFromIgnores(file.getLocation());
    assertAnalyzed(file, projectContext);
  }

  public void test_ignoreFolderInApp() throws Exception {
    MockFolder folder = appFolder.getMockFolder("lib");
    clearInteractions();
    ignoreManager.addToIgnores(folder.getLocation());
    assertIgnored(folder, appContext);
    ignoreManager.removeFromIgnores(folder.getLocation());
    assertAnalyzed(folder, appContext);
  }

  public void test_ignoreFolderInProject() throws Exception {
    MockFolder folder = projectContainer.getMockFolder("web");
    clearInteractions();
    ignoreManager.addToIgnores(folder.getLocation());
    assertIgnored(folder, projectContext);
    ignoreManager.removeFromIgnores(folder.getLocation());
    assertAnalyzed(folder, projectContext);
  }

  public void test_ignoreProjectWithMultipleContexts() throws Exception {
    clearInteractions();
    ignoreManager.addToIgnores(projectContainer.getLocation());

    appContext.assertChanged(null, null, new IResource[] {projectContainer});
    verify(index).removeSources(appContext, projectContainer.asSourceContainer());
    assertIgnored(projectContainer, projectContext);

    ignoreManager.removeFromIgnores(projectContainer.getLocation());

    IResource[] projectFiles = projectContainer.getAllDartAndHtmlFiles();
    IResource[] appFiles = appFolder.getAllDartAndHtmlFiles();
    ArrayList<IResource> diff = new ArrayList<IResource>();
    diff.addAll(Arrays.asList(projectFiles));
    diff.removeAll(Arrays.asList(appFiles));
    projectFiles = diff.toArray(new IResource[diff.size()]);

    verify(markerManager).clearMarkers(projectContainer);
    for (IResource file : projectFiles) {
      verify(markerManager).queueErrors(eq(file), any(LineInfo.class), any(AnalysisError[].class));
    }
    for (IResource file : appFiles) {
      verify(markerManager).queueErrors(eq(file), any(LineInfo.class), any(AnalysisError[].class));
    }
    verify(analysisManager).performAnalysisInBackground(project, projectContext);
    verify(analysisManager).performAnalysisInBackground(project, appContext);
    projectContext.assertChanged(projectFiles, null, null);
    appContext.assertChanged(appFiles, null, null);
    assertNoMoreInteractions();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ignoreManager = new DartIgnoreManager(new MockIgnoreFile());
    rootContainer = new MockWorkspaceRoot();
    projectContainer = TestProjects.newPubProject3(rootContainer);
    projectContainer.remove(DartCore.PUBSPEC_FILE_NAME);
    rootContainer.add(projectContainer);
    appFolder = projectContainer.getMockFolder("myapp");
    assertNotNull(appFolder);
    markerManager = mock(AnalysisMarkerManager.class);
    DartSdk sdk = mock(DartSdk.class);
    String sdkContextId = "sdk-id";
    index = mock(Index.class);
    analysisManager = mock(AnalysisManager.class);

    projectImpl = new ProjectImpl(
        projectContainer,
        sdk,
        sdkContextId,
        index,
        new AnalysisContextFactory() {
          @Override
          public AnalysisContext createContext() {
            return new MockContext();
          }

          @Override
          public File[] getPackageRoots(IContainer container) {
            return new File[] {};
          }
        });

    DartIgnoreManager unused = mock(DartIgnoreManager.class);
    projectManager = new ProjectManagerImpl(rootContainer, sdk, sdkContextId, unused) {
      @Override
      public Project getProject(org.eclipse.core.resources.IProject resource) {
        return resource == projectContainer ? projectImpl : null;
      };
    };

    listener = new ProjectManagerIgnoreListener(
        projectManager,
        rootContainer,
        analysisManager,
        markerManager,
        index);
    ignoreManager.addListener(listener);

    projectContext = (MockContext) projectManager.getContext(projectContainer);
    appContext = (MockContext) projectManager.getContext(appFolder);
    assertNotNull(projectContext);
    assertNotNull(appContext);
    assertNotSame(projectContext, appContext);

    project = projectManager.getProject(projectContainer);
    assertNotNull(project);
  }

  private void assertAnalyzed(MockResource res, MockContext context) {
    IResource[] allDartAndHtmlFiles;
    if (res instanceof MockFolder) {
      allDartAndHtmlFiles = ((MockFolder) res).getAllDartAndHtmlFiles();
    } else {
      allDartAndHtmlFiles = new IResource[] {res};
    }
    verify(markerManager).clearMarkers(res);
    for (IResource file : allDartAndHtmlFiles) {
      verify(markerManager).queueErrors(eq(file), any(LineInfo.class), any(AnalysisError[].class));
    }
    verify(analysisManager).performAnalysisInBackground(project, context);
    context.assertChanged(allDartAndHtmlFiles, null, null);
    assertNoMoreInteractions();
  }

  private void assertIgnored(MockResource res, MockContext context) {
    verify(markerManager).clearMarkers(res);
    context.assertChanged(null, null, new IResource[] {res});
    if (res instanceof MockFile) {
      verify(index).removeSource(context, ((MockFile) res).asSource());
    }
    if (res instanceof MockContainer) {
      verify(index).removeSources(context, ((MockContainer) res).asSourceContainer());
    }
    assertNoMoreInteractions();
  }

  private void assertNoMoreInteractions() {
    verifyNoMoreInteractions(markerManager);
    verifyNoMoreInteractions(analysisManager);
    verifyNoMoreInteractions(index);
    projectContext.assertNoCalls();
    appContext.assertNoCalls();
  }

  private void clearInteractions() {
    verifyNoMoreInteractions(markerManager);
    verifyNoMoreInteractions(analysisManager);
    projectContext.clearCalls();
    appContext.clearCalls();
  }
}
