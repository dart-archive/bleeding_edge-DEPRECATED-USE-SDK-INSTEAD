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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.ContextManager;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.analysis.model.ResolvedHtmlEvent;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspace;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import org.eclipse.core.resources.IResource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

public class AnalysisWorkerTest extends AbstractDartCoreTest {

  private final class Listener implements AnalysisListener {
    ArrayList<AnalysisContext> completed = new ArrayList<AnalysisContext>();
    ArrayList<IResource> resolved = new ArrayList<IResource>();

    @Override
    public void complete(AnalysisEvent event) {
      completed.add(event.getContext());
      assertEquals(originalCacheSize, event.getContext().getAnalysisOptions().getCacheSize());
    }

    @Override
    public void resolved(ResolvedEvent event) {
      resolved.add(event.getResource());
      assertEquals(originalCacheSize * 2, event.getContext().getAnalysisOptions().getCacheSize());
    }

    @Override
    public void resolvedHtml(ResolvedHtmlEvent event) {
      resolved.add(event.getResource());
      assertEquals(originalCacheSize * 2, event.getContext().getAnalysisOptions().getCacheSize());
    }

    void assertCompleted(AnalysisContext expected) {
      assertTrue(completed.contains(expected));
    }

    void assertNotCompleted(AnalysisContext expected) {
      assertFalse(completed.contains(expected));
    }

    void assertResolved(IResource res) {
      assertTrue(resolved.contains(res));
    }
  }

  private final MockWorkspace workspace = new MockWorkspace();
  private final MockWorkspaceRoot root = workspace.getRoot();
  private final MockProject project = root.add(new MockProject(root, getClass().getSimpleName()));
  private final ContextManager contextManager = mock(ContextManager.class);
  private final AnalysisContext analysisContext = AnalysisContextFactory.contextWithCore();
  private final int originalCacheSize = analysisContext.getAnalysisOptions().getCacheSize();
  private final AnalysisMarkerManager markerManager = mock(AnalysisMarkerManager.class);
  private final Listener listener = new Listener();

  public void test_getContext() throws Exception {
    AnalysisWorker worker = new AnalysisWorker(contextManager, analysisContext, null, null);
    assertSame(analysisContext, worker.getContext());
  }

  public void test_new() throws Exception {
    AnalysisWorker worker = new AnalysisWorker(contextManager, analysisContext, null, null);
    verify(contextManager).addWorker(worker);
  }

  public void test_performAnalysis() throws Exception {
    MockFile libFile = project.addFile("test.dart");
    Source libSource = addSource(libFile, "library a;\nmain() {}");
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(libSource);
    analysisContext.applyChanges(changeSet);
    when(contextManager.getResource(libSource)).thenReturn(libFile);

    AnalysisManager analysisManager = new AnalysisManager();
    when(contextManager.getResource()).thenReturn(project);
    AnalysisWorker worker = new AnalysisWorker(contextManager, analysisContext, null, markerManager);
    worker.performAnalysis(analysisManager);
    verify(markerManager).queueHasDartSdk(project, true);
    verify(markerManager, atLeastOnce()).queueErrors(
        eq(libFile),
        any(LineInfo.class),
        any(AnalysisError[].class));
    verify(markerManager).done();
    listener.assertCompleted(analysisContext);
    listener.assertResolved(libFile);
    assertEquals(originalCacheSize, analysisContext.getAnalysisOptions().getCacheSize());
  }

  public void test_performAnalysis_nothingToDo() throws Exception {
    AnalysisManager analysisManager = new AnalysisManager();
    when(contextManager.getResource()).thenReturn(project);
    AnalysisWorker worker = new AnalysisWorker(contextManager, analysisContext, null, markerManager);
    worker.performAnalysis(analysisManager);
    verify(markerManager).queueHasDartSdk(project, true);
    verify(markerManager).done();
    verifyNoMoreInteractions(markerManager);
    listener.assertCompleted(analysisContext);
    assertEquals(originalCacheSize, analysisContext.getAnalysisOptions().getCacheSize());
  }

  public void test_performAnalysis_nullContext() throws Exception {
    AnalysisWorker worker = new AnalysisWorker(contextManager, null, null, null);
    worker.performAnalysis(null);
    verifyNoMoreInteractions(markerManager);
    listener.assertNotCompleted(analysisContext);
    assertEquals(originalCacheSize, analysisContext.getAnalysisOptions().getCacheSize());
  }

  public void test_performAnalysis_stop() throws Exception {
    MockFile libFile = project.addFile("test.dart");
    Source libSource = addSource(libFile, "library a;\nmain() {}");
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(libSource);
    analysisContext.applyChanges(changeSet);
    when(contextManager.getResource(libSource)).thenReturn(libFile);

    AnalysisManager analysisManager = new AnalysisManager();
    when(contextManager.getResource()).thenReturn(project);
    AnalysisWorker worker = new AnalysisWorker(contextManager, analysisContext, null, markerManager);
    worker.stop();
    worker.performAnalysis(analysisManager);
    verifyNoMoreInteractions(markerManager);
    listener.assertNotCompleted(analysisContext);
    assertEquals(originalCacheSize, analysisContext.getAnalysisOptions().getCacheSize());
  }

  public void test_stop() throws Exception {
    AnalysisWorker worker = new AnalysisWorker(contextManager, analysisContext, null, null);
    worker.stop();
    assertNull(worker.getContext());
    verify(contextManager).removeWorker(worker);
  }

  @Override
  protected void setUp() throws Exception {
    AnalysisWorker.addListener(listener);
  }

  @Override
  protected void tearDown() throws Exception {
    AnalysisWorker.removeListener(listener);
  }

  private Source addSource(MockFile file, String contents) {
    Source source = new FileBasedSource(file.toFile());
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    analysisContext.applyChanges(changeSet);
    analysisContext.setContents(source, contents);
    return source;
  }
}
