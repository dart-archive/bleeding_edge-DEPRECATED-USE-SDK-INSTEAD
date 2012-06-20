/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.debug.core.util;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to make listening for resource change events easier.
 */
public class ResourceChangeManager implements IResourceChangeListener {
  private static ResourceChangeManager manager;

  public static ResourceChangeManager getManager() {
    if (manager == null) {
      manager = new ResourceChangeManager();
    }

    return manager;
  }

  public static void removeChangeParticipant(ResourceChangeParticipant listener) {
    if (manager != null) {
      manager.listeners.remove(listener);
    }
  }

  public static void shutdown() {
    if (manager != null) {
      manager.dispose();
      manager = null;
    }
  }

  private List<ResourceChangeParticipant> listeners = new ArrayList<ResourceChangeParticipant>();

  private ResourceChangeManager() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  public void addChangeParticipant(ResourceChangeParticipant listener) {
    listeners.add(listener);
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (listeners.size() == 0) {
      return;
    }

    if (event.getDelta() == null) {
      return;
    }

    try {
      event.getDelta().accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
          if (delta.getKind() == IResourceDelta.CHANGED) {
            IResource resource = delta.getResource();

            if (resource instanceof IFile) {
              notifyChanged((IFile) resource);
            }
          }

          return true;
        }
      });
    } catch (CoreException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

  protected void notifyChanged(IFile file) {
    for (ResourceChangeParticipant participant : listeners) {
      try {
        participant.handleFileChange(file);
      } catch (Throwable t) {
        DartDebugCorePlugin.logError(t);
      }
    }
  }

  private void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

}
