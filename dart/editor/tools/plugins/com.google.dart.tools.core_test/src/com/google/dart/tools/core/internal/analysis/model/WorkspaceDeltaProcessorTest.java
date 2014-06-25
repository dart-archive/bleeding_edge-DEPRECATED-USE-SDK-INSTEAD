/*
 * Copyright (c) 2013, the Dart project authors.
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
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.internal.index.file.MemoryNodeManager;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.builder.MockContext;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockFolder;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockResourceChangeEvent;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;

import java.io.File;

public class WorkspaceDeltaProcessorTest extends TestCase {

  /**
   * Mock {@link Project} for testing {@link WorkspaceDeltaProcessor}
   */
  private class MockProjectImpl extends ProjectImpl {

    public MockProjectImpl(IProject resource, DartSdk sdk, String sdkContextId, Index index) {
      super(resource, sdk, sdkContextId, index, new AnalysisContextFactory() {
        @Override
        public AnalysisContext createContext() {
          return new MockContext();
        }

        @Override
        public File[] getPackageRoots(IContainer container) {
          return new File[] {};
        }
      });
    }
  }

  /**
   * Mock {@link ProjectManager} for testing {@link WorkspaceDeltaProcessor}
   */
  private final class MockProjectManagerImpl extends ProjectManagerImpl {
    private IProject projectRemoved;

    public MockProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk, String sdkContextId,
        DartIgnoreManager ignoreManager) {
      super(
          resource,
          sdk,
          sdkContextId,
          IndexFactory.newIndex(IndexFactory.newSplitIndexStore(new MemoryNodeManager())),
          ignoreManager);
    }

    public void assertProjectRemoved(Project expectedProject) {
      IProject expectedResource = expectedProject != null ? expectedProject.getResource() : null;
      assertEquals(expectedResource, projectRemoved);
    }

    @Override
    public Project getProject(IProject resource) {
      if (projectContainer != resource) {
        fail("Unexpected call: " + resource);
      }
      return project;
    }

    @Override
    public Project[] getProjects() {
      return new Project[] {project};
    }

    @Override
    public void projectRemoved(IProject projectResource) {
      super.projectRemoved(projectResource);
      if (projectRemoved == null) {
        projectRemoved = projectResource;
      } else {
        fail("project already removed");
      }
    }
  }

  private class Target extends WorkspaceDeltaProcessor {
    private boolean backgroundAnalysisStarted = false;

    Target(ProjectManager manager) {
      super(manager);
    }

    public void assertBackgroundAnalysisStarted(boolean expected) {
      assertEquals(expected, backgroundAnalysisStarted);
    }

    @Override
    protected void startBackgroundAnalysis(Project project, AnalysisContext context) {
      backgroundAnalysisStarted = true;
    }
  }

  private MockWorkspaceRoot rootContainer;
  private MockProject projectContainer;
  private MockProjectManagerImpl manager;
  private Project project;
  private Target processor;

  public void test_resourceChanged_file() {
    MockFile file = projectContainer.getMockFile("some.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(file);
    processor.resourceChanged(new MockResourceChangeEvent(delta));

    MockContext context = (MockContext) project.getDefaultContext();
    context.assertNoCalls();
    manager.assertProjectRemoved(null);
    processor.assertBackgroundAnalysisStarted(false);
  }

  public void test_resourceChanged_file_in_package() {
    MockFolder packages = projectContainer.getMockFolder(DartCore.PACKAGES_DIRECTORY_NAME);
    MockFolder pkg1 = packages.getMockFolder("pkg1");
    MockFile file = pkg1.getMockFile("bar.dart");
    MockDelta delta = new MockDelta(projectContainer);
    delta.add(packages).add(pkg1).add(file);
    processor.resourceChanged(new MockResourceChangeEvent(delta));

    ChangeSet expected = new ChangeSet();
    expected.changedSource(new FileBasedSource(file.getLocation().toFile()));
    MockContext context = (MockContext) project.getDefaultContext();
    context.assertChanged(expected);
    context.assertNoCalls();
    manager.assertProjectRemoved(null);
    processor.assertBackgroundAnalysisStarted(true);
  }

  public void test_resourceChanged_project_removed() {
    MockDelta delta = new MockDelta(projectContainer, IResourceDelta.REMOVED);
    processor.resourceChanged(new MockResourceChangeEvent(delta));

    MockContext context = (MockContext) project.getDefaultContext();
    context.assertNoCalls();
    manager.assertProjectRemoved(project);
    processor.assertBackgroundAnalysisStarted(false);
  }

  @Override
  protected void setUp() throws Exception {
    rootContainer = new MockWorkspaceRoot();
    projectContainer = TestProjects.newPubProject3(rootContainer);
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    String sdkContextId = "sdk-id";
    manager = new MockProjectManagerImpl(rootContainer, sdk, sdkContextId, new DartIgnoreManager());
    project = new MockProjectImpl(projectContainer, sdk, sdkContextId, manager.getIndex());
    processor = new Target(manager);
  }
}
