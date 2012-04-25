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

import com.google.common.collect.Sets;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Set;

/**
 * Test for {@link RenameAnalyzeUtil}.
 */
public final class RenameAnalyzeUtilTest extends RefactoringTest {

  public void test_getElementTypeName() throws Exception {
    CompilationUnit unit = setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef MyFunctionTypeAlias();",
        "var myTopLevelVariable;",
        "class MyType {",
        "  var myField;",
        "  myMethod() {",
        "    var myLocalVariable;",
        "  }",
        "}",
        "myTopLevelFunction() {}",
        "");
    DartElement[] unitChildren = unit.getChildren();
    assertEquals("function type alias", RenameAnalyzeUtil.getElementTypeName(unitChildren[0]));
    assertEquals("variable", RenameAnalyzeUtil.getElementTypeName(unitChildren[1]));
    assertEquals("type", RenameAnalyzeUtil.getElementTypeName(unitChildren[2]));
    {
      Type type = (Type) unitChildren[2];
      DartElement[] typeChildren = type.getChildren();
      assertEquals("field", RenameAnalyzeUtil.getElementTypeName(typeChildren[0]));
      assertEquals("method", RenameAnalyzeUtil.getElementTypeName(typeChildren[1]));
      {
        Method method = (Method) typeChildren[1];
        assertEquals(
            "variable",
            RenameAnalyzeUtil.getElementTypeName(method.getLocalVariables()[0]));
      }
    }
  }

  /**
   * Test for {@link RenameAnalyzeUtil#getReferences(DartElement)}.
   */
  public void test_getReferences_field() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var test;",
        "}",
        "f() {",
        "  A a = new A();",
        "  a.test = 1;",
        "}",
        "");
    check_getReferences("test;", "test = 1;", 4);
  }

  /**
   * Test for {@link RenameAnalyzeUtil#getReferences(DartElement)}.
   */
  public void test_getReferences_function() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {}",
        "f() {",
        "  test();",
        "}",
        "");
    check_getReferences("test() {}", "test();", 4);
  }

  /**
   * Test for {@link RenameAnalyzeUtil#getReferences(DartElement)}.
   */
  public void test_getReferences_functionTypeAlias() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef Test();",
        "f() {",
        "  Test a;",
        "}",
        "");
    check_getReferences("Test();", "Test a", 4);
  }

  /**
   * Test for {@link RenameAnalyzeUtil#getReferences(DartElement)}.
   */
  public void test_getReferences_method() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {}",
        "}",
        "f() {",
        "  A a = new A();",
        "  a.test();",
        "}",
        "");
    check_getReferences("test() {}", "test();", 4);
  }

  /**
   * Test for {@link RenameAnalyzeUtil#getReferences(DartElement)}.
   */
  public void test_getReferences_null() throws Exception {
    List<SearchMatch> references = RenameAnalyzeUtil.getReferences((DartElement) null);
    assertThat(references).isEmpty();
  }

  /**
   * Test for {@link RenameAnalyzeUtil#getReferences(DartElement)}.
   */
  public void test_getReferences_type() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class Test {}",
        "f() {",
        "  Test a;",
        "}",
        "");
    check_getReferences("Test {}", "Test a", 4);
  }

  /**
   * Test for {@link RenameAnalyzeUtil#getReferences(DartElement)}.
   */
  public void test_getReferences_variable() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var test;",
        "f() {",
        "  test = 1;",
        "}",
        "");
    check_getReferences("test;", "test = 1;", 4);
  }

  public void test_getSubTypes() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "class A {}");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "class B extends A {}",
        "class C extends B {}",
        "");
    TestProject.waitForAutoBuild();
    Type typeA = getTopLevelElementNamed("A");
    Type typeB = getTopLevelElementNamed("B");
    Type typeC = getTopLevelElementNamed("C");
    // A
    {
      List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(typeA);
      assertThat(subTypes).containsOnly(typeB, typeC);
    }
    // B
    {
      List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(typeB);
      assertThat(subTypes).containsOnly(typeC);
    }
    // C
    {
      List<Type> subTypes = RenameAnalyzeUtil.getSubTypes(typeC);
      assertThat(subTypes).isEmpty();
    }
  }

  public void test_getSuperTypes_classesOnly() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "class A {}");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "class B extends A {}",
        "class C extends B {}",
        "");
    TestProject.waitForAutoBuild();
    Type typeA = getTopLevelElementNamed("A");
    Type typeB = getTopLevelElementNamed("B");
    Type typeC = getTopLevelElementNamed("C");
    // A
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeA);
      assertThat(superTypes).isEmpty();
    }
    // B
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeB);
      assertThat(superTypes).containsOnly(typeA);
    }
    // C
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeC);
      assertThat(superTypes).containsOnly(typeA, typeB);
    }
  }

  public void test_getSuperTypes_withInterfaces() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "interface IA {}",
        "interface IB {}",
        "interface IC {}",
        "interface IAB extends IA, IB {}",
        "interface IBC extends IB, IC {}",
        "class CA implements IAB, IBC {}",
        "");
    TestProject.waitForAutoBuild();
    Type typeIA = getTopLevelElementNamed("IA");
    Type typeIB = getTopLevelElementNamed("IB");
    Type typeIC = getTopLevelElementNamed("IC");
    Type typeIAB = getTopLevelElementNamed("IAB");
    Type typeIBC = getTopLevelElementNamed("IBC");
    Type typeCA = getTopLevelElementNamed("CA");
    // CA
    {
      Set<Type> superTypes = RenameAnalyzeUtil.getSuperTypes(typeCA);
      assertThat(superTypes).containsOnly(typeIAB, typeIBC, typeIA, typeIB, typeIC);
    }
  }

  public void test_getTopLevelElementNamed_class() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass {}",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("MyClass"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  public void test_getTopLevelElementNamed_interface() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface MyInterface {}",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("MyInterface"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  public void test_getTopLevelElementNamed_typedef() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef MyFunctionTypeAlias();",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("MyFunctionTypeAlias"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  public void test_getTopLevelElementNamed_variable() throws Exception {
    setUnitContent(
        "Lib.dart",
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Lib');",
        "var libVar;");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "var testVar;",
        "");
    TestProject.waitForAutoBuild();
    assertNotNull(getTopLevelElementNamed("testVar"));
    assertNotNull(getTopLevelElementNamed("libVar"));
    assertNull(getTopLevelElementNamed("noSuchName"));
  }

  public void test_getTypeMembers() throws Exception {
    CompilationUnit unit = setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var myField;",
        "  myMethod() {}",
        "}",
        "");
    Type type = (Type) unit.getChildren()[0];
    List<TypeMember> typeMembers = RenameAnalyzeUtil.getTypeMembers(type);
    assertThat(typeMembers).contains(type.getChildren()[0], type.getChildren()[1]);
  }

  /**
   * Test for {@link RenameAnalyzeUtil#isTypeHierarchy(Type, Type)}.
   */
  public void test_isTypeHierarchy() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('Lib.dart');",
        "class A {}",
        "class B extends A {}",
        "class C extends B {}",
        "");
    TestProject.waitForAutoBuild();
    Type typeA = getTopLevelElementNamed("A");
    Type typeB = getTopLevelElementNamed("B");
    Type typeC = getTopLevelElementNamed("C");
    // A
    assertFalse(RenameAnalyzeUtil.isTypeHierarchy(typeA, typeA));
    assertFalse(RenameAnalyzeUtil.isTypeHierarchy(typeA, typeB));
    assertFalse(RenameAnalyzeUtil.isTypeHierarchy(typeA, typeC));
    // B
    assertTrue(RenameAnalyzeUtil.isTypeHierarchy(typeB, typeA));
    assertFalse(RenameAnalyzeUtil.isTypeHierarchy(typeB, typeB));
    assertFalse(RenameAnalyzeUtil.isTypeHierarchy(typeB, typeC));
    // C
    assertTrue(RenameAnalyzeUtil.isTypeHierarchy(typeC, typeA));
    assertTrue(RenameAnalyzeUtil.isTypeHierarchy(typeC, typeB));
    assertFalse(RenameAnalyzeUtil.isTypeHierarchy(typeC, typeC));
  }

  private void check_getReferences(String searchPattern, String referencePattern, int length)
      throws Exception {
    TestProject.waitForAutoBuild();
    DartElement variable = findElement(searchPattern);
    // prepare single reference
    List<SearchMatch> references = RenameAnalyzeUtil.getReferences(variable);
    assertThat(references).hasSize(1);
    SearchMatch reference = references.get(0);
    // check source range
    SourceRange referenceRange = reference.getSourceRange();
    assertEquals(testUnit.getSource().indexOf(referencePattern), referenceRange.getOffset());
    assertEquals(length, referenceRange.getLength());
  }

  /**
   * @return the {@link DartElement} in library of {@link #testUnit}.
   */
  @SuppressWarnings("unchecked")
  private <T extends DartElement> T getTopLevelElementNamed(String name) throws DartModelException {
    return (T) RenameAnalyzeUtil.getTopLevelElementNamed(
        Sets.<DartLibrary>newHashSet(),
        testUnit,
        name);
  }
}
