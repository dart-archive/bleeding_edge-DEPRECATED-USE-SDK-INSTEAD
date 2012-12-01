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
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Common superclass for events sent to {@link BuildParticipant}.
 */
public class ParticipantEvent {

  private final IProject project;
  final IProgressMonitor monitor;

  public ParticipantEvent(IProject project, IProgressMonitor monitor) {
    this.project = project;
    this.monitor = monitor;
  }

  public IProject getProject() {
    return project;
  }

  protected void traverseResources(final CleanVisitor visitor, IResource resource,
      final boolean visitPackages) throws CoreException {
    resource.accept(new IResourceProxyVisitor() {
      @Override
      public boolean visit(IResourceProxy proxy) throws CoreException {

        if (monitor.isCanceled()) {
          throw new OperationCanceledException();
        }

        if (proxy.getType() != IResource.FILE) {
          String name = proxy.getName();

          // Skip "hidden" directories
          if (name.startsWith(".")) {
            return false;
          }

          // Visit "packages" directories only if specified
          if (!visitPackages && name.equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
            return false;
          }
        }

        return visitor.visit(proxy, monitor);
      }
    }, 0);
  }
}
