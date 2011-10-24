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
package com.google.dart.tools.deploy;

import org.eclipse.core.internal.localstore.RefreshLocalVisitor;
import org.eclipse.core.internal.localstore.UnifiedTreeNode;
import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Visits a unified tree, and synchronizes the file system with the resource tree. After the visit
 * is complete, the file system will be synchronized with the workspace tree with respect to
 * resource existence, gender, and timestamp. Deletes linked resources if source file does not
 * exist.
 */
@SuppressWarnings("restriction")
public class RefreshLinkedVisitor extends RefreshLocalVisitor {

  /**
   * @param monitor
   */
  public RefreshLinkedVisitor(IProgressMonitor monitor) {
    super(monitor);

  }

  @Override
  protected void deleteResource(UnifiedTreeNode node, Resource target) throws CoreException {
    ResourceInfo info = target.getResourceInfo(false, false);
    int flags = target.getFlags(info);

    if (target.exists(flags, false)) {
      target.deleteResource(true, errors);
    }
    node.setExistsWorkspace(false);
  }

  /**
   * deletion or creation -- Returns: - RL_IN_SYNC - the resource is in-sync with the file system -
   * RL_NOT_IN_SYNC - the resource is not in-sync with file system - RL_UNKNOWN - couldn't determine
   * the sync status for this resource
   */
  @Override
  protected int synchronizeExistence(UnifiedTreeNode node, Resource target) throws CoreException {
    if (node.existsInWorkspace()) {
      if (!node.existsInFileSystem()) {
        deleteResource(node, target);
        resourceChanged = true;
        return RL_NOT_IN_SYNC;
      }
    } else {
      // do we have a gender variant in the workspace?
      IResource genderVariant = workspace.getRoot().findMember(target.getFullPath());
      if (genderVariant != null) {
        return RL_UNKNOWN;
      }
      if (node.existsInFileSystem()) {
        Container parent = (Container) target.getParent();
        if (!parent.exists()) {
          refresh(parent);
          if (!parent.exists()) {
            return RL_NOT_IN_SYNC;
          }
        }
        if (!target.getName().equals(node.getLocalName())) {
          return RL_IN_SYNC;
        }
        if (!Workspace.caseSensitive && node.getLevel() == 0) {
          // do we have any alphabetic variants in the workspace?
          IResource variant = target.findExistingResourceVariant(target.getFullPath());
          if (variant != null) {
            deleteResource(node, ((Resource) variant));
            createResource(node, target);
            resourceChanged = true;
            return RL_NOT_IN_SYNC;
          }
        }
        createResource(node, target);
        resourceChanged = true;
        return RL_NOT_IN_SYNC;
      }
    }
    return RL_UNKNOWN;
  }

}
