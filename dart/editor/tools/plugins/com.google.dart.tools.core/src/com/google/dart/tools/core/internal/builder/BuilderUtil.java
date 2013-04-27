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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Static builder utility methods shared by multiple classes
 */
class BuilderUtil {

  /**
   * Perform a delayed refresh of the given resource.
   * 
   * @param resource
   */
  static void delayedRefresh(final IResource resource) {
    WorkspaceJob job = new WorkspaceJob("Refresh " + resource.getName()) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);

        return Status.OK_STATUS;
      }
    };

    job.setRule(resource.getProject());
    job.schedule();
  }
}
