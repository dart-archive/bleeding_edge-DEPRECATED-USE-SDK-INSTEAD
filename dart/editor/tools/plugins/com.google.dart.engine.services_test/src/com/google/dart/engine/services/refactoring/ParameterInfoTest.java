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

import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.services.internal.refactoring.ParameterInfoImpl;

/**
 * Test for {@link ParameterInfoImpl}.
 */
public class ParameterInfoTest extends AbstractDartTest {
  public void test_methodStubs() throws Exception {
    ParameterInfoImpl parameter = new ParameterInfoImpl("int", "test");
    assertSame(null, parameter.getDefaultValue());
    assertEquals(false, parameter.isAdded());
    assertEquals(false, parameter.isDeleted());
    //
    parameter.setDefaultValue("0");
  }

  public void test_new() throws Exception {
    ParameterInfoImpl parameter = new ParameterInfoImpl("int", "test");
    assertEquals("test", parameter.getOldName());
    assertEquals("test", parameter.getNewName());
    assertEquals("int", parameter.getNewTypeName());
  }

  public void test_setNewName() throws Exception {
    ParameterInfoImpl parameter = new ParameterInfoImpl("int", "test");
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
    ParameterInfoImpl parameter = new ParameterInfoImpl("int", "test");
    // initial state
    assertEquals("int", parameter.getNewTypeName());
    // set new name
    parameter.setNewTypeName("num");
    assertEquals("num", parameter.getNewTypeName());
  }
}
