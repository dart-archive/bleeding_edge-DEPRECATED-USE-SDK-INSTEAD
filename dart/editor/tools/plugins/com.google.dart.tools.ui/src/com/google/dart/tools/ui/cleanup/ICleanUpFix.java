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

import com.google.dart.tools.core.refactoring.CompilationUnitChange;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A clean up fix calculates a {@link CompilationUnitChange} which can be applied on a document to
 * fix one or more problems in a compilation unit.
 * 
 * @since 3.5
 */
public interface ICleanUpFix {

  /**
   * Calculates and returns a {@link CompilationUnitChange} which can be applied on a document to
   * fix one or more problems in a compilation unit.
   * 
   * @param progressMonitor the progress monitor or <code>null</code> if none
   * @return a compilation unit change change which should not be empty
   * @throws CoreException if something went wrong while calculating the change
   */
  public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException;

}
