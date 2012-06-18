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

  @SuppressWarnings("rawtypes")
  @Override
  protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

    IResourceDelta delta = getDelta(getProject());

    if (delta == null) { // add project     
      analyze(getProject(), IResourceDelta.ADDED);
    } else {

      delta.accept(new IResourceDeltaVisitor() {
        @Override
        public boolean visit(IResourceDelta delta) {
          IResource resource = delta.getResource();
          if (resource.getType() != IResource.FILE) {
            switch (delta.getKind()) {
              case IResourceDelta.ADDED:
              case IResourceDelta.REMOVED:
                analyze(resource, delta.getKind());
                return false;
              case IResourceDelta.CHANGED:
                return true;
            }
            return false;
          }
          String name = resource.getName();
          if (name.endsWith(Extensions.DOT_DART)) {
            analyze(resource, delta.getKind());
          }
          return false;
        }
      });

    }

    return null;
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    AnalysisServer server = SystemLibraryManagerProvider.getDefaultAnalysisServer();
    server.reanalyze();
  }

  private void analyze(IResource resource, int kind) {
    AnalysisServer server = SystemLibraryManagerProvider.getDefaultAnalysisServer();
    IPath location = resource.getLocation();
    if (location == null) {
      return;
    }
    File file = location.toFile();
    server.changed(file);
    if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED) {
      server.scan(file, false);
    } else if (kind == IResourceDelta.REMOVED) {
      server.discard(file);
    }

  }

}
