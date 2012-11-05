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
package com.google.dart.tools.ui.refactoring;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameImportProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameImportProcessor}.
 */
public final class RenameImportProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link DartImport}.
   */
  private static void renameImport(DartImport imprt, String newName) throws Exception {
    RenameSupport renameSupport = RenameSupport.create(imprt, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameTypeProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('LibA.dart', prefix: 'aaa');",
        "");
    DartImport imprt = findElement("#import('LibA.dart");
    // do check
    RenameImportProcessor processor = new RenameImportProcessor(imprt);
    assertEquals(RenameImportProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("aaa", processor.getCurrentElementName());
    assertThat(processor.getElements()).containsOnly(imprt);
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('LibA.dart', prefix: 'test');",
        "");
    DartImport imprt = findElement("#import('LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "test");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.FATAL, showStatusSeverities.get(0).intValue());
    assertEquals("Choose another name.", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_invalidIdentifier() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('LibA.dart', prefix: 'test');",
        "");
    DartImport imprt = findElement("#import('LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "not-identifier");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.FATAL, showStatusSeverities.get(0).intValue());
    assertEquals("The prefix 'not-identifier' is not a valid identifier", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    prepareUniqueLibraries();
    setUnitContent("Test1.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of Test;",
        "foo1() {",
        "  test.A a;",
        "  test.B b;",
        "}",
        ""});
    setUnitContent("Test2.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of Test;",
        "foo2() {",
        "  test.A a;",
        "  test.B b;",
        "}",
        ""});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Test;",
        "import 'LibA.dart' as test;",
        "import 'LibB.dart' as test;",
        "part 'Test1.dart';",
        "part 'Test2.dart';",
        "");
    // get units, because they have not library
    CompilationUnit unit1 = testProject.getUnit("Test1.dart");
    CompilationUnit unit2 = testProject.getUnit("Test2.dart");
    DartImport imprt = findElement("import 'LibA.dart");
    // do rename
    renameImport(imprt, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Test;",
        "import 'LibA.dart' as newName;",
        "import 'LibB.dart' as test;",
        "part 'Test1.dart';",
        "part 'Test2.dart';",
        "");
    assertUnitContent(unit1, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of Test;",
        "foo1() {",
        "  newName.A a;",
        "  test.B b;",
        "}",
        ""});
    assertUnitContent(unit2, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of Test;",
        "foo2() {",
        "  newName.A a;",
        "  test.B b;",
        "}",
        ""});
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "import 'LibB.dart' as test;",
        "f() {",
        "  test.A a;",
        "  test.B b;",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // do rename
    renameImport(imprt, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as newName;",
        "import 'LibB.dart' as test;",
        "f() {",
        "  newName.A a;",
        "  test.B b;",
        "}",
        "");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "import 'LibB.dart' as test;",
        "f() {",
        "  test.A a;",
        "  test.B b;",
        "}",
        "");
    DartImport imprt = findElement("test.A a;");
    // do rename
    renameImport(imprt, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as newName;",
        "import 'LibB.dart' as test;",
        "f() {",
        "  newName.A a;",
        "  test.B b;",
        "}",
        "");
  }

  public void test_OK_uniqueTop_wasNoPrefix() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart';",
        "import 'LibB.dart';",
        "f() {",
        "  A a;",
        "  B b;",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // do rename
    renameImport(imprt, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as newName;",
        "import 'LibB.dart';",
        "f() {",
        "  newName.A a;",
        "  B b;",
        "}",
        "");
  }

  public void test_OK_uniqueTop_willNoPrefix() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "import 'LibB.dart' as test;",
        "f() {",
        "  test.A a;",
        "  test.B b;",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // do rename
    renameImport(imprt, "");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart';",
        "import 'LibB.dart' as test;",
        "f() {",
        "  A a;",
        "  test.B b;",
        "}",
        "");
  }

  public void test_postCondition_duplicateTopLevels_withSamePrefix() throws Exception {
    // prepare libraries with type "A"
    testProject.setUnitContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('A');",
            "class A {}",
            "")).getResource();
    testProject.setUnitContent(
        "LibB.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('B');",
            "class A {}",
            "")).getResource();
    //
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('LibA.dart', prefix: 'test');",
        "#import('LibB.dart', prefix: 'newName');",
        "");
    DartImport imprt = findElement("#import('LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(1);
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Prefix 'newName' is used to import libraries 'Test/LibA.dart' and 'Test/LibB.dart', but both declare top-level elements '[A]'",
          showStatusMessages.get(0));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  /**
   * http://code.google.com/p/dart/issues/detail?id=1180
   */
  public void test_postCondition_field_shadowedBy_topLevel() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('LibA.dart', prefix: 'test');",
        "class A {",
        "  var newName;",
        "}",
        "class B extends A {",
        "  foo() {",
        "    newName = 1;", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    DartImport imprt = findElement("#import('LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for declaration in A
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed import prefix will be shadowed by field 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of field 'A.newName' declared in 'Test/Test.dart' will be shadowed by renamed import prefix",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_field_shadows_topLevel() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "class A {",
        "  var newName;",
        "  foo() {",
        "    newName = 1;", // field of the enclosing class
        "    new test.A();",
        "  }",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for shadowing declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed import prefix will be shadowed by field 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed import prefix will be shadowed by field 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inMethod() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "class A {",
        "  f() {",
        "    var newName;",
        "    new test.A();",
        "  }",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for shadowing declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed import prefix will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed import prefix will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inTopLevelFunction() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "f() {",
        "  var newName;",
        "  new test.A();",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for shadowing declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed import prefix will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed import prefix will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  /**
   * http://code.google.com/p/dart/issues/detail?id=1180
   */
  public void test_postCondition_method_shadowedBy_topLevel() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "class A {",
        "  newName() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    newName();", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for declaration in A
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed import prefix will be shadowed by method 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of method 'A.newName' declared in 'Test/Test.dart' will be shadowed by renamed import prefix",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_method_shadows_topLevel() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "class A {",
        "  newName() {}",
        "  foo() {",
        "    newName();", // method of the enclosing class
        "    new test.A();",
        "  }",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for shadowing declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed import prefix will be shadowed by method 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed import prefix will be shadowed by method 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_topLevel_class() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "class newName {",
        "}",
        "");
    check_postCondition_topLevel("type");
  }

  public void test_postCondition_topLevel_function() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "newName() {}",
        "");
    check_postCondition_topLevel("function");
  }

  public void test_postCondition_topLevel_functionTypeAlias() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "typedef newName(int p);",
        "");
    check_postCondition_topLevel("function type alias");
  }

  public void test_postCondition_topLevel_otherLibrary() throws Exception {
    prepareUniqueLibraries();
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Lib;",
        "var newName;");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Test;",
        "import 'LibA.dart';",
        "import 'Lib.dart';",
        "");
    check_postCondition_topLevel("Lib.dart", "variable");
  }

  public void test_postCondition_topLevel_variable() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "var newName;",
        "");
    check_postCondition_topLevel("variable");
  }

  public void test_postCondition_typeParameter_shadows_topLevel() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "class A<newName> {",
        "  test.A a;",
        "}",
        "");
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for shadowing declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed import prefix will be shadowed by type parameter 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed import prefix will be shadowed by type parameter 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    prepareUniqueLibraries();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as test;",
        "import 'LibB.dart' as test;",
        "f() {",
        "  test.A a;",
        "  test.B b;",
        "}",
        "something bad",
        "");
    waitForErrorMarker(testUnit);
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    showStatusCancel = false;
    renameImport(imprt, "newName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "Code modification may not be accurate as affected resource 'Test/Test.dart' has compile errors.",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'LibA.dart' as newName;",
        "import 'LibB.dart' as test;",
        "f() {",
        "  newName.A a;",
        "  test.B b;",
        "}",
        "something bad",
        "");
  }

  private void check_postCondition_topLevel(String shadowName) throws Exception {
    check_postCondition_topLevel("Test.dart", shadowName);
  }

  private void check_postCondition_topLevel(String unitName, String shadowName) throws Exception {
    DartImport imprt = findElement("import 'LibA.dart");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameImport(imprt, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals("File 'Test/"
        + unitName
        + "' in library 'Test' already declares top-level "
        + shadowName
        + " 'newName'", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  /**
   * Prepares "LibA.dart" and "LibB.dart" with classes "A" and "B" respectively.
   */
  private void prepareUniqueLibraries() throws Exception {
    testProject.setUnitContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "class A {}",
            "")).getResource();
    testProject.setUnitContent(
        "LibB.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libB;",
            "class B {}",
            "")).getResource();
  }
}
