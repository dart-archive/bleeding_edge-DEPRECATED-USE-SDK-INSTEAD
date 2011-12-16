/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.tools.core.utilities.ast;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyCompilationUnit;

import junit.framework.TestCase;

import java.util.ArrayList;

public class DartElementLocatorTest extends TestCase {
  public void test_DartElementLocator_searchWithin_declaration_with() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    assertLocation(true, unit, 418, true);
  }

  public void test_DartElementLocator_searchWithin_declaration_without() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    assertLocation(false, unit, 418, false);
  }

  public void test_DartElementLocator_searchWithin_reference() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("simple_money.dart");
    assertLocation(true, unit, 394, false);
  }

  private void assertLocation(boolean expectElement, CompilationUnit unit, int offset,
      boolean includeDeclarations) throws DartModelException {
    DartUnit ast = DartCompilerUtilities.resolveUnit(unit, new ArrayList<DartCompilationError>());
    DartElementLocator locator = new DartElementLocator(unit, offset, includeDeclarations);
    DartElement result = locator.searchWithin(ast);
    if (expectElement) {
      assertNotNull(result);
    } else {
      assertNull(result);
    }
  }
}
