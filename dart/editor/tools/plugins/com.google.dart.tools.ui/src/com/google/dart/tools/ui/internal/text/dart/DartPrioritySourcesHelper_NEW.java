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

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.List;
import java.util.Map;

/**
 * Helper for updating the order in which sources are analyzed in contexts associated with editors.
 * This is called once per instantiated editor on startup and then once for each editor as it
 * becomes active. For example, if there are 2 of 7 editors visible on startup, then this will be
 * called for the 2 visible editors.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartPrioritySourcesHelper_NEW {
  private final IWorkbench workbench;
  private final AnalysisServer analysisServer;

  public DartPrioritySourcesHelper_NEW(IWorkbench workbench, AnalysisServer analysisServer) {
    this.workbench = workbench;
    this.analysisServer = analysisServer;
  }

  /**
   * Schedules helper start, once {@link IWorkbenchPage} is created.
   */
  public void start() {
    workbench.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {
          IWorkbenchPage page = window.getActivePage();
          if (page != null) {
            internalStart(page);
          }
        }
      }
    });
  }

  /**
   * @return the {@link DartPrioritySourceEditor} that corresponds to the given
   *         {@link IWorkbenchPart}, maybe {@code null}.
   */
  private DartPrioritySourceEditor getPrioritySourceEditor(IWorkbenchPart part) {
    if (part != null) {
      Object maybeEditor = part.getAdapter(DartPrioritySourceEditor.class);
      if (maybeEditor instanceof DartPrioritySourceEditor) {
        return (DartPrioritySourceEditor) maybeEditor;
      }
    }
    return null;
  }

  /**
   * Answer the visible {@link DartPrioritySourceEditor}s.
   * 
   * @param context the context (not {@code null})
   * @return a list of sources (not {@code null}, contains no {@code null}s)
   */
  private List<DartPrioritySourceEditor> getVisibleEditors() {
    List<DartPrioritySourceEditor> editors = Lists.newArrayList();;
    for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
      for (IWorkbenchPage page : window.getPages()) {
        for (IEditorReference editorRef : page.getEditorReferences()) {
          IEditorPart part = editorRef.getEditor(false);
          DartPrioritySourceEditor editor = getPrioritySourceEditor(part);
          if (editor != null) {
            if (editor.isVisible()) {
              editors.add(editor);
            }
          }
        }
      }
    }
    return editors;
  }

  /**
   * Answer the visible editors displaying source for the given context. This must be called on the
   * UI thread because it accesses windows, pages, and editors.
   * 
   * @param contextId the context identifier (not {@code null})
   * @return a list of sources (not {@code null}, contains no {@code null}s)
   */
  private List<Source> getVisibleSourcesForContext(String contextId) {
    List<Source> sources = Lists.newArrayList();
    List<DartPrioritySourceEditor> editors = getVisibleEditors();
    for (DartPrioritySourceEditor editor : editors) {
      if (contextId.equals(editor.getInputAnalysisContextId())) {
        Source source = editor.getInputSource();
        if (source != null) {
          sources.add(source);
        }
      }
    }
    return sources;
  }

  private void handlePartActivated(IWorkbenchPart part) {
    DartPrioritySourceEditor editor = getPrioritySourceEditor(part);
    if (editor != null) {
      updateAnalysisPriorityOrderOnUiThread(editor, true);
    }
  }

  private void handlePartDeactivated(IWorkbenchPart part) {
    DartPrioritySourceEditor editor = getPrioritySourceEditor(part);
    if (editor != null) {
      updateAnalysisPriorityOrderOnUiThread(editor, false);
    }
  }

  /**
   * Starts listening for {@link IWorkbenchPage} and adding/removing sources of the visible editors.
   */
  private void internalStart(IWorkbenchPage activePage) {
    // make source of the currently visible editors a priority ones
    {
      Map<String, List<Source>> contextMap = Maps.newHashMap();
      List<DartPrioritySourceEditor> editors = getVisibleEditors();
      for (DartPrioritySourceEditor editor : editors) {
        String contextId = editor.getInputAnalysisContextId();
        if (contextId != null && !contextMap.containsKey(contextId)) {
          List<Source> sources = getVisibleSourcesForContext(contextId);
          contextMap.put(contextId, sources);
        }
      }
      // schedule priority sources setting
      // TODO(scheglov) restore or remove for the new API
//      for (Entry<String, List<Source>> entry : contextMap.entrySet()) {
//        String contextId = entry.getKey();
//        List<Source> prioritySources = entry.getValue();
//        analysisServer.setPrioritySources(
//            contextId,
//            prioritySources.toArray(new Source[prioritySources.size()]));
//      }
    }
    // track visible editors
    activePage.addPartListener(new IPartListener2() {
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partClosed(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partDeactivated(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partHidden(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part != null) {
          handlePartDeactivated(part);
        }
      }

      @Override
      public void partInputChanged(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partOpened(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partVisible(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part != null) {
          handlePartActivated(part);
        }
      }
    });
  }

  /**
   * Update the order in which sources are analyzed in the context associated with the editor. This
   * is called once per instantiated editor on startup and then once for each editor as it becomes
   * active. For example, if there are 2 of 7 editors visible on startup, then this will be called
   * for the 2 visible editors.
   * <p>
   * MUST be called on the UI thread.
   * 
   * @param isOpen {@code true} if the editor is open and the source should be the first source
   *          analyzed or {@code false} if the editor is closed and the source should be removed
   *          from the priority list.
   */
  private void updateAnalysisPriorityOrderOnUiThread(DartPrioritySourceEditor editor, boolean isOpen) {
    String contextId = editor.getInputAnalysisContextId();
    Source source = editor.getInputSource();
    if (contextId != null && source != null) {
      List<Source> sources = getVisibleSourcesForContext(contextId);
      sources.remove(source);
      if (isOpen) {
        sources.add(0, source);
      }
      // TODO(scheglov) restore or remove for the new API
//      analysisServer.setPrioritySources(contextId, sources.toArray(new Source[sources.size()]));
    }
  }
}
