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
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * An IVariable implementation for VM debugging.
 */
public class ServerDebugVariable extends ServerDebugElement implements IVariable {
  private VmVariable vmVariable;

  private ServerDebugValue value;

  public ServerDebugVariable(IDebugTarget target, VmVariable vmVariable) {
    super(target);

    this.vmVariable = vmVariable;
    this.value = new ServerDebugValue(target, vmVariable.getValue());
  }

  public String getDisplayName() {
    // TODO(devoncarew): handle array elements

    // The names of private fields are mangled by the VM.
    // _foo@652376 ==> _foo
    String name = getName();

    if (name.indexOf('@') != -1) {
      name = name.substring(0, name.indexOf('@'));
    }

    return name;
  }

  @Override
  public String getName() {
    return vmVariable.getName();
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return getValue().getReferenceTypeName();
  }

  @Override
  public IValue getValue() {
    return value;
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    // TODO(devoncarew):

    return false;
  }

  public boolean isListValue() {
    return value != null && value.isListValue();
  }

  public boolean isThisObject() {
    return "this".equals(getName());
  }

  public boolean isThrownException() {
    return vmVariable.getIsException();
  }

  @Override
  public void setValue(IValue value) throws DebugException {
    // Not supported.

  }

  @Override
  public void setValue(String expression) throws DebugException {
    // Not supported.

  }

  @Override
  public boolean supportsValueModification() {
    return false;
  }

  @Override
  public boolean verifyValue(IValue value) throws DebugException {
    // Not supported.

    return false;
  }

  @Override
  public boolean verifyValue(String expression) throws DebugException {
    // Not supported.

    return false;
  }

}
