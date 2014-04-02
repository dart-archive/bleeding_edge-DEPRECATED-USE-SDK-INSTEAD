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

package com.google.dart.tools.debug.ui.internal.objectinspector;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.util.IDartDebugVariable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * An IVariable implementation used specifically for the object inspector. It is used to display the
 * root node in the inspector's variable view.
 */
public class InspectorVariable extends DebugElement implements IDartDebugVariable, IVariable {
  private String typeName;
  private IValue value;

  public InspectorVariable(String typeName, IValue value) {
    super(value.getDebugTarget());

    this.typeName = typeName;
    this.value = value;
  }

  @Override
  public String getModelIdentifier() {
    return value.getModelIdentifier();
  }

  @Override
  public String getName() throws DebugException {
    if (typeName != null) {
      return "Instance of " + typeName;
    } else {
      return "";
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return typeName;
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
  public boolean isLocal() {
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
    throw new DebugException(new Status(
        IStatus.ERROR,
        DartDebugCorePlugin.PLUGIN_ID,
        DebugException.NOT_SUPPORTED,
        null,
        null));
  }

  @Override
  public void setValue(String expression) throws DebugException {
    throw new DebugException(new Status(
        IStatus.ERROR,
        DartDebugCorePlugin.PLUGIN_ID,
        DebugException.NOT_SUPPORTED,
        null,
        null));
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
