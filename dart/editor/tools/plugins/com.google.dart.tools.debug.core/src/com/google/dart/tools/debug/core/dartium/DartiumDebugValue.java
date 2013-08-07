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
import com.google.dart.tools.debug.core.expr.IExpressionEvaluator;
import com.google.dart.tools.debug.core.expr.WatchExpressionResult;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IDartDebugValue;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;
import com.google.dart.tools.debug.core.webkit.WebkitResult;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

import java.io.IOException;
import java.util.Collections;

/**
 * The IValue implementation of Dartium Debug element
 */
public class DartiumDebugValue extends DartiumDebugElement implements IValue, IDartDebugValue,
    IExpressionEvaluator {

  public static interface ValueCallback {
    public void detailComputed(String stringValue);
  }

  private static final int MAX_DISPLAYABLE_LIST_LENGTH = 2000;

  private DartiumDebugVariable variable;
  private WebkitRemoteObject value;

  private VariableCollector variableCollector;

  public DartiumDebugValue(DartiumDebugTarget target, DartiumDebugVariable variable,
      WebkitRemoteObject value) {
    super(target);

    this.variable = variable;
    this.value = value;

    // Change to a mode where we only request values that we're going to use.
//    if (!value.isList()) {
//      populate();
//    }
  }

  public void computeDetail(final ValueCallback callback) {
    // If the value is a primitive type, just return the display string.
    if (value.isPrimitive() || (variable != null && variable.isLibraryObject())) {
      callback.detailComputed(getDisplayString());

      return;
    }

    if (value.isNull()) {
      callback.detailComputed(getDisplayString());
      return;
    }

    // Otherwise try and call the toString() method of the object.
    try {
      getConnection().getRuntime().callToString(value.getObjectId(), new WebkitCallback<String>() {
        @Override
        public void handleResult(WebkitResult<String> result) {
          if (result.isError()) {
            callback.detailComputed(result.getErrorMessage());
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
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    String exprText = expression;

    // If they're not trying to access 'this' as an array references, use a period after 'this'.
    if (!exprText.startsWith("[")) {
      exprText = "." + exprText;
    }

    try {
      getConnection().getRuntime().callFunctionOn(
          value.getObjectId(),
          "function(){return this" + exprText + ";}",
          null,
          false,
          new WebkitCallback<WebkitRemoteObject>() {
            @Override
            public void handleResult(WebkitResult<WebkitRemoteObject> result) {
              if (result.isError()) {
                listener.watchEvaluationFinished(WatchExpressionResult.error(
                    expression,
                    result.getErrorMessage()));
              } else if (result.getResult().isUndefined()) {
                listener.watchEvaluationFinished(WatchExpressionResult.error(
                    expression,
                    "undefined"));
              } else if (result.getResult().isSyntaxError()) {
                evalOnGlobalContext(expression, result, listener);
              } else {
                listener.watchEvaluationFinished(WatchExpressionResult.value(
                    expression,
                    new DartiumDebugValue(getTarget(), null, result.getResult())));
              }
            }
          });
    } catch (IOException e) {
      listener.watchEvaluationFinished(WatchExpressionResult.exception(
          expression,
          new DebugException(new Status(
              IStatus.ERROR,
              DartDebugCorePlugin.PLUGIN_ID,
              e.toString(),
              e))));
    }
  }

  /**
   * @return a user-consumable string for the value object
   * @throws DebugException
   */
  public String getDisplayString() {
    if (variable != null && variable.isLibraryObject()) {
      return "";
    }

    if (value.isNull()) {
      return "null";
    }

    if (value.isString()) {
      return DebuggerUtils.printString(value.getValue());
    }

    if (isPrimitive()) {
      return value.getValue();
    }

    if (isListValue()) {
      if (value.getClassName() != null) {
        return value.getClassName() + "[" + value.getListLength() + "]";
      } else {
        return "List[" + value.getListLength() + "]";
      }
    }

    try {
      return getReferenceTypeName();
    } catch (DebugException e) {
      return value.getClassName();
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return value.getClassName();

    // TODO(devocarew): Pavel populated the className field - remove all references to the
    // synthetic @classInfo field

//    try {
//      // The special property @classInfo contains the class name. We need to populate the
//      // variable information before we attempt to retrieve the class name.
//      getVariables();
//
//      return DebuggerUtils.demangleVmName(getConnection().getDebugger().getClassNameSync(value));
//    } catch (Throwable t) {
//      throw createDebugException(t);
//    }
  }

  @Override
  public String getValueString() throws DebugException {
    try {
      return getDisplayString();
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    try {
      if (variableCollector == null) {
        populate();
      }

      return variableCollector.getVariables();
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public boolean hasVariables() throws DebugException {
    try {
      if (isListValue()) {
        return value.getListLength() > 0;
      } else {
        return value.hasObjectId();
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
    return value.isList();
  }

  @Override
  public boolean isNull() {
    return value.isNull();
  }

  @Override
  public boolean isPrimitive() {
    return value.isPrimitive();
  }

  private void evalOnGlobalContext(final String expression,
      final WebkitResult<WebkitRemoteObject> originalResult, final IWatchExpressionListener listener) {
    try {
      getConnection().getRuntime().evaluate(
          expression,
          null,
          false,
          new WebkitCallback<WebkitRemoteObject>() {
            @Override
            public void handleResult(WebkitResult<WebkitRemoteObject> result) {
              if (result.isError()) {
                listener.watchEvaluationFinished(WatchExpressionResult.error(
                    expression,
                    originalResult.getErrorMessage()));
              } else if (result.getResult().isSyntaxError()) {
                listener.watchEvaluationFinished(WatchExpressionResult.value(
                    expression,
                    new DartiumDebugValue(getTarget(), null, originalResult.getResult())));
              } else {
                listener.watchEvaluationFinished(WatchExpressionResult.value(
                    expression,
                    new DartiumDebugValue(getTarget(), null, result.getResult())));
              }
            }
          });
    } catch (IOException e) {
      listener.watchEvaluationFinished(WatchExpressionResult.exception(
          expression,
          new DebugException(new Status(
              IStatus.ERROR,
              DartDebugCorePlugin.PLUGIN_ID,
              e.toString(),
              e))));
    }
  }

  private void populate() {
    if (value.hasObjectId()) {
      // TODO(devoncarew): this prevents crashes in the editor and Dartium, but a better
      // solution is needed. That solution will likely involve use of the Runtime.callFunctionOn
      // method. See https://code.google.com/p/dart/issues/detail?id=7794.
      if (value.isList() && value.getListLength() > MAX_DISPLAYABLE_LIST_LENGTH) {
        variableCollector = VariableCollector.empty();
//      if (value.isList()) {
//        variableCollector = VariableCollector.fixed(
//            getTarget(),
//            ListSlicer.createValues(getTarget(), value));
      } else {
        variableCollector = VariableCollector.createCollector(
            getTarget(),
            variable,
            Collections.singletonList(value));
      }
    } else {
      variableCollector = VariableCollector.empty();
    }
  }

}
