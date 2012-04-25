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

import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameTypeParameterProcessor;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Test for {@link RenameTypeParameterProcessor}.
 */
public final class RenameTypeParameterProcessorTest extends RefactoringTest {
  /**
   * Uses {@link RenameSupport} to rename {@link DartTypeParameter}.
   */
  private static void renameTypeParameter(DartTypeParameter parameter, String newName)
      throws Exception {
    TestProject.waitForAutoBuild();
    RenameSupport renameSupport = RenameSupport.create(parameter, newName);
    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    renameSupport.perform(workbenchWindow.getShell(), workbenchWindow);
  }

  /**
   * Just for coverage of {@link RenameTypeParameterProcessor} accessors.
   */
  public void test_accessors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "}");
    DartTypeParameter parameter = findElement("Test>");
    // do check
    RenameTypeParameterProcessor processor = new RenameTypeParameterProcessor(parameter);
    assertEquals(RenameTypeParameterProcessor.IDENTIFIER, processor.getIdentifier());
    assertEquals("Test", processor.getCurrentElementName());
  }

  public void test_badNewName_alreadyNamed() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "}");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameTypeParameter(parameter, "Test");
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

  public void test_badNewName_hasSuchTypeParameter() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<NewName, Test> {",
        "  Test f;",
        "}");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameTypeParameter(parameter, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(0).intValue());
    assertEquals("Type parameter with name 'NewName' already declared", showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_badNewName_shouldBeUpperCase() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "}");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    showStatusCancel = false;
    renameTypeParameter(parameter, "newName");
    // warning should be displayed
    assertThat(openInformationMessages).isEmpty();
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
    assertEquals(
        "By convention, type parameter names usually start with an uppercase letter",
        showStatusMessages.get(0));
    // status was warning, so rename was done
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<newName> {",
        "  newName f;",
        "}");
  }

  public void test_OK_inFunctionTypeAlias_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef A<Test>(Test p);",
        "");
    DartTypeParameter parameter = findElement("Test>");
    // do rename
    renameTypeParameter(parameter, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef A<NewName>(NewName p);",
        "");
  }

  public void test_OK_inType_onDeclaration() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "}");
    DartTypeParameter parameter = findElement("Test>");
    // do rename
    renameTypeParameter(parameter, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<NewName> {",
        "  NewName f;",
        "}");
  }

  public void test_OK_inType_onReference() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "}");
    DartTypeParameter parameter = findElement("Test f;");
    // do rename
    renameTypeParameter(parameter, "NewName");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<NewName> {",
        "  NewName f;",
        "}");
  }

  public void test_postCondition_element_shadowedBy_localVariable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  f() {",
        "    var NewName;",
        "    Test v;",
        "  }",
        "}",
        "");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameTypeParameter(parameter, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for parameter declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed type parameter will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(0));
      // error for super-type member usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type parameter will be shadowed by variable in method 'A.f()' in file 'Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadowedBy_method() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  NewName() {}",
        "  f() {",
        "    Test v;",
        "  }",
        "}",
        "");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameTypeParameter(parameter, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for parameter declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of renamed type parameter will be shadowed by method 'A.NewName' in '/Test/Test.dart'",
          showStatusMessages.get(0));
      // error for super-type member usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of renamed type parameter will be shadowed by method 'A.NewName' in '/Test/Test.dart'",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadows_superTypeMember() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  NewName() {",
        "  }",
        "}",
        "class B<Test> extends A {",
        "  f() {",
        "    NewName();",
        "  }",
        "}",
        "");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameTypeParameter(parameter, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for parameter declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of method 'A.NewName' in '/Test/Test.dart' will be shadowed by renamed type parameter",
          showStatusMessages.get(0));
      // error for super-type member usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of method 'A.NewName' declared in '/Test/Test.dart' will be shadowed by renamed type parameter",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_postCondition_element_shadows_topLevel() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  f() {",
        "    new NewName();",
        "  }",
        "}",
        "class NewName {}",
        "");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    String source = testUnit.getSource();
    try {
      renameTypeParameter(parameter, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(openInformationMessages).isEmpty();
    {
      assertThat(showStatusMessages).hasSize(2);
      // warning for parameter declaration
      assertEquals(RefactoringStatus.WARNING, showStatusSeverities.get(0).intValue());
      assertEquals(
          "Declaration of type 'NewName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed type parameter",
          showStatusMessages.get(0));
      // error for type usage
      assertEquals(RefactoringStatus.ERROR, showStatusSeverities.get(1).intValue());
      assertEquals(
          "Usage of type 'NewName' in file 'Test/Test.dart' in library 'Test' will be shadowed by renamed type parameter",
          showStatusMessages.get(1));
    }
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_preCondition_canNotFindNode() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "}");
    final DartTypeParameter parameter = findElement("Test>");
    DartTypeParameter parameterProxy = (DartTypeParameter) Proxy.newProxyInstance(
        RenameTypeParameterProcessorTest.class.getClassLoader(),
        new Class[] {DartTypeParameter.class},
        new InvocationHandler() {
          @Override
          public Object invoke(Object o, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("getNameRange")) {
              return new SourceRangeImpl(0, 0);
            }
            return method.invoke(parameter, args);
          }
        });
    // try to rename
    String source = testUnit.getSource();
    try {
      renameTypeParameter(parameterProxy, "NewName");
      fail();
    } catch (InterruptedException e) {
    }
    // error should be displayed
    assertThat(showStatusMessages).hasSize(1);
    assertEquals(RefactoringStatus.FATAL, showStatusSeverities.get(0).intValue());
    assertEquals(
        "A type parameter declaration or reference must be selected to activate this refactoring",
        showStatusMessages.get(0));
    // no source changes
    assertEquals(source, testUnit.getSource());
  }

  public void test_preCondition_hasCompilationErrors() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A<Test> {",
        "  Test f;",
        "}",
        "something bad");
    DartTypeParameter parameter = findElement("Test>");
    // try to rename
    showStatusCancel = false;
    renameTypeParameter(parameter, "NewName");
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
        "class A<NewName> {",
        "  NewName f;",
        "}",
        "something bad");
  }
}
