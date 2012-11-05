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
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameFieldProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameFieldProcessor}.
 */
public final class RenameFieldProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link Field}.
   */
  private static void renameField(Field field, String newName) throws Exception {
    RenameSupport renameSupport = RenameSupport.create(field, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameFieldProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "}");
    Field field = findElement("test = 1;");
    // do check
    RenameFieldProcessor processor = new RenameFieldProcessor(field);
    assertEquals(RenameFieldProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "}");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "test");
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

  public void test_badNewName_enclosingTypeHasField() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "  int newName = 2;",
        "}");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals(
        "Enclosing type 'A' in 'Test/Test.dart' already declares member with name 'newName'",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_enclosingTypeHasMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "  newName() {}",
        "}");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals(
        "Enclosing type 'A' in 'Test/Test.dart' already declares member with name 'newName'",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_shouldBeLowerCase() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "}");
    Field field = findElement("test = 1;");
    // try to rename
    showStatusCancel = false;
    renameField(field, "NAME");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "By convention, field names usually start with a lowercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int NAME = 1;",
        "}");
  }

  /**
   * When we make field private, we should warn about using outside of declaring library.
   */
  public void test_OK_addUnderscore_otherLibrary() throws Exception {
    setTestUnitContent(
        "library Test;",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "}",
        "f1() {",
        "  A a = new A();",
        "  a.test = 2;",
        "}",
        "");
    setUnitContent("User.dart", new String[] {
        "library User;",
        "import 'Test.dart';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  A a = new A();",
        "  a.test = 3;",
        "}",
        ""});
    CompilationUnit userUnit = testProject.getUnit("User.dart");
    Field field = findElement("test = 1;");
    // do rename
    showStatusCancel = false;
    renameField(field, "_newName");
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals(
        "Renamed field will become private, so will be not visible in library 'Test/User.dart'",
        showStatusMessages.get(0));
    assertTestUnitContent(
        "library Test;",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int _newName = 1;",
        "}",
        "f1() {",
        "  A a = new A();",
        "  a._newName = 2;",
        "}",
        "");
    assertUnitContent(userUnit, new String[] {
        "library User;",
        "import 'Test.dart';",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  A a = new A();",
        "  a._newName = 3;",
        "}",
        ""});
  }

  public void test_OK_addUnderscore_sameLibrary() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.test = 2;",
        "}",
        "");
    Field field = findElement("test = 1;");
    // do rename
    renameField(field, "_newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int _newName = 1;",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a._newName = 2;",
        "}",
        "");
  }

  public void test_OK_inStringInterpolation() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "  f1() {",
        "    String s = 'hello $test';",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  String s = 'hello ${a.test}';",
        "}",
        "");
    Field field = findElement("test = 1;");
    // do rename
    renameField(field, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName = 1;",
        "  f1() {",
        "    String s = 'hello $newName';",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  String s = 'hello ${a.newName}';",
        "}",
        "");
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    setUnitContent("Test1.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "class A {",
        "  int test = 1;",
        "  int bar = 2;",
        "  f1() {",
        "    test = 3;",
        "    bar = 4;",
        "  }",
        "}"});
    setUnitContent("Test2.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "f2() {",
        "  A a = new A();",
        "  a.test = 5;",
        "}"});
    setUnitContent("Test3.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "class B extends A {",
        "  f3() {",
        "    test = 6;",
        "  }",
        "}"});
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library test;",
        "part 'Test1.dart';",
        "part 'Test2.dart';",
        "part 'Test3.dart';");
    // get units, because they have not library
    CompilationUnit unit1 = testProject.getUnit("Test1.dart");
    CompilationUnit unit2 = testProject.getUnit("Test2.dart");
    CompilationUnit unit3 = testProject.getUnit("Test3.dart");
    // find Field to rename
    Field field = findElement(unit2, "test = 5;");
    // do rename
    renameField(field, "newName");
    assertUnitContent(unit1, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "class A {",
        "  int newName = 1;",
        "  int bar = 2;",
        "  f1() {",
        "    newName = 3;",
        "    bar = 4;",
        "  }",
        "}"});
    assertUnitContent(unit2, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "f2() {",
        "  A a = new A();",
        "  a.newName = 5;",
        "}"});
    assertUnitContent(unit3, new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of test;",
        "class B extends A {",
        "  f3() {",
        "    newName = 6;",
        "  }",
        "}"});
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "  int bar = 2;",
        "  f1() {",
        "    test = 3;",
        "    bar = 4;",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.test = 5;",
        "}",
        "class B extends A {",
        "  f3() {",
        "    test = 6;",
        "  }",
        "}");
    Field field = findElement("test = 1;");
    // do rename
    renameField(field, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName = 1;",
        "  int bar = 2;",
        "  f1() {",
        "    newName = 3;",
        "    bar = 4;",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.newName = 5;",
        "}",
        "class B extends A {",
        "  f3() {",
        "    newName = 6;",
        "  }",
        "}");
  }

  public void test_OK_singleUnit_onDeclaration_withoutInitializer() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test; // rename here",
        "}",
        "");
    Field field = findElement("test; // rename here");
    // do rename
    renameField(field, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var newName; // rename here",
        "}",
        "");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "  int bar = 2;",
        "  f1() {",
        "    test = 3;",
        "    bar = 4;",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.test = 5;",
        "}",
        "class B extends A {",
        "  f3() {",
        "    test = 6;",
        "  }",
        "}");
    Field field = findElement("test = 5;");
    // do rename
    renameField(field, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName = 1;",
        "  int bar = 2;",
        "  f1() {",
        "    newName = 3;",
        "    bar = 4;",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.newName = 5;",
        "}",
        "class B extends A {",
        "  f3() {",
        "    newName = 6;",
        "  }",
        "}");
  }

  public void test_OK_singleUnit_onReference_inSubClass() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test = 1;",
        "}",
        "class B extends A {",
        "  f() {",
        "    test = 2;",
        "  }",
        "}");
    Field field = findElement("test = 2;");
    // do rename
    renameField(field, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var newName = 1;",
        "}",
        "class B extends A {",
        "  f() {",
        "    newName = 2;",
        "  }",
        "}");
  }

  public void test_OK_singleUnit_withThisFieldConstructor() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "  A(this.test) {",
        "  }",
        "  f1() {",
        "    test = 2;",
        "  }",
        "}");
    Field field = findElement("test = 1;");
    // do rename
    renameField(field, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int newName = 1;",
        "  A(this.newName) {",
        "  }",
        "  f1() {",
        "    newName = 2;",
        "  }",
        "}");
  }

  public void test_postCondition_element_shadowedBy_localFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test = 1;",
        "  foo() {",
        "    newName() {};",
        "    test = 2;",
        "  }",
        "}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for variable declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed field will be shadowed by function in method 'A.foo()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for field usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed field will be shadowed by function in method 'A.foo()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadowedBy_localVariable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test = 1;",
        "  foo() {",
        "    var newName;",
        "    test = 2;",
        "  }",
        "}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for variable declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed field will be shadowed by variable in method 'A.foo()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for field usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed field will be shadowed by variable in method 'A.foo()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadowedBy_subTypeMember() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test = 1;",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  newName() {}",
        "  f() {",
        "    test = 2;",
        "  }",
        "}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for sub-type method declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed field will be shadowed by method 'C.newName' in 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for field usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed field will be shadowed by method 'C.newName' in 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadowedBy_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test = 1;",
        "}",
        "class B extends A {",
        "  f() {",
        "    test = 2;",
        "  }",
        "}",
        "class newName {}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for field declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of type 'newName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed field",
          showStatusMessages.get(0));
      // error for type usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of field 'A.test' will be shadowed by top-level type 'newName' from 'Test/Test.dart' in library 'Test'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadowedBy_typeParameter() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test = 1;",
        "}",
        "class B<newName> extends A {",
        "  f() {",
        "    test = 2;",
        "  }",
        "}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for field declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed field will be shadowed by type parameter 'B.newName' in '/Test/Test.dart'",
          showStatusMessages.get(0));
      // error for type usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed field will be shadowed by type parameter 'B.newName' in '/Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadows_importPrefix() throws Exception {
    testProject.setUnitContent(
        "LibA.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('A');",
            "class A {}",
            ""));
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('LibA.dart', prefix: 'newName');",
        "class A {",
        "  var test = 1;",
        "  f() {",
        "    new newName.A();",
        "  }",
        "}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for field declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of import prefix 'newName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed field",
          showStatusMessages.get(0));
      // error for type usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of import prefix 'newName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed field",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadows_superTypeMember() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  var test = 1;",
        "  f() {",
        "    newName();",
        "  }",
        "}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for field declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of method 'A.newName' in 'Test/Test.dart' will be shadowed by renamed field",
          showStatusMessages.get(0));
      // error for super-type member usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of method 'A.newName' declared in 'Test/Test.dart' will be shadowed by renamed field",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test = 1;",
        "  f() {",
        "    new newName();",
        "  }",
        "}",
        "class newName {}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for field declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of type 'newName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed field",
          showStatusMessages.get(0));
      // error for type usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of type 'newName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed field",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadows_typeParameter() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<newName> {",
        "  var test = 1;",
        "  f() {",
        "    newName v;",
        "  }",
        "}",
        "");
    Field field = findElement("test = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameField(field, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for field declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of type parameter 'A.newName' in '/Test/Test.dart' will be shadowed by renamed field",
          showStatusMessages.get(0));
      // error for type usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of type parameter 'A.newName' declared in '/Test/Test.dart' will be shadowed by renamed field",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int test = 1;",
        "  f() {",
        "    test = 2;",
        "  }",
        "}",
        "somethingBad");
    waitForErrorMarker(testUnit);
    Field field = findElement("test = 1;");
    // try to rename
    showStatusCancel = false;
    renameField(field, "newName");
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
        "class A {",
        "  int newName = 1;",
        "  f() {",
        "    newName = 2;",
        "  }",
        "}",
        "somethingBad");
  }
}
