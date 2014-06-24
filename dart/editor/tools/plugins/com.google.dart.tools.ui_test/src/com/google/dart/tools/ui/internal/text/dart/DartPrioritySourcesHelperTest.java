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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.internal.model.MockIgnoreFile;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

/**
 * Test for {@link DartPrioritySourcesHelper}.
 */
public class DartPrioritySourcesHelperTest extends TestCase {
  private DartIgnoreManager ignoreManager;
  private DartPrioritySourcesHelper helper;
  private Display display = mock(Display.class);
  private IWorkbenchWindow workbenchWindow = mock(IWorkbenchWindow.class);
  private IWorkbenchPage workbenchPage = mock(IWorkbenchPage.class);
  private IWorkbench workbench = mock(IWorkbench.class);
  private List<IPartListener2> listeners = Lists.newArrayList();
  private Map<AnalysisContext, List<Source>> contextSources = Maps.newHashMap();

  private IEditorReference editorRefA = mock(IEditorReference.class);
  private IEditorReference editorRefB = mock(IEditorReference.class);
  private IEditorReference editorRefC = mock(IEditorReference.class);
  private IEditorReference editorRefX = mock(IEditorReference.class);

  private IEditorPart editorA = mock(IEditorPart.class);
  private IEditorPart editorB = mock(IEditorPart.class);
  private IEditorPart editorC = mock(IEditorPart.class);
  private IEditorPart editorX = mock(IEditorPart.class);

  private DartPriorityFileEditor prioritySourceEditorA = mock(DartPriorityFileEditor.class);
  private DartPriorityFileEditor prioritySourceEditorB = mock(DartPriorityFileEditor.class);
  private DartPriorityFileEditor prioritySourceEditorC = mock(DartPriorityFileEditor.class);

  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);
  private Source sourceC = mock(Source.class);

  private AnalysisContext contextA = mock(AnalysisContext.class);
  private AnalysisContext contextB = mock(AnalysisContext.class);
  private AnalysisContext contextC = mock(AnalysisContext.class);

  public void test_partHidden() throws Exception {
    when(prioritySourceEditorB.getInputAnalysisContext()).thenReturn(contextA);
    when(prioritySourceEditorA.isVisible()).thenReturn(true);
    when(prioritySourceEditorB.isVisible()).thenReturn(true);
    helper.start();
    // initial state
    helper.test_waitForQueueEmpty();
    assertPrioritySources(contextA, sourceA, sourceB);
    // [A, B] - A = [B]
    when(prioritySourceEditorA.isVisible()).thenReturn(false);
    notifyPartHidden(editorRefA);
    assertPrioritySources(contextA, sourceB);
    // [B] - B = []
    when(prioritySourceEditorB.isVisible()).thenReturn(false);
    notifyPartHidden(editorRefB);
    assertPrioritySources(contextA);
  }

  public void test_partVisible() throws Exception {
    when(prioritySourceEditorB.getInputAnalysisContext()).thenReturn(contextA);
    helper.start();
    // [] + A = [A]
    when(prioritySourceEditorA.isVisible()).thenReturn(true);
    notifyPartVisible(editorRefA);
    assertPrioritySources(contextA, sourceA);
    // [A] + B = [A, B]
    when(prioritySourceEditorB.isVisible()).thenReturn(true);
    notifyPartVisible(editorRefB);
    assertPrioritySources(contextA, sourceA, sourceB);
  }

  public void test_setPriorityOnStart_oneContext_oneEditor() throws Exception {
    when(prioritySourceEditorA.isVisible()).thenReturn(true);
    helper.start();
    helper.test_waitForQueueEmpty();
    assertPrioritySources(contextA, sourceA);
  }

  public void test_setPriorityOnStart_oneContext_twoEditors() throws Exception {
    when(prioritySourceEditorB.getInputAnalysisContext()).thenReturn(contextA);
    when(prioritySourceEditorA.isVisible()).thenReturn(true);
    when(prioritySourceEditorB.isVisible()).thenReturn(true);
    helper.start();
    helper.test_waitForQueueEmpty();
    assertPrioritySources(contextA, sourceA, sourceB);
    verifyNoMoreInteractions(contextB);
    verifyNoMoreInteractions(contextC);
  }

  public void test_setPriorityOnStart_twoContexts() throws Exception {
    when(prioritySourceEditorB.getInputAnalysisContext()).thenReturn(contextA);
    when(prioritySourceEditorC.getInputAnalysisContext()).thenReturn(contextB);
    when(prioritySourceEditorA.isVisible()).thenReturn(true);
    when(prioritySourceEditorB.isVisible()).thenReturn(true);
    when(prioritySourceEditorC.isVisible()).thenReturn(true);
    helper.start();
    helper.test_waitForQueueEmpty();
    assertPrioritySources(contextA, sourceA, sourceB);
    assertPrioritySources(contextB, sourceC);
  }

  public void test_sourceIgnored() throws Exception {
    ignoreManager.addToIgnores("/sourceA.dart");
    when(prioritySourceEditorB.getInputAnalysisContext()).thenReturn(contextA);
    helper.start();
    // [] + A(ignored) = []
    when(prioritySourceEditorA.isVisible()).thenReturn(true);
    notifyPartVisible(editorRefA);
    assertPrioritySources(contextA);
    // [] + B = [B]
    when(prioritySourceEditorB.isVisible()).thenReturn(true);
    notifyPartVisible(editorRefB);
    assertPrioritySources(contextA, sourceB);
  }

  public void test_unusedPartListenerMethods() throws Exception {
    helper.start();
    for (IPartListener2 listener : listeners) {
      listener.partActivated(null);
      listener.partBroughtToTop(null);
      listener.partClosed(null);
      listener.partDeactivated(null);
      listener.partInputChanged(null);
      listener.partOpened(null);
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ignoreManager = new DartIgnoreManager(new MockIgnoreFile());
    helper = new DartPrioritySourcesHelper(workbench, ignoreManager);
    // perform "asyncExec" synchronously
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Runnable runnable = (Runnable) invocation.getArguments()[0];
        runnable.run();
        return null;
      }
    }).when(display).asyncExec(any(Runnable.class));
    // configure workbench
    when(workbench.getDisplay()).thenReturn(display);
    when(workbench.getWorkbenchWindows()).thenReturn(new IWorkbenchWindow[] {workbenchWindow});
    when(workbench.getActiveWorkbenchWindow()).thenReturn(workbenchWindow);
    when(workbenchWindow.getPages()).thenReturn(new IWorkbenchPage[] {workbenchPage});
    when(workbenchWindow.getActivePage()).thenReturn(workbenchPage);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        IPartListener2 listener = (IPartListener2) invocation.getArguments()[0];
        listeners.add(listener);
        return null;
      }
    }).when(workbenchPage).addPartListener(any(IPartListener2.class));
    // configure editors
    when(workbenchPage.getEditorReferences()).thenReturn(
        new IEditorReference[] {editorRefA, editorRefB, editorRefC, editorRefX});
    when(editorRefA.getEditor(anyBoolean())).thenReturn(editorA);
    when(editorRefB.getEditor(anyBoolean())).thenReturn(editorB);
    when(editorRefC.getEditor(anyBoolean())).thenReturn(editorC);
    when(editorRefX.getEditor(anyBoolean())).thenReturn(editorX);
    when(editorRefA.getPart(anyBoolean())).thenReturn(editorA);
    when(editorRefB.getPart(anyBoolean())).thenReturn(editorB);
    when(editorRefC.getPart(anyBoolean())).thenReturn(editorC);
    when(editorRefX.getPart(anyBoolean())).thenReturn(editorX);
    when(editorA.getAdapter(DartPriorityFileEditor.class)).thenReturn(prioritySourceEditorA);
    when(editorB.getAdapter(DartPriorityFileEditor.class)).thenReturn(prioritySourceEditorB);
    when(editorC.getAdapter(DartPriorityFileEditor.class)).thenReturn(prioritySourceEditorC);
    // configure sources
    when(sourceA.toString()).thenReturn("sourceA");
    when(sourceB.toString()).thenReturn("sourceB");
    when(sourceC.toString()).thenReturn("sourceC");
    when(sourceA.getFullName()).thenReturn("/sourceA.dart");
    when(sourceB.getFullName()).thenReturn("/sourceB.dart");
    when(sourceC.getFullName()).thenReturn("/sourceC.dart");
    // record priority sources
    recordContextPrioritySources(contextA);
    recordContextPrioritySources(contextB);
    recordContextPrioritySources(contextC);
    // configure editors
    when(prioritySourceEditorA.getInputSource()).thenReturn(sourceA);
    when(prioritySourceEditorB.getInputSource()).thenReturn(sourceB);
    when(prioritySourceEditorC.getInputSource()).thenReturn(sourceC);
    when(prioritySourceEditorA.getInputAnalysisContext()).thenReturn(contextA);
    when(prioritySourceEditorB.getInputAnalysisContext()).thenReturn(contextB);
    when(prioritySourceEditorC.getInputAnalysisContext()).thenReturn(contextC);
  }

  @Override
  protected void tearDown() throws Exception {
    helper.stop();
    super.tearDown();
  }

  private void assertPrioritySources(AnalysisContext context, Source... expected) throws Exception {
    helper.test_waitForQueueEmpty();
    List<Source> actual = contextSources.get(context);
    assertThat(actual).containsOnly((Object[]) expected);
  }

  private void notifyPartHidden(IWorkbenchPartReference ref) {
    for (IPartListener2 listener : listeners) {
      listener.partHidden(ref);
    }
  }

  private void notifyPartVisible(IWorkbenchPartReference ref) {
    for (IPartListener2 listener : listeners) {
      listener.partVisible(ref);
    }
  }

  @SuppressWarnings("unchecked")
  private void recordContextPrioritySources(final AnalysisContext context) {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        contextSources.put(context, (List<Source>) invocation.getArguments()[0]);
        return null;
      }
    }).when(context).setAnalysisPriorityOrder(any(List.class));
  }
}
