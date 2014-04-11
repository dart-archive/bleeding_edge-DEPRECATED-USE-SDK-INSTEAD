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
import java.util.Map;

/**
 * The unique instance of the class {@code ListState} represents the state of an object representing
 * a map.
 */
public class MapState extends InstanceState {
  /**
   * The entries in the map.
   */
  private HashMap<DartObjectImpl, DartObjectImpl> entries;

  /**
   * Initialize a newly created state to represent a map with the given entries.
   * 
   * @param entries the entries in the map
   */
  public MapState(HashMap<DartObjectImpl, DartObjectImpl> entries) {
    this.entries = entries;
  }

  @Override
  public StringState convertToString() {
    return StringState.UNKNOWN_VALUE;
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
  public boolean equals(Object object) {
    if (!(object instanceof MapState)) {
      return false;
    }
    HashMap<DartObjectImpl, DartObjectImpl> otherElements = ((MapState) object).entries;
    int count = entries.size();
    if (otherElements.size() != count) {
      return false;
    } else if (count == 0) {
      return true;
    }
    for (Map.Entry<DartObjectImpl, DartObjectImpl> entry : entries.entrySet()) {
      DartObjectImpl key = entry.getKey();
      DartObjectImpl value = entry.getValue();
      DartObjectImpl otherValue = otherElements.get(key);
      if (!value.equals(otherValue)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getTypeName() {
    return "Map"; //$NON-NLS-0$
  }

  @Override
  public Map<Object, Object> getValue() {
    HashMap<Object, Object> result = new HashMap<Object, Object>();
    for (Map.Entry<DartObjectImpl, DartObjectImpl> entry : entries.entrySet()) {
      DartObjectImpl key = entry.getKey();
      DartObjectImpl value = entry.getValue();
      if (!key.hasExactValue() || !value.hasExactValue()) {
        return null;
      }
      result.put(key.getValue(), value.getValue());
    }
    return result;
  }

  @Override
  public boolean hasExactValue() {
    for (Map.Entry<DartObjectImpl, DartObjectImpl> entry : entries.entrySet()) {
      if (!entry.getKey().hasExactValue() || !entry.getValue().hasExactValue()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int value = 0;
    for (DartObjectImpl key : entries.keySet()) {
      value = (value << 3) ^ key.hashCode();
    }
    return value;
  }
}
