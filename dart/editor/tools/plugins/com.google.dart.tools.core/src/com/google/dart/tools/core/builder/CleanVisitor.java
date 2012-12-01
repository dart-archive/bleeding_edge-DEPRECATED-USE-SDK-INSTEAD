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

import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface used by {@link BuildParticipant} when visiting resources in a {@link CleanEvent}
 */
public interface CleanVisitor {

  /**
   * Called by {@link BuildEvent#traverse(BuildParticipant, boolean)} when a resource has been added
   * or is otherwise being visited for the first time since the last call to
   * {@link #clean(CleanEvent, IProgressMonitor)}.
   * 
   * @param proxy a proxy to the resource being visited
   * @param monitor the progress monitor (not <code>null</code>) to use for reporting progress to
   *          the user. It is the caller's responsibility to call done() on the given monitor.
   * @return <code>true</code> if the resource's children should be visited
   */
  boolean visit(IResourceProxy proxy, IProgressMonitor monitor) throws CoreException;
}
