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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/**
 * Helper for opening an editor on a search result.
 */
public class DartSearchEditorOpener {

  private IEditorReference reusedEditor;

  public IEditorPart openElement(Object element) throws PartInitException {
    IWorkbenchPage wbPage = DartToolsPlugin.getActivePage();
    IEditorPart editor;
    if (NewSearchUI.reuseEditor()) {
      editor = showWithReuse(element, wbPage);
    } else {
      editor = showWithoutReuse(element);
    }

    if (element instanceof DartElement) {
      EditorUtility.revealInEditor(editor, (DartElement) element);
    }

    return editor;
  }

  public IEditorPart openMatch(Match match) throws PartInitException {
    Object element = getElementToOpen(match);
    return openElement(element);
  }

  protected Object getElementToOpen(Match match) {
    return match.getElement();
  }

  private IEditorPart showInEditor(IWorkbenchPage page, IEditorInput input, String editorId) {
    IEditorPart editor = page.findEditor(input);
    if (editor != null) {
      page.bringToTop(editor);
      return editor;
    }
    IEditorReference reusedEditorRef = reusedEditor;
    if (reusedEditorRef != null) {
      boolean isOpen = reusedEditorRef.getEditor(false) != null;
      boolean canBeReused = isOpen && !reusedEditorRef.isDirty() && !reusedEditorRef.isPinned();
      if (canBeReused) {
        boolean showsSameInputType = reusedEditorRef.getId().equals(editorId);
        if (!showsSameInputType) {
          page.closeEditors(new IEditorReference[] {reusedEditorRef}, false);
          reusedEditor = null;
        } else {
          editor = reusedEditorRef.getEditor(true);
          if (editor instanceof IReusableEditor) {
            ((IReusableEditor) editor).setInput(input);
            page.bringToTop(editor);
            return editor;
          }
        }
      }
    }
    // could not reuse
    try {
      editor = page.openEditor(input, editorId, false);
      if (editor instanceof IReusableEditor) {
        IEditorReference reference = (IEditorReference) page.getReference(editor);
        reusedEditor = reference;
      } else {
        reusedEditor = null;
      }
      return editor;
    } catch (PartInitException ex) {
      MessageDialog.openError(
          DartToolsPlugin.getActiveWorkbenchShell(),
          SearchMessages.Search_Error_openEditor_title,
          SearchMessages.Search_Error_openEditor_message);
      return null;
    }
  }

  private IEditorPart showWithoutReuse(Object element) throws PartInitException {
    try {
      return EditorUtility.openInEditor(element, false);
    } catch (PartInitException e) {
      //TODO (pquitslund): add this constant and make sure it's set in the part init exception
//      if (e.getStatus().getCode() != DartStatusConstants.EDITOR_NO_EDITOR_INPUT) {
//        throw e;
//      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  private IEditorPart showWithReuse(Object element, IWorkbenchPage wbPage) throws PartInitException {
    IEditorInput input = EditorUtility.getEditorInput(element);
    if (input == null) {
      return null;
    }
    String editorID = EditorUtility.getEditorID(input);
    return showInEditor(wbPage, input, editorID);
  }

}
