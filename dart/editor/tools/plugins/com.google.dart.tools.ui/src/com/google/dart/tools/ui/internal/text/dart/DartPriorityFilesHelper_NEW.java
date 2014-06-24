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

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.List;

/**
 * Helper for updating the order in which files are analyzed in contexts associated with editors.
 * This is called once per instantiated editor on startup and then once for each editor as it
 * becomes active. For example, if there are 2 of 7 editors visible on startup, then this will be
 * called for the 2 visible editors.
 * 
 * @coverage dart.editor.ui.text
 */
public class DartPriorityFilesHelper_NEW {
  private final IWorkbench workbench;
  private final AnalysisServer analysisServer;
  private final List<String> files = Lists.newArrayList();

  public DartPriorityFilesHelper_NEW(IWorkbench workbench, AnalysisServer analysisServer) {
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
   * @return the {@link DartPriorityFileEditor} that corresponds to the given
   *         {@link IWorkbenchPart}, maybe {@code null}.
   */
  private DartPriorityFileEditor getPriorityFileEditor(IWorkbenchPart part) {
    if (part != null) {
      Object maybeEditor = part.getAdapter(DartPriorityFileEditor.class);
      if (maybeEditor instanceof DartPriorityFileEditor) {
        return (DartPriorityFileEditor) maybeEditor;
      }
    }
    return null;
  }

  /**
   * Answer the visible {@link DartPriorityFileEditor}s.
   */
  private List<DartPriorityFileEditor> getVisibleEditors() {
    List<DartPriorityFileEditor> editors = Lists.newArrayList();;
    for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
      for (IWorkbenchPage page : window.getPages()) {
        for (IEditorReference editorRef : page.getEditorReferences()) {
          IEditorPart part = editorRef.getEditor(false);
          DartPriorityFileEditor editor = getPriorityFileEditor(part);
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

  private void handlePartActivated(IWorkbenchPart part) {
    DartPriorityFileEditor editor = getPriorityFileEditor(part);
    if (editor != null) {
      updateAnalysisPriorityOrderOnUiThread(editor, true);
    }
  }

  private void handlePartDeactivated(IWorkbenchPart part) {
    DartPriorityFileEditor editor = getPriorityFileEditor(part);
    if (editor != null) {
      updateAnalysisPriorityOrderOnUiThread(editor, false);
    }
  }

  /**
   * Starts listening for {@link IWorkbenchPage} and adding/removing files of the visible editors.
   */
  private void internalStart(IWorkbenchPage activePage) {
    // make files of the currently visible editors a priority ones
    {
      List<DartPriorityFileEditor> editors = getVisibleEditors();
      for (DartPriorityFileEditor editor : editors) {
        String file = editor.getInputFilePath();
        if (file != null) {
          files.add(file);
        }
      }
      // set priority files
      analysisServer.setPriorityFiles(files);
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
   * Update the order in which files are analyzed in the context associated with the editor. This is
   * called once per instantiated editor on startup and then once for each editor as it becomes
   * active. For example, if there are 2 of 7 editors visible on startup, then this will be called
   * for the 2 visible editors.
   * <p>
   * MUST be called on the UI thread.
   * 
   * @param isOpen {@code true} if the editor is open and the file should be the first file analyzed
   *          or {@code false} if the editor is closed and the file should be removed from the
   *          priority list.
   */
  private void updateAnalysisPriorityOrderOnUiThread(DartPriorityFileEditor editor, boolean isOpen) {
    String file = editor.getInputFilePath();
    if (file != null) {
      if (isOpen) {
        if (!files.contains(file)) {
          files.add(file);
        }
      } else {
        files.remove(file);
      }
    }
    analysisServer.setPriorityFiles(files);
  }
}
