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
package com.google.dart.tools.core.internal.model;

import static org.fest.assertions.Assertions.assertThat;

import com.google.common.base.Joiner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

/**
 * Test for {@link DartFunctionImpl}.
 */
public class DartFunctionImplTest extends TestCase {
  /**
   * Test for {@link DartFunction#getVisibleRange()} for local functions.
   */
  public void test_getVisibleRange() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "f() {",
              "  v1() {};",
              "  {",
              "    v2() {};",
              "  }//marker1",
              "  v3() {};",
              "}//marker2",
              ""));
      String source = unit.getSource();
      DartFunction globalFunction = (DartFunction) unit.getChildren()[0];
      DartElement[] functions = globalFunction.getChildren();
      assertThat(functions).hasSize(3);
      // v1
      {
        DartFunction f = (DartFunction) functions[0];
        SourceRange range = f.getVisibleRange();
        assertEquals("v1", f.getElementName());
        assertEquals(source.indexOf("v1"), range.getOffset());
        assertEquals(source.indexOf("marker2") - 1, range.getOffset() + range.getLength());
      }
      // v2
      {
        DartFunction f = (DartFunction) functions[1];
        SourceRange range = f.getVisibleRange();
        assertEquals("v2", f.getElementName());
        assertEquals(source.indexOf("v2"), range.getOffset());
        assertEquals(source.indexOf("marker1") - 1, range.getOffset() + range.getLength());
      }
      // v3
      {
        DartFunction f = (DartFunction) functions[2];
        SourceRange range = f.getVisibleRange();
        assertEquals("v3", f.getElementName());
        assertEquals(source.indexOf("v3"), range.getOffset());
        assertEquals(source.indexOf("marker2") - 1, range.getOffset() + range.getLength());
      }
    } finally {
      testProject.dispose();
    }
  }

  /**
   * Test for {@link DartFunction#isGlobal()} and {@link DartFunction#isLocal()}.
   */
  public void test_isGlobal_isLocal() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "f() {",
              "  v() {};",
              "}",
              ""));
      // global function
      DartFunction globalFunction = (DartFunction) unit.getChildren()[0];
      assertTrue(globalFunction.isGlobal());
      assertFalse(globalFunction.isLocal());
      // local functions
      DartElement[] functions = globalFunction.getChildren();
      assertThat(functions).hasSize(1);
      // v
      {
        DartFunction f = (DartFunction) functions[0];
        assertFalse(f.isGlobal());
        assertTrue(f.isLocal());
      }
    } finally {
      testProject.dispose();
    }
  }
}
