/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.ui.texteditor.IUpdate;

public interface IExtendedEditorAction extends IUpdate {

  /**
   * Returns whether this action item is visible
   * 
   * @return <code>true</code> if this item is visible, and <code>false</code> otherwise
   */
  public boolean isVisible();

  /**
   * Sets the active editor for the action. Implementors should disconnect from the old editor,
   * connect to the new editor, and update the action to reflect the new editor.
   * 
   * @param targetEditor the new editor target
   */
  public void setActiveExtendedEditor(IExtendedSimpleEditor targetEditor);
}
