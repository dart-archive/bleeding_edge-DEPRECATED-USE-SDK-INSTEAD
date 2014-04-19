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
      assertThat(outlines_A).hasSize(9);
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
      }
      {
        Outline outline = outlines_A[4];
        assertSame(OutlineKind.CONSTRUCTOR, outline.getKind());
        assertEquals("A.name", outline.getName());
        assertEquals(code.indexOf("name(num p);"), outline.getOffset());
        assertEquals("name".length(), outline.getLength());
        assertEquals("(num p)", outline.getParameters());
        assertNull(outline.getReturnType());
      }
      {
        Outline outline = outlines_A[5];
        assertSame(OutlineKind.METHOD, outline.getKind());
        {
          String maCode = "String ma(int pa) => null;";
          int offset = code.indexOf("p);") + "p);".length();
          int end = code.indexOf(maCode) + maCode.length();
          assertEquals(offset, outline.getSourceRegion().getOffset());
          assertEquals(end - offset, outline.getSourceRegion().getLength());
        }
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
        Outline outline = outlines_A[6];
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
        Outline outline = outlines_A[7];
        assertSame(OutlineKind.GETTER, outline.getKind());
        assertEquals("propA", outline.getName());
        assertEquals(code.indexOf("propA => null;"), outline.getOffset());
        assertEquals("propA".length(), outline.getLength());
        assertEquals("", outline.getParameters());
        assertEquals("String", outline.getReturnType());
      }
      {
        Outline outline = outlines_A[8];
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
}
