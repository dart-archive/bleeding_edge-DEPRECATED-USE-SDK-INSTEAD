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

import static org.fest.assertions.Assertions.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;

import java.util.List;

/**
 * Test for {@link DartElementLocator}.
 */
public class DartElementLocatorTest extends TestCase {

  @SuppressWarnings("unchecked")
  private static <T extends DartElement> T assertLocation(CompilationUnit unit, String posMarker,
      Class<?> expectedElementType, String expectedMarker, int expectedLen) throws Exception {
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
      if (expectedLen > 0) {
        int expectedPos = source.indexOf(expectedMarker);
        assertTrue("Unable to find expected marker '" + expectedMarker + "'", expectedPos > 0);
        assertEquals(expectedPos, locator.getCandidateRegion().getOffset());
        assertEquals(expectedLen, locator.getCandidateRegion().getLength());
      }
      return (T) result;
    } else {
      assertNull(result);
      return null;
    }
  }

  private static String[] formatLines(String... lines) {
    return lines;
  }

  private static void testElementLocator(String[] sourceLines, String posMarker,
      Class<?> expectedElementType, String expectedMarker, int expectedLen) throws Exception {
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

  public void test_DartFunction_onReference() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "test() {}",
            "f() {",
            "  test();",
            "}"),
        "test();",
        DartFunction.class,
        "test() {}",
        4);
  }

  /**
   * Test for {@link DartImport}.
   */
  public void test_DartImport_onDeclaration() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('libA');",
              "class A {}",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart', prefix: 'aaa');",
              "f() {",
              "  aaa.A a;",
              "}",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      // should be DartImport of 'aaa' prefix
      {
        String importSource = "#import('LibA.dart', prefix: 'aaa');";
        DartImport imprt = assertLocation(
            libraryTest.getDefiningCompilationUnit(),
            "aaa');",
            DartImport.class,
            importSource,
            importSource.length());
        assertEquals(libraryA, imprt.getLibrary());
        assertEquals("aaa", imprt.getPrefix());
      }
      // should be DartImport of any place of DartImport except of URI
      {
        String importSource = "#import('LibA.dart', prefix: 'aaa');";
        DartImport imprt = assertLocation(
            libraryTest.getDefiningCompilationUnit(),
            "#import('LibA",
            DartImport.class,
            importSource,
            importSource.length());
        assertEquals(libraryA, imprt.getLibrary());
        assertEquals("aaa", imprt.getPrefix());
      }
    } finally {
      testProject.dispose();
    }
  }

  /**
   * Test for {@link DartImport}.
   */
  public void test_DartImport_onPrefixUsage() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('libA');",
              "class A {}",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart', prefix: 'aaa');",
              "f() {",
              "  aaa.A a;",
              "}",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      // usage of "aaa" = "libraryA"
      {
        DartImport imprt = assertLocation(
            libraryTest.getDefiningCompilationUnit(),
            "aaa.A",
            DartImport.class,
            "'aaa');",
            5);
        assertEquals(libraryA, imprt.getLibrary());
        assertEquals("aaa", imprt.getPrefix());
      }
    } finally {
      testProject.dispose();
    }
  }

  /**
   * Test for {@link DartImport}.
   */
  public void test_DartImport_onPrefixUsage_inFieldReference() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibB.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('libB');",
              "var field;",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibB.dart', prefix: 'bbb');",
              "f() {",
              "  bbb.field = 0;",
              "}",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      // usage of "aaa" = "libraryA"
      {
        DartImport imprt = assertLocation(
            libraryTest.getDefiningCompilationUnit(),
            "bbb.field",
            DartImport.class,
            "'bbb');",
            5);
        assertEquals(libraryA, imprt.getLibrary());
        assertEquals("bbb", imprt.getPrefix());
      }
    } finally {
      testProject.dispose();
    }
  }

  /**
   * Test for {@link DartImport}.
   */
  public void test_DartImport_onPrefixUsage_inFunctionReference() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('libC');",
              "f() {}",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibC.dart', prefix: 'ccc');",
              "f() {",
              "  ccc.f();",
              "}",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      // usage of "aaa" = "libraryA"
      {
        DartImport imprt = assertLocation(
            libraryTest.getDefiningCompilationUnit(),
            "ccc.f();",
            DartImport.class,
            "'ccc');",
            5);
        assertEquals(libraryA, imprt.getLibrary());
        assertEquals("ccc", imprt.getPrefix());
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_DartImport_onURI() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "LibA.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('libA');",
              "class A {}",
              "")).getResource();
      IResource resourceTest = testProject.setUnitContent(
          "TestC.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "#library('Test');",
              "#import('LibA.dart', prefix: 'aaa');",
              "f() {",
              "  aaa.A a;",
              "}",
              "")).getResource();
      DartLibrary libraryA = testProject.getDartProject().getDartLibrary(libResourceA);
      DartLibrary libraryTest = testProject.getDartProject().getDartLibrary(resourceTest);
      // URI of "libraryA"
      {
        CompilationUnit unit = assertLocation(
            libraryTest.getDefiningCompilationUnit(),
            "ibA.dart'",
            CompilationUnit.class,
            "<ignored>",
            -1);
        assertEquals(libraryA.getDefiningCompilationUnit(), unit);
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_DartTypeParameter_inFunctonTypeAlias_onTypeName() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "typedef Test<A extends String>(A a);",
            ""),
        "A a",
        DartTypeParameter.class,
        "A extends String>",
        1);
  }

  public void test_DartTypeParameter_inType_onTypeName() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "class Test<A> {",
            "  f(A a) {",
            "  }",
            "}"),
        "A a",
        DartTypeParameter.class,
        "A>",
        1);
  }

  public void test_FieldElement_classMember() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  var bbb = 1;",
            "}",
            "foo() {",
            "  A a = new A();",
            "  process(a.bbb);",
            "}"),
        "bbb);",
        Field.class,
        "bbb = 1",
        3);
  }

  public void test_Function_getter_topLevel() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "get test => 0;",
            "set test(x) {}",
            "main() {",
            "  process( test );",
            "}",
            ""),
        "test );",
        DartFunction.class,
        "test => 0;",
        4);
  }

  public void test_Function_onDeclaration() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "test() {",
            "}",
            ""),
        "est() {",
        DartFunction.class,
        "test() {",
        4);
  }

  public void test_Function_onReference() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "test() {",
            "}",
            "f() {",
            "  test();",
            "}",
            ""),
        "test();",
        DartFunction.class,
        "test() {",
        4);
  }

  public void test_Function_setter_topLevel() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "get test => 0;",
            "set test(x) {}",
            "main() {",
            "  test = 0;",
            "}",
            ""),
        "test = 0;",
        DartFunction.class,
        "test(x)",
        4);
  }

  public void test_LibraryUnit_onPartOf() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      IResource libResourceA = testProject.setUnitContent(
          "Lib.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "library my.lib;",
              "part 'Test.dart';",
              "")).getResource();
      testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "part of my.lib;",
              ""));
      DartLibrary library = testProject.getDartProject().getDartLibrary(libResourceA);
      CompilationUnit testUnit = library.getCompilationUnit("Test.dart");
      // usage of "aaa" = "libraryA"
      {
        DartElement element = assertLocation(
            testUnit,
            "my.lib",
            CompilationUnit.class,
            "library my.lib);",
            0);
        assertSame(library.getDefiningCompilationUnit(), element);
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_Method_call() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  call() {",
            "  }",
            "}",
            "f() {",
            "  A a = new A();",
            "  a(); // marker",
            "}",
            ""),
        "; // marker",
        Method.class,
        "call() {",
        4);
  }

  public void test_Method_getter_onDeclaration() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  get test() {}",
            "  set test(x) {}",
            "}",
            ""),
        "est() {",
        Method.class,
        "test() {",
        4);
  }

  public void test_Method_getter_onReference() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  get test() {}",
            "  set test(x) {}",
            "}",
            "f() {",
            "  A a = new A();",
            "  process(a.test);",
            "}",
            ""),
        "test);",
        Method.class,
        "test() {",
        4);
  }

  public void test_Method_getter_static() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  static get test => 0;",
            "  static set test(x) {}",
            "}",
            "main() {",
            "  A a = new A();",
            "  process( A.test );",
            "}",
            ""),
        "test );",
        DartFunction.class,
        "test => 0;",
        4);
  }

  public void test_Method_onDeclaration() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  test() {",
            "  }",
            "}",
            ""),
        "est() {",
        Method.class,
        "test() {",
        4);
  }

  public void test_Method_onReference() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  test() {",
            "  }",
            "}",
            "f() {",
            "  A a = new A();",
            "  a.test();",
            "}",
            ""),
        "test();",
        Method.class,
        "test() {",
        4);
  }

  public void test_Method_setter_onDeclaration() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  get test() {}",
            "  set test(x) {}",
            "}",
            ""),
        "est(x) {",
        Method.class,
        "test(x) {",
        4);
  }

  public void test_Method_setter_onReference() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  get test() {}",
            "  set test(x) {}",
            "}",
            "f() {",
            "  A a = new A();",
            "  a.test = 42;",
            "}",
            ""),
        "test = 42;",
        Method.class,
        "test(x) {",
        4);
  }

  public void test_Method_setter_static() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  static get test => 0;",
            "  static set test(x) {}",
            "}",
            "main() {",
            "  A a = new A();",
            "  A.test = 1;",
            "}",
            ""),
        "test = 1;",
        DartFunction.class,
        "test(x) {}",
        4);
  }

  public void test_type_newExpression() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  A() {}",
            "}",
            "foo() {",
            "  A a = new A();",
            "}"),
        "A();",
        Method.class,
        "A() {}",
        1);
  }

  public void test_type_typeName() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "class A {}",
            "foo() {",
            "  A a = null;",
            "}"),
        "A a =",
        Type.class,
        "A {}",
        1);
  }

  public void test_VariableElement_localVariable() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "foo() {",
            "  var aaa = 1;",
            "  process(aaa);",
            "}"),
        "aaa);",
        DartVariableDeclaration.class,
        "aaa = 1",
        3);
  }

  public void test_VariableElement_parameter_declaration() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "foo(a, bb, ccc) {",
            "  process(bb);",
            "}"),
        "b, ccc",
        DartVariableDeclaration.class,
        "bb, ",
        2);
  }

  public void test_VariableElement_parameter_namedInInvocation() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "class A {",
            "  static f({test: 0}) {}",
            "}",
            "",
            "void main() {",
            "  A.f(test: 42);",
            "}",
            ""),
        "test: 42",
        DartVariableDeclaration.class,
        "test: 0",
        4);
  }

  public void test_VariableElement_parameter_reference_inClassMethod() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "class A {",
            "  foo(a, bb, ccc) {",
            "    process(bb);",
            "  }",
            "}"),
        "bb);",
        DartVariableDeclaration.class,
        "bb, ",
        2);
  }

  public void test_VariableElement_parameter_reference_inTopMethod() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "foo(a, bb, ccc) {",
            "  process(bb);",
            "}"),
        "bb);",
        DartVariableDeclaration.class,
        "bb, ",
        2);
  }

  public void test_VariableElement_topLevel() throws Exception {
    testElementLocator(
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler filler",
            "process(x) {}",
            "var aaa = 1;",
            "foo() {",
            "  process(aaa);",
            "}"),
        "aaa);",
        DartVariableDeclaration.class,
        "aaa = 1",
        3);
  }

}
