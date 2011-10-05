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
package com.google.dart.tools.core.utilities.compiler;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.test.util.MoneyProjectUtilities;

import junit.framework.TestCase;

public class DartCompilerUtilitiesTest extends TestCase {
  public void test_DartCompilerUtilities_resolveUnit_bundled() throws Exception {
    DartLibrary[] libraries = DartModelManager.getInstance().getDartModel().getBundledLibraries();
    assertNotNull(libraries);
    assertTrue(libraries.length > 0);
    CompilationUnit[] units = libraries[0].getCompilationUnits();
    assertNotNull(units);
    assertTrue(units.length > 0);
    DartUnit ast = DartCompilerUtilities.resolveUnit(units[0]);
    assertNotNull(ast);
  }

  public void test_DartCompilerUtilities_resolveUnit_nonBundled() throws Exception {
    CompilationUnit unit = MoneyProjectUtilities.getMoneyCompilationUnit("simple_money.dart");
    DartUnit ast = DartCompilerUtilities.resolveUnit(unit);
    assertNotNull(ast);
  }
}
