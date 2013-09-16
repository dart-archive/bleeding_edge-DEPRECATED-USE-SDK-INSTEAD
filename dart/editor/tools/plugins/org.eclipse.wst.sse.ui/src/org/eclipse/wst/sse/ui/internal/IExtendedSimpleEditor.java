/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

/**
 * @deprecated, obtain a ISourceEditingTextTools adapter from the editor part
 */

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;

public interface IExtendedSimpleEditor {

  public int getCaretPosition();

  public IDocument getDocument();

  public IEditorPart getEditorPart();

  public Point getSelectionRange();

}
