package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.analysis.model.ProjectManagerImpl;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockResource;
import com.google.dart.tools.core.mock.MockWorkspace;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;

public class AnalysisWorkerTest extends TestCase {

  public void test_performAnalysis() throws Exception {
    MockWorkspace workspace = new MockWorkspace();
    MockWorkspaceRoot rootRes = (MockWorkspaceRoot) workspace.getRoot();
    MockProject projectRes = rootRes.add(new MockProject(rootRes, getClass().getSimpleName()));
    MockFile fileRes = projectRes.add(new MockFile(projectRes, "a.dart", "library a;#"));

    DartSdk sdk = mock(DartSdk.class);
    ProjectManager manager = new ProjectManagerImpl(rootRes, sdk, new DartIgnoreManager());
    Project project = manager.getProject(projectRes);
    AnalysisContext context = project.getDefaultContext();

    File file = fileRes.getLocation().toFile();
    FileBasedSource source = new FileBasedSource(context.getSourceFactory(), file);
    context.getSourceFactory().setContents(source, "library a;#");
    ChangeSet changes = new ChangeSet();
    changes.added(source);
    context.applyChanges(changes);

    Index index = mock(Index.class);

    AnalysisMarkerManager markerManager = new AnalysisMarkerManager(workspace);
    AnalysisWorker worker = new AnalysisWorker(project, context, index, markerManager);
    worker.performAnalysis();
    markerManager.waitForMarkers(10000);

    assertMarkersDeleted(fileRes);
    assertTrue(fileRes.getMarkers().size() > 0);

    verify(index, atLeastOnce()).indexUnit(eq(context), any(CompilationUnit.class));

    // TODO (danrubel): Assert no log entries once context only returns errors for added sources
  }

  private void assertMarkersDeleted(MockResource resource) {
    resource.getMarkerCallList().assertCall(
        resource,
        MockFile.DELETE_MARKERS,
        DartCore.DART_PROBLEM_MARKER_TYPE,
        true,
        IResource.DEPTH_ZERO);
  }
}
