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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;

public abstract class ResourceDeltaSwitch implements IResourceDeltaVisitor {
  @Override
  public final boolean visit(IResourceDelta delta) {
    switch (delta.getKind()) {
      case IResourceDelta.ADDED:
        return visitAddedDelta(delta);
      case IResourceDelta.REMOVED:
        return visitRemovedDelta(delta);
      case IResourceDelta.CHANGED:
        return visitChangedDelta(delta);
      default:
        // phantom resources - not normally reached
        return true;
    }
  }

  protected abstract boolean visitAddedContainer(IContainer container);

  protected boolean visitAddedDelta(IResourceDelta delta) {
    IResource resource = delta.getResource();
    if (resource.getType() == IResource.FILE) {
      visitAddedFile((IFile) resource);
      return false;
    } else {
      return visitAddedContainer((IContainer) resource);
    }
  }

  protected abstract void visitAddedFile(IFile file);

  protected boolean visitChangedContainer(IContainer container, IResourceDelta delta) {
    if ((delta.getFlags() & IResourceDelta.OPEN) == IResourceDelta.OPEN) {
      if (((IProject) container).isAccessible()) {
        visitAddedContainer(container);
      } else {
        visitRemovedContainer(container);
      }
      return false;
    }
    return true;
  }

  protected boolean visitChangedDelta(IResourceDelta delta) {
    IResource resource = delta.getResource();
    if (resource.getType() == IResource.FILE) {
      visitChangedFile((IFile) resource, delta);
      return false;
    } else {
      return visitChangedContainer((IContainer) resource, delta);
    }
  }

  protected abstract void visitChangedFile(IFile file, IResourceDelta delta);

  protected abstract boolean visitRemovedContainer(IContainer container);

  protected boolean visitRemovedDelta(IResourceDelta delta) {
    IResource resource = delta.getResource();
    if (resource.getType() == IResource.FILE) {
      visitRemovedFile((IFile) resource);
      return false;
    } else {
      return visitRemovedContainer((IContainer) resource);
    }
  }

  protected abstract void visitRemovedFile(IFile file);
}
