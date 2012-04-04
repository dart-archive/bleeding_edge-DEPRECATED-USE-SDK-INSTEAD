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

import com.google.common.base.Joiner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link DartVariableImpl}.
 */
public class DartVariableImplTest extends TestCase {
  /**
   * Test for {@link DartVariableDeclaration#getVisibleRange()} for local variables and parameters.
   */
  public void test_getVisibleRange_local() throws Exception {
    TestProject testProject = new TestProject("Test");
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "f(var v1) {",
              "  var v2;",
              "  {",
              "    var v3;",
              "  }//marker1",
              "  var v4;",
              "}//marker2",
              ""));
      String source = unit.getSource();
      DartFunction function = (DartFunction) unit.getChildren()[0];
      DartVariableDeclaration[] variables = function.getLocalVariables();
      assertThat(variables).hasSize(4);
      // v1
      {
        DartVariableDeclaration var = variables[0];
        SourceRange range = var.getVisibleRange();
        assertEquals("v1", var.getElementName());
        assertEquals(source.indexOf("v1"), range.getOffset());
        assertEquals(source.indexOf("marker2") - 1, range.getOffset() + range.getLength());
      }
      // v2
      {
        DartVariableDeclaration var = variables[1];
        SourceRange range = var.getVisibleRange();
        assertEquals("v2", var.getElementName());
        assertEquals(source.indexOf("v2"), range.getOffset());
        assertEquals(source.indexOf("marker2") - 1, range.getOffset() + range.getLength());
      }
      // v3
      {
        DartVariableDeclaration var = variables[2];
        SourceRange range = var.getVisibleRange();
        assertEquals("v3", var.getElementName());
        assertEquals(source.indexOf("v3"), range.getOffset());
        assertEquals(source.indexOf("marker1") - 1, range.getOffset() + range.getLength());
      }
      // v4
      {
        DartVariableDeclaration var = variables[3];
        SourceRange range = var.getVisibleRange();
        assertEquals("v4", var.getElementName());
        assertEquals(source.indexOf("v4"), range.getOffset());
        assertEquals(source.indexOf("marker2") - 1, range.getOffset() + range.getLength());
      }
    } finally {
      testProject.dispose();
    }
  }
}
