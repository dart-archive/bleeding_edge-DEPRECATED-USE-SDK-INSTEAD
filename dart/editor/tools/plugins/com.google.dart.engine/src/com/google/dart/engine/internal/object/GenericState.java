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
package com.google.dart.engine.internal.object;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Instances of the class {@code GenericState} represent the state of an object representing a Dart
 * object for which there is no more specific state.
 */
public class GenericState extends InstanceState {
  /**
   * The values of the fields of this instance.
   */
  private HashMap<String, DartObjectImpl> fieldMap = new HashMap<String, DartObjectImpl>();

  /**
   * A state that can be used to represent an object whose state is not known.
   */
  public static final GenericState UNKNOWN_VALUE = new GenericState(
      new HashMap<String, DartObjectImpl>());

  /**
   * Initialize a newly created state to represent a newly created object.
   * 
   * @param fieldMap the values of the fields of this instance
   */
  public GenericState(HashMap<String, DartObjectImpl> fieldMap) {
    this.fieldMap = fieldMap;
  }

  @Override
  public StringState convertToString() {
    return StringState.UNKNOWN_VALUE;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof GenericState)) {
      return false;
    }
    GenericState state = (GenericState) object;
    HashSet<String> otherFields = new HashSet<String>(state.fieldMap.keySet());
    for (String fieldName : fieldMap.keySet()) {
      if (!fieldMap.get(fieldName).equals(state.fieldMap.get(fieldName))) {
        return false;
      }
      otherFields.remove(fieldName);
    }
    for (String fieldName : otherFields) {
      if (!state.fieldMap.get(fieldName).equals(fieldMap.get(fieldName))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    assertBoolNumStringOrNull(rightOperand);
    if (rightOperand instanceof DynamicState) {
      return BoolState.UNKNOWN_VALUE;
    }
    return BoolState.from(equals(rightOperand));
  }

  @Override
  public String getTypeName() {
    return "user defined type";
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    for (DartObjectImpl value : fieldMap.values()) {
      hashCode += value.hashCode();
    }
    return hashCode;
  }
}
