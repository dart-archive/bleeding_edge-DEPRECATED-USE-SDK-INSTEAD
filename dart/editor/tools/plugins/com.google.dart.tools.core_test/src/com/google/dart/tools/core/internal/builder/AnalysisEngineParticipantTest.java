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
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.internal.analysis.model.ProjectImpl;
import com.google.dart.tools.core.internal.analysis.model.ProjectManagerImpl;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockDelta;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspace;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import static com.google.dart.tools.core.internal.builder.TestProjects.MONITOR;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;

import java.io.File;
import java.util.ArrayList;

public class AnalysisEngineParticipantTest extends AbstractDartCoreTest {

  /**
   * Mock {@link AnalysisContext} for testing {@link AnalysisEngineParticipant}
   */
  private class MockAnalysisContextImpl extends AnalysisContextImpl {
    private ArrayList<Source> added = new ArrayList<Source>();
    private ArrayList<Source> changed = new ArrayList<Source>();

    @Override
    public void applyChanges(ChangeSet changeSet) {
      added.addAll(changeSet.getAdded());
      changed.addAll(changeSet.getChanged());
      super.applyChanges(changeSet);
    }

    public void assertApplyChanges(Source[] sourcesAdded, Source[] sourcesChanged) {
      assertEqualContents(sourcesAdded, added);
      assertEqualContents(sourcesChanged, changed);
      added.clear();
      changed.clear();
    }

    private void assertEqualContents(Source[] expected, ArrayList<Source> actual) {
      if (expected.length == actual.size()) {
        boolean isEqual = true;
        for (Source source : expected) {
          if (!actual.contains(source)) {
            isEqual = false;
            break;
          }
        }
        if (isEqual) {
          return;
        }
      }
      PrintStringWriter msg = new PrintStringWriter();
      msg.println("Expected:");
      for (Source source : expected) {
        msg.println("    " + source);
      }
      msg.println("Actual:");
      for (Source source : actual) {
        msg.println("    " + source);
      }
      fail(msg.toString().trim());
    }
  }

  /**
   * Mock {@link Project} for testing {@link AnalysisEngineParticipant}
   */
  private class MockProjectImpl extends ProjectImpl {

    public MockProjectImpl(IProject resource, DartSdk sdk, Index index) {
      super(resource, sdk, index, new AnalysisContextFactory() {
        @Override
        public AnalysisContext createContext() {
          return new MockAnalysisContextImpl();
        }

        @Override
        public File[] getPackageRoots(IContainer container) {
          return new File[] {};
        }
      });
    }
  }

  /**
   * Mock {@link ProjectManager} for testing {@link AnalysisEngineParticipant}
   */
  private final class MockProjectManagerImpl extends ProjectManagerImpl {
    private final ArrayList<Project> analyzed = new ArrayList<Project>();

    public MockProjectManagerImpl(IWorkspaceRoot resource, DartSdk sdk,
        DartIgnoreManager ignoreManager) {
      super(resource, sdk, ignoreManager);
    }

    public void assertProjectAnalyzed() {
      assertEquals(1, analyzed.size());
      assertSame(project, analyzed.get(0));
      analyzed.clear();
    }

    @Override
    public Project getProject(IProject resource) {
      if (projectRes != resource) {
        fail("Unexpected call: " + resource);
      }
      return project;
    }

    @Override
    public Project[] getProjects() {
      return new Project[] {project};
    }

    @Override
    public void projectAnalyzed(Project project) {
      analyzed.add(project);
    }
  }

  private MockWorkspace workspace;
  private MockWorkspaceRoot rootRes;
  private MockProject projectRes;
  private MockFile fileRes;

  private AnalysisMarkerManager markerManager;

  private DartSdk sdk;
  private MockProjectManagerImpl manager;
  private MockProjectImpl project;
  private Source fileSource;

  private AnalysisEngineParticipant participant;

  public void test_build() throws Exception {

    // new project
    MockDelta delta = null;
    participant.build(new BuildEvent(projectRes, delta, MONITOR), MONITOR);

    manager.assertProjectAnalyzed();
    ((MockAnalysisContextImpl) project.getDefaultContext()).assertApplyChanges(
        new Source[] {fileSource},
        new Source[] {});
    markerManager.waitForMarkers(10000);
    fileRes.assertMarkersDeleted();
    fileRes.assertMarkersNotDeleted();

    // file in project changed
    delta = new MockDelta(projectRes);
    delta.add(fileRes);
    participant.build(new BuildEvent(projectRes, delta, MONITOR), MONITOR);

    manager.assertProjectAnalyzed();
    ((MockAnalysisContextImpl) project.getDefaultContext()).assertApplyChanges(
        new Source[] {},
        new Source[] {fileSource});
    markerManager.waitForMarkers(10000);
    fileRes.assertMarkersDeleted();
  }

  @Override
  protected void setUp() throws Exception {
    workspace = new MockWorkspace();
    rootRes = workspace.getRoot();
    projectRes = rootRes.addProject(getClass().getSimpleName());
    String fileContents = "library a;#";
    fileRes = projectRes.addFile("a.dart", fileContents);

    markerManager = new AnalysisMarkerManager(workspace);

    sdk = DirectoryBasedDartSdk.getDefaultSdk();
    manager = new MockProjectManagerImpl(rootRes, sdk, new DartIgnoreManager());
    project = new MockProjectImpl(projectRes, sdk, manager.getIndex());

    AnalysisContext context = project.getDefaultContext();
    File file = fileRes.getLocation().toFile();
    SourceFactory factory = context.getSourceFactory();
    fileSource = new FileBasedSource(factory.getContentCache(), file);
    factory.setContents(fileSource, fileContents);

    participant = new AnalysisEngineParticipant(true, manager, markerManager) {
      @Override
      protected void performAnalysis(AnalysisWorker worker) {
        worker.performAnalysis();
      }
    };
  }
}
