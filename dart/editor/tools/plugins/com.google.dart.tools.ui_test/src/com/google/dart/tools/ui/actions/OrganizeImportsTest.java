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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.internal.corext.codemanipulation.OrganizeImportsOperation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.refactoring.RefactoringTest;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Test for {@link OrganizeImportsAction} and {@link OrganizeImportsOperation}.
 */
public final class OrganizeImportsTest extends RefactoringTest {
  private OrganizeImportsAction action;

  public void test_empty() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "");
    doOrganize(testUnit);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "");
  }

  public void test_multiple() throws Exception {
    CompilationUnit unitA = setUnitContent(
        "LibA.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "import 'fooB';",
            "import 'fooA';",
            ""));
    CompilationUnit unitB = setUnitContent(
        "LibB.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libB;",
            "import 'barB';",
            "import 'barA';",
            ""));
    doOrganize(unitA, unitB);
    assertUnitContent(
        unitA,
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "import 'fooA';",
            "import 'fooB';",
            ""));
    assertUnitContent(
        unitB,
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libB;",
            "import 'barA';",
            "import 'barB';",
            ""));
  }

  public void test_multiple_withSingle() throws Exception {
    CompilationUnit unitA = setUnitContent(
        "LibA.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "import 'fooB';",
            "import 'fooA';",
            ""));
    doOrganize(unitA);
    assertUnitContent(
        unitA,
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "import 'fooA';",
            "import 'fooB';",
            ""));
  }

  public void test_notLibraryUnit() throws Exception {
    String[] libLines = formatLines(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library myLib;",
        "part 'Test.dart';",
        "",
        "import 'dart:io';",
        "import 'dart:isolate';",
        "");
    CompilationUnit libUnit = setUnitContent("Lib.dart", libLines);
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "");
    // Test.dart is not library, it is part of library, so we ignore request
    doOrganize(testUnit);
    assertUnitContent(libUnit, libLines);
  }

  public void test_single() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:io';",
        "import 'fooB';",
        "import 'fooA';",
        "import 'package:bbb';",
        "import 'package:aaa';",
        "import 'dart:isolate';",
        "");
    doOrganize(testUnit);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:io';",
        "import 'dart:isolate';",
        "import 'package:aaa';",
        "import 'package:bbb';",
        "import 'fooA';",
        "import 'fooB';",
        "");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    action = new OrganizeImportsAction(window);
  }

  private void doOrganize(CompilationUnit... units) {
    action.doRun(new StructuredSelection(units), null, UIInstrumentation.getNullBuilder());
  }
}
