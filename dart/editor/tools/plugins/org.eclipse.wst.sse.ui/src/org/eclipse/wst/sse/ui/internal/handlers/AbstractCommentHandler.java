/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * This class contains all of the shared functionality for comment handlers
 */
public abstract class AbstractCommentHandler extends AbstractHandler {

  /**
   * <p>
   * Default constructor must exist because sub classes are created by java reflection
   * </p>
   */
  public AbstractCommentHandler() {
    super();
  }

  /**
   * <p>
   * Gets the important information out of the event and passes it onto the internal method
   * {@link #processAction}
   * </p>
   * 
   * @see org.eclipse.wst.xml.ui.internal.handlers.CommentHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  public final Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    ITextEditor textEditor = null;
    if (editor instanceof ITextEditor)
      textEditor = (ITextEditor) editor;
    else {
      Object o = editor.getAdapter(ITextEditor.class);
      if (o != null)
        textEditor = (ITextEditor) o;
    }
    if (textEditor != null) {
      IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
      if (document != null && document instanceof IStructuredDocument) {
        // get current text selection
        ITextSelection textSelection = getCurrentSelection(textEditor);
        if (!textSelection.isEmpty()) {
          //call the implementers code to deal with the event
          processAction(textEditor, (IStructuredDocument) document, textSelection);
        }
      }
    }
    return null;
  }

  /**
   * <p>
   * This method is called by the public {@link #execute} method whenever the comment handler is
   * invoked. This method should be used for the logic of handling the structured comment event.
   * </p>
   * 
   * @param textEditor the text editor the initiating event was caused in
   * @param document the document the text editor is editing
   * @param textSelection the user selection when the event was caused
   */
  protected abstract void processAction(ITextEditor textEditor, IStructuredDocument document,
      ITextSelection textSelection);

  /**
   * <p>
   * Gets the current user selection in the given {@link ITextEditor}
   * </p>
   * 
   * @param textEditor get the user selection from here
   * @return the current user selection in <code>textEdtior</code>
   */
  private static ITextSelection getCurrentSelection(ITextEditor textEditor) {
    ISelectionProvider provider = textEditor.getSelectionProvider();
    if (provider != null) {
      ISelection selection = provider.getSelection();
      if (selection instanceof ITextSelection) {
        return (ITextSelection) selection;
      }
    }
    return TextSelection.emptySelection();
  }
}
