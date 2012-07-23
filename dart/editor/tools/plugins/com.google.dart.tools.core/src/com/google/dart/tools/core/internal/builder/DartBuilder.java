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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.internal.util.Extensions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.util.Map;

/**
 * Instances of the class <code>DartBuilder</code> implement the incremental builder for Dart
 * projects.
 */
public class DartBuilder extends IncrementalProjectBuilder {

  private final AnalysisServer server = SystemLibraryManagerProvider.getDefaultAnalysisServer();

  @SuppressWarnings("rawtypes")
  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

    IResourceDelta delta = getDelta(getProject());

    // If delta is null, then building a new project

    if (delta == null) {
      IPath location = getProject().getLocation();
      if (location != null) {
        server.scan(location.toFile(), false);
      }
      return null;
    }

    // Recursively process the resource delta

    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) {

        IResource resource = delta.getResource();
        IPath location = resource.getLocation();
        if (location == null) {
          return false;
        }
        File file = location.toFile();

        // Process folder

        if (resource.getType() != IResource.FILE) {
          switch (delta.getKind()) {
            case IResourceDelta.ADDED:
              server.scan(file, false);
              return false;
            case IResourceDelta.REMOVED:
              server.discard(file);
              return false;
            case IResourceDelta.CHANGED:
              // recurse child deltas
              return true;
          }
          return false;
        }

        // Process file

        if (resource.getName().endsWith(Extensions.DOT_DART)) {
          switch (delta.getKind()) {
            case IResourceDelta.ADDED:
              server.scan(file, false);
              return false;
            case IResourceDelta.REMOVED:
              server.discard(file);
              return false;
            case IResourceDelta.CHANGED:
              server.changed(file);
              return false;
          }
          return false;
        }

        return false;
      }
    });

    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    AnalysisServer server = SystemLibraryManagerProvider.getDefaultAnalysisServer();
    server.reanalyze();
  }
}
