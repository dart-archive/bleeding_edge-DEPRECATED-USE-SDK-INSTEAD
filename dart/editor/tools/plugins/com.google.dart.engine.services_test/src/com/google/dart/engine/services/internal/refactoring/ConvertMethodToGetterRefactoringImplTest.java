/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.ConvertMethodToGetterRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.Source;

/**
 * Test for {@link ConvertMethodToGetterRefactoringImpl}.
 */
public class ConvertMethodToGetterRefactoringImplTest extends RefactoringImplTest {
  private ExecutableElement selectionElement;
  private ConvertMethodToGetterRefactoringImpl refactoring;
  private RefactoringStatus refactoringStatus;
  private Change refactoringChange;

  public void test_access() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test() => 42;",
        "}");
    selectionElement = findIdentifierElement("test() =>");
    createRefactoring();
    // check
    assertEquals("Convert Method to Getter", refactoring.getRefactoringName());
  }

  public void test_FunctionElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int test() => 42;",
        "main() {",
        "  var v = test();",
        "}");
    selectionElement = findIdentifierElement("test() =>");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int get test => 42;",
        "main() {",
        "  var v = test;",
        "}");
  }

  public void test_FunctionElement_noReturnType() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 42;",
        "main() {",
        "  var v = test();",
        "}");
    selectionElement = findIdentifierElement("test() =>");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get test => 42;",
        "main() {",
        "  var v = test;",
        "}");
  }

  public void test_ignoreSetter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() => 42;",
        "  set test(x) {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.test = 0;",
        "  var v = a.test();",
        "}");
    selectionElement = findIdentifierElement("test() => 42");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test => 42;",
        "  set test(x) {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.test = 0;",
        "  var v = a.test;",
        "}");
  }

  public void test_initialConditions_bad_alreadyGetter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test => 42;",
        "}");
    selectionElement = findIdentifierElement("test =>");
    createRefactoring();
    // check
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Only class method or top-level function can be converted to getter.");
  }

  public void test_initialConditions_bad_hasParameters() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test(x) => 42;",
        "}");
    selectionElement = findIdentifierElement("test(x) =>");
    createRefactoring();
    // check
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Only method without parameters can be converted to getter.");
  }

  public void test_initialConditions_bad_localFunction() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  test() => 42;",
        "}");
    selectionElement = findIdentifierElement("test() =>");
    createRefactoring();
    // check
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Function should be top-level to converted it to getter.");
  }

  public void test_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test() => 42;",
        "}",
        "main() {",
        "  A a = new A();",
        "  var v = a.test();",
        "}");
    selectionElement = findIdentifierElement("test() =>");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get test => 42;",
        "}",
        "main() {",
        "  A a = new A();",
        "  var v = a.test;",
        "}");
  }

  public void test_MethodElement_processHierarchy() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() => 1;",
        "}",
        "class B extends A {",
        "  test() => 2;",
        "}",
        "class C extends B {",
        "  test() => 3;",
        "}",
        "class D extends C {",
        "  test() => 4;",
        "}",
        "class E extends D {",
        "  test() => 5;",
        "}",
        "",
        "main() {",
        "  A a = new E();",
        "  var v = a.test();",
        "}");
    selectionElement = findIdentifierElement("test() => 3");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test => 1;",
        "}",
        "class B extends A {",
        "  get test => 2;",
        "}",
        "class C extends B {",
        "  get test => 3;",
        "}",
        "class D extends C {",
        "  get test => 4;",
        "}",
        "class E extends D {",
        "  get test => 5;",
        "}",
        "",
        "main() {",
        "  A a = new E();",
        "  var v = a.test;",
        "}");
  }

  public void test_unitSharedBetweenLibraries() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int test() => 42;");
    Source sourceShared = addSource(
        "/shared.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of my_lib;",
            "foo() {",
            "  return test();",
            "}"));
    Source sourceA = addSource(
        "/libA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library my_lib;",
            "import 'Test.dart';",
            "part 'shared.dart';"));
    Source sourceB = addSource(
        "/libB.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library my_lib;",
            "import 'Test.dart';",
            "part 'shared.dart';"));
    CompilationUnit sharedUnitA = analysisContext.resolveCompilationUnit(sourceShared, sourceA);
    CompilationUnit sharedUnitB = analysisContext.resolveCompilationUnit(sourceShared, sourceB);
    indexUnit(sharedUnitA);
    indexUnit(sharedUnitB);
    selectionElement = findIdentifierElement("test() =>");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int get test => 42;");
    assertChangeResult(
        refactoringChange,
        sourceShared,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of my_lib;",
            "foo() {",
            "  return test;",
            "}"));
  }

  /**
   * Checks that all conditions are <code>OK</code> and applying {@link Change} to the
   * {@link #testUnit} is same source as given lines.
   */
  protected final void assertSuccessfulRefactoring(String... lines) throws Exception {
    assertRefactoringStatusOK(refactoringStatus);
    refactoringChange = refactoring.createChange(pm);
    assertTestChangeResult(refactoringChange, makeSource(lines));
  }

  @Override
  protected void tearDown() throws Exception {
    selectionElement = null;
    refactoring = null;
    refactoringStatus = null;
    refactoringChange = null;
    super.tearDown();
  }

  /**
   * Creates {@link ConvertMethodToGetterRefactoring} in {@link #refactoring}.
   */
  private void createRefactoring() throws Exception {
    refactoring = new ConvertMethodToGetterRefactoringImpl(searchEngine, selectionElement);
    // prepare status
    refactoringStatus = refactoring.checkAllConditions(pm);
  }
}
