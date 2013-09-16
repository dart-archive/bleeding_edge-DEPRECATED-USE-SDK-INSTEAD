/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint;

import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.texteditor.IElementStateListener;

public interface IExtendedStorageEditorInput extends IStorageEditorInput {
  /**
   * Adds the given element state listener to this input. Has no effect if an identical listener is
   * already registered. Typically used by the IDocumentProvider to register itself for change
   * notification.
   * 
   * @param listener the listener
   */
  void addElementStateListener(IElementStateListener listener);

  /**
   * Removes the given element state listener from this input. Has no affect if an identical
   * listener is not registered.
   * 
   * @param listener the listener
   */
  void removeElementStateListener(IElementStateListener listener);
}
