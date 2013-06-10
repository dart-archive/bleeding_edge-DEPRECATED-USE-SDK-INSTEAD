package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ProjectEvent;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.internal.analysis.model.ProjectManagerImpl;
import com.google.dart.tools.core.internal.builder.AnalysisWorker.Event;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspace;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;

public class AnalysisWorkerTest extends TestCase {

  private MockWorkspace workspace;
  private MockWorkspaceRoot rootRes;
  private MockProject projectRes;
  private DartSdk sdk;
  private ProjectManager manager;
  private AnalysisContext context;
  private Project project;
  private AnalysisMarkerManager markerManager;
  private AnalysisWorker worker;
  private final ArrayList<Project> analyzedProjects = new ArrayList<Project>();

  private boolean resolveCalled;
  private final AnalysisWorker.Listener listener = new AnalysisWorker.Listener() {
    @Override
    public void resolved(Event event) {
      resolveCalled = true;
    }
  };

  public void test_performAnalysis() throws Exception {
    worker = new AnalysisWorker(project, context, manager, markerManager);
    resolveCalled = false;

    // Perform the analysis and wait for the results to flow through the marker manager
    MockFile fileRes = addLibrary();
    worker.performAnalysis();
    markerManager.waitForMarkers(10000);

    fileRes.assertMarkersDeleted();
    assertTrue(fileRes.getMarkers().size() > 0);
    assertTrue(resolveCalled);
    assertEquals(1, analyzedProjects.size());
    assertEquals(project, analyzedProjects.get(0));
    // TODO (danrubel): Assert no log entries once context only returns errors for added sources
  }

  public void test_performAnalysis_ignoredResource() throws Exception {
    worker = new AnalysisWorker(project, context, manager, markerManager);
    resolveCalled = false;

    // Perform the analysis and wait for the results to flow through the marker manager
    MockFile fileRes = addLibrary();
    DartCore.addToIgnores(fileRes);
    try {
      worker.performAnalysis();
      markerManager.waitForMarkers(10000);

      fileRes.assertMarkersNotDeleted();
      assertTrue(fileRes.getMarkers().size() == 0);
      assertFalse(resolveCalled);
    } finally {
      DartCore.removeFromIgnores(fileRes);
    }
  }

  public void test_performAnalysisInBackground() throws Exception {
    worker = new AnalysisWorker(project, context, manager, markerManager);
    resolveCalled = false;

    // Perform the analysis and wait for the results to flow through the marker manager
    MockFile fileRes = addLibrary();
    worker.performAnalysisInBackground();
    AnalysisWorker.waitForBackgroundAnalysis(10000);
    markerManager.waitForMarkers(10000);

    fileRes.assertMarkersDeleted();
    assertTrue(fileRes.getMarkers().size() > 0);
    assertTrue(resolveCalled);
    assertEquals(1, analyzedProjects.size());
    assertEquals(project, analyzedProjects.get(0));
  }

  public void test_stop() throws Exception {
    worker = new AnalysisWorker(project, context, manager, markerManager);
    resolveCalled = false;

    // Perform the analysis and wait for the results to flow through the marker manager
    MockFile fileRes = addLibrary();
    worker.stop();
    worker.performAnalysis();
    markerManager.waitForMarkers(50);

    fileRes.assertMarkersNotDeleted();
    assertTrue(fileRes.getMarkers().size() == 0);
    assertFalse(resolveCalled);
    assertEquals(0, analyzedProjects.size());
  }

  @Override
  protected void setUp() {
    workspace = new MockWorkspace();
    rootRes = workspace.getRoot();
    projectRes = rootRes.add(new MockProject(rootRes, getClass().getSimpleName()));

//    sdk = mock(DartSdk.class);
    sdk = DirectoryBasedDartSdk.getDefaultSdk();
    manager = new ProjectManagerImpl(rootRes, sdk, new DartIgnoreManager());
    manager.addProjectListener(new ProjectListener() {
      @Override
      public void projectAnalyzed(ProjectEvent event) {
        analyzedProjects.add(event.getProject());
      }
    });
    project = manager.getProject(projectRes);
    context = project.getDefaultContext();

    markerManager = new AnalysisMarkerManager(workspace);
    AnalysisWorker.addListener(listener);
  }

  @Override
  protected void tearDown() throws Exception {
    AnalysisWorker.removeListener(listener);
    // Ensure worker is not analyzing
    worker.stop();
  }

  private MockFile addLibrary() {
    MockFile fileRes = projectRes.add(new MockFile(projectRes, "a.dart", "library a;#"));
    File file = fileRes.getLocation().toFile();
    FileBasedSource source = new FileBasedSource(context.getSourceFactory().getContentCache(), file);
    context.getSourceFactory().setContents(source, fileRes.getContentsAsString());
    ChangeSet changes = new ChangeSet();
    changes.added(source);
    context.applyChanges(changes);
    return fileRes;
  }
}
