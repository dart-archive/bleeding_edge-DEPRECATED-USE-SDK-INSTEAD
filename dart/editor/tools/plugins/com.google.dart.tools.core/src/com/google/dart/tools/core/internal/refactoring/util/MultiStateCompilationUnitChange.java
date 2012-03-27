/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.refactoring.util;

import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.MultiStateTextFileChange;

/**
 * Multi state compilation unit change for composite refactorings.
 */
public final class MultiStateCompilationUnitChange extends MultiStateTextFileChange {

  /** The compilation unit */
  private final CompilationUnit fUnit;

  /**
   * Creates a new multi state compilation unit change.
   * 
   * @param name the name of the change
   * @param unit the compilation unit
   */
  public MultiStateCompilationUnitChange(final String name, final CompilationUnit unit) {
    super(name, (IFile) unit.getResource());

    fUnit = unit;

    setTextType("java"); //$NON-NLS-1$
  }

  @SuppressWarnings("rawtypes")
  @Override
  public final Object getAdapter(final Class adapter) {
    if (CompilationUnit.class.equals(adapter)) {
      return fUnit;
    }

    return super.getAdapter(adapter);
  }

  /**
   * Returns the compilation unit.
   * 
   * @return the compilation unit
   */
  public final CompilationUnit getCompilationUnit() {
    return fUnit;
  }

//  @Override
//  public String getName() {
//    return Messages.format(
//        RefactoringCoreMessages.MultiStateCompilationUnitChange_name_pattern,
//        new String[] {
//            BasicElementLabels.getFileName(fUnit),
//            BasicElementLabels.getPathLabel(fUnit.getParent().getPath(), false)});
//  }

}
