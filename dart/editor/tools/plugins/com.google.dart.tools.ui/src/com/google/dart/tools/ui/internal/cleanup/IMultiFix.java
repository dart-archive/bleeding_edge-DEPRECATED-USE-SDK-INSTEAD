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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.cleanup.CleanUpContext;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.text.dart.IProblemLocation;

public interface IMultiFix extends ICleanUp {

  public class MultiFixContext extends CleanUpContext {

    private final IProblemLocation[] fLocations;

    public MultiFixContext(CompilationUnit unit, DartUnit ast, IProblemLocation[] locations) {
      super(unit, ast);
      fLocations = locations;
    }

    /**
     * @return locations of problems to fix.
     */
    public IProblemLocation[] getProblemLocations() {
      return fLocations;
    }
  }

  /**
   * True if <code>problem</code> in <code>ICompilationUnit</code> can be fixed by this CleanUp.
   * <p>
   * <strong>This must be a fast operation, the result can be a guess.</strong>
   * </p>
   * 
   * @param compilationUnit The compilation unit to fix not null
   * @param problem The location of the problem to fix
   * @return True if problem can be fixed
   */
  public boolean canFix(CompilationUnit compilationUnit, IProblemLocation problem);

  /**
   * Maximal number of problems this clean up will fix in compilation unit. There may be less then
   * the returned number but never more.
   * 
   * @param compilationUnit The compilation unit to fix, not null
   * @return The maximal number of fixes or -1 if unknown.
   */
  public abstract int computeNumberOfFixes(DartUnit compilationUnit);

}
