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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.util.IDartDebugValue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * This ILogicalStructureTypeDelegate handles displaying Dart types, like maps, in a more logical
 * view.
 */
public class MapStructureType implements ILogicalStructureTypeDelegate {
  // TODO(devoncarew): add a unit test to ensure that "_HashMapImpl" is available.
  // That way we'll fast fail if the name changes or the class is removed.
  private static final String MAP_IMPL_NAME = "_HashMapImpl";

  public MapStructureType() {

  }

  @Override
  public IValue getLogicalStructure(IValue value) throws CoreException {
    try {
      return createHashMap(value);
    } catch (CoreException ce) {
      DartDebugCorePlugin.logError(ce);

      return value;
    }
  }

  @Override
  public boolean providesLogicalStructure(IValue value) {
    if (!(value instanceof IDartDebugValue)) {
      return false;
    }

    try {
      return MAP_IMPL_NAME.equals(value.getReferenceTypeName());
    } catch (DebugException e) {
      return false;
    }
  }

  private IValue createHashMap(IValue value) throws DebugException {
    IValue _keys = getNamedField(value, "_keys");
    IValue _values = getNamedField(value, "_values");

    IVariable[] _key_values = _keys.getVariables();
    IVariable[] _value_values = _values.getVariables();

    List<IVariable> vars = new ArrayList<IVariable>();

    for (int i = 0; i < _key_values.length; i++) {
      if (!isNull(_key_values[i])) {
        IValue keyValue = _key_values[i].getValue();
        String keyName = keyValue.getValueString();

        vars.add(new LogicalDebugVariable(keyName, _value_values[i].getValue()));
      }
    }

    return new LogicalDebugValue(value, vars.toArray(new IVariable[vars.size()]));
  }

  private IValue getNamedField(IValue value, String name) throws DebugException {
    IVariable[] childVars = value.getVariables();

    for (IVariable child : childVars) {
      if (name.equals(child.getName())) {
        return child.getValue();
      }
    }

    throw new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID, "field '"
        + name + "' does not exist"));
  }

  private boolean isNull(IVariable var) throws DebugException {
    IValue val = var.getValue();

    if (val instanceof IDartDebugValue) {
      IDartDebugValue value = (IDartDebugValue) val;

      return value.isNull();
    }

    return false;
  }

}
