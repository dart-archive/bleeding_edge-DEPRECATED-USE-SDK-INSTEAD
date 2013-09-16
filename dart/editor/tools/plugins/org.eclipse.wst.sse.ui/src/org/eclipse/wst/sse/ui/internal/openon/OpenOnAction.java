/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.openon;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.wst.sse.ui.internal.IExtendedSimpleEditor;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;

import java.util.ResourceBundle;

/**
 * Determines the appropriate IOpenFileAction to call based on current partition.
 * 
 * @deprecated Use base support for hyperlink navigation
 */
public class OpenOnAction extends TextEditorAction {
  public OpenOnAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
    super(bundle, prefix, editor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#run()
   */
  public void run() {
    BusyIndicator.showWhile(getTextEditor().getEditorSite().getShell().getDisplay(),
        new Runnable() {
          public void run() {
            ITextEditor editor = getTextEditor();

            // figure out current offset
            int offset = -1;
            ISourceEditingTextTools textTools = (ISourceEditingTextTools) getTextEditor().getAdapter(
                ISourceEditingTextTools.class);
            if (textTools != null) {
              offset = textTools.getCaretOffset();
            } else if (editor instanceof IExtendedSimpleEditor) {
              offset = ((IExtendedSimpleEditor) editor).getCaretPosition();
            } else {
              if (editor.getSelectionProvider() != null) {
                ISelection sel = editor.getSelectionProvider().getSelection();
                if (sel instanceof ITextSelection) {
                  offset = ((ITextSelection) sel).getOffset();
                }
              }
            }
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            IOpenOn openOn = OpenOnProvider.getInstance().getOpenOn(document, offset);
            if (openOn != null) {
              openOn.openOn(document, new Region(offset, 0));
            }
          }
        });
  }
}
