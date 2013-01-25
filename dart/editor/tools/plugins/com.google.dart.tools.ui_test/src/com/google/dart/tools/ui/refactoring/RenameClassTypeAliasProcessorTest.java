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
import com.google.dart.tools.core.model.DartClassTypeAlias;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameClassTypeAliasProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameClassTypeAliasProcessor}.
 */
public final class RenameClassTypeAliasProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link DartClassTypeAlias}.
   */
  private static void renameType(DartClassTypeAlias classTypeAlias, String newName)
      throws Exception {
    RenameSupport renameSupport = RenameSupport.create(classTypeAlias, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameTypeProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef Test = Object with A;",
        "");
    DartClassTypeAlias classTypeAlias = findElement("Test =");
    // do check
    RenameClassTypeAliasProcessor processor = new RenameClassTypeAliasProcessor(classTypeAlias);
    assertEquals(RenameClassTypeAliasProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("Test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef Test = Object with A;",
        "");
    DartClassTypeAlias classTypeAlias = findElement("Test =");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(classTypeAlias, "Test");
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
        "class A {}",
        "typedef Test = Object with A;",
        "");
    DartClassTypeAlias classTypeAlias = findElement("Test =");
    // try to rename
    showStatusCancel = false;
    renameType(classTypeAlias, "newName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "By convention, class names usually start with an uppercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef newName = Object with A;",
        "");
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    setUnitContent(
        "Test1.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of test;",
            "class A {}",
            "typedef Test = Object with A;",
            ""));
    setUnitContent(
        "Test2.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of test;",
            "f(Test test) {",
            "}"));
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library test;",
        "part 'Test1.dart';",
        "part 'Test2.dart';");
    // get units, because they have not library
    CompilationUnit unit1 = testProject.getUnit("Test1.dart");
    CompilationUnit unit2 = testProject.getUnit("Test2.dart");
    // find DartClassTypeAlias to rename
    DartClassTypeAlias classTypeAlias = findElement(unit2, "Test test");
    // do rename
    renameType(classTypeAlias, "NewName");
    assertUnitContent(
        unit1,
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of test;",
            "class A {}",
            "typedef NewName = Object with A;",
            ""));
    assertUnitContent(
        unit2,
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of test;",
            "f(NewName test) {",
            "}"));
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef Test = Object with A;",
        "f(Test test) {",
        "}",
        "");
    DartClassTypeAlias classTypeAlias = findElement("Test =");
    // do rename
    renameType(classTypeAlias, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef NewName = Object with A;",
        "f(NewName test) {",
        "}",
        "");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef Test = Object with A;",
        "f(Test test) {",
        "}");
    DartClassTypeAlias classTypeAlias = findElement("Test test)");
    // do rename
    renameType(classTypeAlias, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "typedef NewName = Object with A;",
        "f(NewName test) {",
        "}");
  }

  /**
   * http://code.google.com/p/dart/issues/detail?id=1180
   */
  public void test_postCondition_field_shadowedBy_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "class A {",
        "  var NewName;",
        "}",
        "class B extends A {",
        "  foo() {",
        "    NewName = 1;", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    DartClassTypeAlias type = findElement("Test = ");
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
          "Declaration of renamed class will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of field 'A.NewName' declared in 'Test/Test.dart' will be shadowed by renamed class",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_field_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "class A {",
        "  var NewName;",
        "  foo() {",
        "    NewName = 1;", // field of the enclosing class
        "    Test t;",
        "  }",
        "}",
        "");
    DartClassTypeAlias type = findElement("Test =");
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
          "Declaration of renamed class will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed class will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "class A {",
        "  f() {",
        "    var NewName;",
        "    Test t;",
        "  }",
        "}",
        "");
    DartClassTypeAlias type = findElement("Test =");
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
          "Declaration of renamed class will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed class will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inTopLevelFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "f() {",
        "  var NewName;",
        "  Test t;",
        "}",
        "");
    DartClassTypeAlias type = findElement("Test =");
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
          "Declaration of renamed class will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed class will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
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
        "class M {}",
        "typedef Test = Object with M;",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    NewName();", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    DartClassTypeAlias type = findElement("Test =");
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
          "Declaration of renamed class will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of method 'A.NewName' declared in 'Test/Test.dart' will be shadowed by renamed class",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_method_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "class A {",
        "  NewName() {}",
        "  foo() {",
        "    NewName();", // method of the enclosing class
        "    Test t;",
        "  }",
        "}",
        "");
    DartClassTypeAlias type = findElement("Test =");
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
          "Declaration of renamed class will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed class will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_topLevel_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "class NewName {",
        "}",
        "");
    check_postCondition_topLevel("type");
  }

  public void test_postCondition_topLevel_classTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName = Object with M;",
        "class M {}",
        "typedef Test = Object with M;",
        "");
    check_postCondition_topLevel("class");
  }

  public void test_postCondition_topLevel_function() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "NewName() {}",
        "");
    check_postCondition_topLevel("function");
  }

  public void test_postCondition_topLevel_functionTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName(int p);",
        "class M {}",
        "typedef Test = Object with M;",
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
        "class M {}",
        "typedef Test = Object with M;",
        "");
    check_postCondition_topLevel("Lib.dart", "variable");
  }

  public void test_postCondition_topLevel_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var NewName;",
        "class M {}",
        "typedef Test = Object with M;",
        "");
    check_postCondition_topLevel("variable");
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class M {}",
        "typedef Test = Object with M;",
        "f(Test test) {",
        "}",
        "somethingBad");
    waitForErrorMarker(testUnit);
    DartClassTypeAlias classTypeAlias = findElement("Test =");
    // try to rename
    showStatusCancel = false;
    renameType(classTypeAlias, "NewName");
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
        "class M {}",
        "typedef NewName = Object with M;",
        "f(NewName test) {",
        "}",
        "somethingBad");
  }

  private void check_postCondition_topLevel(String shadowName) throws Exception {
    check_postCondition_topLevel("Test.dart", shadowName);
  }

  private void check_postCondition_topLevel(String unitName, String shadowName) throws Exception {
    DartClassTypeAlias classTypeAlias = findElement("Test =");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(classTypeAlias, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals("File 'Test/" + unitName + "' in library 'Test' already declares top-level "
        + shadowName + " 'NewName'", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }
}
