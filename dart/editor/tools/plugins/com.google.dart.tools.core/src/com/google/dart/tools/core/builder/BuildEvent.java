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
package com.google.dart.tools.core.builder;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Event passed to {@link BuildParticipant}s when
 * {@link BuildParticipant#build(BuildEvent, IProgressMonitor)} is called. Use
 * {@link #traverse(BuildParticipant, boolean)} to visit all resources to be processed
 */
public class BuildEvent extends ParticipantEvent {

  private final IResourceDelta projectDelta;

  public BuildEvent(IProject project, IResourceDelta delta, IProgressMonitor monitor) {
    super(project, monitor);
    this.projectDelta = delta;
  }

  /**
   * Called by the participant to traverse the set of files to be built.
   * 
   * @param visitor the build visitor (not <code>null</code>)
   * @param visitPackages <code>true</code> if files and folders in the "packages" directory should
   *          be visited, and <code>false</code> if not
   * @throws CoreException if a problem occurred during traversal
   * @throws OperationCanceledException if the operation is canceled during traversal
   */
  public void traverse(final BuildVisitor visitor, final boolean visitPackages)
      throws CoreException {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    // If there is no projectDelta, then traverse all resources in the project
    if (projectDelta == null) {
      traverseResources(visitor, getProject(), visitPackages);
      return;
    }

    // Traverse only those resources that have changed
    projectDelta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();

        // Traverse added resources via proxy
        if (delta.getKind() == IResourceDelta.ADDED) {
          traverseResources(visitor, resource, visitPackages);
          return false;
        }

        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        if (resource.getType() != IResource.FILE) {
          String name = resource.getName();

          // Skip "hidden" directories
          if (name.startsWith(".")) {
            return false;
          }

          // Visit "packages" directories only if specified
          if (!visitPackages && name.equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
            return false;
          }
        }

        return visitor.visit(delta, monitor);
      }
    });
  }
}
