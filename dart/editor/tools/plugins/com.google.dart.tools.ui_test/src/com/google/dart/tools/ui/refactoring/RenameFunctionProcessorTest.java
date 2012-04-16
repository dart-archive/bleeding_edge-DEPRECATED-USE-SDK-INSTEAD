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
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameFunctionProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameFunctionProcessor}.
 */
public final class RenameFunctionProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link DartFunction}.
   */
  private static void renameFunction(DartFunction function, String newName) throws Exception {
    TestProject.waitForAutoBuild();
    RenameSupport renameSupport = RenameSupport.create(function, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameTypeProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "");
    DartFunction function = findElement("test() {");
    // do check
    RenameFunctionProcessor processor = new RenameFunctionProcessor(function);
    assertEquals(RenameFunctionProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "");
    DartFunction function = findElement("test() {");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameFunction(function, "test");
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

  public void test_badNewName_shouldBeLowerCase() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "");
    DartFunction function = findElement("test() {");
    // try to rename
    showStatusCancel = false;
    renameFunction(function, "NewName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "By convention, function names usually start with a lowercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "NewName() {}",
        "");
  }

  public void test_OK_multipleUnits_onReference() throws Exception {
    setUnitContent(
        "Test1.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "");
    setUnitContent(
        "Test2.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  test();",
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
    DartFunction function = findElement(unit2, "test();");
    // do rename
    renameFunction(function, "newName");
    assertUnitContent(
        unit1,
        "// filler filler filler filler filler filler filler filler filler filler",
        "newName() {}",
        "");
    assertUnitContent(
        unit2,
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  newName();",
        "}");
  }

  public void test_OK_singleUnit_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "f() {",
        "  test();",
        "}",
        "");
    DartFunction function = findElement("test() {");
    // do rename
    renameFunction(function, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "newName() {}",
        "f() {",
        "  newName();",
        "}",
        "");
  }

  public void test_OK_singleUnit_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "f() {",
        "  test();",
        "}",
        "");
    DartFunction function = findElement("test();");
    // do rename
    renameFunction(function, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "newName() {}",
        "f() {",
        "  newName();",
        "}",
        "");
  }

  public void test_postCondition_localVariable_inMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "class A {",
        "  f() {",
        "    var newName;",
        "  }",
        "}",
        "");
    DartFunction function = findElement("test() {");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameFunction(function, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "Method 'A.f()' in 'Test/Test.dart' declares variable 'newName' which will shadow renamed function",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_localVariable_inTopLevelFunction() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "f() {",
        "  var newName;",
        "}",
        "");
    DartFunction function = findElement("test() {");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameFunction(function, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "Function 'f()' in 'Test/Test.dart' declares variable 'newName' which will shadow renamed function",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_topLevel_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "class newName {",
        "}",
        "");
    check_postCondition_topLevel("type");
  }

  public void test_postCondition_topLevel_function() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "newName() {}",
        "");
    check_postCondition_topLevel("function");
  }

  public void test_postCondition_topLevel_functionTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef newName(int p);",
        "test() {}",
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
        "test() {}",
        "");
    check_postCondition_topLevel("Lib.dart", "variable");
  }

  public void test_postCondition_topLevel_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "test() {}",
        "");
    check_postCondition_topLevel("variable");
  }

  public void test_postCondition_type_field() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "class A {",
        "  var newName;",
        "}",
        "");
    check_postCondition_typeMember("field");
  }

  public void test_postCondition_type_method() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "class A {",
        "  newName() {}",
        "}",
        "");
    check_postCondition_typeMember("method");
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setUnitContent(
        "Test1.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "");
    setUnitContent(
        "Test2.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  test();",
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
    DartFunction function = findElement(unit1, "test() {");
    // try to rename
    showStatusCancel = false;
    renameFunction(function, "newName");
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
        "newName() {}",
        "");
    assertUnitContent(
        unit2,
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  newName();",
        "}",
        "somethingBad");
  }

  private void check_postCondition_topLevel(String shadowName) throws Exception {
    check_postCondition_topLevel("Test.dart", shadowName);
  }

  private void check_postCondition_topLevel(String unitName, String shadowName) throws Exception {
    DartFunction function = findElement("test() {");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameFunction(function, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals("File 'Test/"
        + unitName
        + "' in library 'Test' already declares top-level "
        + shadowName
        + " 'newName'", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  private void check_postCondition_typeMember(String shadowName) throws Exception {
    DartFunction function = findElement("test() {");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameFunction(function, "newName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals("Type 'A' in 'Test/Test.dart' declares "
        + shadowName
        + " 'newName' which will shadow renamed function", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }
}
