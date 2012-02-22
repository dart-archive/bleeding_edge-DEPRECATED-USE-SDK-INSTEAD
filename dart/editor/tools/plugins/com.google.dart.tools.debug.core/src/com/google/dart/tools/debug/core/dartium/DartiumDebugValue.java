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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;
import com.google.dart.tools.debug.core.webkit.WebkitResult;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import java.io.IOException;
import java.util.Collections;

/**
 * The IValue implementation of Dartium Debug element
 */
public class DartiumDebugValue extends DartiumDebugElement implements IValue {
  public static interface ValueCallback {
    public void detailComputed(String stringValue);
  }

  private WebkitRemoteObject value;

  private VariableCollector variableCollector = VariableCollector.empty();

  public DartiumDebugValue(DartiumDebugTarget target, WebkitRemoteObject value) {
    super(target);

    this.value = value;
  }

  public void computeDetail(final ValueCallback callback) {
    try {
      getConnection().getRuntime().callToString(value.getObjectId(), new WebkitCallback<String>() {
        @Override
        public void handleResult(WebkitResult<String> result) {
          if (result.isError()) {
            callback.detailComputed(null);
          } else {
            callback.detailComputed(result.getResult());
          }
        }
      });
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);

      callback.detailComputed(null);
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return value.getClassName();
  }

  @Override
  public String getValueString() throws DebugException {
    if (value.isList()) {
      return value.getDescription();
    } else {
      return value.getValue();
    }
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    try {
      return variableCollector.getVariables();
    } catch (InterruptedException e) {
      throw new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          e.toString(), e));
    }
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return value.hasObjectId();
  }

  @Override
  public boolean isAllocated() throws DebugException {
    return true;
  }

  public boolean isList() {
    return value.isList();
  }

  public boolean isPrimitive() {
    return value.isPrimitive();
  }

  protected void populate() {
    if (value.hasObjectId()) {
      variableCollector = VariableCollector.createCollector(getTarget(),
          Collections.singletonList(value));
    }
  }

}
