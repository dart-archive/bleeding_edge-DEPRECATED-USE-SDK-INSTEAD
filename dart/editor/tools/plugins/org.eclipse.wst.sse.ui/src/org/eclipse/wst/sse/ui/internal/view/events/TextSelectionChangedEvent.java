/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.view.events;

/**
 * @deprecated - use base selection notification
 */
public class TextSelectionChangedEvent extends java.util.EventObject {
  /**
   * Comment for <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = 1L;

  int fTextSelectionEnd;

  int fTextSelectionStart;

  public TextSelectionChangedEvent(Object source, int textSelectionStart, int textSelectionEnd) {
    super(source);
    fTextSelectionStart = textSelectionStart;
    fTextSelectionEnd = textSelectionEnd;
  }

  public int getTextSelectionEnd() {
    return fTextSelectionEnd;
  }

  public int getTextSelectionStart() {
    return fTextSelectionStart;
  }
}
