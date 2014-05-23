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

package com.google.dart.tools.ui.internal.text.dart;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServer;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

/**
 * Test for {@link DartPrioritySourcesHelper_NEW}.
 */
// TODO(scheglov) restore or remove for the new API
public class DartPrioritySourcesHelper_NEW_Test extends TestCase {
  private AnalysisServer analysisServer = mock(AnalysisServer.class);
  private DartPrioritySourcesHelper_NEW helper;
  private Display display = mock(Display.class);
  private IWorkbenchWindow workbenchWindow = mock(IWorkbenchWindow.class);
  private IWorkbenchPage workbenchPage = mock(IWorkbenchPage.class);
  private IWorkbench workbench = mock(IWorkbench.class);
  private List<IPartListener2> listeners = Lists.newArrayList();
  private Map<String, List<Source>> contextIdSources = Maps.newHashMap();

  private IEditorReference editorRefA = mock(IEditorReference.class);
  private IEditorReference editorRefB = mock(IEditorReference.class);
  private IEditorReference editorRefC = mock(IEditorReference.class);
  private IEditorReference editorRefX = mock(IEditorReference.class);

  private IEditorPart editorA = mock(IEditorPart.class);
  private IEditorPart editorB = mock(IEditorPart.class);
  private IEditorPart editorC = mock(IEditorPart.class);
  private IEditorPart editorX = mock(IEditorPart.class);

  private DartPrioritySourceEditor prioritySourceEditorA = mock(DartPrioritySourceEditor.class);
  private DartPrioritySourceEditor prioritySourceEditorB = mock(DartPrioritySourceEditor.class);
  private DartPrioritySourceEditor prioritySourceEditorC = mock(DartPrioritySourceEditor.class);

  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);
  private Source sourceC = mock(Source.class);

  private String contextIdA = "contextIdA";
  private String contextIdB = "contextIdB";
  private String contextIdC = "contextIdC";

  public void test_partHidden() throws Exception {
//    when(prioritySourceEditorB.getInputAnalysisContextId()).thenReturn(contextIdA);
//    when(prioritySourceEditorA.isVisible()).thenReturn(true);
//    when(prioritySourceEditorB.isVisible()).thenReturn(true);
//    helper.start();
//    // initial state
//    assertPrioritySources(contextIdA, sourceA, sourceB);
//    // [A, B] - A = [B]
//    when(prioritySourceEditorA.isVisible()).thenReturn(false);
//    notifyPartHidden(editorRefA);
//    assertPrioritySources(contextIdA, sourceB);
//    // [B] - B = []
//    when(prioritySourceEditorB.isVisible()).thenReturn(false);
//    notifyPartHidden(editorRefB);
//    assertPrioritySources(contextIdA);
  }

//  public void test_partVisible() throws Exception {
//    when(prioritySourceEditorB.getInputAnalysisContextId()).thenReturn(contextIdA);
//    helper.start();
//    // [] + A = [A]
//    when(prioritySourceEditorA.isVisible()).thenReturn(true);
//    notifyPartVisible(editorRefA);
//    assertPrioritySources(contextIdA, sourceA);
//    // [A] + B = [A, B]
//    when(prioritySourceEditorB.isVisible()).thenReturn(true);
//    notifyPartVisible(editorRefB);
//    assertPrioritySources(contextIdA, sourceA, sourceB);
//  }
//
//  public void test_setPriorityOnStart_oneContext_oneEditor() throws Exception {
//    when(prioritySourceEditorA.isVisible()).thenReturn(true);
//    helper.start();
//    assertPrioritySources(contextIdA, sourceA);
//  }
//
//  public void test_setPriorityOnStart_oneContext_twoEditors() throws Exception {
//    when(prioritySourceEditorB.getInputAnalysisContextId()).thenReturn(contextIdA);
//    when(prioritySourceEditorA.isVisible()).thenReturn(true);
//    when(prioritySourceEditorB.isVisible()).thenReturn(true);
//    helper.start();
//    assertPrioritySources(contextIdA, sourceA, sourceB);
//  }
//
//  public void test_setPriorityOnStart_twoContexts() throws Exception {
//    when(prioritySourceEditorB.getInputAnalysisContextId()).thenReturn(contextIdA);
//    when(prioritySourceEditorC.getInputAnalysisContextId()).thenReturn(contextIdB);
//    when(prioritySourceEditorA.isVisible()).thenReturn(true);
//    when(prioritySourceEditorB.isVisible()).thenReturn(true);
//    when(prioritySourceEditorC.isVisible()).thenReturn(true);
//    helper.start();
//    assertPrioritySources(contextIdA, sourceA, sourceB);
//    assertPrioritySources(contextIdB, sourceC);
//  }
//
//  public void test_unusedPartListenerMethods() throws Exception {
//    helper.start();
//    for (IPartListener2 listener : listeners) {
//      listener.partActivated(null);
//      listener.partBroughtToTop(null);
//      listener.partClosed(null);
//      listener.partDeactivated(null);
//      listener.partInputChanged(null);
//      listener.partOpened(null);
//    }
//  }
//
//  @Override
//  protected void setUp() throws Exception {
//    super.setUp();
//    helper = new DartPrioritySourcesHelper_NEW(workbench, analysisServer);
//    // perform "asyncExec" synchronously
//    doAnswer(new Answer<Void>() {
//      @Override
//      public Void answer(InvocationOnMock invocation) throws Throwable {
//        Runnable runnable = (Runnable) invocation.getArguments()[0];
//        runnable.run();
//        return null;
//      }
//    }).when(display).asyncExec(any(Runnable.class));
//    // configure workbench
//    when(workbench.getDisplay()).thenReturn(display);
//    when(workbench.getWorkbenchWindows()).thenReturn(new IWorkbenchWindow[] {workbenchWindow});
//    when(workbench.getActiveWorkbenchWindow()).thenReturn(workbenchWindow);
//    when(workbenchWindow.getPages()).thenReturn(new IWorkbenchPage[] {workbenchPage});
//    when(workbenchWindow.getActivePage()).thenReturn(workbenchPage);
//    doAnswer(new Answer<Void>() {
//      @Override
//      public Void answer(InvocationOnMock invocation) throws Throwable {
//        IPartListener2 listener = (IPartListener2) invocation.getArguments()[0];
//        listeners.add(listener);
//        return null;
//      }
//    }).when(workbenchPage).addPartListener(any(IPartListener2.class));
//    // configure editors
//    when(workbenchPage.getEditorReferences()).thenReturn(
//        new IEditorReference[] {editorRefA, editorRefB, editorRefC, editorRefX});
//    when(editorRefA.getEditor(anyBoolean())).thenReturn(editorA);
//    when(editorRefB.getEditor(anyBoolean())).thenReturn(editorB);
//    when(editorRefC.getEditor(anyBoolean())).thenReturn(editorC);
//    when(editorRefX.getEditor(anyBoolean())).thenReturn(editorX);
//    when(editorRefA.getPart(anyBoolean())).thenReturn(editorA);
//    when(editorRefB.getPart(anyBoolean())).thenReturn(editorB);
//    when(editorRefC.getPart(anyBoolean())).thenReturn(editorC);
//    when(editorRefX.getPart(anyBoolean())).thenReturn(editorX);
//    when(editorA.getAdapter(DartPrioritySourceEditor.class)).thenReturn(prioritySourceEditorA);
//    when(editorB.getAdapter(DartPrioritySourceEditor.class)).thenReturn(prioritySourceEditorB);
//    when(editorC.getAdapter(DartPrioritySourceEditor.class)).thenReturn(prioritySourceEditorC);
//    // configure sources
//    when(sourceA.toString()).thenReturn("sourceA");
//    when(sourceB.toString()).thenReturn("sourceB");
//    when(sourceC.toString()).thenReturn("sourceC");
//    // record priority sources
//    recordContextPrioritySources(contextIdA);
//    recordContextPrioritySources(contextIdB);
//    recordContextPrioritySources(contextIdC);
//    // record priority sources
//    recordContextPrioritySources(contextIdA);
//    recordContextPrioritySources(contextIdB);
//    recordContextPrioritySources(contextIdC);
//    // configure editors
//    when(prioritySourceEditorA.getInputSource()).thenReturn(sourceA);
//    when(prioritySourceEditorB.getInputSource()).thenReturn(sourceB);
//    when(prioritySourceEditorC.getInputSource()).thenReturn(sourceC);
//    when(prioritySourceEditorA.getInputAnalysisContextId()).thenReturn(contextIdA);
//    when(prioritySourceEditorB.getInputAnalysisContextId()).thenReturn(contextIdB);
//    when(prioritySourceEditorC.getInputAnalysisContextId()).thenReturn(contextIdC);
//  }
//
//  private void assertPrioritySources(String contextId, Source... expected) throws Exception {
//    List<Source> actual = contextIdSources.get(contextId);
//    assertThat(actual).containsOnly((Object[]) expected);
//  }
//
//  private void notifyPartHidden(IWorkbenchPartReference ref) {
//    for (IPartListener2 listener : listeners) {
//      listener.partHidden(ref);
//    }
//  }
//
//  private void notifyPartVisible(IWorkbenchPartReference ref) {
//    for (IPartListener2 listener : listeners) {
//      listener.partVisible(ref);
//    }
//  }
//
//  private void recordContextPrioritySources(String contextId) {
//    if (analysisServer == null) {
//      return;
//    }
//    doAnswer(new Answer<Void>() {
//      @Override
//      public Void answer(InvocationOnMock invocation) throws Throwable {
//        String contextId = (String) invocation.getArguments()[0];
//        Source[] sources = (Source[]) invocation.getArguments()[1];
//        contextIdSources.put(contextId, Lists.newArrayList(sources));
//        return null;
//      }
//    }).when(analysisServer).setPrioritySources(anyString(), any(Source[].class));
//  }
}
