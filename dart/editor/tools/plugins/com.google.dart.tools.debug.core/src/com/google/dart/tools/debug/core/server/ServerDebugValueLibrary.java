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

import com.google.dart.tools.debug.core.expr.WatchExpressionResult;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A ServerDebugValue subclass specifically for exposing VmLibrary instances.
 */
public class ServerDebugValueLibrary extends ServerDebugValue {
  private VmLibrary vmLibrary;

  public ServerDebugValueLibrary(ServerDebugTarget target, VmLibrary vmLibrary) {
    super(target);

    this.vmLibrary = vmLibrary;
  }

  @Override
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    try {
      getConnection().evaluateLibrary(
          vmLibrary.getIsolate(),
          vmLibrary,
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
    return vmLibrary.getUrl();
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return vmLibrary.getUrl();
  }

  @Override
  public String getValueString() throws DebugException {
    return vmLibrary.getUrl();
  }

  @Override
  public VmClass getVmClass() {
    return null;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return vmLibrary.getGlobals().size() > 0;
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

    for (VmVariable variable : vmLibrary.getGlobals()) {
      fields.add(new ServerDebugVariable(getTarget(), variable));
    }
  }
}
