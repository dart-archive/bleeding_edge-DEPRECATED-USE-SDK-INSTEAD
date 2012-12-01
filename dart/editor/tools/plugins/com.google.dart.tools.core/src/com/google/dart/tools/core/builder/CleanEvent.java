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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Event passed to {@link BuildParticipant}s when
 * {@link BuildParticipant#clean(CleanEvent, IProgressMonitor)} is called.
 */
public class CleanEvent extends ParticipantEvent {

  public CleanEvent(IProject project, IProgressMonitor monitor) {
    super(project, monitor);
  }

  /**
   * Called by the participant to traverse the set of files to be cleaned.
   * 
   * @param visitor the clean visitor (not <code>null</code>)
   * @param visitPackages <code>true</code> if files and folders in the "packages" directory should
   *          be visited, and <code>false</code> if not
   * @throws CoreException if a problem occurred during traversal
   * @throws OperationCanceledException if the operation is canceled during traversal
   */
  public void traverse(CleanVisitor visitor, final boolean visitPackages) throws CoreException {
    traverseResources(visitor, getProject(), visitPackages);
  }
}
