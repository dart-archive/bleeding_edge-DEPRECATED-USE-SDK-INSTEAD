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
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.internal.index.file.MemoryNodeManager;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl.AnalysisContextFactory;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.internal.model.MockIgnoreFile;
import com.google.dart.tools.core.mock.MockContainer;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockResource;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;

public class ProjectManagerIgnoreListenerTest extends TestCase {

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

    assertIgnored(projectContainer, projectContext);

    ignoreManager.removeFromIgnores(projectContainer.getLocation());

    MockFile[] allDartAndHtmlFiles;
    MockContainer container = projectContainer;
    for (MockFile file3 : container.getAllDartAndHtmlFiles()) {
      MockContext context = (MockContext) projectManager.getContext(file3);
      AnalysisLevel level = AnalysisLevel.ALL;
      if (inPackage(file3)) {
        if (inNestedPackage(file3)) {
          continue;
        }
        level = AnalysisLevel.RESOLVED;
      }
      context.assertAnalysisLevel(file3.asSource(), level);
    }
    allDartAndHtmlFiles = container.getAllDartAndHtmlFiles();
    verify(markerManager).clearMarkers(projectContainer);
    for (MockFile file1 : allDartAndHtmlFiles) {
      if (!inNestedPackage(file1)) {
        verify(markerManager).queueErrors(
            eq(file1),
            any(LineInfo.class),
            any(AnalysisError[].class));
      }
    }
    verify(analysisManager, atLeast(1)).performAnalysisInBackground(project, projectContext);
    verify(analysisManager, atLeast(1)).performAnalysisInBackground(project, appContext);
    assertNoMoreInteractions();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ignoreManager = new DartIgnoreManager(new MockIgnoreFile());
    rootContainer = new MockWorkspaceRoot();
    projectContainer = TestProjects.newPubProject3(rootContainer);
    projectContainer.remove(DartCore.PUBSPEC_FILE_NAME);
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
    projectManager = new ProjectManagerImpl(
        rootContainer,
        sdk,
        sdkContextId,
        IndexFactory.newIndex(IndexFactory.newSplitIndexStore(new MemoryNodeManager())),
        unused) {
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
    MockFile[] allDartAndHtmlFiles;
    if (res instanceof MockContainer) {
      MockContainer container = (MockContainer) res;
      for (MockFile file : container.getAllDartAndHtmlFiles()) {
        AnalysisLevel level = AnalysisLevel.ALL;
        if (inPackage(file)) {
          if (inNestedPackage(file)) {
            continue;
          }
          level = AnalysisLevel.RESOLVED;
        }
        context.assertAnalysisLevel(file.asSource(), level);
      }
      allDartAndHtmlFiles = container.getAllDartAndHtmlFiles();
    } else {
      MockFile file = (MockFile) res;
      context.assertAnalysisLevel(file, AnalysisLevel.ALL);
      allDartAndHtmlFiles = new MockFile[] {file};
    }
    verify(markerManager).clearMarkers(res);
    for (MockFile file : allDartAndHtmlFiles) {
      if (!inNestedPackage(file)) {
        verify(markerManager).queueErrors(eq(file), any(LineInfo.class), any(AnalysisError[].class));
      }
    }
    verify(analysisManager, atLeast(1)).performAnalysisInBackground(project, context);
    assertNoMoreInteractions();
  }

  private void assertIgnored(MockResource res, MockContext context) {
    verify(markerManager).clearMarkers(res);
    if (res instanceof MockContainer) {
      MockContainer container = (MockContainer) res;
      for (MockFile file : container.getAllDartAndHtmlFiles()) {
        Source source = file.asSource();
        if (source != null && !inNestedPackage(file)) {
          MockContext subContext = (MockContext) projectManager.getContext(file);
          verify(index).removeSource(subContext, source);
          subContext.assertAnalysisLevel(source, AnalysisLevel.NONE);
        }
      }
    } else {
      MockFile file = (MockFile) res;
      verify(index).removeSource(context, file.asSource());
      context.assertAnalysisLevel(file.asSource(), AnalysisLevel.NONE);
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

  private boolean inNestedPackage(MockFile file) {
    IContainer parent = file.getParent();
    while (parent != null) {
      if (parent.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
        parent = parent.getParent();
        if (parent != null) {
          try {
            IResource[] members = parent.members();
            for (IResource res : members) {
              if (res.getName().equals(DartCore.PUBSPEC_FILE_NAME)) {
                return false;
              }
            }
            return true;
          } catch (CoreException e) {
            throw new RuntimeException(e);
          }
        }
      }
      parent = parent.getParent();
    }
    return false;
  }

  private boolean inPackage(MockFile file) {
    IContainer parent = file.getParent();
    while (parent != null) {
      if (parent.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
        return true;
      }
      parent = parent.getParent();
    }
    return false;
  }
}
