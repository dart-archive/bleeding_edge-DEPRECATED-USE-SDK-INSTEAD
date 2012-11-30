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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * 
 */
public class BuildEvent extends ParticipantEvent {

  private final IResourceDelta delta;
  private final IProgressMonitor monitor;

  public BuildEvent(IProject project, IResourceDelta delta, IProgressMonitor monitor) {
    super(project);
    this.delta = delta;
    this.monitor = monitor;
  }

  /**
   * Called by the participant to traverse the set of files to be built.
   * 
   * @param participant the build participant (not <code>null</code>)
   * @throws CoreException if a problem occurred during traversal
   * @throws OperationCanceledException if the operation is canceled during traversal
   */
  public void traverse(final BuildParticipant participant) throws CoreException {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    // If there is no delta, then traverse all resources in the project
    if (delta == null) {
      traverseResources(participant, getProject());
      return;
    }

    // Traverse only those resources that have changed
    delta.accept(new IResourceDeltaVisitor() {
      @Override
      public boolean visit(IResourceDelta delta) throws CoreException {
        if (delta.getKind() == IResourceDelta.ADDED) {
          traverseResources(participant, delta.getResource());
          return false;
        }
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        return participant.visit(delta, monitor);
      }
    });
  }

  private void traverseResources(final BuildParticipant participant, IResource resource)
      throws CoreException {
    resource.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {
        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
        return participant.visit(proxy, monitor);
      }
    }, 0);
  }
}
