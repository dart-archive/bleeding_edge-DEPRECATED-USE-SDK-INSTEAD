/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Listens for resource changes and forwards them to the {@link AnalysisServer}
 */
public class ResourceChangeListener implements IResourceChangeListener, IResourceDeltaVisitor {

  private static final int DELTA_MASK = IResourceDelta.CONTENT | IResourceDelta.REPLACED;

  private AnalysisServer server;

  /**
   * Construct a new instance that listens for resource changes and forwards that information on to
   * the specified {@link AnalysisServer}
   */
  public ResourceChangeListener(AnalysisServer server) {
    this.server = server;
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

    // TODO (danrubel) load serialized model on startup rather than this
    for (IProject project : workspace.getRoot().getProjects()) {
      DartLibrary[] libraries;
      try {
        libraries = DartCore.create(project).getDartLibraries();
      } catch (DartModelException e) {
        DartCore.logError("Failed to determine dart libraries in " + project, e);
        continue;
      }
      for (DartLibrary dartLib : libraries) {
        IResource libraryResource;
        try {
          libraryResource = dartLib.getCorrespondingResource();
        } catch (DartModelException e) {
          DartCore.logError("Failed to determine file for library " + dartLib, e);
          continue;
        }
        if (libraryResource == null) {
          DartCore.logError("Failed to determine file for library " + dartLib);
          continue;
        }
        server.analyzeLibrary(libraryResource.getLocation().toFile());
      }
    }
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    try {
      event.getDelta().accept(this);
    } catch (Exception e) {
      DartCore.logError("Failed to process resource changes for " + event.getResource(), e);
    }
  }

  /**
   * Stop listening for resource changes
   */
  public void stop() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  @Override
  public boolean visit(IResourceDelta delta) throws CoreException {
    if (delta.getKind() != IResourceDelta.CHANGED) {
      return false;
    }
    IResource resource = delta.getResource();
    if (resource.getType() != IResource.FILE) {
      return true;
    }
    int flags = delta.getFlags();
    if ((flags & DELTA_MASK) == 0) {
      return false;
    }
    IPath path = resource.getLocation();
    if (path != null) {
      server.fileChanged(path.toFile());
    }
    return false;
  }
}
