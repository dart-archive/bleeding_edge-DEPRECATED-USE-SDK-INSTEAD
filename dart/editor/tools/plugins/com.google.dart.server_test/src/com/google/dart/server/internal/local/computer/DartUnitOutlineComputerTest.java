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
import com.google.dart.server.ListSourceSet;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.Outline;
import com.google.dart.server.OutlineKind;
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
      assertSame(unitOutline, outline_A.getParent());
      assertSame(OutlineKind.CLASS, outline_A.getKind());
      assertEquals("A", outline_A.getName());
      assertEquals(code.indexOf("A {"), outline_A.getOffset());
      assertEquals(1, outline_A.getLength());
      assertSame(null, outline_A.getParameters());
      assertSame(null, outline_A.getReturnType());
      // A children
      Outline[] outlines_A = outline_A.getChildren();
      assertThat(outlines_A).hasSize(10);
      {
        Outline outline = outlines_A[0];
        assertSame(OutlineKind.FIELD, outline.getKind());
        assertEquals("fa", outline.getName());
        assertNull(outline.getParameters());
        assertEquals("int", outline.getReturnType());
      }
      {
        Outline outline = outlines_A[1];
        assertSame(OutlineKind.FIELD, outline.getKind());
        assertEquals("fb", outline.getName());
        assertNull(outline.getParameters());
        assertEquals("int", outline.getReturnType());
      }
      {
        Outline outline = outlines_A[2];
        assertSame(OutlineKind.FIELD, outline.getKind());
        assertEquals("fc", outline.getName());
        assertNull(outline.getParameters());
        assertEquals("String", outline.getReturnType());
      }
      {
        Outline outline = outlines_A[3];
        assertSame(OutlineKind.CONSTRUCTOR, outline.getKind());
        assertEquals("A", outline.getName());
        assertEquals(code.indexOf("A(int i, String s);"), outline.getOffset());
        assertEquals("A".length(), outline.getLength());
        assertEquals("(int i, String s)", outline.getParameters());
        assertNull(outline.getReturnType());
        assertFalse(outline.isAbstract());
        assertFalse(outline.isPrivate());
        assertFalse(outline.isStatic());
      }
      {
        Outline outline = outlines_A[4];
        assertSame(OutlineKind.CONSTRUCTOR, outline.getKind());
        assertEquals("A.name", outline.getName());
        assertEquals(code.indexOf("name(num p);"), outline.getOffset());
        assertEquals("name".length(), outline.getLength());
        assertEquals("(num p)", outline.getParameters());
        assertNull(outline.getReturnType());
        assertFalse(outline.isAbstract());
        assertFalse(outline.isPrivate());
        assertFalse(outline.isStatic());
      }
      {
        Outline outline = outlines_A[5];
        assertSame(OutlineKind.CONSTRUCTOR, outline.getKind());
        assertEquals("A._privateName", outline.getName());
        assertEquals(code.indexOf("_privateName(num p);"), outline.getOffset());
        assertEquals("_privateName".length(), outline.getLength());
        assertEquals("(num p)", outline.getParameters());
        assertNull(outline.getReturnType());
        assertFalse(outline.isAbstract());
        assertTrue(outline.isPrivate());
        assertFalse(outline.isStatic());
      }
      {
        Outline outline = outlines_A[6];
        assertSame(OutlineKind.METHOD, outline.getKind());
        assertEquals("ma", outline.getName());
        assertEquals(code.indexOf("ma(int pa) => null;"), outline.getOffset());
        assertEquals("ma".length(), outline.getLength());
        assertEquals("(int pa)", outline.getParameters());
        assertEquals("String", outline.getReturnType());
        assertFalse(outline.isAbstract());
        assertFalse(outline.isPrivate());
        assertTrue(outline.isStatic());
      }
      {
        Outline outline = outlines_A[7];
        assertSame(OutlineKind.METHOD, outline.getKind());
        assertEquals("_mb", outline.getName());
        assertEquals(code.indexOf("_mb(int pb);"), outline.getOffset());
        assertEquals("_mb".length(), outline.getLength());
        assertEquals("(int pb)", outline.getParameters());
        assertEquals("", outline.getReturnType());
        assertTrue(outline.isAbstract());
        assertTrue(outline.isPrivate());
        assertFalse(outline.isStatic());
      }
      {
        Outline outline = outlines_A[8];
        assertSame(OutlineKind.GETTER, outline.getKind());
        assertEquals("propA", outline.getName());
        assertEquals(code.indexOf("propA => null;"), outline.getOffset());
        assertEquals("propA".length(), outline.getLength());
        assertEquals("", outline.getParameters());
        assertEquals("String", outline.getReturnType());
      }
      {
        Outline outline = outlines_A[9];
        assertSame(OutlineKind.SETTER, outline.getKind());
        assertEquals("propB", outline.getName());
        assertEquals(code.indexOf("propB(int v) {}"), outline.getOffset());
        assertEquals("propB".length(), outline.getLength());
        assertEquals("(int v)", outline.getParameters());
        assertEquals("", outline.getReturnType());
      }
    }
    // B
    {
      Outline outline_B = topOutlines[1];
      assertSame(unitOutline, outline_B.getParent());
      assertSame(OutlineKind.CLASS, outline_B.getKind());
      assertEquals("B", outline_B.getName());
      assertEquals(code.indexOf("B {"), outline_B.getOffset());
      assertEquals(1, outline_B.getLength());
      assertSame(null, outline_B.getParameters());
      assertSame(null, outline_B.getReturnType());
      // B children
      Outline[] outlines_B = outline_B.getChildren();
      assertThat(outlines_B).hasSize(1);
      {
        Outline outline = outlines_B[0];
        assertSame(OutlineKind.CONSTRUCTOR, outline.getKind());
        assertEquals("B", outline.getName());
        assertEquals(code.indexOf("B(int p);"), outline.getOffset());
        assertEquals("B".length(), outline.getLength());
        assertEquals("(int p)", outline.getParameters());
        assertNull(outline.getReturnType());
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
      assertSame(OutlineKind.METHOD, outline.getKind());
      assertEquals("methodA", outline.getName());
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
      assertSame(OutlineKind.METHOD, outline.getKind());
      assertEquals("methodB", outline.getName());
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
      assertSame(OutlineKind.FIELD, outline.getKind());
      assertEquals("fieldA", outline.getName());
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
      assertSame(OutlineKind.FIELD, outline.getKind());
      assertEquals("fieldB", outline.getName());
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
      assertSame(OutlineKind.FIELD, outline.getKind());
      assertEquals("fieldC", outline.getName());
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
      assertSame(OutlineKind.FIELD, outline.getKind());
      assertEquals("fieldD", outline.getName());
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
      assertSame(OutlineKind.CLASS, outline.getKind());
      assertEquals("A", outline.getName());
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
      assertSame(OutlineKind.CLASS, outline.getKind());
      assertEquals("B", outline.getName());
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
      assertSame(unitOutline, outline_A.getParent());
      assertSame(OutlineKind.CLASS, outline_A.getKind());
      assertEquals("A", outline_A.getName());
      assertEquals(code.indexOf("A {"), outline_A.getOffset());
      assertEquals("A".length(), outline_A.getLength());
      assertSame(null, outline_A.getParameters());
      assertSame(null, outline_A.getReturnType());
      // A children
      Outline[] outlines_A = outline_A.getChildren();
      assertThat(outlines_A).hasSize(2);
      {
        Outline constructorOutline = outlines_A[0];
        assertSame(OutlineKind.CONSTRUCTOR, constructorOutline.getKind());
        assertEquals("A", constructorOutline.getName());
        assertEquals(code.indexOf("A() {"), constructorOutline.getOffset());
        assertEquals("A".length(), constructorOutline.getLength());
        assertEquals("()", constructorOutline.getParameters());
        assertNull(constructorOutline.getReturnType());
        // local function
        Outline[] outlines_constructor = constructorOutline.getChildren();
        assertThat(outlines_constructor).hasSize(1);
        {
          Outline outline = outlines_constructor[0];
          assertSame(OutlineKind.FUNCTION, outline.getKind());
          assertEquals("local_A", outline.getName());
          assertEquals(code.indexOf("local_A() {}"), outline.getOffset());
          assertEquals("local_A".length(), outline.getLength());
          assertEquals("()", outline.getParameters());
          assertEquals("int", outline.getReturnType());
        }
      }
      {
        Outline outlines_m = outlines_A[1];
        assertSame(OutlineKind.METHOD, outlines_m.getKind());
        assertEquals("m", outlines_m.getName());
        assertEquals(code.indexOf("m() {"), outlines_m.getOffset());
        assertEquals("m".length(), outlines_m.getLength());
        assertEquals("()", outlines_m.getParameters());
        assertEquals("", outlines_m.getReturnType());
        // local function
        Outline[] methodChildren = outlines_m.getChildren();
        assertThat(methodChildren).hasSize(1);
        {
          Outline outline = methodChildren[0];
          assertSame(OutlineKind.FUNCTION, outline.getKind());
          assertEquals("local_m", outline.getName());
          assertEquals(code.indexOf("local_m() {}"), outline.getOffset());
          assertEquals("local_m".length(), outline.getLength());
          assertEquals("()", outline.getParameters());
          assertEquals("", outline.getReturnType());
        }
      }
    }
    // f()
    {
      Outline outline_f = topOutlines[1];
      assertSame(unitOutline, outline_f.getParent());
      assertSame(OutlineKind.FUNCTION, outline_f.getKind());
      assertEquals("f", outline_f.getName());
      assertEquals(code.indexOf("f() {"), outline_f.getOffset());
      assertEquals("f".length(), outline_f.getLength());
      assertEquals("()", outline_f.getParameters());
      assertEquals("", outline_f.getReturnType());
      // f() children
      Outline[] outlines_f = outline_f.getChildren();
      assertThat(outlines_f).hasSize(2);
      {
        Outline outline_f1 = outlines_f[0];
        assertSame(OutlineKind.FUNCTION, outline_f1.getKind());
        assertEquals("local_f1", outline_f1.getName());
        assertEquals(code.indexOf("local_f1(int i) {}"), outline_f1.getOffset());
        assertEquals("local_f1".length(), outline_f1.getLength());
        assertEquals("(int i)", outline_f1.getParameters());
        assertEquals("", outline_f1.getReturnType());
      }
      {
        Outline outline_f2 = outlines_f[1];
        assertSame(OutlineKind.FUNCTION, outline_f2.getKind());
        assertEquals("local_f2", outline_f2.getName());
        assertEquals(code.indexOf("local_f2(String s) {"), outline_f2.getOffset());
        assertEquals("local_f2".length(), outline_f2.getLength());
        assertEquals("(String s)", outline_f2.getParameters());
        assertEquals("", outline_f2.getReturnType());
        // local_f2() local function
        Outline[] outlines_f2 = outline_f2.getChildren();
        assertThat(outlines_f2).hasSize(1);
        {
          Outline outline_f21 = outlines_f2[0];
          assertSame(OutlineKind.FUNCTION, outline_f21.getKind());
          assertEquals("local_f21", outline_f21.getName());
          assertEquals(code.indexOf("local_f21(int p) {"), outline_f21.getOffset());
          assertEquals("local_f21".length(), outline_f21.getLength());
          assertEquals("(int p)", outline_f21.getParameters());
          assertEquals("", outline_f21.getReturnType());
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
      assertSame(unitOutline, outline.getParent());
      assertSame(OutlineKind.FUNCTION_TYPE_ALIAS, outline.getKind());
      assertEquals("FTA", outline.getName());
      assertEquals(code.indexOf("FTA("), outline.getOffset());
      assertEquals("FTA".length(), outline.getLength());
      assertEquals("(int i, String s)", outline.getParameters());
      assertEquals("String", outline.getReturnType());
    }
    // FTB
    {
      Outline outline = topOutlines[1];
      assertSame(unitOutline, outline.getParent());
      assertSame(OutlineKind.FUNCTION_TYPE_ALIAS, outline.getKind());
      assertEquals("FTB", outline.getName());
      assertEquals(code.indexOf("FTB("), outline.getOffset());
      assertEquals("FTB".length(), outline.getLength());
      assertEquals("(int p)", outline.getParameters());
      assertEquals("", outline.getReturnType());
    }
    // CTA
    {
      Outline outline = topOutlines[4];
      assertSame(unitOutline, outline.getParent());
      assertSame(OutlineKind.CLASS_TYPE_ALIAS, outline.getKind());
      assertEquals("CTA", outline.getName());
      assertEquals(code.indexOf("CTA ="), outline.getOffset());
      assertEquals("CTA".length(), outline.getLength());
      assertNull(outline.getParameters());
      assertNull(outline.getReturnType());
    }
    // fA
    {
      Outline outline = topOutlines[5];
      assertSame(unitOutline, outline.getParent());
      assertSame(OutlineKind.FUNCTION, outline.getKind());
      assertEquals("fA", outline.getName());
      assertEquals(code.indexOf("fA("), outline.getOffset());
      assertEquals("fA".length(), outline.getLength());
      assertEquals("(int i, String s)", outline.getParameters());
      assertEquals("String", outline.getReturnType());
    }
    // fB
    {
      Outline outline = topOutlines[6];
      assertSame(unitOutline, outline.getParent());
      assertSame(OutlineKind.FUNCTION, outline.getKind());
      assertEquals("fB", outline.getName());
      assertEquals(code.indexOf("fB("), outline.getOffset());
      assertEquals("fB".length(), outline.getLength());
      assertEquals("(int p)", outline.getParameters());
      assertEquals("", outline.getReturnType());
    }
    // propA
    {
      Outline outline = topOutlines[7];
      assertSame(unitOutline, outline.getParent());
      assertSame(OutlineKind.GETTER, outline.getKind());
      assertEquals("propA", outline.getName());
      assertEquals(code.indexOf("propA => null;"), outline.getOffset());
      assertEquals("propA".length(), outline.getLength());
      assertEquals("", outline.getParameters());
      assertEquals("String", outline.getReturnType());
    }
    // propB
    {
      Outline outline = topOutlines[8];
      assertSame(unitOutline, outline.getParent());
      assertSame(OutlineKind.SETTER, outline.getKind());
      assertEquals("propB", outline.getName());
      assertEquals(code.indexOf("propB(int v) {}"), outline.getOffset());
      assertEquals("propB".length(), outline.getLength());
      assertEquals("(int v)", outline.getParameters());
      assertEquals("", outline.getReturnType());
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
      assertSame(unitOutline, outline_main.getParent());
      assertSame(OutlineKind.FUNCTION, outline_main.getKind());
      assertEquals("main", outline_main.getName());
      assertEquals(code.indexOf("main() {"), outline_main.getOffset());
      assertEquals("main".length(), outline_main.getLength());
      // "main" children
      Outline[] outlines_main = outline_main.getChildren();
      assertThat(outlines_main).hasSize(2);
      // "groupA"
      {
        Outline groupA = outlines_main[0];
        assertSame(OutlineKind.UNIT_TEST_GROUP, groupA.getKind());
        assertEquals("groupA", groupA.getName());
        assertEquals(code.indexOf("groupA'"), groupA.getOffset());
        assertEquals("groupA".length(), groupA.getLength());
        assertNull(groupA.getParameters());
        assertSame(null, groupA.getReturnType());
        assertSame(null, groupA.getParameters());
        {
          SourceRegion sourceRegion = groupA.getSourceRegion();
          int offset = code.indexOf("group('groupA'");
          int end = code.indexOf("; // groupA end");
          assertEquals(offset, sourceRegion.getOffset());
          assertEquals(end - offset, sourceRegion.getLength());
        }
        // "groupA" children
        Outline[] outlines_groupA = groupA.getChildren();
        assertThat(outlines_groupA).hasSize(2);
        // "groupAA"
        {
          Outline groupAA = outlines_groupA[0];
          assertSame(OutlineKind.UNIT_TEST_GROUP, groupAA.getKind());
          assertEquals("groupAA", groupAA.getName());
          assertEquals(code.indexOf("groupAA'"), groupAA.getOffset());
          assertEquals("groupAA".length(), groupAA.getLength());
          assertNull(groupAA.getParameters());
          assertSame(null, groupAA.getReturnType());
          assertSame(null, groupAA.getParameters());
          {
            SourceRegion sourceRegion = groupAA.getSourceRegion();
            int offset = code.indexOf("group('groupAA'");
            int end = code.indexOf("; // groupAA end");
            assertEquals(offset, sourceRegion.getOffset());
            assertEquals(end - offset, sourceRegion.getLength());
          }
        }
        // "groupAA"
        {
          Outline groupAA = outlines_groupA[0];
          assertSame(OutlineKind.UNIT_TEST_GROUP, groupAA.getKind());
          assertEquals("groupAA", groupAA.getName());
          assertEquals(code.indexOf("groupAA'"), groupAA.getOffset());
          assertEquals("groupAA".length(), groupAA.getLength());
          assertNull(groupAA.getParameters());
          assertSame(null, groupAA.getReturnType());
          assertSame(null, groupAA.getParameters());
          {
            SourceRegion sourceRegion = groupAA.getSourceRegion();
            int offset = code.indexOf("group('groupAA'");
            int end = code.indexOf("; // groupAA end");
            assertEquals(offset, sourceRegion.getOffset());
            assertEquals(end - offset, sourceRegion.getLength());
          }
          // "groupAA" children
          Outline[] outlines_groupAA = groupAA.getChildren();
          assertThat(outlines_groupAA).hasSize(2);
          // "testAA_A"
          {
            Outline testAA_A = outlines_groupAA[0];
            assertSame(OutlineKind.UNIT_TEST_CASE, testAA_A.getKind());
            assertEquals("testAA_A", testAA_A.getName());
          }
          // "testAA_B"
          {
            Outline testAA_B = outlines_groupAA[1];
            assertSame(OutlineKind.UNIT_TEST_CASE, testAA_B.getKind());
            assertEquals("testAA_B", testAA_B.getName());
          }
        }
        // "testA_A"
        {
          Outline testA_A = outlines_groupA[1];
          assertSame(OutlineKind.UNIT_TEST_CASE, testA_A.getKind());
          assertEquals("testA_A", testA_A.getName());
          assertEquals(code.indexOf("testA_A'"), testA_A.getOffset());
          assertEquals("testA_A".length(), testA_A.getLength());
          assertNull(testA_A.getParameters());
          assertSame(null, testA_A.getReturnType());
          assertSame(null, testA_A.getParameters());
          {
            SourceRegion sourceRegion = testA_A.getSourceRegion();
            int offset = code.indexOf("test('testA_A'");
            int end = code.indexOf("; // testA_A end");
            assertEquals(offset, sourceRegion.getOffset());
            assertEquals(end - offset, sourceRegion.getLength());
          }
          assertThat(testA_A.getChildren()).isEmpty();
        }
      }
      // "groupB"
      {
        Outline groupB = outlines_main[1];
        assertSame(OutlineKind.UNIT_TEST_GROUP, groupB.getKind());
        assertEquals("groupB", groupB.getName());
        assertEquals(code.indexOf("groupB'"), groupB.getOffset());
        assertEquals("groupB".length(), groupB.getLength());
        assertNull(groupB.getParameters());
        assertSame(null, groupB.getReturnType());
        assertSame(null, groupB.getParameters());
        {
          SourceRegion sourceRegion = groupB.getSourceRegion();
          int offset = code.indexOf("group('groupB'");
          int end = code.indexOf("; // groupB end");
          assertEquals(offset, sourceRegion.getOffset());
          assertEquals(end - offset, sourceRegion.getLength());
        }
        // "groupB" children
        Outline[] outlines_groupB = groupB.getChildren();
        assertThat(outlines_groupB).hasSize(3);
        // "testB_A"
        {
          Outline testB_A = outlines_groupB[0];
          assertSame(OutlineKind.UNIT_TEST_CASE, testB_A.getKind());
          assertEquals("testB_A", testB_A.getName());
        }
        // "testB_B"
        {
          Outline testB_B = outlines_groupB[1];
          assertSame(OutlineKind.UNIT_TEST_CASE, testB_B.getKind());
          assertEquals("testB_B", testB_B.getName());
        }
        // "testB_C"
        {
          Outline testB_C = outlines_groupB[2];
          assertSame(OutlineKind.UNIT_TEST_CASE, testB_C.getKind());
          assertEquals("testB_C", testB_C.getName());
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
      assertEquals("main", outline_main.getName());
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
      assertEquals("main", outline_main.getName());
      // "main" children
      Outline[] outlines_main = outline_main.getChildren();
      assertThat(outlines_main).hasSize(2);
      {
        Outline outline = outlines_main[0];
        assertSame(OutlineKind.UNIT_TEST_GROUP, outline.getKind());
        assertEquals("??????????", outline.getName());
        assertEquals(code.indexOf("groupName, ()"), outline.getOffset());
        assertEquals("groupName".length(), outline.getLength());
      }
      {
        Outline outline = outlines_main[1];
        assertSame(OutlineKind.UNIT_TEST_CASE, outline.getKind());
        assertEquals("??????????", outline.getName());
        assertEquals(code.indexOf("testName, ()"), outline.getOffset());
        assertEquals("testName".length(), outline.getLength());
      }
    }
  }
}
