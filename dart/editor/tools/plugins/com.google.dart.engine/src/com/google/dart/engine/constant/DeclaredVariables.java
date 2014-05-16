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

import com.google.dart.engine.internal.object.BoolState;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.IntState;
import com.google.dart.engine.internal.object.StringState;
import com.google.dart.engine.internal.resolver.TypeProvider;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * Instances of the class {@code DeclaredVariables} provide access to the values of variables that
 * have been defined on the command line using the {@code -D} option.
 */
public class DeclaredVariables {
  /**
   * A table mapping the names of declared variables to their values.
   */
  private HashMap<String, String> declaredVariables = new HashMap<String, String>();

  /**
   * Initialize a newly created set of declared variables to be empty.
   */
  public DeclaredVariables() {
    super();
  }

  /**
   * Define a variable with the given name to have the given value.
   * 
   * @param variableName the name of the variable being defined
   * @param value the value of the variable
   */
  public void define(String variableName, String value) {
    declaredVariables.put(variableName, value);
  }

  /**
   * Return the value of the variable with the given name interpreted as a boolean value, or
   * {@code null} if the variable is not defined.
   * 
   * @param typeProvider the type provider used to find the type 'bool'
   * @param variableName the name of the variable whose value is to be returned
   */
  public DartObject getBool(TypeProvider typeProvider, String variableName) {
    String value = declaredVariables.get(variableName);
    if (value == null) {
      return null;
    }
    if (value.equals("true")) {
      return new DartObjectImpl(typeProvider.getBoolType(), BoolState.from(true));
    } else if (value.equals("false")) {
      return new DartObjectImpl(typeProvider.getBoolType(), BoolState.from(false));
    }
    return null;
  }

  /**
   * Return the value of the variable with the given name interpreted as an integer value, or
   * {@code null} if the variable is not defined.
   * 
   * @param typeProvider the type provider used to find the type 'int'
   * @param variableName the name of the variable whose value is to be returned
   * @throws NumberFormatException if the value of the variable is not a valid integer value
   */
  public DartObject getInt(TypeProvider typeProvider, String variableName) {
    String value = declaredVariables.get(variableName);
    if (value == null) {
      return null;
    }
    return new DartObjectImpl(typeProvider.getIntType(), new IntState(new BigInteger(value)));
  }

  /**
   * Return the value of the variable with the given name interpreted as a String value, or
   * {@code null} if the variable is not defined.
   * 
   * @param typeProvider the type provider used to find the type 'String'
   * @param variableName the name of the variable whose value is to be returned
   */
  public DartObject getString(TypeProvider typeProvider, String variableName) {
    String value = declaredVariables.get(variableName);
    if (value == null) {
      return null;
    }
    return new DartObjectImpl(typeProvider.getStringType(), new StringState(value));
  }
}
