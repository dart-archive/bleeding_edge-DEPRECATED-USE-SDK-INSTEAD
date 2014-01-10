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

/**
 * The unique instance of the class {@code ListState} represents the state of an object representing
 * a list.
 */
public class ListState extends InstanceState {
  /**
   * The elements of the list.
   */
  private DartObjectImpl[] elements;

  /**
   * Initialize a newly created state to represent a list with the given elements.
   * 
   * @param elements the elements of the list
   */
  public ListState(DartObjectImpl[] elements) {
    this.elements = elements;
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
    if (!(object instanceof ListState)) {
      return false;
    }
    DartObjectImpl[] otherElements = ((ListState) object).elements;
    int count = elements.length;
    if (otherElements.length != count) {
      return false;
    } else if (count == 0) {
      return true;
    }
    for (int i = 0; i < count; i++) {
      if (!elements[i].equals(otherElements[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getTypeName() {
    return "List"; //$NON-NLS-0$
  }

  @Override
  public Object[] getValue() {
    int count = elements.length;
    Object[] result = new Object[count];
    for (int i = 0; i < count; i++) {
      DartObjectImpl element = elements[i];
      if (!element.hasExactValue()) {
        return null;
      }
      result[i] = element.getValue();
    }
    return result;
  }

  @Override
  public boolean hasExactValue() {
    int count = elements.length;
    for (int i = 0; i < count; i++) {
      if (!elements[i].hasExactValue()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int value = 0;
    int count = elements.length;
    for (int i = 0; i < count; i++) {
      value = (value << 3) ^ elements[i].hashCode();
    }
    return value;
  }
}
