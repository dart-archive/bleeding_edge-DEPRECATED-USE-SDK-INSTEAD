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
package com.google.dart.tools.search2.internal.ui.text;

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

public class WindowAnnotationManager {
  private IWorkbenchWindow fWindow;
  private Map<IEditorPart, EditorAnnotationManager> fAnnotationManagers;
  private IPartListener2 fPartListener;
  private ArrayList<AbstractTextSearchResult> fSearchResults;

  public WindowAnnotationManager(IWorkbenchWindow window) {
    fWindow = window;
    fAnnotationManagers = new HashMap<IEditorPart, EditorAnnotationManager>();

    fSearchResults = new ArrayList<AbstractTextSearchResult>();

    initEditors();
    fPartListener = new IPartListener2() {
      public void partActivated(IWorkbenchPartReference partRef) {
        startHighlighting(getEditor(partRef));
      }

      public void partBroughtToTop(IWorkbenchPartReference partRef) {
        startHighlighting(getEditor(partRef));
      }

      public void partClosed(IWorkbenchPartReference partRef) {
        stopHighlighting(getEditor(partRef));
      }

      public void partDeactivated(IWorkbenchPartReference partRef) {
      }

      public void partOpened(IWorkbenchPartReference partRef) {
      }

      public void partHidden(IWorkbenchPartReference partRef) {
        stopHighlighting(getEditor(partRef));
      }

      public void partVisible(IWorkbenchPartReference partRef) {
        startHighlighting(getEditor(partRef));
      }

      public void partInputChanged(IWorkbenchPartReference partRef) {
        updateHighlighting(getEditor(partRef));
      }
    };
    fWindow.getPartService().addPartListener(fPartListener);

  }

  private void startHighlighting(IEditorPart editor) {
    if (editor == null)
      return;
    EditorAnnotationManager mgr = fAnnotationManagers.get(editor);
    if (mgr == null) {
      mgr = new EditorAnnotationManager(editor);
      fAnnotationManagers.put(editor, mgr);
      mgr.setSearchResults(fSearchResults);
    }
  }

  private void updateHighlighting(IEditorPart editor) {
    if (editor == null)
      return;
    EditorAnnotationManager mgr = fAnnotationManagers.get(editor);
    if (mgr != null) {
      mgr.doEditorInputChanged();
    }
  }

  private void initEditors() {
    IWorkbenchPage[] pages = fWindow.getPages();
    for (int i = 0; i < pages.length; i++) {
      IEditorReference[] editors = pages[i].getEditorReferences();
      for (int j = 0; j < editors.length; j++) {
        IEditorPart editor = editors[j].getEditor(false);
        if (editor != null && pages[i].isPartVisible(editor)) {
          startHighlighting(editor);
        }
      }
    }
  }

  private void stopHighlighting(IEditorPart editor) {
    if (editor == null)
      return;
    EditorAnnotationManager mgr = fAnnotationManagers.remove(editor);
    if (mgr != null)
      mgr.dispose();
  }

  private IEditorPart getEditor(IWorkbenchPartReference partRef) {
    if (partRef instanceof IEditorReference) {
      return ((IEditorReference) partRef).getEditor(false);
    }
    return null;
  }

  void dispose() {
    fWindow.getPartService().removePartListener(fPartListener);
    for (Iterator<EditorAnnotationManager> mgrs = fAnnotationManagers.values().iterator(); mgrs.hasNext();) {
      EditorAnnotationManager mgr = mgrs.next();
      mgr.dispose();
    }
    fAnnotationManagers = null;
  }

  void addSearchResult(AbstractTextSearchResult result) {
    boolean alreadyShown = fSearchResults.contains(result);
    fSearchResults.add(result);
    if (!alreadyShown) {
      for (Iterator<EditorAnnotationManager> mgrs = fAnnotationManagers.values().iterator(); mgrs.hasNext();) {
        EditorAnnotationManager mgr = mgrs.next();
        mgr.addSearchResult(result);
      }
    }
  }

  void removeSearchResult(AbstractTextSearchResult result) {
    fSearchResults.remove(result);
    boolean stillShown = fSearchResults.contains(result);
    if (!stillShown) {
      for (Iterator<EditorAnnotationManager> mgrs = fAnnotationManagers.values().iterator(); mgrs.hasNext();) {
        EditorAnnotationManager mgr = mgrs.next();
        mgr.removeSearchResult(result);
      }
    }
  }

}
