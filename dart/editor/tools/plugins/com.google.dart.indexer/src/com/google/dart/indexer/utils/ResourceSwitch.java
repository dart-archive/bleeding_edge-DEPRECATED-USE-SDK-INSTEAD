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
package com.google.dart.indexer.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;

public abstract class ResourceSwitch implements IResourceVisitor {
  @Override
  public final boolean visit(IResource resource) throws CoreException {
    switch (resource.getType()) {
      case IResource.FILE:
        return visitFile((IFile) resource);
      case IResource.FOLDER:
        return visitFolder((IFolder) resource);
      case IResource.PROJECT:
        return visitProject((IProject) resource);
      case IResource.ROOT:
        return visitRoot((IWorkspaceRoot) resource);
    }
    throw new AssertionError("Unreachable code");
  }

  protected abstract boolean visitFile(IFile resource);

  protected abstract boolean visitFolder(IFolder resource);

  protected boolean visitProject(IProject resource) {
    throw new AssertionError("Unexpected type of resource: " + resource.getFullPath());
  }

  protected boolean visitRoot(IWorkspaceRoot resource) {
    throw new AssertionError("Unexpected type of resource: " + resource.getFullPath());
  }
}
