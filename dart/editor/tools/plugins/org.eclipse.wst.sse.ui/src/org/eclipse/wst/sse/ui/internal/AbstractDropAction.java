/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;

/**
 */
public abstract class AbstractDropAction implements IDropAction {

  /*
   * Replaces targetEditor's current selection by "text"
   */
  protected boolean insert(String text, IEditorPart targetEditor) {
    if (text == null || text.length() == 0) {
      return true;
    }

    ITextSelection textSelection = null;
    IDocument doc = null;
    ISelection selection = null;

    ISourceEditingTextTools tools = (ISourceEditingTextTools) targetEditor.getAdapter(ISourceEditingTextTools.class);
    if (tools != null) {
      doc = tools.getDocument();
      selection = tools.getSelection();
    }

    ITextEditor textEditor = null;
    if (targetEditor instanceof ITextEditor) {
      textEditor = (ITextEditor) targetEditor;
    }
    if (textEditor == null) {
      textEditor = (ITextEditor) ((IAdaptable) targetEditor).getAdapter(ITextEditor.class);
    }
    if (textEditor == null && tools != null && tools.getEditorPart() instanceof ITextEditor) {
      textEditor = (ITextEditor) tools.getEditorPart();
    }
    if (textEditor == null && tools != null && tools.getEditorPart() != null) {
      textEditor = (ITextEditor) tools.getEditorPart().getAdapter(ITextEditor.class);
    }

    if (selection == null && textEditor != null) {
      selection = textEditor.getSelectionProvider().getSelection();
    }
    if (doc == null && textEditor != null) {
      doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
    }

    if (selection instanceof ITextSelection) {
      textSelection = (ITextSelection) selection;
      try {
        doc.replace(textSelection.getOffset(), textSelection.getLength(), text);
      } catch (BadLocationException e) {
        return false;
      }
    }
    if (textEditor != null && textSelection != null) {
      ISelectionProvider sp = textEditor.getSelectionProvider();
      ITextSelection sel = new TextSelection(textSelection.getOffset(), text.length());
      sp.setSelection(sel);
      textEditor.selectAndReveal(sel.getOffset(), sel.getLength());
    }

    return true;
  }

  public boolean isSupportedData(Object data) {
    return true;
  }

  public abstract boolean run(DropTargetEvent event, IEditorPart targetEditor);
}
