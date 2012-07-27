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
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameGlobalVariableProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameGlobalVariableProcessor}.
 */
public final class RenameGlobalVariableProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link DartVariableDeclaration}.
   */
  private static void renameVariable(DartVariableDeclaration variable, String newName)
      throws Exception {
    RenameSupport renameSupport = RenameSupport.create(variable, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameGlobalVariableProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // do check
    RenameGlobalVariableProcessor processor = new RenameGlobalVariableProcessor(variable);
    assertEquals(RenameGlobalVariableProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "test");
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

  public void test_badNewName_shouldBeLowerCase() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    showStatusCancel = false;
    renameVariable(variable, "NewName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "By convention, variable names usually start with a lowercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var NewName;",
        "");
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    setUnitContent(
        "Test1.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "");
    setUnitContent(
        "Test2.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  test = 1;",
        "}");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('test');",
        "#source('Test1.dart');",
        "#source('Test2.dart');");
    // get units, because they have not library
    CompilationUnit unit1 = testProject.getUnit("Test1.dart");
    CompilationUnit unit2 = testProject.getUnit("Test2.dart");
    // find Function to rename
    DartVariableDeclaration variable = findElement(unit2, "test = 1;");
    // do rename
    renameVariable(variable, "newName");
    assertUnitContent(
        unit1,
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "");
    assertUnitContent(
        unit2,
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  newName = 1;",
        "}");
  }

  /**
   * <p>
   * http://code.google.com/p/dart/issues/detail?id=2749
   */
  public void test_OK_referenceFromQualifiedInvocation() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "f() {",
        "  var server;",
        "  server.listen(test);",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // do rename
    renameVariable(variable, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "f() {",
        "  var server;",
        "  server.listen(newName);",
        "}",
        "");
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "f() {",
        "  test = 1;",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // do rename
    renameVariable(variable, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "f() {",
        "  newName = 1;",
        "}",
        "");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "f() {",
        "  test = 1;",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test = 1;");
    // do rename
    renameVariable(variable, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "f() {",
        "  newName = 1;",
        "}",
        "");
  }

  /**
   * http://code.google.com/p/dart/issues/detail?id=1180
   */
  public void test_postCondition_field_shadowedBy_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "class A {",
        "  var newName;",
        "}",
        "class B extends A {",
        "  foo() {",
        "    newName = 1;", // will be shadowed by global variable
        "  }",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "newName");
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
          "Declaration of renamed variable will be shadowed by field 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of field 'A.newName' declared in 'Test/Test.dart' will be shadowed by renamed variable",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_field_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "class A {",
        "  var newName;",
        "  foo() {",
        "    newName = 1;", // field of the enclosing class
        "    test = 2;",
        "  }",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "newName");
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
          "Declaration of renamed variable will be shadowed by field 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed variable will be shadowed by field 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "class A {",
        "  f() {",
        "    var newName;",
        "    test = 1;",
        "  }",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "newName");
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
          "Declaration of renamed variable will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed variable will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inTopLevelFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "f() {",
        "  var newName;",
        "  test = 1;",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "newName");
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
          "Declaration of renamed variable will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed variable will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  /**
   * http://code.google.com/p/dart/issues/detail?id=1180
   */
  public void test_postCondition_method_shadowedBy_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "class A {",
        "  newName() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    newName();", // will be shadowed by global variable
        "  }",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "newName");
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
          "Declaration of renamed variable will be shadowed by method 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of method 'A.newName' declared in 'Test/Test.dart' will be shadowed by renamed variable",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_method_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "class A {",
        "  newName() {}",
        "  foo() {",
        "    newName();", // method of the enclosing class
        "    test = 1;",
        "  }",
        "}",
        "");
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "newName");
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
          "Declaration of renamed variable will be shadowed by method 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed variable will be shadowed by method 'A.newName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_topLevel_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "class newName {",
        "}",
        "");
    check_postCondition_topLevel("type");
  }

  public void test_postCondition_topLevel_function() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "newName() {}",
        "");
    check_postCondition_topLevel("function");
  }

  public void test_postCondition_topLevel_functionTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef newName(int p);",
        "var test;",
        "");
    check_postCondition_topLevel("function type alias");
  }

  public void test_postCondition_topLevel_otherLibrary() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "var newName;");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "var test;",
        "");
    check_postCondition_topLevel("Lib.dart", "variable");
  }

  public void test_postCondition_topLevel_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "var test;",
        "");
    check_postCondition_topLevel("variable");
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "f() {",
        "  test = 1;",
        "}",
        "somethingBad");
    waitForErrorMarker(testUnit);
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    showStatusCancel = false;
    renameVariable(variable, "newName");
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
        "var newName;",
        "f() {",
        "  newName = 1;",
        "}",
        "somethingBad");
  }

  private void check_postCondition_topLevel(String shadowName) throws Exception {
    check_postCondition_topLevel("Test.dart", shadowName);
  }

  private void check_postCondition_topLevel(String unitName, String shadowName) throws Exception {
    DartVariableDeclaration variable = findElement("test;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameVariable(variable, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals("File 'Test/" + unitName + "' in library 'Test' already declares top-level "
        + shadowName + " 'newName'", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }
}
