/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.extensions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;

/**
 * Interface to provide convenient functions for editors that may not be ITextEditors, but need to
 * expose properties usually found in text editors
 */
public interface ISourceEditingTextTools {

  /**
   * @return the document offset at which the Caret is located
   */
  public int getCaretOffset();

  /**
   * @return the IDocument being edited. If this editor supports multiple documents, the document
   *         currently possessing the caret will be returned.
   */
  public IDocument getDocument();

  /**
   * @return The IEditorPart instance for this editor that is known to the workbench.
   */
  public IEditorPart getEditorPart();

  /**
   * @return The current selection within the editor. Implementors may support other types of
   *         selection.
   */
  public ITextSelection getSelection();
}
