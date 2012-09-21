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

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * A logical debug value - used to display implementation formats in more user friendly structures.
 */
class LogicalDebugValue extends DebugElement implements IValue {
  private final IValue proxyValue;
  private final IVariable[] variables;

  public LogicalDebugValue(IValue proxyValue, IVariable[] variables) {
    super(proxyValue.getDebugTarget());

    this.proxyValue = proxyValue;
    this.variables = variables;
  }

  @Override
  public String getModelIdentifier() {
    return proxyValue.getModelIdentifier();
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return proxyValue.getReferenceTypeName();
  }

  @Override
  public String getValueString() throws DebugException {
    return proxyValue.getValueString();
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    return variables;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return getVariables().length > 0;
  }

  @Override
  public boolean isAllocated() throws DebugException {
    return proxyValue.isAllocated();
  }

}
