/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.tools.core.utilities.ast;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

/**
 * Test for {@link DartElementLocator}.
 */
public class DartElementLocatorTest extends TestCase {

  private static void assertLocation(
      CompilationUnit unit,
      String posMarker,
      Class<?> expectedElementType,
      String expectedMarker,
      int expectedLen) throws Exception {
    String source = unit.getSource();
    // prepare DartUnit
    DartUnit dartUnit;
    {
      List<DartCompilationError> errors = Lists.newArrayList();
      dartUnit = DartCompilerUtilities.resolveUnit(unit, errors);
      // we don't want errors
      if (!errors.isEmpty()) {
        fail("Parse/resolve errors: " + errors);
      }
    }
    // prepare position to search on
    int pos = source.indexOf(posMarker);
    assertTrue("Unable to find position marker '" + posMarker + "'", pos > 0);
    // use Locator
    DartElementLocator locator = new DartElementLocator(unit, pos, true);
    DartElement result = locator.searchWithin(dartUnit);
    // verify
    if (expectedMarker != null) {
      assertNotNull(result);
      assertThat(result).isInstanceOf(expectedElementType);
      int expectedPos = source.indexOf(expectedMarker);
      assertTrue("Unable to find expected marker '" + expectedMarker + "'", expectedPos > 0);
      assertEquals(expectedPos, locator.getCandidateRegion().getOffset());
      assertEquals(expectedLen, locator.getCandidateRegion().getLength());
    } else {
      assertNull(result);
    }
  }

  private static void testElementLocator(
      String[] sourceLines,
      String posMarker,
      Class<?> expectedElementType,
      String expectedMarker,
      int expectedLen) throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(sourceLines));
      assertLocation(unit, posMarker, expectedElementType, expectedMarker, expectedLen);
    } finally {
      testProject.dispose();
    }
  }

  public void test_DartTypeParameter_inFunctonTypeAlias_onTypeName() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "typedef Test<A extends String>(A a);",
        ""}, "A a", DartTypeParameter.class, "A extends String>", 1);
  }

  public void test_DartTypeParameter_inType_onTypeName() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "class Test<A> {",
        "  f(A a) {",
        "  }",
        "}"}, "A a", DartTypeParameter.class, "A>", 1);
  }

  public void test_FieldElement_classMember() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "process(x) {}",
        "class A {",
        "  var bbb = 1;",
        "}",
        "foo() {",
        "  A a = new A();",
        "  process(a.bbb);",
        "}"}, "bbb);", Field.class, "bbb = 1", 3);
  }

  public void test_methodInvocation() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "foo() {}",
        "bar() {",
        "  foo();",
        "}"}, "foo();", DartFunction.class, "foo() {}", 3);
  }

  public void test_type_newExpression() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {}",
        "}",
        "foo() {",
        "  A a = new A();",
        "}"}, "A();", Method.class, "A() {}", 1);
  }

  public void test_type_typeName() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "foo() {",
        "  A a = null;",
        "}"}, "A a =", Type.class, "A {}", 1);
  }

  public void test_VariableElement_localVariable() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "process(x) {}",
        "foo() {",
        "  var aaa = 1;",
        "  process(aaa);",
        "}"}, "aaa);", DartVariableDeclaration.class, "aaa = 1", 3);
  }

  public void test_VariableElement_parameter_declaration() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "process(x) {}",
        "foo(a, bb, ccc) {",
        "  process(bb);",
        "}"}, "b, ccc", DartVariableDeclaration.class, "bb, ", 2);
  }

  public void test_VariableElement_parameter_reference_inClassMethod() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "process(x) {}",
        "class A {",
        "  foo(a, bb, ccc) {",
        "    process(bb);",
        "  }",
        "}"}, "bb);", DartVariableDeclaration.class, "bb, ", 2);
  }

  public void test_VariableElement_parameter_reference_inTopMethod() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "process(x) {}",
        "foo(a, bb, ccc) {",
        "  process(bb);",
        "}"}, "bb);", DartVariableDeclaration.class, "bb, ", 2);
  }

  public void test_VariableElement_topLevel() throws Exception {
    testElementLocator(new String[] {
        "// filler filler filler filler filler filler filler filler filler filler filler",
        "process(x) {}",
        "var aaa = 1;",
        "foo() {",
        "  process(aaa);",
        "}"}, "aaa);", DartVariableDeclaration.class, "aaa = 1", 3);
  }
}
