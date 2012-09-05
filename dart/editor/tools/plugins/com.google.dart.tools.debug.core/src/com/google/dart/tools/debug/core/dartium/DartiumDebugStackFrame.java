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
import com.google.dart.tools.debug.core.dartium.DartiumDebugValue.ValueCallback;
import com.google.dart.tools.debug.core.expr.IExpressionEvaluator;
import com.google.dart.tools.debug.core.expr.WatchExpressionResult;
import com.google.dart.tools.debug.core.source.ISourceLookup;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IExceptionStackFrame;
import com.google.dart.tools.debug.core.util.IVariableResolver;
import com.google.dart.tools.debug.core.webkit.WebkitCallFrame;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitLocation;
import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;
import com.google.dart.tools.debug.core.webkit.WebkitResult;
import com.google.dart.tools.debug.core.webkit.WebkitScope;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The IStackFrame implementation for the dartium debug elements. This stack frame element
 * represents a Dart frame.
 */
public class DartiumDebugStackFrame extends DartiumDebugElement implements IStackFrame,
    ISourceLookup, IExceptionStackFrame, IVariableResolver, IExpressionEvaluator {
  private IThread thread;
  private WebkitCallFrame webkitFrame;
  private boolean isExceptionStackFrame;
  private VariableCollector variableCollector = VariableCollector.empty();

  public DartiumDebugStackFrame(IDebugTarget target, IThread thread, WebkitCallFrame webkitFrame) {
    this(target, thread, webkitFrame, null);
  }

  public DartiumDebugStackFrame(IDebugTarget target, IThread thread, WebkitCallFrame webkitFrame,
      WebkitRemoteObject exception) {
    super(target);

    this.thread = thread;
    this.webkitFrame = webkitFrame;

    fillInDartiumVariables(exception);
  }

  @Override
  public boolean canResume() {
    return getThread().canResume();
  }

  @Override
  public boolean canStepInto() {
    return !hasException() && getThread().canStepInto();
  }

  @Override
  public boolean canStepOver() {
    return !hasException() && getThread().canStepOver();
  }

  @Override
  public boolean canStepReturn() {
    return !hasException() && getThread().canStepReturn();
  }

  @Override
  public boolean canSuspend() {
    return getThread().canSuspend();
  }

  @Override
  public boolean canTerminate() {
    return getThread().canTerminate();
  }

  @Override
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    try {
      getConnection().getDebugger().evaluateOnCallFrame(
          webkitFrame.getCallFrameId(),
          expression,
          new WebkitCallback<WebkitRemoteObject>() {
            @Override
            public void handleResult(WebkitResult<WebkitRemoteObject> result) {
              if (result.isError()) {
                if (result.getError() instanceof WebkitRemoteObject) {
                  WebkitRemoteObject error = (WebkitRemoteObject) result.getError();

                  String desc;

                  if (error.isObject()) {
                    desc = error.getDescription();
                  } else if (error.isString()) {
                    desc = error.getValue();
                  } else {
                    desc = error.toString();
                  }

                  listener.watchEvaluationFinished(WatchExpressionResult.error(expression, desc));
                } else {
                  listener.watchEvaluationFinished(WatchExpressionResult.error(
                      expression,
                      result.getError().toString()));
                }
              } else {
                IValue value = new DartiumDebugValue(getTarget(), null, result.getResult());

                listener.watchEvaluationFinished(WatchExpressionResult.value(expression, value));
              }
            }
          });
    } catch (IOException e) {
      listener.watchEvaluationFinished(WatchExpressionResult.noOp(expression));
    }
  }

  @Override
  public IVariable findVariable(String varName) throws DebugException {
    IVariable[] variables = getVariables();

    for (int i = 0; i < variables.length; i++) {
      IVariable var = variables[i];

      if (var.getName().equals(varName)) {
        return var;
      }

    }

    return null;
  }

  @Override
  public int getCharEnd() throws DebugException {
    return -1;
  }

  @Override
  public int getCharStart() throws DebugException {
    return -1;
  }

  @Override
  public String getExceptionDisplayText() {
    DartiumDebugVariable variable;

    try {
      variable = (DartiumDebugVariable) variableCollector.getVariables()[0];
    } catch (InterruptedException ie) {
      return null;
    }

    final String[] result = new String[1];
    final CountDownLatch latch = new CountDownLatch(1);

    ((DartiumDebugValue) variable.getValue()).computeDetail(new ValueCallback() {
      @Override
      public void detailComputed(String stringValue) {
        result[0] = stringValue;

        latch.countDown();
      }
    });

    try {
      latch.await(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return "Exception: " + ((DartiumDebugValue) variable.getValue()).getDisplayString();
    }

    return "Exception: " + result[0];
  }

  @Override
  public int getLineNumber() throws DebugException {
    return WebkitLocation.webkitToElipseLine(webkitFrame.getLocation().getLineNumber());
  }

  @Override
  public String getLongName() {
    String file = getFileOrLibraryName();

    return getShortName() + (file == null ? "" : " - " + file);
  }

  @Override
  public String getName() throws DebugException {
    if (DebuggerUtils.areSiblingNamesUnique(this)) {
      return getShortName();
    } else {
      return getLongName();
    }
  }

  @Override
  public IRegisterGroup[] getRegisterGroups() throws DebugException {
    return new IRegisterGroup[0];
  }

  @Override
  public String getShortName() {
    return DebuggerUtils.demanglePrivateName(webkitFrame.getFunctionName()) + "()";
  }

  @Override
  public String getSourceLocationPath() {
    String scriptId = webkitFrame.getLocation().getScriptId();

    WebkitScript script = getConnection().getDebugger().getScript(scriptId);

    if (script != null) {
      if (script.isSystemScript()) {
        return script.getUrl();
      } else {
        URI uri = URI.create(script.getUrl());

        return uri.getPath();
      }
    }

    return null;
  }

  @Override
  public IThread getThread() {
    return thread;
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    try {
      return variableCollector.getVariables();
    } catch (InterruptedException e) {
      throw new DebugException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }

  @Override
  public boolean hasException() {
    return isExceptionStackFrame;
  }

  @Override
  public boolean hasRegisterGroups() throws DebugException {
    return false;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return getVariables().length > 0;
  }

  public boolean isPrivate() {
    return DebuggerUtils.isPrivateName(webkitFrame.getFunctionName());
  }

  public boolean isPrivateMethod() {
    return webkitFrame.isPrivateMethod();
  }

  @Override
  public boolean isStepping() {
    return getThread().isStepping();
  }

  @Override
  public boolean isSuspended() {
    return getThread().isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return getThread().isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    getThread().resume();
  }

  @Override
  public void stepInto() throws DebugException {
    getThread().stepInto();
  }

  @Override
  public void stepOver() throws DebugException {
    getThread().stepOver();
  }

  @Override
  public void stepReturn() throws DebugException {
    getThread().stepReturn();
  }

  @Override
  public void suspend() throws DebugException {
    getThread().suspend();
  }

  @Override
  public void terminate() throws DebugException {
    getThread().terminate();
  }

  /**
   * Fill in the IVariables from the Webkit variables.
   * 
   * @param exception can be null
   */
  private void fillInDartiumVariables(WebkitRemoteObject exception) {
    isExceptionStackFrame = (exception != null);

    List<WebkitRemoteObject> remoteObjects = new ArrayList<WebkitRemoteObject>();

    WebkitRemoteObject thisObject = null;

    if (!webkitFrame.isStaticMethod()) {
      thisObject = webkitFrame.getThisObject();
    }

    WebkitRemoteObject libraryObject = null;

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (!scope.isGlobal()) {
        remoteObjects.add(scope.getObject());
      } else {
        libraryObject = scope.getObject();
      }
    }

    variableCollector = VariableCollector.createCollector(
        getTarget(),
        thisObject,
        remoteObjects,
        libraryObject,
        exception);
  }

  private String getFileOrLibraryName() {
    String path = getSourceLocationPath();

    if (path != null) {
      int index = path.lastIndexOf('/');

      if (index != -1) {
        return path.substring(index + 1);
      } else {
        return path;
      }
    }

    return null;
  }

}
