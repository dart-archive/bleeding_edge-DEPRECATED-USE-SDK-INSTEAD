/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.source.Source;
import com.google.dart.server.Element;
import com.google.dart.server.ElementKind;
import com.google.dart.server.ListSourceSet;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.Outline;
import com.google.dart.server.SourceRegion;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import static org.fest.assertions.Assertions.assertThat;

public class DartUnitOutlineComputerTest extends AbstractLocalServerTest {
  public void test_class() throws Exception {
    String contextId = createContext("test");
    String code = makeSource(//
        "class A {",
        "  int fa, fb;",
        "  String fc;",
        "  A(int i, String s);",
        "  A.name(num p);",
        "  A._privateName(num p);",
        "  static String ma(int pa) => null;",
        "  _mb(int pb);",
        "  String get propA => null;",
        "  set propB(int v) {}",
        "}",
        "class B {",
        "  B(int p);",
        "}");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] topOutlines = unitOutline.getChildren();
    assertThat(topOutlines).hasSize(2);
    // A
    {
      Outline outline_A = topOutlines[0];
      Element element_A = outline_A.getElement();
      assertSame(unitOutline, outline_A.getParent());
      assertSame(ElementKind.CLASS, element_A.getKind());
      assertEquals("A", element_A.getName());
      assertEquals(code.indexOf("A {"), element_A.getOffset());
      assertEquals(1, element_A.getLength());
      assertSame(null, element_A.getParameters());
      assertSame(null, element_A.getReturnType());
      // A children
      Outline[] outlines_A = outline_A.getChildren();
      assertThat(outlines_A).hasSize(10);
      {
        Outline outline = outlines_A[0];
        Element element = outline.getElement();
        assertSame(ElementKind.FIELD, element.getKind());
        assertEquals("fa", element.getName());
        assertNull(element.getParameters());
        assertEquals("int", element.getReturnType());
      }
      {
        Outline outline = outlines_A[1];
        Element element = outline.getElement();
        assertSame(ElementKind.FIELD, element.getKind());
        assertEquals("fb", element.getName());
        assertNull(element.getParameters());
        assertEquals("int", element.getReturnType());
      }
      {
        Outline outline = outlines_A[2];
        Element element = outline.getElement();
        assertSame(ElementKind.FIELD, element.getKind());
        assertEquals("fc", element.getName());
        assertNull(element.getParameters());
        assertEquals("String", element.getReturnType());
      }
      {
        Outline outline = outlines_A[3];
        Element element = outline.getElement();
        assertSame(ElementKind.CONSTRUCTOR, element.getKind());
        assertEquals("A", element.getName());
        assertEquals(code.indexOf("A(int i, String s);"), element.getOffset());
        assertEquals("A".length(), element.getLength());
        assertEquals("(int i, String s)", element.getParameters());
        assertNull(element.getReturnType());
        assertFalse(element.isAbstract());
        assertFalse(element.isPrivate());
        assertFalse(element.isStatic());
      }
      {
        Outline outline = outlines_A[4];
        Element element = outline.getElement();
        assertSame(ElementKind.CONSTRUCTOR, element.getKind());
        assertEquals("A.name", element.getName());
        assertEquals(code.indexOf("name(num p);"), element.getOffset());
        assertEquals("name".length(), element.getLength());
        assertEquals("(num p)", element.getParameters());
        assertNull(element.getReturnType());
        assertFalse(element.isAbstract());
        assertFalse(element.isPrivate());
        assertFalse(element.isStatic());
      }
      {
        Outline outline = outlines_A[5];
        Element elemnet = outline.getElement();
        assertSame(ElementKind.CONSTRUCTOR, elemnet.getKind());
        assertEquals("A._privateName", elemnet.getName());
        assertEquals(code.indexOf("_privateName(num p);"), elemnet.getOffset());
        assertEquals("_privateName".length(), elemnet.getLength());
        assertEquals("(num p)", elemnet.getParameters());
        assertNull(elemnet.getReturnType());
        assertFalse(elemnet.isAbstract());
        assertTrue(elemnet.isPrivate());
        assertFalse(elemnet.isStatic());
      }
      {
        Outline outline = outlines_A[6];
        Element element = outline.getElement();
        assertSame(ElementKind.METHOD, element.getKind());
        assertEquals("ma", element.getName());
        assertEquals(code.indexOf("ma(int pa) => null;"), element.getOffset());
        assertEquals("ma".length(), element.getLength());
        assertEquals("(int pa)", element.getParameters());
        assertEquals("String", element.getReturnType());
        assertFalse(element.isAbstract());
        assertFalse(element.isPrivate());
        assertTrue(element.isStatic());
      }
      {
        Outline outline = outlines_A[7];
        Element element = outline.getElement();
        assertSame(ElementKind.METHOD, element.getKind());
        assertEquals("_mb", element.getName());
        assertEquals(code.indexOf("_mb(int pb);"), element.getOffset());
        assertEquals("_mb".length(), element.getLength());
        assertEquals("(int pb)", element.getParameters());
        assertEquals("", element.getReturnType());
        assertTrue(element.isAbstract());
        assertTrue(element.isPrivate());
        assertFalse(element.isStatic());
      }
      {
        Outline outline = outlines_A[8];
        Element element = outline.getElement();
        assertSame(ElementKind.GETTER, element.getKind());
        assertEquals("propA", element.getName());
        assertEquals(code.indexOf("propA => null;"), element.getOffset());
        assertEquals("propA".length(), element.getLength());
        assertEquals("", element.getParameters());
        assertEquals("String", element.getReturnType());
      }
      {
        Outline outline = outlines_A[9];
        Element element = outline.getElement();
        assertSame(ElementKind.SETTER, element.getKind());
        assertEquals("propB", element.getName());
        assertEquals(code.indexOf("propB(int v) {}"), element.getOffset());
        assertEquals("propB".length(), element.getLength());
        assertEquals("(int v)", element.getParameters());
        assertEquals("", element.getReturnType());
      }
    }
    // B
    {
      Outline outline_B = topOutlines[1];
      Element element_B = outline_B.getElement();
      assertSame(unitOutline, outline_B.getParent());
      assertSame(ElementKind.CLASS, element_B.getKind());
      assertEquals("B", element_B.getName());
      assertEquals(code.indexOf("B {"), element_B.getOffset());
      assertEquals(1, element_B.getLength());
      assertSame(null, element_B.getParameters());
      assertSame(null, element_B.getReturnType());
      // B children
      Outline[] outlines_B = outline_B.getChildren();
      assertThat(outlines_B).hasSize(1);
      {
        Outline outline = outlines_B[0];
        Element element = outline.getElement();
        assertSame(ElementKind.CONSTRUCTOR, element.getKind());
        assertEquals("B", element.getName());
        assertEquals(code.indexOf("B(int p);"), element.getOffset());
        assertEquals("B".length(), element.getLength());
        assertEquals("(int p)", element.getParameters());
        assertNull(element.getReturnType());
      }
    }
  }

  public void test_getSourceRange_inClass() throws Exception {
    String contextId = createContext("test");
    String code = makeSource(//
        "class A { // leftA",
        "  int methodA() {} // endA",
        "  int methodB() {} // endB",
        "}");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] outlines = unitOutline.getChildren()[0].getChildren();
    assertThat(outlines).hasSize(2);
    // methodA
    {
      Outline outline = outlines[0];
      Element element = outline.getElement();
      assertSame(ElementKind.METHOD, element.getKind());
      assertEquals("methodA", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = code.indexOf(" // leftA");
        int end = code.indexOf(" // endA");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
    // methodB
    {
      Outline outline = outlines[1];
      Element element = outline.getElement();
      assertSame(ElementKind.METHOD, element.getKind());
      assertEquals("methodB", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = code.indexOf(" // endA");
        int end = code.indexOf(" // endB");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
  }

  public void test_getSourceRange_inClass_inVariableList() throws Exception {
    String contextId = createContext("test");
    String code = makeSource(//
        "class A { // leftA",
        "  int fieldA, fieldB, fieldC; // marker",
        "  int fieldD; // marker2",
        "}");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] outlines = unitOutline.getChildren()[0].getChildren();
    assertThat(outlines).hasSize(4);
    // fieldA
    {
      Outline outline = outlines[0];
      Element element = outline.getElement();
      assertSame(ElementKind.FIELD, element.getKind());
      assertEquals("fieldA", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = code.indexOf(" // leftA");
        int end = code.indexOf(", fieldB");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
    // fieldB
    {
      Outline outline = outlines[1];
      Element element = outline.getElement();
      assertSame(ElementKind.FIELD, element.getKind());
      assertEquals("fieldB", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = code.indexOf(", fieldB");
        int end = code.indexOf(", fieldC");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
    // fieldC
    {
      Outline outline = outlines[2];
      Element element = outline.getElement();
      assertSame(ElementKind.FIELD, element.getKind());
      assertEquals("fieldC", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = code.indexOf(", fieldC");
        int end = code.indexOf(" // marker");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
    // fieldD
    {
      Outline outline = outlines[3];
      Element element = outline.getElement();
      assertSame(ElementKind.FIELD, element.getKind());
      assertEquals("fieldD", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = code.indexOf(" // marker");
        int end = code.indexOf(" // marker2");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
  }

  public void test_getSourceRange_inUnit() throws Exception {
    String contextId = createContext("test");
    String code = makeSource(//
        "class A {",
        "} // endA",
        "class B {",
        "} // endB");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] topOutlines = unitOutline.getChildren();
    assertThat(topOutlines).hasSize(2);
    // A
    {
      Outline outline = topOutlines[0];
      Element element = outline.getElement();
      assertSame(ElementKind.CLASS, element.getKind());
      assertEquals("A", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = 0;
        int end = code.indexOf(" // endA");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
    // B
    {
      Outline outline = topOutlines[1];
      Element element = outline.getElement();
      assertSame(ElementKind.CLASS, element.getKind());
      assertEquals("B", element.getName());
      {
        SourceRegion sourceRegion = outline.getSourceRegion();
        int offset = code.indexOf(" // endA");
        int end = code.indexOf(" // endB");
        assertEquals(offset, sourceRegion.getOffset());
        assertEquals(end - offset, sourceRegion.getLength());
      }
    }
  }

  public void test_localFunctions() throws Exception {
    String contextId = createContext("test");
    String code = makeSource(//
        "class A {",
        "  A() {",
        "    int local_A() {}",
        "  }",
        "  m() {",
        "    local_m() {}",
        "  }",
        "}",
        "f() {",
        "  local_f1(int i) {}",
        "  local_f2(String s) {",
        "    local_f21(int p) {}",
        "  }",
        "}");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] topOutlines = unitOutline.getChildren();
    assertThat(topOutlines).hasSize(2);
    // A
    {
      Outline outline_A = topOutlines[0];
      Element element_A = outline_A.getElement();
      assertSame(unitOutline, outline_A.getParent());
      assertSame(ElementKind.CLASS, element_A.getKind());
      assertEquals("A", element_A.getName());
      assertEquals(code.indexOf("A {"), element_A.getOffset());
      assertEquals("A".length(), element_A.getLength());
      assertSame(null, element_A.getParameters());
      assertSame(null, element_A.getReturnType());
      // A children
      Outline[] outlines_A = outline_A.getChildren();
      assertThat(outlines_A).hasSize(2);
      {
        Outline constructorOutline = outlines_A[0];
        Element constructorElement = constructorOutline.getElement();
        assertSame(ElementKind.CONSTRUCTOR, constructorElement.getKind());
        assertEquals("A", constructorElement.getName());
        assertEquals(code.indexOf("A() {"), constructorElement.getOffset());
        assertEquals("A".length(), constructorElement.getLength());
        assertEquals("()", constructorElement.getParameters());
        assertNull(constructorElement.getReturnType());
        // local function
        Outline[] outlines_constructor = constructorOutline.getChildren();
        assertThat(outlines_constructor).hasSize(1);
        {
          Outline outline = outlines_constructor[0];
          Element element = outline.getElement();
          assertSame(ElementKind.FUNCTION, element.getKind());
          assertEquals("local_A", element.getName());
          assertEquals(code.indexOf("local_A() {}"), element.getOffset());
          assertEquals("local_A".length(), element.getLength());
          assertEquals("()", element.getParameters());
          assertEquals("int", element.getReturnType());
        }
      }
      {
        Outline outline_m = outlines_A[1];
        Element element_m = outline_m.getElement();
        assertSame(ElementKind.METHOD, element_m.getKind());
        assertEquals("m", element_m.getName());
        assertEquals(code.indexOf("m() {"), element_m.getOffset());
        assertEquals("m".length(), element_m.getLength());
        assertEquals("()", element_m.getParameters());
        assertEquals("", element_m.getReturnType());
        // local function
        Outline[] methodChildren = outline_m.getChildren();
        assertThat(methodChildren).hasSize(1);
        {
          Outline outline = methodChildren[0];
          Element element = outline.getElement();
          assertSame(ElementKind.FUNCTION, element.getKind());
          assertEquals("local_m", element.getName());
          assertEquals(code.indexOf("local_m() {}"), element.getOffset());
          assertEquals("local_m".length(), element.getLength());
          assertEquals("()", element.getParameters());
          assertEquals("", element.getReturnType());
        }
      }
    }
    // f()
    {
      Outline outline_f = topOutlines[1];
      Element element_f = outline_f.getElement();
      assertSame(unitOutline, outline_f.getParent());
      assertSame(ElementKind.FUNCTION, element_f.getKind());
      assertEquals("f", element_f.getName());
      assertEquals(code.indexOf("f() {"), element_f.getOffset());
      assertEquals("f".length(), element_f.getLength());
      assertEquals("()", element_f.getParameters());
      assertEquals("", element_f.getReturnType());
      // f() children
      Outline[] outlines_f = outline_f.getChildren();
      assertThat(outlines_f).hasSize(2);
      {
        Outline outline_f1 = outlines_f[0];
        Element element_f1 = outline_f1.getElement();
        assertSame(ElementKind.FUNCTION, element_f1.getKind());
        assertEquals("local_f1", element_f1.getName());
        assertEquals(code.indexOf("local_f1(int i) {}"), element_f1.getOffset());
        assertEquals("local_f1".length(), element_f1.getLength());
        assertEquals("(int i)", element_f1.getParameters());
        assertEquals("", element_f1.getReturnType());
      }
      {
        Outline outline_f2 = outlines_f[1];
        Element element_f2 = outline_f2.getElement();
        assertSame(ElementKind.FUNCTION, element_f2.getKind());
        assertEquals("local_f2", element_f2.getName());
        assertEquals(code.indexOf("local_f2(String s) {"), element_f2.getOffset());
        assertEquals("local_f2".length(), element_f2.getLength());
        assertEquals("(String s)", element_f2.getParameters());
        assertEquals("", element_f2.getReturnType());
        // local_f2() local function
        Outline[] outlines_f2 = outline_f2.getChildren();
        assertThat(outlines_f2).hasSize(1);
        {
          Outline outline_f21 = outlines_f2[0];
          Element element_f21 = outline_f21.getElement();
          assertSame(ElementKind.FUNCTION, element_f21.getKind());
          assertEquals("local_f21", element_f21.getName());
          assertEquals(code.indexOf("local_f21(int p) {"), element_f21.getOffset());
          assertEquals("local_f21".length(), element_f21.getLength());
          assertEquals("(int p)", element_f21.getParameters());
          assertEquals("", element_f21.getReturnType());
        }
      }
    }
  }

  public void test_topLevel() throws Exception {
    String contextId = createContext("test");
    String code = makeSource(//
        "typedef String FTA(int i, String s);",
        "typedef FTB(int p);",
        "class A {}",
        "class B {}",
        "class CTA = A with B;",
        "String fA(int i, String s) => null;",
        "fB(int p) => null;",
        "String get propA => null;",
        "set propB(int v) {}",
        "");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] topOutlines = unitOutline.getChildren();
    assertThat(topOutlines).hasSize(9);
    // FTA
    {
      Outline outline = topOutlines[0];
      Element element = outline.getElement();
      assertSame(unitOutline, outline.getParent());
      assertSame(ElementKind.FUNCTION_TYPE_ALIAS, element.getKind());
      assertEquals("FTA", element.getName());
      assertEquals(code.indexOf("FTA("), element.getOffset());
      assertEquals("FTA".length(), element.getLength());
      assertEquals("(int i, String s)", element.getParameters());
      assertEquals("String", element.getReturnType());
    }
    // FTB
    {
      Outline outline = topOutlines[1];
      Element element = outline.getElement();
      assertSame(unitOutline, outline.getParent());
      assertSame(ElementKind.FUNCTION_TYPE_ALIAS, element.getKind());
      assertEquals("FTB", element.getName());
      assertEquals(code.indexOf("FTB("), element.getOffset());
      assertEquals("FTB".length(), element.getLength());
      assertEquals("(int p)", element.getParameters());
      assertEquals("", element.getReturnType());
    }
    // CTA
    {
      Outline outline = topOutlines[4];
      Element element = outline.getElement();
      assertSame(unitOutline, outline.getParent());
      assertSame(ElementKind.CLASS_TYPE_ALIAS, element.getKind());
      assertEquals("CTA", element.getName());
      assertEquals(code.indexOf("CTA ="), element.getOffset());
      assertEquals("CTA".length(), element.getLength());
      assertNull(element.getParameters());
      assertNull(element.getReturnType());
    }
    // fA
    {
      Outline outline = topOutlines[5];
      Element element = outline.getElement();
      assertSame(unitOutline, outline.getParent());
      assertSame(ElementKind.FUNCTION, element.getKind());
      assertEquals("fA", element.getName());
      assertEquals(code.indexOf("fA("), element.getOffset());
      assertEquals("fA".length(), element.getLength());
      assertEquals("(int i, String s)", element.getParameters());
      assertEquals("String", element.getReturnType());
    }
    // fB
    {
      Outline outline = topOutlines[6];
      Element element = outline.getElement();
      assertSame(unitOutline, outline.getParent());
      assertSame(ElementKind.FUNCTION, element.getKind());
      assertEquals("fB", element.getName());
      assertEquals(code.indexOf("fB("), element.getOffset());
      assertEquals("fB".length(), element.getLength());
      assertEquals("(int p)", element.getParameters());
      assertEquals("", element.getReturnType());
    }
    // propA
    {
      Outline outline = topOutlines[7];
      Element element = outline.getElement();
      assertSame(unitOutline, outline.getParent());
      assertSame(ElementKind.GETTER, element.getKind());
      assertEquals("propA", element.getName());
      assertEquals(code.indexOf("propA => null;"), element.getOffset());
      assertEquals("propA".length(), element.getLength());
      assertEquals("", element.getParameters());
      assertEquals("String", element.getReturnType());
    }
    // propB
    {
      Outline outline = topOutlines[8];
      Element element = outline.getElement();
      assertSame(unitOutline, outline.getParent());
      assertSame(ElementKind.SETTER, element.getKind());
      assertEquals("propB", element.getName());
      assertEquals(code.indexOf("propB(int v) {}"), element.getOffset());
      assertEquals("propB".length(), element.getLength());
      assertEquals("(int v)", element.getParameters());
      assertEquals("", element.getReturnType());
    }
  }

  public void test_unittest() throws Exception {
    String contextId = createContext("test");
    addSource(contextId, "/unittest.dart", makeSource(//
        "library unittest;",
        "group(String name, f()) {}",
        "test(String name, f()) {}"));
    String code = makeSource(//
        "import 'unittest.dart';",
        "main() {",
        "  group('groupA', () {",
        "    group('groupAA', () {",
        "      test('testAA_A', () {}); // testAA_A end",
        "      test('testAA_B', () {}); // testAA_B end",
        "    }); // groupAA end",
        "    test('testA_A', () {}); // testA_A end",
        "  }); // groupA end",
        "  group('groupB', () {",
        "    test('testB_A', () {}); // testB_A end",
        "    test('testB_B', () {}); // testB_A end",
        "    test('testB_C', () {}); // testB_A end",
        "  }); // groupB end",
        "}");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] topOutlines = unitOutline.getChildren();
    assertThat(topOutlines).hasSize(1);
    // "main"
    {
      Outline outline_main = topOutlines[0];
      Element element_main = outline_main.getElement();
      assertSame(unitOutline, outline_main.getParent());
      assertSame(ElementKind.FUNCTION, element_main.getKind());
      assertEquals("main", element_main.getName());
      assertEquals(code.indexOf("main() {"), element_main.getOffset());
      assertEquals("main".length(), element_main.getLength());
      // "main" children
      Outline[] outlines_main = outline_main.getChildren();
      assertThat(outlines_main).hasSize(2);
      // "groupA"
      {
        Outline outline_A = outlines_main[0];
        Element element_A = outline_A.getElement();
        assertSame(ElementKind.UNIT_TEST_GROUP, element_A.getKind());
        assertEquals("groupA", element_A.getName());
        assertEquals(code.indexOf("groupA'"), element_A.getOffset());
        assertEquals("groupA".length(), element_A.getLength());
        assertNull(element_A.getParameters());
        assertSame(null, element_A.getReturnType());
        assertSame(null, element_A.getParameters());
        {
          SourceRegion sourceRegion = outline_A.getSourceRegion();
          int offset = code.indexOf("group('groupA'");
          int end = code.indexOf("; // groupA end");
          assertEquals(offset, sourceRegion.getOffset());
          assertEquals(end - offset, sourceRegion.getLength());
        }
        // "groupA" children
        Outline[] outlines_groupA = outline_A.getChildren();
        assertThat(outlines_groupA).hasSize(2);
        // "groupAA"
        {
          Outline outline_AA = outlines_groupA[0];
          Element element_AA = outline_AA.getElement();
          assertSame(ElementKind.UNIT_TEST_GROUP, element_AA.getKind());
          assertEquals("groupAA", element_AA.getName());
          assertEquals(code.indexOf("groupAA'"), element_AA.getOffset());
          assertEquals("groupAA".length(), element_AA.getLength());
          assertNull(element_AA.getParameters());
          assertSame(null, element_AA.getReturnType());
          assertSame(null, element_AA.getParameters());
          {
            SourceRegion sourceRegion = outline_AA.getSourceRegion();
            int offset = code.indexOf("group('groupAA'");
            int end = code.indexOf("; // groupAA end");
            assertEquals(offset, sourceRegion.getOffset());
            assertEquals(end - offset, sourceRegion.getLength());
          }
          // "groupAA" children
          Outline[] outlines_groupAA = outline_AA.getChildren();
          assertThat(outlines_groupAA).hasSize(2);
          // "testAA_A"
          {
            Outline outline_AA_A = outlines_groupAA[0];
            Element element_AA_A = outline_AA_A.getElement();
            assertSame(ElementKind.UNIT_TEST_CASE, element_AA_A.getKind());
            assertEquals("testAA_A", element_AA_A.getName());
          }
          // "testAA_B"
          {
            Outline outline_AA_B = outlines_groupAA[1];
            Element element_AA_B = outline_AA_B.getElement();
            assertSame(ElementKind.UNIT_TEST_CASE, element_AA_B.getKind());
            assertEquals("testAA_B", element_AA_B.getName());
          }
        }
        // "testA_A"
        {
          Outline outline_A_A = outlines_groupA[1];
          Element element_A_A = outline_A_A.getElement();
          assertSame(ElementKind.UNIT_TEST_CASE, element_A_A.getKind());
          assertEquals("testA_A", element_A_A.getName());
          assertEquals(code.indexOf("testA_A'"), element_A_A.getOffset());
          assertEquals("testA_A".length(), element_A_A.getLength());
          assertNull(element_A_A.getParameters());
          assertSame(null, element_A_A.getReturnType());
          assertSame(null, element_A_A.getParameters());
          {
            SourceRegion sourceRegion = outline_A_A.getSourceRegion();
            int offset = code.indexOf("test('testA_A'");
            int end = code.indexOf("; // testA_A end");
            assertEquals(offset, sourceRegion.getOffset());
            assertEquals(end - offset, sourceRegion.getLength());
          }
          assertThat(outline_A_A.getChildren()).isEmpty();
        }
      }
      // "groupB"
      {
        Outline outline_B = outlines_main[1];
        Element element_B = outline_B.getElement();
        assertSame(ElementKind.UNIT_TEST_GROUP, element_B.getKind());
        assertEquals("groupB", element_B.getName());
        assertEquals(code.indexOf("groupB'"), element_B.getOffset());
        assertEquals("groupB".length(), element_B.getLength());
        assertNull(element_B.getParameters());
        assertSame(null, element_B.getReturnType());
        assertSame(null, element_B.getParameters());
        {
          SourceRegion sourceRegion = outline_B.getSourceRegion();
          int offset = code.indexOf("group('groupB'");
          int end = code.indexOf("; // groupB end");
          assertEquals(offset, sourceRegion.getOffset());
          assertEquals(end - offset, sourceRegion.getLength());
        }
        // "groupB" children
        Outline[] outlines_groupB = outline_B.getChildren();
        assertThat(outlines_groupB).hasSize(3);
        // "testB_A"
        {
          Outline outline_B_A = outlines_groupB[0];
          Element element_B_A = outline_B_A.getElement();
          assertSame(ElementKind.UNIT_TEST_CASE, element_B_A.getKind());
          assertEquals("testB_A", element_B_A.getName());
        }
        // "testB_B"
        {
          Outline outline_B_B = outlines_groupB[1];
          Element element_B_B = outline_B_B.getElement();
          assertSame(ElementKind.UNIT_TEST_CASE, element_B_B.getKind());
          assertEquals("testB_B", element_B_B.getName());
        }
        // "testB_C"
        {
          Outline outline_B_C = outlines_groupB[2];
          Element element_B_C = outline_B_C.getElement();
          assertSame(ElementKind.UNIT_TEST_CASE, element_B_C.getKind());
          assertEquals("testB_C", element_B_C.getName());
        }
      }
    }
  }

  public void test_unittest_bad() throws Exception {
    String contextId = createContext("test");
    addSource(contextId, "/unittest.dart", makeSource(//
        "library unittest;",
        "group(String name, f()) {}",
        "test(String name, f()) {}"));
    String code = makeSource(//
        "import 'unittest.dart';",
        "main() {",
        "  someOtherInvocation();",
        "  group(); // not 2 arguments",
        "  group('groupA', null); // not function expression",
        "  test(); // not 2 arguments",
        "  test('groupA', null); // not function expression",
        "}");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] topOutlines = unitOutline.getChildren();
    assertThat(topOutlines).hasSize(1);
    // "main"
    {
      Outline outline_main = topOutlines[0];
      Element element_main = outline_main.getElement();
      assertEquals("main", element_main.getName());
      // "main" children
      Outline[] outlines_main = outline_main.getChildren();
      assertThat(outlines_main).isEmpty();
    }
  }

  public void test_unittest_bad_notLiteralName() throws Exception {
    String contextId = createContext("test");
    addSource(contextId, "/unittest.dart", makeSource(//
        "library unittest;",
        "group(String name, f()) {}",
        "test(String name, f()) {}"));
    String code = makeSource(//
        "import 'unittest.dart';",
        "main() {",
        "  var groupName = 'foo';",
        "  var testName = 'bar';",
        "  group(groupName, () {});",
        "  test(testName, () {});",
        "}");
    Source source = addSource(contextId, "/test.dart", code);
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.OUTLINE, ListSourceSet.create(source)));
    server.test_waitForWorkerComplete();
    // validate
    Outline unitOutline = serverListener.getOutline(contextId, source);
    Outline[] topOutlines = unitOutline.getChildren();
    assertThat(topOutlines).hasSize(1);
    // "main"
    {
      Outline outline_main = topOutlines[0];
      Element element_main = outline_main.getElement();
      assertEquals("main", element_main.getName());
      // "main" children
      Outline[] outlines_main = outline_main.getChildren();
      assertThat(outlines_main).hasSize(2);
      {
        Outline outline = outlines_main[0];
        Element element = outline.getElement();
        assertSame(ElementKind.UNIT_TEST_GROUP, element.getKind());
        assertEquals("??????????", element.getName());
        assertEquals(code.indexOf("groupName, ()"), element.getOffset());
        assertEquals("groupName".length(), element.getLength());
      }
      {
        Outline outline = outlines_main[1];
        Element element = outline.getElement();
        assertSame(ElementKind.UNIT_TEST_CASE, element.getKind());
        assertEquals("??????????", element.getName());
        assertEquals(code.indexOf("testName, ()"), element.getOffset());
        assertEquals("testName".length(), element.getLength());
      }
    }
  }
}
