/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class CoreUtility {

  /**
   * Creates a folder and all parent folders if not existing. Project must exist.
   * <code> org.eclipse.ui.dialogs.ContainerGenerator</code> is too heavy (creates a runnable)
   */
  public static void createFolder(IFolder folder, boolean force, boolean local,
      IProgressMonitor monitor) throws CoreException {
    if (!folder.exists()) {
      IContainer parent = folder.getParent();
      if (parent instanceof IFolder) {
        createFolder((IFolder) parent, force, local, null);
      }
      folder.create(force, local, monitor);
    }
  }

  /**
   * Sets whether building automatically is enabled in the workspace or not and returns the old
   * value.
   * 
   * @param state <code>true</code> if automatically building is enabled
   * @return the old state
   * @throws CoreException thrown if the operation failed
   */
  public static boolean setAutoBuilding(boolean state) throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceDescription desc = workspace.getDescription();
    boolean isAutoBuilding = desc.isAutoBuilding();
    if (isAutoBuilding != state) {
      desc.setAutoBuilding(state);
      workspace.setDescription(desc);
    }
    return isAutoBuilding;
  }

}
