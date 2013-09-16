/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.ui.IEditorPart;

/**
 * Action for file drop
 */
public class FileDropAction extends AbstractDropAction {
  public boolean run(DropTargetEvent event, IEditorPart targetEditor) {
    String[] strs = (String[]) event.data;
    if (strs == null || strs.length == 0) {
      return false;
    }

    String str = ""; //$NON-NLS-1$
    for (int i = 0; i < strs.length; ++i) {
      IPath path = new Path(strs[i]);
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IFile file = root.getFileForLocation(path);
      if (file != null) {
        path = file.getProjectRelativePath();
      }

      str += "\"" + path.toString() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
    }

    return insert(str, targetEditor);
  }
}
