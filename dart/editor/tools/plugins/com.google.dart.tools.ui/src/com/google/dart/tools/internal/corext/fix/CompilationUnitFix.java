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
package com.google.dart.tools.internal.corext.fix;

import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.ui.cleanup.ICleanUpFix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * {@link ICleanUpFix} wrapper around existing {@link CompilationUnitChange}.
 */
public class CompilationUnitFix extends AbstractFix {

  private final CompilationUnitChange change;

  public CompilationUnitFix(CompilationUnitChange change) {
    this.change = change;
  }

  @Override
  public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
    return change;
  }
}
