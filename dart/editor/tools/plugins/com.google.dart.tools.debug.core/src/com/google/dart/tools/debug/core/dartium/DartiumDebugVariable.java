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
package com.google.dart.tools.debug.core.dartium;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * The IVariable implementation of the Dartium Debug Element
 */
public class DartiumDebugVariable extends DartiumDebugElement implements IVariable {
  private String name;
  private String currentValue;
  private String previousValue;

  /**
   * Create a new Dartium Debug Variable
   * 
   * @param target
   */
  public DartiumDebugVariable(IDebugTarget target) {
    this(target, null);
  }

  /**
   * Create a new Dartium Debug Variable
   * 
   * @param target
   * @param name
   */
  public DartiumDebugVariable(IDebugTarget target, String name) {
    super(target);
    this.name = name;
  }

  @Override
  public String getName() throws DebugException {
    return name;
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return null;
  }

  @Override
  public IValue getValue() throws DebugException {
    return new DartiumDebugValue(getDebugTarget());
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    if (previousValue != null) {
      // TODO(keertip): check to see if value has changed
    }
    return false;
  }

  @Override
  public void setValue(IValue value) throws DebugException {
    setValue(value.getValueString());
  }

  @Override
  public void setValue(String expression) throws DebugException {
    previousValue = currentValue;
    currentValue = expression;
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
