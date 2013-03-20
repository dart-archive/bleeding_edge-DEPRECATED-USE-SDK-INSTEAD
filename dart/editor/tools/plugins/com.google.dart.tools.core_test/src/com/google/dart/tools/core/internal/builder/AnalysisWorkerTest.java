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
//
//  private static final class MockContext extends AnalysisContextImpl {
//    private ArrayList<Source> sourcesToAnalyze = new ArrayList<Source>();
//
//    @Override
//    public void applyChanges(ChangeSet changeSet) {
//      super.applyChanges(changeSet);
//      sourcesToAnalyze.addAll(changeSet.getAddedWithContent().keySet());
//    }
//
//    @Override
//    public ChangeNotice[] performAnalysisTask() {
//      if (sourcesToAnalyze.size() == 0) {
//        return null;
//      }
//      Source source = sourcesToAnalyze.remove(0);
//      CompilationUnit unit = null;
//      try {
//        unit = parse(source);
//      } catch (AnalysisException e) {
//        fail("Failed to parse " + source + " : " + e);
//      }
//      return new ChangeNotice[] {
//          new ChangeNotice(source, unit),
//          new ChangeNotice(source, unit.getErrors(), unit.getLineInfo())};
//    }
//  }

  public void test_performAnalysis() throws Exception {
    MockWorkspace workspace = new MockWorkspace();
    MockWorkspaceRoot rootRes = (MockWorkspaceRoot) workspace.getRoot();
    MockProject projectRes = rootRes.add(new MockProject(rootRes, getClass().getSimpleName()));
    MockFile fileRes = projectRes.add(new MockFile(projectRes, "a.dart", "library a;#"));

    DartSdk sdk = DartSdk.getDefaultSdk();
    ProjectManager manager = new ProjectManagerImpl(rootRes, sdk, new DartIgnoreManager());
    Project project = manager.getProject(projectRes);
    AnalysisContext context = project.getDefaultContext();

    File file = fileRes.getLocation().toFile();
    FileBasedSource source = new FileBasedSource(context.getSourceFactory(), file);
    ChangeSet changes = new ChangeSet();
    changes.added(source, "library a;#");
    context.applyChanges(changes);

    Index index = mock(Index.class);

    AnalysisWorker worker = new AnalysisWorker(project, context, index);
    worker.performAnalysis();

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
