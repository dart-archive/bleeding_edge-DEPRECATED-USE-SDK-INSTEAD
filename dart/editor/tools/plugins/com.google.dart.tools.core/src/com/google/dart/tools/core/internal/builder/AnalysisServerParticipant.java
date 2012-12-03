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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.builder.BuildEvent;
import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.BuildVisitor;
import com.google.dart.tools.core.builder.CleanEvent;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;

import static com.google.dart.tools.core.internal.util.Extensions.DOT_DART;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;
import static org.eclipse.core.resources.IResource.FILE;

import java.io.File;

/**
 * Performs source analysis using instances of {@link AnalysisServer} and DartC.
 * {@link AnalysisEngineParticipant} should be disabled when this participant is enabled.
 * 
 * @see DartCoreDebug#ENABLE_NEW_ANALYSIS
 */
public class AnalysisServerParticipant implements BuildParticipant, BuildVisitor {

  private final AnalysisServer server;
  private final boolean enabled;

  public AnalysisServerParticipant() {
    this(
        PackageLibraryManagerProvider.getDefaultAnalysisServer(),
        !DartCoreDebug.ENABLE_NEW_ANALYSIS);
  }

  public AnalysisServerParticipant(AnalysisServer server, boolean enabled) {
    this.server = server;
    this.enabled = enabled;
  }

  @Override
  public void build(BuildEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisEngineParticipant are mutually exclusive
    if (!enabled) {
      return;
    }

    event.traverse(this, true);
  }

  @Override
  public void clean(CleanEvent event, IProgressMonitor monitor) throws CoreException {

    // This participant and AnalysisEngineParticipant are mutually exclusive
    if (!enabled) {
      return;
    }

    IProject project = event.getProject();
    project.deleteMarkers(DartCore.DART_PROBLEM_MARKER_TYPE, true, DEPTH_INFINITE);
    IPath location = project.getLocation();
    if (location != null) {
      server.discard(location.toFile());
    }
  }

  @Override
  public boolean visit(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
    IResource resource = delta.getResource();

    // Ignore files that are not dart related
    if (resource.getType() == FILE && !resource.getName().endsWith(DOT_DART)) {
      return false;
    }

    // Discard any resources that have been removed 
    if (delta.getKind() == IResourceDelta.REMOVED) {
      IPath location = resource.getLocation();
      if (location != null) {
        server.discard(location.toFile());
      }
      return false;
    }

    // Recurse into any containers
    if (resource.getType() != FILE) {
      return true;
    }

    // Notify server of any changed *.dart files
    IPath location = resource.getLocation();
    if (location != null) {
      server.changed(location.toFile());
    }
    return false;
  }

  @Override
  public boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException {
    IResource resource = proxy.requestResource();

    // Ignore files that are not dart related
    if (resource.getType() == FILE && !resource.getName().endsWith(DOT_DART)) {
      return false;
    }

    // If a *.dart file or a directory is added in the "packages" directory
    // then re-analyze the containing application directory
    IPath fullPath = resource.getFullPath();
    int segmentCount = fullPath.segmentCount();
    for (int index = 0; index < segmentCount; index++) {
      if (fullPath.segment(index).equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
        for (int count = index > 0 ? index : 1; count < segmentCount; count++) {
          resource = resource.getParent();
        }
        IPath location = resource.getLocation();
        if (location != null) {
          File file = location.toFile();
          server.discard(file);
          server.scan(file, null);
        }
        return false;
      }
    }

    // Scan the newly added resources
    IPath location = resource.getLocation();
    if (location != null) {
      File file = location.toFile();
      server.scan(file, null);
    }
    return false;
  }

  /**
   * If the resource is in the "packages" directory hierarchy, then return the application directory
   * containing that "packages" directory, otherwise return {@code null}.
   * 
   * @param resource the resource (not {@code null})
   * @return the resource or the application directory resource (not {@code null})
   */
  private IResource getPackagesParent(IResource resource) {
    IPath fullPath = resource.getFullPath();
    int segmentCount = fullPath.segmentCount();
    for (int index = 0; index < segmentCount; index++) {
      if (fullPath.segment(index).equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
        for (int count = index > 0 ? index : 1; count < segmentCount; count++) {
          ;
        }
        resource = resource.getParent();
        return resource;
      }
    }
    return null;
  }
}
