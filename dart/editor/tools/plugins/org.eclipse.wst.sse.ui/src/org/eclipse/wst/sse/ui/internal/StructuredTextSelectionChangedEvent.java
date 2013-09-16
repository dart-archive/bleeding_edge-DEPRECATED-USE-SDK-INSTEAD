/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * This event is used by the SourceEditorTreeViewer to tell the ViewerSelectionManager that the
 * selection really came from a user click on the content outliner, instead of being set
 * programatically.
 */
public class StructuredTextSelectionChangedEvent extends
    org.eclipse.jface.viewers.SelectionChangedEvent {

  /**
   * Comment for <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = 1L;

  public StructuredTextSelectionChangedEvent(ISelectionProvider source, ISelection selection) {
    super(source, selection);
  }
}
