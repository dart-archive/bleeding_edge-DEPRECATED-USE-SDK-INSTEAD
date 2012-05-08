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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.File;
import java.util.HashMap;

public class EditorOpener {

  private IEditorReference fReusedEditor;

  public IEditorPart open(IWorkbenchPage wbPage, IFile file, boolean activate)
      throws PartInitException {
    if (NewSearchUI.reuseEditor()) {
      return showWithReuse(file, wbPage, getEditorID(file), activate);
    }
    return showWithoutReuse(file, wbPage, getEditorID(file), activate);
  }

  public IEditorPart openAndSelect(IWorkbenchPage wbPage, IFile file, int offset, int length,
      boolean activate) throws PartInitException {
    String editorId = null;
    IEditorDescriptor desc = IDE.getEditorDescriptor(file);
    if (desc == null || !desc.isInternal()) {
      editorId = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
    } else {
      editorId = desc.getId();
    }

    IEditorPart editor;
    if (NewSearchUI.reuseEditor()) {
      editor = showWithReuse(file, wbPage, editorId, activate);
    } else {
      editor = showWithoutReuse(file, wbPage, editorId, activate);
    }

    if (editor instanceof ITextEditor) {
      ITextEditor textEditor = (ITextEditor) editor;
      textEditor.selectAndReveal(offset, length);
    } else if (editor != null) {
      showWithMarker(editor, file, offset, length);
    }
    return editor;
  }

  public void openAndSelect(IWorkbenchPage page, Match match, int offset, int length,
      boolean activate) throws PartInitException {

    Object resource = match.getElement();

    if (resource instanceof IFile) {
      IFile file = (IFile) resource;
      if (offset >= 0 && length != 0) {
        openAndSelect(page, file, offset, length, activate);
      } else {
        open(page, file, activate);
      }
    } else if (resource instanceof File) {
      IFileStore file = EFS.getLocalFileSystem().fromLocalFile((File) resource);
      try {
        IEditorPart editor = IDE.openEditorOnFileStore(page, file);
        if (editor instanceof ITextEditor) {
          ITextEditor textEditor = (ITextEditor) editor;
          textEditor.selectAndReveal(offset, length);
        }
      } catch (PartInitException e) {
        ErrorDialog.openError(page.getWorkbenchWindow().getShell(),
            SearchMessages.FileSearchPage_open_file_dialog_title,
            SearchMessages.FileSearchPage_open_file_failed, e.getStatus());
      }
    }

  }

  private String getEditorID(IFile file) throws PartInitException {
    IEditorDescriptor desc = IDE.getEditorDescriptor(file);
    if (desc == null) {
      return SearchPlugin.getDefault().getWorkbench().getEditorRegistry().findEditor(
          IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID).getId();
    }
    return desc.getId();
  }

  private void showWithMarker(IEditorPart editor, IFile file, int offset, int length)
      throws PartInitException {
    IMarker marker = null;
    try {
      marker = file.createMarker(NewSearchUI.SEARCH_MARKER);
      HashMap<String, Integer> attributes = new HashMap<String, Integer>(4);
      attributes.put(IMarker.CHAR_START, new Integer(offset));
      attributes.put(IMarker.CHAR_END, new Integer(offset + length));
      marker.setAttributes(attributes);
      IDE.gotoMarker(editor, marker);
    } catch (CoreException e) {
      throw new PartInitException(SearchMessages.FileSearchPage_error_marker, e);
    } finally {
      if (marker != null) {
        try {
          marker.delete();
        } catch (CoreException e) {
          // ignore
        }
      }
    }
  }

  private IEditorPart showWithoutReuse(IFile file, IWorkbenchPage wbPage, String editorID,
      boolean activate) throws PartInitException {
    return IDE.openEditor(wbPage, file, editorID, activate);
  }

  private IEditorPart showWithReuse(IFile file, IWorkbenchPage page, String editorId,
      boolean activate) throws PartInitException {
    IEditorInput input = new FileEditorInput(file);
    IEditorPart editor = page.findEditor(input);
    if (editor != null) {
      page.bringToTop(editor);
      if (activate) {
        page.activate(editor);
      }
      return editor;
    }
    IEditorReference reusedEditorRef = fReusedEditor;
    if (reusedEditorRef != null) {
      boolean isOpen = reusedEditorRef.getEditor(false) != null;
      boolean canBeReused = isOpen && !reusedEditorRef.isDirty() && !reusedEditorRef.isPinned();
      if (canBeReused) {
        boolean showsSameInputType = reusedEditorRef.getId().equals(editorId);
        if (!showsSameInputType) {
          page.closeEditors(new IEditorReference[] {reusedEditorRef}, false);
          fReusedEditor = null;
        } else {
          editor = reusedEditorRef.getEditor(true);
          if (editor instanceof IReusableEditor) {
            ((IReusableEditor) editor).setInput(input);
            page.bringToTop(editor);
            if (activate) {
              page.activate(editor);
            }
            return editor;
          }
        }
      }
    }
    editor = page.openEditor(input, editorId, activate);
    if (editor instanceof IReusableEditor) {
      IEditorReference reference = (IEditorReference) page.getReference(editor);
      fReusedEditor = reference;
    } else {
      fReusedEditor = null;
    }
    return editor;
  }

}
