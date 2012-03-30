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

import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameLocalVariableProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameLocalVariableProcessor}.
 */
public final class RenameLocalVariableProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link DartVariableDeclaration}.
   */
  private static void renameLocalVariable(DartVariableDeclaration variable, String newName)
      throws Exception {
    RenameSupport renameSupport = RenameSupport.create(variable, newName, 0);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  public void test_badFinalState_conflictWithNextVariable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int foo = 1;",
        "  int bar = 2;",
        "}");
    DartVariableDeclaration variable = findElement("foo = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameLocalVariable(variable, "bar");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals("Duplicate local variable 'bar'", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_notIdentifier() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int foo = 1;",
        "  int bar = 2;",
        "  foo = 3;",
        "  bar = 4;",
        "}");
    DartVariableDeclaration variable = findElement("foo = 1;");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameLocalVariable(variable, "-notIdentifier");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "The variable name '-notIdentifier' is not a valid identifier",
        showStatusMessages.get(0));
    assertThat(showStatusMessages.get(0)).contains(
        "The variable name '-notIdentifier' is not a valid identifier");
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_shouldBeLowerCase() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int foo = 1;",
        "  int bar = 2;",
        "  foo = 3;",
        "  bar = 4;",
        "}");
    DartVariableDeclaration variable = findElement("foo = 1;");
    // try to rename
    showStatusCancel = false;
    renameLocalVariable(variable, "NotLowerCase");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(
        "By convention, variable names usually start with a lowercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int NotLowerCase = 1;",
        "  int bar = 2;",
        "  NotLowerCase = 3;",
        "  bar = 4;",
        "}");
  }

  public void test_notAvailable_noElement() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int foo = 1;",
        "  int bar = 2;",
        "  foo = 3;",
        "  bar = 4;",
        "}");
    DartVariableDeclaration variable = null;
    // try to rename
    String source = testUnit.getSource();
    renameLocalVariable(variable, "newName");
    // error should be displayed
    assertThat(openInformationMessages).hasSize(1);
    assertEquals("The refactoring operation is not available", openInformationMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_OK_local_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int foo = 1;",
        "  int bar = 2;",
        "  foo = 3;",
        "  bar = 4;",
        "}");
    DartVariableDeclaration variable = findElement("foo = 1;");
    // do rename
    renameLocalVariable(variable, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int newName = 1;",
        "  int bar = 2;",
        "  newName = 3;",
        "  bar = 4;",
        "}");
  }

  public void test_OK_local_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int foo = 1;",
        "  int bar = 2;",
        "  foo = 3;",
        "  bar = 4;",
        "}");
    DartVariableDeclaration variable = findElement("foo = 3;");
    // do rename
    renameLocalVariable(variable, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  int newName = 1;",
        "  int bar = 2;",
        "  newName = 3;",
        "  bar = 4;",
        "}");
  }

  public void test_OK_parameter_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(foo) {",
        "  foo = 1;",
        "  int bar = 2;",
        "}");
    DartVariableDeclaration variable = findElement("foo)");
    // do rename
    renameLocalVariable(variable, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(newName) {",
        "  newName = 1;",
        "  int bar = 2;",
        "}");
  }
}
