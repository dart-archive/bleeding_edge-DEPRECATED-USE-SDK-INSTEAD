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

package com.google.dart.tools.debug.core.logical;

import com.google.dart.tools.debug.core.util.IDartDebugVariable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * A logical debug variable - used to display implementation formats in more user friendly
 * structures.
 */
class LogicalDebugVariable extends DebugElement implements IVariable, IDartDebugVariable {
  private final String name;
  private final IValue value;

  public LogicalDebugVariable(String name, IValue value) {
    super(value.getDebugTarget());

    this.name = name;
    this.value = value;
  }

  @Override
  public String getModelIdentifier() {
    return value.getModelIdentifier();
  }

  @Override
  public String getName() throws DebugException {
    return name;
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return value.getReferenceTypeName();
  }

  @Override
  public IValue getValue() throws DebugException {
    return value;
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    return false;
  }

  @Override
  public boolean isLibraryObject() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isThisObject() {
    return false;
  }

  @Override
  public boolean isThrownException() {
    return false;
  }

  @Override
  public void setValue(IValue value) throws DebugException {

  }

  @Override
  public void setValue(String expression) throws DebugException {

  }

  @Override
  public boolean supportsValueModification() {
    return false;
  }

  @Override
  public boolean verifyValue(IValue value) throws DebugException {
    return false;
  }

  @Override
  public boolean verifyValue(String expression) throws DebugException {
    return false;
  }

}
