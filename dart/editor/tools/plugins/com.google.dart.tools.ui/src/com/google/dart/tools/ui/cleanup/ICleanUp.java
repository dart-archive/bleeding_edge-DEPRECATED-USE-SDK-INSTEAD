/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.cleanup;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * A clean up solves problems in a compilation unit.
 * <p>
 * The clean up is asked for its requirements through a call to {@link #getRequirements()}. The
 * clean up can request an AST and define how to build this AST. It can base its requirements on the
 * options passed through {@link #setOptions(CleanUpOptions)}.
 * </p>
 * <p>
 * A context containing the information requested by the requirements are passed to
 * {@link #createFix(CleanUpContext)}. A fix capable of fixing the problems is returned by this
 * function if {@link #checkPreConditions(IJavaProject, ICompilationUnit[], IProgressMonitor)} has
 * returned a non fatal error status.
 * </p>
 * <p>
 * At the end {@link #checkPostConditions(IProgressMonitor)} is called.
 * </p>
 * 
 * @since 3.5
 */
public interface ICleanUp {

  /**
   * Called when done cleaning up.
   * 
   * @param monitor the monitor to show progress
   * @return the result of the postcondition check, not null
   * @throws CoreException if an unexpected error occurred
   */
  RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException;

  /**
   * After call to checkPreConditions clients will start creating fixes for
   * <code>compilationUnits</code> in <code>project</code> unless the result of checkPreConditions
   * contains a fatal error
   * 
   * @param project the project to clean up
   * @param compilationUnits an array of compilation units to clean up, all member of
   *          <code>project</code>
   * @param monitor the monitor to show progress
   * @return the result of the precondition check
   * @throws CoreException if an unexpected error occurred
   */
  RefactoringStatus checkPreConditions(DartProject project, CompilationUnit[] compilationUnits,
      IProgressMonitor monitor) throws CoreException;

  /**
   * Create an <code>ICleanUpFix</code> which fixes all problems in <code>context</code> or
   * <code>null</code> if nothing to fix.
   * 
   * @param context a context containing all information requested by {@link #getRequirements()}
   * @return the fix for the problems or <code>null</code> if nothing to fix
   * @throws CoreException if an unexpected error occurred
   */
  ICleanUpFix createFix(CleanUpContext context) throws CoreException;

  /**
   * The requirements of this clean up.
   * <p>
   * <strong>Note:</strong> This method must only be called after the options have been set.
   * </p>
   * 
   * @return the requirements used for {@link #createFix(CleanUpContext)} to work
   */
  CleanUpRequirements getRequirements();

  /**
   * Human readable description for each step this clean up will execute.
   * <p>
   * <strong>Note:</strong> This method must only be called after the options have been set.
   * </p>
   * 
   * @return descriptions an array of {@linkplain String strings} or <code>null</code>
   */
  String[] getStepDescriptions();

  /**
   * Sets the options that will be used.
   * 
   * @param options the options to use
   */
  void setOptions(CleanUpOptions options);

}
