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

package com.google.dart.tools.debug.core.server;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * This subclass of ServerDebugValue is used specifically for array types. The Eclipse debugging
 * framework will display arrays in groups of 100 elements if it can identify which IValues are
 * arrays.
 */
public class ServerDebugIndexedValue extends ServerDebugValue implements IIndexedValue {

  ServerDebugIndexedValue(IDebugTarget target, VmValue value) {
    super(target, value);
  }

  @Override
  public int getInitialOffset() {
    return 0;
  }

  @Override
  public int getSize() throws DebugException {
    return value.getLength();
  }

  @Override
  public IVariable getVariable(int offset) throws DebugException {
    return new ServerDebugVariable(getTarget(), VmVariable.createArrayEntry(
        getConnection(),
        value,
        offset));
  }

  @Override
  public IVariable[] getVariables(int offset, int length) throws DebugException {
    IVariable[] results = new IVariable[length];

    for (int i = 0; i < length; i++) {
      results[i] = getVariable(offset + i);
    }

    return results;
  }

  @Override
  public boolean isListValue() {
    return true;
  }

}
