/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.internal;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.JsVariable.SetValueCallback;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import java.util.Map;

/**
 * The IVariable implementation for the Chrome debug elements.
 */
public class ChromeDebugVariable extends ChromeDebugElement implements IVariable {
  private ChromeDebugValue value;
  private JsVariable jsVariable;
  private String name;
  private String previousValue;

  /**
   * Create a new ChromeDebugVariable.
   * 
   * @param target
   * @param jsVariable
   */
  public ChromeDebugVariable(IDebugTarget target, JsVariable jsVariable) {
    this(target, jsVariable, null);
  }

  /**
   * Create a new ChromeDebugVariable.
   * 
   * @param target
   * @param jsVariable
   * @param name
   */
  public ChromeDebugVariable(IDebugTarget target, JsVariable jsVariable, String name) {
    super(target);

    this.jsVariable = jsVariable;
    this.name = name;
  }

  @Override
  public String getName() throws DebugException {
    if (name == null) {
      return jsVariable.getName();
    } else {
      return name;
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    JsValue value = jsVariable.getValue();

    return value == null ? "" : value.getType().toString();
  }

  @Override
  public IValue getValue() throws DebugException {
    if (value == null) {
      value = new ChromeDebugValue(getDebugTarget(), jsVariable.getValue());
    }

    return value;
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    if (previousValue != null) {
      ChromeDebugValue value = (ChromeDebugValue) getValue();

      if (!value.isObject()) {
        String currentValue = value.getValueString();

        if (currentValue != null) {
          return !currentValue.equals(previousValue);
        }
      }
    }

    return false;
  }

  @Override
  public void setValue(IValue value) throws DebugException {
    setValue(value.getValueString());
  }

  /**
   * Attempts to set the value of this variable to the value of the given expression.
   * 
   * @param expression an expression to generate a new value
   * @exception DebugException on failure. Reasons include:
   *              <ul>
   *              <li>TARGET_REQUEST_FAILED - The request failed in the target
   *              <li>NOT_SUPPORTED - The capability is not supported by the target
   *              </ul>
   */
  @Override
  public void setValue(final String valueStr) throws DebugException {
    // TODO(devoncarew): the library we're using (and V8 itself) does not support value
    // modification. Tabled for now.

    try {
      jsVariable.setValue(valueStr, new SetValueCallback() {
        @Override
        public void failure(String errorMessage) {
          // TODO(devoncarew): V8 does not currently support value modification
        }

        @Override
        public void success() {
          // TODO(devoncarew): V8 does not currently support value modification
        }
      });
    } catch (UnsupportedOperationException ex) {
      DartDebugCorePlugin.logError(ex);
    }
  }

  @Override
  public boolean supportsValueModification() {
    return jsVariable.isMutable();
  }

  @Override
  public boolean verifyValue(IValue value) throws DebugException {
    return verifyValue(value.getValueString());
  }

  @Override
  public boolean verifyValue(String valueStr) throws DebugException {
    return true;
  }

  void setPreviousValues(Map<String, String> previousVariableStates) throws DebugException {
    if (previousVariableStates.get(getName()) != null) {
      previousValue = previousVariableStates.get(getName());
    }
  }

  void storeValue(Map<String, String> frameVarStates) throws DebugException {
    ChromeDebugValue value = (ChromeDebugValue) getValue();

    if (!value.isObject()) {
      frameVarStates.put(getName(), value.getValueString());
    }
  }

}
