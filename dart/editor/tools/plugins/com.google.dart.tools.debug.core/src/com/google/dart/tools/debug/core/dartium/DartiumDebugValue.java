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

import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * The IValue implementation of Dartium Debug element
 */
public class DartiumDebugValue extends DartiumDebugElement implements IValue {
  private static final IVariable[] EMPTY = new IVariable[0];

  private WebkitRemoteObject value;

  public DartiumDebugValue(DartiumDebugTarget target, WebkitRemoteObject value) {
    super(target);

    this.value = value;
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return value.getClassName();
  }

  @Override
  public String getValueString() throws DebugException {
    return String.valueOf(value.getValue());
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    return EMPTY;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return getVariables().length > 0;
  }

  @Override
  public boolean isAllocated() throws DebugException {
    return true;
  }

}
