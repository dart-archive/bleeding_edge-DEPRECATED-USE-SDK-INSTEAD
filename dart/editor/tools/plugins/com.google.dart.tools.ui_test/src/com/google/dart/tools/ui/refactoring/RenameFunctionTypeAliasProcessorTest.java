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
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameFunctionTypeAliasProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameFunctionTypeAliasProcessor}.
 */
public final class RenameFunctionTypeAliasProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link DartFunctionTypeAlias}.
   */
  private static void renameType(DartFunctionTypeAlias functionTypeAlias, String newName)
      throws Exception {
    RenameSupport renameSupport = RenameSupport.create(functionTypeAlias, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameTypeProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "");
    DartFunctionTypeAlias functionTypeAlias = findElement("Test();");
    // do check
    RenameFunctionTypeAliasProcessor processor = new RenameFunctionTypeAliasProcessor(
        functionTypeAlias);
    assertEquals(RenameFunctionTypeAliasProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("Test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "");
    DartFunctionTypeAlias functionTypeAlias = findElement("Test();");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(functionTypeAlias, "Test");
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

  public void test_badNewName_shouldBeUpperCase() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "");
    DartFunctionTypeAlias functionTypeAlias = findElement("Test();");
    // try to rename
    showStatusCancel = false;
    renameType(functionTypeAlias, "newName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "By convention, function type alias names usually start with an uppercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef newName();",
        "");
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    setUnitContent("Test1.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "typedef Test();",
        ""});
    setUnitContent("Test2.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "f(Test test) {",
        "}"});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library test;",
        "part 'Test1.dart';",
        "part 'Test2.dart';");
    // get units, because they have not library
    CompilationUnit unit1 = testProject.getUnit("Test1.dart");
    CompilationUnit unit2 = testProject.getUnit("Test2.dart");
    // find DartFunctionTypeAlias to rename
    DartFunctionTypeAlias functionTypeAlias = findElement(unit2, "Test test");
    // do rename
    renameType(functionTypeAlias, "NewName");
    assertUnitContent(unit1, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "typedef NewName();",
        ""});
    assertUnitContent(unit2, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "f(NewName test) {",
        "}"});
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "f(Test test) {",
        "}",
        "");
    DartFunctionTypeAlias functionTypeAlias = findElement("Test();");
    // do rename
    renameType(functionTypeAlias, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName();",
        "f(NewName test) {",
        "}",
        "");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "f(Test test) {",
        "}");
    DartFunctionTypeAlias functionTypeAlias = findElement("Test test)");
    // do rename
    renameType(functionTypeAlias, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName();",
        "f(NewName test) {",
        "}");
  }

  /**
   * http://code.google.com/p/dart/issues/detail?id=1180
   */
  public void test_postCondition_field_shadowedBy_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "class A {",
        "  var NewName;",
        "}",
        "class B extends A {",
        "  foo() {",
        "    NewName = 1;", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    DartFunctionTypeAlias type = findElement("Test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "NewName");
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
          "Declaration of renamed function type alias will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of field 'A.NewName' declared in 'Test/Test.dart' will be shadowed by renamed function type alias",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_field_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "class A {",
        "  var NewName;",
        "  foo() {",
        "    NewName = 1;", // field of the enclosing class
        "    Test t;",
        "  }",
        "}",
        "");
    DartFunctionTypeAlias type = findElement("Test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "NewName");
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
          "Declaration of renamed function type alias will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed function type alias will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "class A {",
        "  f() {",
        "    var NewName;",
        "    Test t;",
        "  }",
        "}",
        "");
    DartFunctionTypeAlias type = findElement("Test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "NewName");
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
          "Declaration of renamed function type alias will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed function type alias will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inTopLevelFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "f() {",
        "  var NewName;",
        "  Test t;",
        "}",
        "");
    DartFunctionTypeAlias type = findElement("Test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "NewName");
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
          "Declaration of renamed function type alias will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed function type alias will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
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
        "typedef Test();",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    NewName();", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    DartFunctionTypeAlias type = findElement("Test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "NewName");
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
          "Declaration of renamed function type alias will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of method 'A.NewName' declared in 'Test/Test.dart' will be shadowed by renamed function type alias",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_method_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "class A {",
        "  NewName() {}",
        "  foo() {",
        "    NewName();", // method of the enclosing class
        "    Test t;",
        "  }",
        "}",
        "");
    DartFunctionTypeAlias type = findElement("Test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "NewName");
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
          "Declaration of renamed function type alias will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed function type alias will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_topLevel_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "class NewName {",
        "}",
        "");
    check_postCondition_topLevel("type");
  }

  public void test_postCondition_topLevel_function() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "NewName() {}",
        "");
    check_postCondition_topLevel("function");
  }

  public void test_postCondition_topLevel_functionTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName(int p);",
        "typedef Test();",
        "");
    check_postCondition_topLevel("function type alias");
  }

  public void test_postCondition_topLevel_otherLibrary() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "var NewName;");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "typedef Test();",
        "");
    check_postCondition_topLevel("Lib.dart", "variable");
  }

  public void test_postCondition_topLevel_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var NewName;",
        "typedef Test();",
        "");
    check_postCondition_topLevel("variable");
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "f(Test test) {",
        "}",
        "somethingBad");
    waitForErrorMarker(testUnit);
    DartFunctionTypeAlias functionTypeAlias = findElement("Test();");
    // try to rename
    showStatusCancel = false;
    renameType(functionTypeAlias, "NewName");
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
        "typedef NewName();",
        "f(NewName test) {",
        "}",
        "somethingBad");
  }

  private void check_postCondition_topLevel(String shadowName) throws Exception {
    check_postCondition_topLevel("Test.dart", shadowName);
  }

  private void check_postCondition_topLevel(String unitName, String shadowName) throws Exception {
    DartFunctionTypeAlias functionTypeAlias = findElement("Test();");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(functionTypeAlias, "NewName");
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
        + " 'NewName'", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }
}
