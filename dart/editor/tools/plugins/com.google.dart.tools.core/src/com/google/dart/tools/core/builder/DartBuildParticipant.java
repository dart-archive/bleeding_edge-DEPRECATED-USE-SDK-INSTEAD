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
package com.google.dart.tools.core.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Map;

/**
 * A participant in the building of dart projects. This interface has been DEPRECATED in favor of
 * {@link BuildParticipant}.
 */
public interface DartBuildParticipant {

  /**
   * Called before dart projects are built.
   * 
   * @param kind the kind of build being requested. Valid values are
   *          <ul>
   *          <li>{@link IncrementalProjectBuilder#FULL_BUILD} - indicates a full build.</li>
   *          <li>{@link IncrementalProjectBuilder#INCREMENTAL_BUILD}- indicates an incremental
   *          build.</li>
   *          <li>{@link IncrementalProjectBuilder#AUTO_BUILD} - indicates an automatically
   *          triggered incremental build (autobuilding on).</li>
   *          </ul>
   * @param args a table of builder-specific arguments keyed by argument name (key type:
   *          <code>String</code>, value type: <code>String</code>); <code>null</code> is equivalent
   *          to an empty map
   * @param monitor a progress monitor, or <code>null</code> if progress reporting and cancellation
   *          are not desired
   * @param delta the resource delta being built
   * @exception CoreException if this build fails.
   * @see IncrementalProjectBuilder#getDelta(org.eclipse.core.resources.IProject
   * @see IProject#build(int, String, Map, IProgressMonitor)
   */
  void build(int kind, Map<String, String> args, IResourceDelta delta, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Called before dart projects are built clean. Clean is an opportunity for a builder to discard
   * any additional state that has been computed as a result of previous builds. It is recommended
   * that builders override this method to delete all derived resources created by previous builds,
   * and to remove all markers of type {@link IMarker#PROBLEM} that were created by previous
   * invocations of the builder.
   * <p>
   * This method is called as a result of invocations of
   * {@link IWorkspace#build(int, IProgressMonitor)} or
   * {@link IProject#build(int, IProgressMonitor)} where the build kind is
   * {@link IncrementalProjectBuilder#CLEAN_BUILD}.
   * 
   * @param project the project being cleaned
   * @param monitor a progress monitor, or <code>null</code> if progress reporting and cancellation
   *          are not desired
   * @exception CoreException if this build fails.
   * @see IWorkspace#build(int, IProgressMonitor)
   * @see IncrementalProjectBuilder#CLEAN_BUILD
   */
  void clean(IProject project, IProgressMonitor monitor) throws CoreException;

}
