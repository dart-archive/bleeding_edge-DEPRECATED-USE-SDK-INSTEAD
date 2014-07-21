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

import com.google.dart.tools.debug.core.expr.WatchExpressionResult;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A ServerDebugValue subclass specifically for exposing VmClass instances.
 */
public class ServerDebugValueClass extends ServerDebugValue {
  private VmClass vmClass;

  public ServerDebugValueClass(ServerDebugTarget target, VmClass vmClass) {
    super(target);

    this.vmClass = vmClass;
  }

  @Override
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    try {
      getConnection().evaluateObject(
          vmClass.getIsolate(),
          vmClass,
          expression,
          new VmCallback<VmValue>() {
            @Override
            public void handleResult(VmResult<VmValue> result) {
              if (result.isError()) {
                listener.watchEvaluationFinished(WatchExpressionResult.error(
                    expression,
                    result.getError()));
              } else {
                listener.watchEvaluationFinished(WatchExpressionResult.value(
                    expression,
                    ServerDebugValue.createValue(getTarget(), result.getResult())));
              }
            }
          });
    } catch (IOException e) {
      DebugException exception = createDebugException(e);

      listener.watchEvaluationFinished(WatchExpressionResult.exception(expression, exception));
    }
  }

  @Override
  public IValue getClassValue() {
    return null;
  }

  @Override
  public String getDetailValue() {
    return vmClass.getName();
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return vmClass.getName();
  }

  @Override
  public String getValueString() throws DebugException {
    return vmClass.getName();
  }

  @Override
  public VmClass getVmClass() {
    return null;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return vmClass.getFields().size() > 0;
  }

  @Override
  public boolean isListValue() {
    return false;
  }

  @Override
  protected synchronized void fillInFields() {
    if (fields != null) {
      return;
    }

    fields = new ArrayList<IVariable>();

    for (VmVariable variable : vmClass.getFields()) {
      fields.add(new ServerDebugVariable(getTarget(), variable));
    }
  }
}
