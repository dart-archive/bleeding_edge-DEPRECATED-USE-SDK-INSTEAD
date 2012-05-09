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

import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameFunctionProcessor;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameLocalFunctionProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link RenameLocalFunctionProcessor}.
 */
public final class RenameLocalFunctionProcessorTest extends RefactoringTest {
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
   * Just for coverage of {@link RenameFunctionProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  test() {}",
        "}",
        "");
    DartFunction function = findElement("test() {");
    // do check
    RenameLocalFunctionProcessor processor = new RenameLocalFunctionProcessor(function);
    assertEquals(RenameLocalFunctionProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  test() {}",
        "}",
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
    assertEquals(RefactoringStatus.FATAL, showStatusSeverities.get(0).intValue());
    assertEquals("Choose another name.", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_shouldBeLowerCase() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  test() {}",
        "}",
        "");
    DartFunction function = findElement("test() {");
    // try to rename
    showStatusCancel = false;
    renameFunction(function, "NewName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "By convention, function names usually start with a lowercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  NewName() {}",
        "}",
        "");
  }

  public void test_OK() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  test() {}",
        "  test();",
        "}");
    DartFunction function = findElement("test() {}");
    // do rename
    renameFunction(function, "newName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  newName() {}",
        "  newName();",
        "}");
  }

  public void test_postCondition_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var newName;",
        "f() {",
        "  test() {}",
        "  newName = 2;",
        "}",
        "");
    DartFunction function = findElement("test() {}");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameFunction(function, "newName");
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
          "Declaration of variable 'newName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed function",
          showStatusMessages.get(0));
      // error for field usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of variable 'newName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed function",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

}
