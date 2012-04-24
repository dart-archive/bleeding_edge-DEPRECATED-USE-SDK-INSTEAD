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
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameTypeProcessor}.
 */
public final class RenameTypeProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link Type}.
   */
  private static void renameType(Type type, String newName) throws Exception {
    TestProject.waitForAutoBuild();
    RenameSupport renameSupport = RenameSupport.create(type, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameTypeProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}");
    Type type = findElement("Test {");
    // do check
    RenameTypeProcessor processor = new RenameTypeProcessor(type);
    assertEquals(RenameTypeProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("Test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}");
    Type type = findElement("Test {");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "Test");
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
        "class Test {",
        "}");
    Type type = findElement("Test {");
    // try to rename
    showStatusCancel = false;
    renameType(type, "newName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "By convention, type names usually start with an uppercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class newName {",
        "}");
  }

  public void test_OK_interfaceFactory_hasImpl_renameFactory() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface I factory F {",
        "  I();",
        "  I.named();",
        "}",
        "class F implements I {",
        "  factory F() {}",
        "  factory F.named() {}",
        "}",
        "f() {",
        "  new I();",
        "  new I.named();",
        "}",
        "");
    Type type = findElement("F implements I {");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface I factory NewName {",
        "  I();",
        "  I.named();",
        "}",
        "class NewName implements I {",
        "  factory NewName() {}",
        "  factory NewName.named() {}",
        "}",
        "f() {",
        "  new I();",
        "  new I.named();",
        "}",
        "");
  }

  public void test_OK_interfaceFactory_hasImpl_renameInterface() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface I factory F {",
        "  I();",
        "  I.named();",
        "}",
        "class F implements I {",
        "  factory F() {}",
        "  factory F.named() {}",
        "}",
        "f() {",
        "  new I();",
        "  new I.named();",
        "}",
        "");
    Type type = findElement("I factory F {");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface NewName factory F {",
        "  NewName();",
        "  NewName.named();",
        "}",
        "class F implements NewName {",
        "  factory F() {}",
        "  factory F.named() {}",
        "}",
        "f() {",
        "  new NewName();",
        "  new NewName.named();",
        "}",
        "");
  }

  public void test_OK_interfaceFactory_notImpl_renameFactory() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface I factory F {",
        "  I();",
        "  I.named();",
        "}",
        "class F { // marker",
        "  factory I() {}",
        "  factory I.named() {}",
        "}",
        "f() {",
        "  new I();",
        "  new I.named();",
        "}",
        "");
    Type type = findElement("F { // marker");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface I factory NewName {",
        "  I();",
        "  I.named();",
        "}",
        "class NewName { // marker",
        "  factory I() {}",
        "  factory I.named() {}",
        "}",
        "f() {",
        "  new I();",
        "  new I.named();",
        "}",
        "");
  }

  public void test_OK_interfaceFactory_notImpl_renameInterface() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface I factory F {",
        "  I();",
        "  I.named();",
        "}",
        "class F {",
        "  factory I() {}",
        "  factory I.named() {}",
        "}",
        "f() {",
        "  new I();",
        "  new I.named();",
        "}",
        "");
    Type type = findElement("I factory F {");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface NewName factory F {",
        "  NewName();",
        "  NewName.named();",
        "}",
        "class F {",
        "  factory NewName() {}",
        "  factory NewName.named() {}",
        "}",
        "f() {",
        "  new NewName();",
        "  new NewName.named();",
        "}",
        "");
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    setUnitContent(
        "Test1.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}");
    setUnitContent(
        "Test2.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  Test test = new Test();",
        "}");
    setUnitContent(
        "Test3.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class B extends Test {",
        "}");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('test');",
        "#source('Test1.dart');",
        "#source('Test2.dart');",
        "#source('Test3.dart');");
    // get units, because they have not library
    CompilationUnit unit1 = testProject.getUnit("Test1.dart");
    CompilationUnit unit2 = testProject.getUnit("Test2.dart");
    CompilationUnit unit3 = testProject.getUnit("Test3.dart");
    // find Type to rename
    Type type = findElement(unit2, "Test test =");
    // do rename
    renameType(type, "NewName");
    assertUnitContent(
        unit1,
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}");
    assertUnitContent(
        unit2,
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  NewName test = new NewName();",
        "}");
    assertUnitContent(
        unit3,
        "// filler filler filler filler filler filler filler filler filler filler",
        "class B extends NewName {",
        "}");
  }

  /**
   * When we rename {@link Type}, we should rename also its constructors and references to
   * constructors.
   */
  public void test_OK_renameConstructor() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "  Test() {}",
        "  Test.named() {}",
        "}",
        "f() {",
        "  new Test();",
        "  new Test.named();",
        "}",
        "");
    Type type = findElement("Test {");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "  NewName() {}",
        "  NewName.named() {}",
        "}",
        "f() {",
        "  new NewName();",
        "  new NewName.named();",
        "}",
        "");
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f() {",
        "  Test test = new Test();",
        "}",
        "class B extends Test {",
        "}");
    Type type = findElement("Test {");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}",
        "f() {",
        "  NewName test = new NewName();",
        "}",
        "class B extends NewName {",
        "}");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f() {",
        "  Test test = new Test();",
        "}",
        "class B extends Test {",
        "}");
    Type type = findElement("Test test =");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}",
        "f() {",
        "  NewName test = new NewName();",
        "}",
        "class B extends NewName {",
        "}");
  }

  public void test_OK_singleUnit_onReference_inSubClass() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f() {",
        "  Test test = new Test();",
        "}",
        "class B extends Test { // rename here",
        "}");
    Type type = findElement("Test { // rename here");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}",
        "f() {",
        "  NewName test = new NewName();",
        "}",
        "class B extends NewName { // rename here",
        "}");
  }

  /**
   * http://code.google.com/p/dart/issues/detail?id=1180
   */
  public void test_postCondition_field_shadowedBy_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  var NewName;",
        "}",
        "class B extends A {",
        "  foo() {",
        "    NewName = 1;", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    Type type = findElement("Test {");
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
          "Declaration of renamed type will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of field 'A.NewName' declared in 'Test/Test.dart' will be shadowed by renamed type",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_field_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  var NewName;",
        "  foo() {",
        "    NewName = 1;", // field of the enclosing class
        "    new Test();",
        "  }",
        "}",
        "");
    Type type = findElement("Test {");
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
          "Declaration of renamed type will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type will be shadowed by field 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "class A {",
        "  f() {",
        "    var NewName;",
        "    new Test();",
        "  }",
        "}",
        "");
    Type type = findElement("Test {");
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
          "Declaration of renamed type will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inTopLevelFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f() {",
        "  var NewName;",
        "  new Test();",
        "}",
        "");
    Type type = findElement("Test {");
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
          "Declaration of renamed type will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type will be shadowed by variable in function 'f()' in file 'Test/Test.dart'",
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
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    NewName();", // will be shadowed by top-level element
        "  }",
        "}",
        "");
    Type type = findElement("Test {");
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
          "Declaration of renamed type will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of method 'A.NewName' declared in 'Test/Test.dart' will be shadowed by renamed type",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_method_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A {",
        "  NewName() {}",
        "  foo() {",
        "    NewName();", // method of the enclosing class
        "    new Test();",
        "  }",
        "}",
        "");
    Type type = findElement("Test {");
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
          "Declaration of renamed type will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type will be shadowed by method 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_topLevel_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "class NewName {",
        "}",
        "");
    check_postCondition_topLevel("type");
  }

  public void test_postCondition_topLevel_function() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "NewName() {}",
        "");
    check_postCondition_topLevel("function");
  }

  public void test_postCondition_topLevel_functionTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef NewName(int p);",
        "class Test {",
        "}",
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
        "class Test {",
        "}",
        "");
    check_postCondition_topLevel("Lib.dart", "variable");
  }

  public void test_postCondition_topLevel_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var NewName;",
        "class Test {",
        "}",
        "");
    check_postCondition_topLevel("variable");
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setUnitContent(
        "Test1.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}");
    setUnitContent(
        "Test2.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  Test test = new Test();",
        "}",
        "somethingBad");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('test');",
        "#source('Test1.dart');",
        "#source('Test2.dart');");
    // get units, because they have not library
    CompilationUnit unit1 = testProject.getUnit("Test1.dart");
    CompilationUnit unit2 = testProject.getUnit("Test2.dart");
    Type type = findElement(unit1, "Test {");
    // try to rename
    showStatusCancel = false;
    renameType(type, "NewName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "Code modification may not be accurate as affected resource 'Test/Test2.dart' has compile errors.",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertUnitContent(
        unit1,
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}");
    assertUnitContent(
        unit2,
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  NewName test = new NewName();",
        "}",
        "somethingBad");
  }

  private void check_postCondition_topLevel(String shadowName) throws Exception {
    check_postCondition_topLevel("Test.dart", shadowName);
  }

  private void check_postCondition_topLevel(String unitName, String shadowName) throws Exception {
    Type type = findElement("Test {");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameType(type, "NewName");
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
