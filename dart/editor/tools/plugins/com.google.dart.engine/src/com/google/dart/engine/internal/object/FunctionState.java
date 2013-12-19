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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.utilities.general.ObjectUtilities;

/**
 * Instances of the class {@code FunctionState} represent the state of an object representing a
 * function.
 */
public class FunctionState extends InstanceState {
  /**
   * The element representing the function being modeled.
   */
  private ExecutableElement element;

  /**
   * Initialize a newly created state to represent the given function.
   * 
   * @param element the element representing the function being modeled
   */
  public FunctionState(ExecutableElement element) {
    this.element = element;
  }

  @Override
  public StringState convertToString() throws EvaluationException {
    if (element == null) {
      return StringState.UNKNOWN_VALUE;
    }
    return new StringState(element.getName());
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof FunctionState
        && ObjectUtilities.equals(element, ((FunctionState) object).element);
  }

  @Override
  public BoolState equalEqual(InstanceState rightOperand) throws EvaluationException {
    if (element == null) {
      return BoolState.UNKNOWN_VALUE;
    }
    if (rightOperand instanceof FunctionState) {
      ExecutableElement rightElement = ((FunctionState) rightOperand).element;
      if (rightElement == null) {
        return BoolState.UNKNOWN_VALUE;
      }
      return BoolState.from(element.equals(rightElement));
    } else if (rightOperand instanceof DynamicState) {
      return BoolState.UNKNOWN_VALUE;
    }
    return BoolState.FALSE_STATE;
  }

  @Override
  public String getTypeName() {
    return "Function"; //$NON-NLS-0$
  }

  @Override
  public int hashCode() {
    return element == null ? 0 : element.hashCode();
  }

  @Override
  public String toString() {
    return element == null ? "-unknown-" : element.getName();
  }
}
