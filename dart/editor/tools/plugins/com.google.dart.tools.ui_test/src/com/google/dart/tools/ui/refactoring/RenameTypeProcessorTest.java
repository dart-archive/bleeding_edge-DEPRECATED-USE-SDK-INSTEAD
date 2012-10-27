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
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeProcessor;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameTypeProcessor}.
 */
public final class RenameTypeProcessorTest extends RefactoringTest {
  private static boolean renameUnit = false;

  /**
   * Uses {@link RenameSupport} to rename {@link Type}.
   */
  private static void renameType(Type type, String newName) throws Exception {
    RenameSupport renameSupport = RenameSupport.create(type, newName);
    // we rename type Test in unit Test.dart and usually don't want to rename unit
    {
      RenameTypeProcessor renameTypeProcessor = ReflectionUtils.invokeMethod(
          renameSupport,
          "getDartRenameProcessor()");
      renameTypeProcessor.setRenameUnit(renameUnit);
    }
    // do rename
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

  /**
   * When we make type private, we should warn about using outside of declaring library.
   */
  public void test_OK_addUnderscore_otherLibrary() throws Exception {
    setTestUnitContent(
        "#library('Test');",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f1() {",
        "  new Test();",
        "}",
        "");
    setUnitContent("User.dart", new String[] {
        "#library('User');",
        "#import('Test.dart');",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  new Test();",
        "}",
        ""});
    CompilationUnit userUnit = testProject.getUnit("User.dart");
    Type type = findElement("Test {");
    // do rename
    showStatusCancel = false;
    renameType(type, "_NewName");
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals(
        "Renamed type will become private, so will be not visible in library 'Test/User.dart'",
        showStatusMessages.get(0));
    assertTestUnitContent(
        "#library('Test');",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class _NewName {",
        "}",
        "f1() {",
        "  new _NewName();",
        "}",
        "");
    assertUnitContent(userUnit, new String[] {
        "#library('User');",
        "#import('Test.dart');",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  new _NewName();",
        "}",
        ""});
  }

  public void test_OK_addUnderscore_sameLibrary() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f() {",
        "  new Test();",
        "}",
        "");
    Type type = findElement("Test {");
    // do rename
    renameType(type, "_NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class _NewName {",
        "}",
        "f() {",
        "  new _NewName();",
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

  public void test_OK_renameUnit_whenNotCorrespondingType() throws Exception {
    renameUnit = true;
    CompilationUnit unit = setUnitContent("SomeName.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        ""});
    Type type = findElement(unit, "Test {");
    // do rename
    renameType(type, "NewName");
    // still same unit
    assertTrue(unit.exists());
    assertUnitContent(unit, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}",
        ""});
  }

  public void test_OK_renameUnit_whenSameNameAsType() throws Exception {
    renameUnit = true;
    CompilationUnit unit = setUnitContent("Test.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        ""});
    Type type = findElement(unit, "Test {");
    // do rename
    renameType(type, "NewName");
    unit = testProject.getUnit("NewName.dart");
    assertNotNull(unit);
    assertTrue(unit.exists());
    assertUnitContent(unit, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewName {",
        "}",
        ""});
  }

  public void test_OK_renameUnit_whenUnderscoreType() throws Exception {
    renameUnit = true;
    CompilationUnit unit = setUnitContent("old_type_name.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class OldTypeName {",
        "}",
        ""});
    Type type = findElement(unit, "OldTypeName {");
    // do rename
    renameType(type, "NewTypeName");
    unit = testProject.getUnit("new_type_name.dart");
    assertNotNull(unit);
    assertTrue(unit.exists());
    assertUnitContent(unit, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "class NewTypeName {",
        "}",
        ""});
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

  public void test_postCondition_localFunction_inMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "class A {",
        "  f() {",
        "    NewName() {};",
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
          "Declaration of renamed type will be shadowed by function in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type will be shadowed by function in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localFunction_inTopLevelFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f() {",
        "  NewName() {};",
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
          "Declaration of renamed type will be shadowed by function in function 'f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // warning for shadowing usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type will be shadowed by function in function 'f()' in file 'Test/Test.dart'",
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

  public void test_postCondition_topLevel_importPrefix() throws Exception {
    testProject.setUnitContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('A');",
            ""));
    testProject.setUnitContent(
        "LibB.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('B');",
            ""));
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('LibA.dart', prefix: 'NewName');",
        "#import('LibB.dart', prefix: 'NewName');",
        "class Test {",
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
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals(
        "File 'Test/Test.dart' in library 'Test' already declares top-level import prefix 'NewName'",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
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

  public void test_postCondition_topLevel_withPrefixes_hasConflict() throws Exception {
    setUnitContent("LibA.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('LibA');",
        "class Test {",
        "}"});
    setUnitContent("LibB.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('LibB');",
        "class NewName {",
        "}"});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('test');",
        "#import('LibA.dart', prefix: 'a');",
        "#import('LibB.dart', prefix: 'a');",
        "main() {",
        "  a.Test test = null;",
        "}",
        "");
    // get units, because they have not library
    CompilationUnit unitA = testProject.getUnit("LibA.dart");
    // find Type to rename
    Type type = findElement(unitA, "Test {");
    // try to rename
    try {
      renameType(type, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(1);
      // error for reference
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Reference to the renamed type will conflict with element from library 'Test/LibB.dart'",
          showStatusMessages.get(0));
    }
  }

  public void test_postCondition_topLevel_withPrefixes_noConflict() throws Exception {
    setUnitContent("LibA.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('LibA');",
        "class Test {",
        "}"});
    setUnitContent("LibB.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('LibB');",
        "class NewName {",
        "}"});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('test');",
        "#import('LibA.dart', prefix: 'a');",
        "#import('LibB.dart', prefix: 'b');",
        "main() {",
        "  a.Test test = null;",
        "}",
        "");
    // get units, because they have not library
    CompilationUnit unitA = testProject.getUnit("LibA.dart");
    CompilationUnit unitB = testProject.getUnit("LibB.dart");
    // find Type to rename
    Type type = findElement(unitA, "Test {");
    // do rename
    renameType(type, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('test');",
        "#import('LibA.dart', prefix: 'a');",
        "#import('LibB.dart', prefix: 'b');",
        "main() {",
        "  a.NewName test = null;",
        "}",
        "");
    assertUnitContent(unitA, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('LibA');",
        "class NewName {",
        "}"});
    assertUnitContent(unitB, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('LibB');",
        "class NewName {",
        "}"});
  }

  public void test_postCondition_typeParameter_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "class A<NewName> {",
        "  Test f;",
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
          "Declaration of renamed type will be shadowed by type parameter 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for usage in B
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type will be shadowed by type parameter 'A.NewName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {",
        "}",
        "f() {",
        "  Test test = new Test();",
        "}",
        "somethingBad");
    waitForErrorMarker(testUnit);
    Type type = findElement("Test {");
    // try to rename
    showStatusCancel = false;
    renameType(type, "NewName");
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
        "class NewName {",
        "}",
        "f() {",
        "  NewName test = new NewName();",
        "}",
        "somethingBad");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    renameUnit = false;
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
