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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

/**
 * Test for {@link DartTypeParameterImpl}.
 */
public class DartTypeParameterImplTest extends TestCase {
  public void test_inFunctionTypeAlias() throws Exception {
    TestProject testProject = new TestProject();
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "typedef Test<A extends String>();",
              ""));
      DartFunctionTypeAlias type = (DartFunctionTypeAlias) unit.getChildren()[0];
      DartTypeParameter[] typeParameters = type.getTypeParameters();
      // "A"
      DartTypeParameter typeParameter = typeParameters[0];
      assertEquals("A", typeParameter.getElementName());
      assertEquals(type, typeParameter.getParent());
      assertEquals("String", typeParameter.getBoundName());
      assertEquals(
          new SourceRangeImpl(unit.getSource().indexOf("A extends"), 1),
          typeParameter.getNameRange());
      // use getHandleIdentifier() and DartCore.create()
      {
        String id = typeParameter.getHandleIdentifier();
        DartTypeParameter fromId = (DartTypeParameter) DartCore.create(id);
        assertEquals(typeParameter, fromId);
        assertEquals("String", fromId.getBoundName());
      }
    } finally {
      testProject.dispose();
    }
  }

  public void test_inType() throws Exception {
    TestProject testProject = new TestProject();
    try {
      CompilationUnit unit = testProject.setUnitContent(
          "Test.dart",
          Joiner.on("\n").join(
              "// filler filler filler filler filler filler filler filler filler filler",
              "class Test<A, B extends String> {}",
              ""));
      Type type = unit.getType("Test");
      DartElement[] children = type.getChildren();
      // "A"
      DartTypeParameter typeParameterA = (DartTypeParameter) children[0];
      {
        assertEquals("A", typeParameterA.getElementName());
        assertEquals(type, typeParameterA.getParent());
        assertEquals(null, typeParameterA.getBoundName());
        assertEquals(
            new SourceRangeImpl(unit.getSource().indexOf("A,"), 1),
            typeParameterA.getNameRange());
        // use getHandleIdentifier() and DartCore.create()
        {
          String id = typeParameterA.getHandleIdentifier();
          DartTypeParameter fromId = (DartTypeParameter) DartCore.create(id);
          assertEquals(typeParameterA, fromId);
          assertEquals(null, fromId.getBoundName());
        }
      }
      // "B"
      DartTypeParameter typeParameterB = (DartTypeParameter) children[1];
      {
        assertEquals("B", typeParameterB.getElementName());
        assertEquals(type, typeParameterB.getParent());
        assertEquals("String", typeParameterB.getBoundName());
        assertEquals(
            new SourceRangeImpl(unit.getSource().indexOf("B extends"), 1),
            typeParameterB.getNameRange());
        // use getHandleIdentifier() and DartCore.create()
        {
          String id = typeParameterB.getHandleIdentifier();
          DartTypeParameter fromId = (DartTypeParameter) DartCore.create(id);
          assertEquals(typeParameterB, fromId);
          assertEquals("String", fromId.getBoundName());
        }
      }
      // "equals"
      assertTrue(typeParameterA.equals(typeParameterA));
      assertFalse(typeParameterA.equals(type));
      assertFalse(typeParameterA.equals(typeParameterB));
    } finally {
      testProject.dispose();
    }
  }
}
