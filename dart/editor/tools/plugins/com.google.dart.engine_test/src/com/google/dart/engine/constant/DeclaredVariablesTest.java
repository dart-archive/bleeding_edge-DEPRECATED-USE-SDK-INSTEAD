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
package com.google.dart.engine.constant;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;

public class DeclaredVariablesTest extends EngineTestCase {
  public void test_getBool_false() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();
    variables.define(variableName, "false");

    DartObject object = variables.getBool(typeProvider, variableName);
    assertNotNull(object);
    assertEquals(Boolean.FALSE, object.getBoolValue());
  }

  public void test_getBool_invalid() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();
    variables.define(variableName, "not true");

    assertNullDartObject(typeProvider, variables.getBool(typeProvider, variableName));
  }

  public void test_getBool_true() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();
    variables.define(variableName, "true");

    DartObject object = variables.getBool(typeProvider, variableName);
    assertNotNull(object);
    assertEquals(Boolean.TRUE, object.getBoolValue());
  }

  public void test_getBool_undefined() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();

    assertUnknownDartObject(variables.getBool(typeProvider, variableName));
  }

  public void test_getInt_invalid() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();
    variables.define(variableName, "four score and seven years");

    assertNullDartObject(typeProvider, variables.getInt(typeProvider, variableName));
  }

  public void test_getInt_undefined() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();

    assertUnknownDartObject(variables.getInt(typeProvider, variableName));
  }

  public void test_getInt_valid() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();
    variables.define(variableName, "23");

    DartObject object = variables.getInt(typeProvider, variableName);
    assertNotNull(object);
    assertEquals(23, object.getIntValue().intValue());
  }

  public void test_getString_defined() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    String value = "value";
    DeclaredVariables variables = new DeclaredVariables();
    variables.define(variableName, value);

    DartObject object = variables.getString(typeProvider, variableName);
    assertNotNull(object);
    assertEquals(value, object.getStringValue());
  }

  public void test_getString_undefined() {
    TestTypeProvider typeProvider = new TestTypeProvider();
    String variableName = "var";
    DeclaredVariables variables = new DeclaredVariables();

    assertUnknownDartObject(variables.getString(typeProvider, variableName));
  }

  private void assertNullDartObject(TestTypeProvider typeProvider, DartObject result) {
    assertEquals(typeProvider.getNullType(), result.getType());
  }

  private void assertUnknownDartObject(DartObject result) {
    assertTrue(((DartObjectImpl) result).isUnknown());
  }
}
