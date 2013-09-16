/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.view.events;

import java.util.List;

/**
 * @deprecated - use base selection notification
 */
public class NodeSelectionChangedEvent extends java.util.EventObject {
  /**
   * Comment for <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = 1L;

  int fCaretPosition;

  List fSelectedNodes;

  public NodeSelectionChangedEvent(Object source, List selectedNodes, int caretPosition) {
    super(source);
    fSelectedNodes = selectedNodes;
    fCaretPosition = caretPosition;
  }

  public int getCaretPosition() {
    return fCaretPosition;
  }

  public List getSelectedNodes() {
    return fSelectedNodes;
  }
}
