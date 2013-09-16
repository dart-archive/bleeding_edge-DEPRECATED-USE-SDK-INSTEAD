/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.xml.ui.internal.Logger;

/**
 * Abstract comment action delegate for XML editors
 */
abstract public class AbstractCommentActionXMLDelegate implements IEditorActionDelegate,
    IActionDelegate2, IViewActionDelegate {
  static final String CLOSE_COMMENT = "-->"; //$NON-NLS-1$
  static final String OPEN_COMMENT = "<!--"; //$NON-NLS-1$

  IEditorPart fEditor;

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    fEditor = targetEditor;
  }

  public void dispose() {
    // nulling out just in case
    fEditor = null;
  }

  public void runWithEvent(IAction action, Event event) {
    run(action);
  }

  public void run(IAction action) {
    if (fEditor instanceof ITextEditor) {
      ITextEditor textEditor = (ITextEditor) fEditor;
      IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
      if (document != null) {
        // get current text selection
        ITextSelection textSelection = getCurrentSelection();
        if (textSelection.isEmpty()) {
          return;
        }

        processAction(document, textSelection);
      }
    }
  }

  public void init(IViewPart view) {
    // do nothing
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // do nothing
  }

  private ITextSelection getCurrentSelection() {
    if (fEditor instanceof ITextEditor) {
      ISelectionProvider provider = ((ITextEditor) fEditor).getSelectionProvider();
      if (provider != null) {
        ISelection selection = provider.getSelection();
        if (selection instanceof ITextSelection) {
          return (ITextSelection) selection;
        }
      }
    }
    return TextSelection.emptySelection();
  }

  abstract void processAction(IDocument document, ITextSelection textSelection);

  void removeOpenCloseComments(IDocument document, int offset, int length) {
    try {
      int adjusted_length = length;

      // remove open comments
      String string = document.get(offset, length);
      int index = string.lastIndexOf(OPEN_COMMENT);
      while (index != -1) {
        document.replace(offset + index, OPEN_COMMENT.length(), ""); //$NON-NLS-1$
        index = string.lastIndexOf(OPEN_COMMENT, index - 1);
        adjusted_length -= OPEN_COMMENT.length();
      }

      // remove close comments
      string = document.get(offset, adjusted_length);
      index = string.lastIndexOf(CLOSE_COMMENT);
      while (index != -1) {
        document.replace(offset + index, CLOSE_COMMENT.length(), ""); //$NON-NLS-1$
        index = string.lastIndexOf(CLOSE_COMMENT, index - 1);
      }
    } catch (BadLocationException e) {
      Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
    }
  }
}
