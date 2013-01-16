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

package com.google.dart.tools.debug.core.server;

import com.google.dart.tools.debug.core.util.IDartDebugVariable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of IDartDebugVariable, used specifically for displaying array splices.
 */
class ServerDebugVariableArraySlice extends ServerDebugElement implements IDartDebugVariable {
  class ListSliceValueRetriever implements IValueRetriever {
    public ListSliceValueRetriever() {

    }

    @Override
    public String getDisplayName() {
      return "";
    }

    @Override
    public List<IVariable> getVariables() {
      List<IVariable> entries = new ArrayList<IVariable>();

      for (int i = 0; i < length; i++) {
        entries.add(new ServerDebugVariable(getTarget(), VmVariable.createArrayEntry(
            getConnection(),
            parentArray,
            offset + i)));
      }

      return entries;
    }

    @Override
    public boolean hasVariables() {
      return true;
    }
  }

  private static final NumberFormat nf = NumberFormat.getIntegerInstance();

  private VmValue parentArray;
  private int offset;
  private int length;

  public ServerDebugVariableArraySlice(IDebugTarget target, VmValue parentArray, int offset,
      int length) {
    super(target);

    this.parentArray = parentArray;
    this.offset = offset;
    this.length = length;

    if (offset + length > parentArray.getLength()) {
      this.length = parentArray.getLength() - offset;
    }
  }

  @Override
  public String getName() throws DebugException {
    return "[" + nf.format(offset) + "-" + nf.format(offset + length - 1) + "]";
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return getValue().getReferenceTypeName();
  }

  @Override
  public IValue getValue() throws DebugException {
    return new ServerDebugValue(getTarget(), new ListSliceValueRetriever());
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    // TODO:
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
    // Not supported.

  }

  @Override
  public void setValue(String expression) throws DebugException {
    // Not supported.

  }

  @Override
  public boolean supportsValueModification() {
    // TODO:
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
