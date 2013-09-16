/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;

public abstract class NodeAction extends Action {

  public String getSortKey() {
    return null;
  }

  /**
   * Checks that the resource backing the model is writeable utilizing <code>validateEdit</code> on
   * a given <tt>IWorkspace</tt>.
   * 
   * @param model the model to be checked
   * @param context the shell context for which <code>validateEdit</code> will be run
   * @return boolean result of checking <code>validateEdit</code>. If the resource is unwriteable,
   *         <code>status.isOK()</code> will return true; otherwise, false.
   */
  protected final boolean validateEdit(IStructuredModel model, Shell context) {
    if (model != null && model.getBaseLocation() != null) {
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
          new Path(model.getBaseLocation()));
      return !file.isAccessible()
          || ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, context).isOK();
    }
    return false; //$NON-NLS-1$
  }

  public abstract String getUndoDescription();
}
