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
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameMethodProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameMethodProcessor}.
 */
public final class RenameMethodProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link Method}.
   */
  private static void renameMethod(Method method, String newName) throws Exception {
    TestProject.waitForAutoBuild();
    RenameSupport renameSupport = RenameSupport.create(method, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameMethodProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}");
    Method method = findElement("test()");
    // do check
    RenameMethodProcessor processor = new RenameMethodProcessor(method);
    assertEquals(RenameMethodProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}");
    Method method = findElement("test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "test");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals("Choose another name.", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_enclosingTypeHasField() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  var newName;",
        "}");
    Method method = findElement("test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
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
        "  test() {}",
        "  newName() {}",
        "}");
    Method method = findElement("test()");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
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
        "  test() {}",
        "}");
    Method method = findElement("test()");
    // try to rename
    showStatusCancel = false;
    renameMethod(method, "NAME");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "By convention, method names usually start with a lowercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  NAME() {}",
        "}");
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    setUnitContent(
        "Test1.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  bar() {}",
        "  f1() {",
        "    test();",
        "    bar();",
        "  }",
        "}");
    setUnitContent(
        "Test2.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  A a = new A();",
        "  a.test();",
        "}");
    setUnitContent(
        "Test3.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class B extends A {",
        "  f3() {",
        "    test();",
        "  }",
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
    // find Method to rename
    Method method = findElement(unit2, "test();");
    // do rename
    renameMethod(method, "newName");
    assertUnitContent(
        unit1,
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "  bar() {}",
        "  f1() {",
        "    newName();",
        "    bar();",
        "  }",
        "}");
    assertUnitContent(
        unit2,
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  A a = new A();",
        "  a.newName();",
        "}");
    assertUnitContent(
        unit3,
        "// filler filler filler filler filler filler filler filler filler filler",
        "class B extends A {",
        "  f3() {",
        "    newName();",
        "  }",
        "}");
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  bar() {}",
        "  f1() {",
        "    test();",
        "    bar();",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.test();",
        "}",
        "class B extends A {",
        "  f3() {",
        "    test();",
        "  }",
        "}");
    Method method = findElement("test() {}");
    // do rename
    renameMethod(method, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "  bar() {}",
        "  f1() {",
        "    newName();",
        "    bar();",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.newName();",
        "}",
        "class B extends A {",
        "  f3() {",
        "    newName();",
        "  }",
        "}");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  bar() {}",
        "  f1() {",
        "    test();",
        "    bar();",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.test(); // rename here",
        "}",
        "class B extends A {",
        "  f3() {",
        "    test();",
        "  }",
        "}");
    Method method = findElement("test(); // rename here");
    // do rename
    renameMethod(method, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "  bar() {}",
        "  f1() {",
        "    newName();",
        "    bar();",
        "  }",
        "}",
        "f2() {",
        "  A a = new A();",
        "  a.newName(); // rename here",
        "}",
        "class B extends A {",
        "  f3() {",
        "    newName();",
        "  }",
        "}");
  }

  public void test_OK_singleUnit_onReference_inSubClass() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class B extends A {",
        "  f() {",
        "    test(); // rename here",
        "  }",
        "}");
    Method method = findElement("test(); // rename here");
    // do rename
    renameMethod(method, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "}",
        "class B extends A {",
        "  f() {",
        "    newName(); // rename here",
        "  }",
        "}");
  }

  public void test_postCondition_hasFieldOverride_inSubType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  var newName;",
        "}",
        "");
    Method method = findElement("test() {}");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "Type 'C' in 'Test/Test.dart' declares field 'newName' which will shadow renamed method",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_hasFieldOverride_inSuperType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var newName;",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  test() {}",
        "}",
        "");
    Method method = findElement("test() {}");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "Type 'A' in 'Test/Test.dart' declares field 'newName' which will be shadowed by renamed method",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_hasLocalVariableHiding_inSubType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class B extends A {",
        "  foo() {",
        "    var newName;",
        "  }",
        "}",
        "");
    Method method = findElement("test() {}");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "Method 'B.foo()' in 'Test/Test.dart' declares local variable 'newName' which will shadow renamed method",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_hasParameterHiding_inSubType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class B extends A {",
        "  foo(var newName) {",
        "  }",
        "}",
        "");
    Method method = findElement("test() {}");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "Method 'B.foo()' in 'Test/Test.dart' declares parameter 'newName' which will shadow renamed method",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_shadowsTopLevel_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "class newName {",
        "}",
        "");
    check_postCondition_shadowsTopLevel("type");
  }

  public void test_postCondition_shadowsTopLevel_functionTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef newName(int p);",
        "class A {",
        "  test() {}",
        "}",
        "");
    check_postCondition_shadowsTopLevel("function type alias");
  }

  public void test_postCondition_shadowsTopLevel_otherLibrary() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "var newName;");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "class A {",
        "  test() {}",
        "}",
        "");
    check_postCondition_shadowsTopLevel("Lib.dart", "variable");
  }

  public void test_postCondition_shadowsTopLevel_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "class A {",
        "  test() {}",
        "}",
        "");
    check_postCondition_shadowsTopLevel("variable");
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setUnitContent(
        "Test1.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "  f1() {",
        "    test();",
        "  }",
        "}");
    setUnitContent(
        "Test2.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  A a = new A();",
        "  a.test();",
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
    Method method = findElement(unit1, "test() {}");
    // try to rename
    showStatusCancel = false;
    renameMethod(method, "newName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "Code modification may not be accurate as affected resource 'Test/Test2.dart' has compile errors.",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertUnitContent(
        unit1,
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  newName() {}",
        "  f1() {",
        "    newName();",
        "  }",
        "}");
    assertUnitContent(
        unit2,
        "// filler filler filler filler filler filler filler filler filler filler",
        "f2() {",
        "  A a = new A();",
        "  a.newName();",
        "}",
        "somethingBad");
  }

  private void check_postCondition_shadowsTopLevel(String shadowName) throws Exception {
    check_postCondition_shadowsTopLevel("Test.dart", shadowName);
  }

  private void check_postCondition_shadowsTopLevel(String unitName, String shadowName)
      throws Exception {
    Method method = findElement("test() {}");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameMethod(method, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals("File 'Test/"
        + unitName
        + "' in library 'Test' declares top-level "
        + shadowName
        + " 'newName' which will shadow renamed method", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }
}
