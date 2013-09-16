/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;

public class NullSourceEditingTextTools implements ISourceEditingTextTools {
  public static final String ID = "sourceeditingtexttools"; //$NON-NLS-1$
  private ITextEditor fTextEditor;

  /**
   * @return
   */
  public synchronized static ISourceEditingTextTools getInstance() {
    return new NullSourceEditingTextTools();
  }

  private NullSourceEditingTextTools() {
    super();
  }

  public int getCaretOffset() {
    ISelection sel = fTextEditor.getSelectionProvider().getSelection();
    if (sel instanceof ITextSelection) {
      return ((ITextSelection) sel).getOffset();
    }
    return -1;
  }

  public IDocument getDocument() {
    return fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());
  }

  public IEditorPart getEditorPart() {
    return fTextEditor;
  }

  public ITextSelection getSelection() {
    if (fTextEditor != null)
      return (ITextSelection) fTextEditor.getSelectionProvider().getSelection();
    return TextSelection.emptySelection();
  }

  public void setTextEditor(ITextEditor editor) {
    fTextEditor = editor;
  }
}
