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
import com.google.dart.server.AnalysisServer;

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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

/**
 * Test for {@link DartPriorityFilesHelper_NEW}.
 */
public class DartPriorityFilesHelper_NEW_Test extends TestCase {
  private AnalysisServer analysisServer = mock(AnalysisServer.class);
  private DartPriorityFilesHelper_NEW helper;
  private Display display = mock(Display.class);
  private IWorkbenchWindow workbenchWindow = mock(IWorkbenchWindow.class);
  private IWorkbenchPage workbenchPage = mock(IWorkbenchPage.class);
  private IWorkbench workbench = mock(IWorkbench.class);
  private List<IPartListener2> listeners = Lists.newArrayList();
  private List<String> files = Lists.newArrayList();

  private IEditorReference editorRefA = mock(IEditorReference.class);
  private IEditorReference editorRefB = mock(IEditorReference.class);
  private IEditorReference editorRefC = mock(IEditorReference.class);
  private IEditorReference editorRefX = mock(IEditorReference.class);

  private IEditorPart editorA = mock(IEditorPart.class);
  private IEditorPart editorB = mock(IEditorPart.class);
  private IEditorPart editorC = mock(IEditorPart.class);
  private IEditorPart editorX = mock(IEditorPart.class);

  private DartPriorityFileEditor priorityFileEditorA = mock(DartPriorityFileEditor.class);
  private DartPriorityFileEditor priorityFileEditorB = mock(DartPriorityFileEditor.class);
  private DartPriorityFileEditor priorityFileEditorC = mock(DartPriorityFileEditor.class);

  private String fileA = "a.dart";
  private String fileB = "b.dart";
  private String fileC = "c.dart";

  public void test_partHidden() throws Exception {
    when(priorityFileEditorA.isVisible()).thenReturn(true);
    when(priorityFileEditorB.isVisible()).thenReturn(true);
    helper.start();
    // initial state
    assertPriorityFiles(fileA, fileB);
    // [A, B] - A = [B]
    when(priorityFileEditorA.isVisible()).thenReturn(false);
    notifyPartHidden(editorRefA);
    assertPriorityFiles(fileB);
    // [B] - B = []
    when(priorityFileEditorB.isVisible()).thenReturn(false);
    notifyPartHidden(editorRefB);
    assertPriorityFiles();
  }

  public void test_partVisible() throws Exception {
    helper.start();
    // [] + A = [A]
    when(priorityFileEditorA.isVisible()).thenReturn(true);
    notifyPartVisible(editorRefA);
    assertPriorityFiles(fileA);
    // [A] + B = [A, B]
    when(priorityFileEditorB.isVisible()).thenReturn(true);
    notifyPartVisible(editorRefB);
    assertPriorityFiles(fileA, fileB);
  }

  public void test_setPriorityOnStart_oneEditor() throws Exception {
    when(priorityFileEditorA.isVisible()).thenReturn(true);
    helper.start();
    assertPriorityFiles(fileA);
  }

  public void test_setPriorityOnStart_threeEditors() throws Exception {
    when(priorityFileEditorA.isVisible()).thenReturn(true);
    when(priorityFileEditorB.isVisible()).thenReturn(true);
    when(priorityFileEditorC.isVisible()).thenReturn(true);
    helper.start();
    assertPriorityFiles(fileA, fileB, fileC);
  }

  public void test_setPriorityOnStart_twoEditors() throws Exception {
    when(priorityFileEditorA.isVisible()).thenReturn(true);
    when(priorityFileEditorB.isVisible()).thenReturn(true);
    helper.start();
    assertPriorityFiles(fileA, fileB);
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
    helper = new DartPriorityFilesHelper_NEW(workbench, analysisServer);
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
    when(editorA.getAdapter(DartPriorityFileEditor.class)).thenReturn(priorityFileEditorA);
    when(editorB.getAdapter(DartPriorityFileEditor.class)).thenReturn(priorityFileEditorB);
    when(editorC.getAdapter(DartPriorityFileEditor.class)).thenReturn(priorityFileEditorC);
    // record priority files
    recordPriorityFiles();
    // configure editors
    when(priorityFileEditorA.getInputFilePath()).thenReturn(fileA);
    when(priorityFileEditorB.getInputFilePath()).thenReturn(fileB);
    when(priorityFileEditorC.getInputFilePath()).thenReturn(fileC);
  }

  private void assertPriorityFiles(String... expected) throws Exception {
    assertThat(files).containsOnly((Object[]) expected);
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

  private void recordPriorityFiles() {
    if (analysisServer == null) {
      return;
    }
    doAnswer(new Answer<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void answer(InvocationOnMock invocation) throws Throwable {
        files = (List<String>) invocation.getArguments()[0];
        return null;
      }
    }).when(analysisServer).setPriorityFiles(anyListOf(String.class));
  }
}
