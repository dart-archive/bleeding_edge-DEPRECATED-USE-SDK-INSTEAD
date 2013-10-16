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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.expr.IExpressionEvaluator;
import com.google.dart.tools.debug.core.expr.WatchExpressionResult;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IDartDebugValue;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * An IValue implementation for VM debugging.
 */
public class ServerDebugValue extends ServerDebugElement implements IValue, IDartDebugValue,
    IExpressionEvaluator {

  static ServerDebugValue createValue(IDebugTarget target, VmValue value) {
    if (value != null && value.isList()) {
      return new ServerDebugIndexedValue(target, value);
    } else {
      return new ServerDebugValue(target, value);
    }
  }

  protected VmValue value;

  private IValueRetriever valueRetriever;

  protected List<IVariable> fields;

  protected ServerDebugValue(IDebugTarget target, IValueRetriever valueRetriever) {
    super(target);

    this.valueRetriever = valueRetriever;
  }

  protected ServerDebugValue(IDebugTarget target, VmValue value) {
    super(target);

    this.value = value;
  }

  protected ServerDebugValue(ServerDebugTarget target) {
    super(target);
  }

  @Override
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    if (valueRetriever != null) {
      // This is not a valid object to evaluate expressions against.
      listener.watchEvaluationFinished(WatchExpressionResult.error(expression, "undefined"));
      return;
    }

    try {
      getConnection().evaluateObject(
          value.getIsolate(),
          value,
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
                    new ServerDebugValue(getTarget(), result.getResult())));
              }
            }
          });
    } catch (IOException e) {
      DebugException exception = createDebugException(e);

      listener.watchEvaluationFinished(WatchExpressionResult.exception(expression, exception));
    }
  }

  public IValue getClassValue() {
    if (value.getVmObject() != null) {
      VmClass vmClass = getConnection().getClassInfoSync(value.getVmObject());

      if (vmClass == null) {
        return null;
      } else {
        return new ServerDebugValueClass(getTarget(), vmClass);
      }
    } else {
      return null;
    }
  }

  public String getDetailValue() {
    if (value == null) {
      return null;
    } else if (value.isString()) {
      try {
        return printNull(getValueString());
      } catch (DebugException ex) {
        DartDebugCorePlugin.logError(ex);
        return null;
      }
    } else {
      return printNull(value.getText());
    }
  }

  public String getDisplayString() throws DebugException {
    return getValueString();
  }

  @Override
  public String getId() {
    if (value == null || value.isNull()) {
      return null;
    }

    if (!value.isPrimitive() || value.isString()) {
      return Integer.toString(value.getObjectId());
    } else {
      return null;
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    try {
      if (value == null) {
        return null;
      }

      if (value.isObject() && value.isNull()) {
        return "null";
      }

      if (value.isObject()) {
        getVariables();

        if (value.getVmObject() != null) {
          return DebuggerUtils.demangleVmName(getConnection().getClassNameSync(value.getVmObject()));
        }
      }

      return DebuggerUtils.demangleVmName(value.getKind());
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public String getValueString() throws DebugException {
    try {
      if (value == null) {
        return getValueString_impl();
      } else if (value.isString()) {
        return DebuggerUtils.printString(getValueString_impl());
      } else if (value.isObject()) {
        try {
          return getReferenceTypeName();
        } catch (DebugException e) {

        }
      } else if (value.isList()) {
        return "List[" + value.getLength() + "]";
      }

      return getValueString_impl();
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    try {
      fillInFields();

      return fields.toArray(new IVariable[fields.size()]);
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  public VmClass getVmClass() {
    // TODO: implement

    return null;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    try {
      if (isListValue()) {
        return value.getLength() > 0;
      } else if (fields != null) {
        return fields.size() > 0;
      } else if (valueRetriever != null) {
        return valueRetriever.hasVariables();
      } else {
        return getVariables().length > 0;
      }
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public boolean isAllocated() throws DebugException {
    return true;
  }

  @Override
  public boolean isListValue() {
    return false;
  }

  @Override
  public boolean isNull() {
    return value != null && value.isNull();
  }

  @Override
  public boolean isPrimitive() {
    if (value == null) {
      return false;
    } else {
      return value.isPrimitive();
    }
  }

  protected synchronized void fillInFields() {
    if (fields != null) {
      return;
    }

    if (value == null) {
      fields = valueRetriever.getVariables();
    } else if (value.isObject()) {
      fillInFieldsSync();
    } else if (value.isList()) {
      fillInListFields();
    } else {
      fields = Collections.emptyList();
    }
  }

  protected void fillInFieldsSync() {
    final CountDownLatch latch = new CountDownLatch(1);

    final List<IVariable> tempFields = new ArrayList<IVariable>();

    try {
      getConnection().getObjectProperties(
          value.getIsolate(),
          value.getObjectId(),
          new VmCallback<VmObject>() {
            @Override
            public void handleResult(VmResult<VmObject> result) {
              try {
                if (!result.isError()) {
                  value.setVmObject(result.getResult());

                  tempFields.addAll(convert(result.getResult()));
                }

                latch.countDown();
              } catch (Throwable t) {
                latch.countDown();

                DartDebugCorePlugin.logError(t);
              }
            }
          });
    } catch (Exception e) {
      latch.countDown();
    }

    try {
      latch.await();

      fields = tempFields;
    } catch (InterruptedException e) {

    }
  }

  protected void fillInListFields() {
    fields = new ArrayList<IVariable>();

    ServerDebugTarget target = getTarget();
    VmConnection connection = getConnection();

    for (int i = 0; i < value.getLength(); i++) {
      ServerDebugVariable variable = new ServerDebugVariable(target, VmVariable.createArrayEntry(
          connection,
          value,
          i));

      fields.add(variable);
    }
  }

  protected boolean isValueRetriever() {
    return valueRetriever != null;
  }

  private List<IVariable> convert(VmObject vmObject) {
    List<IVariable> vars = new ArrayList<IVariable>();

    // Add instance fields.
    for (VmVariable vmVariable : vmObject.getFields()) {
      vars.add(new ServerDebugVariable(getTarget(), vmVariable));
    }

    return vars;
  }

  private String getValueString_impl() {
    if (valueRetriever != null) {
      return valueRetriever.getDisplayName();
    } else {
      return value.getText();
    }
  }

  private String printNull(String str) {
    if (str == null) {
      return "null";
    }

    return str;
  }

}
