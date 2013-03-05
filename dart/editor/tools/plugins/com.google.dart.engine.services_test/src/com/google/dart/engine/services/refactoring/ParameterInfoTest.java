/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.type.Type;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ParameterInfo}.
 */
public class ParameterInfoTest extends AbstractDartTest {
  private final Type type = mock(Type.class);
  private final VariableElement element = mock(VariableElement.class);

  public void test_new() throws Exception {
    ParameterInfo parameter = new ParameterInfo(element);
    assertEquals("test", parameter.getOldName());
    assertEquals("test", parameter.getNewName());
    assertEquals("int", parameter.getNewTypeName());
  }

  public void test_setNewName() throws Exception {
    ParameterInfo parameter = new ParameterInfo(element);
    // initial state
    assertEquals("test", parameter.getOldName());
    assertEquals("test", parameter.getNewName());
    assertFalse(parameter.isRenamed());
    // set new name
    parameter.setNewName("newName");
    assertEquals("test", parameter.getOldName());
    assertEquals("newName", parameter.getNewName());
    assertTrue(parameter.isRenamed());
  }

  public void test_setNewTypeName() throws Exception {
    ParameterInfo parameter = new ParameterInfo(element);
    // initial state
    assertEquals("int", parameter.getNewTypeName());
    // set new name
    parameter.setNewTypeName("num");
    assertEquals("num", parameter.getNewTypeName());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(type.toString()).thenReturn("int");
    when(element.getName()).thenReturn("test");
    when(element.getType()).thenReturn(type);
  }
}
