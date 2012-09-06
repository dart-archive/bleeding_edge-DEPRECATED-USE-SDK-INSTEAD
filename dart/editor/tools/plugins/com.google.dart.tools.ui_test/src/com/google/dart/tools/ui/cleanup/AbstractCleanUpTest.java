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
package com.google.dart.tools.ui.cleanup;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.ui.internal.cleanup.migration.AbstractMigrateCleanUp;
import com.google.dart.tools.ui.refactoring.AbstractDartTest;

/**
 * Test for {@link AbstractMigrateCleanUp}.
 */
public abstract class AbstractCleanUpTest extends AbstractDartTest {

  protected final void assertCleanUp(ICleanUp cleanUp, String initial, String expected)
      throws Exception {
    ICleanUpFix fix = prepareFix(cleanUp, initial);
    assertNotNull(fix);
    // assert result
    CompilationUnitChange unitChange = fix.createChange(null);
    String result = unitChange.getPreviewContent(null);
    assertEquals(expected, result);
  }

  protected final void assertNoFix(ICleanUp cleanUp, String initial) throws Exception {
    ICleanUpFix fix = prepareFix(cleanUp, initial);
    assertNull(fix);
  }

  private ICleanUpFix prepareFix(ICleanUp cleanUp, String initial) throws Exception {
    setTestUnitContent(initial);
    // prepare CleanUpContext
    CleanUpContext context;
    {
      DartUnit ast = DartCompilerUtilities.resolveUnit(testUnit);
      context = new CleanUpContext(testUnit, ast);
    }
    // check requirements
    {
      CleanUpRequirements requirements = cleanUp.getRequirements();
      assertTrue(requirements.requiresAST());
    }
    // prepare ICleanUpFix
    ICleanUpFix fix = cleanUp.createFix(context);
    return fix;
  }

}
