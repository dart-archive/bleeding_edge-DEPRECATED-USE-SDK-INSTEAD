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
import com.google.dart.tools.debug.core.source.WorkspaceSourceContainer;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IDartStackFrame;
import com.google.dart.tools.debug.core.util.IExceptionStackFrame;
import com.google.dart.tools.debug.core.util.IVariableResolver;
import com.google.dart.tools.debug.core.webkit.WebkitCallFrame;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitLocation;
import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;
import com.google.dart.tools.debug.core.webkit.WebkitResult;
import com.google.dart.tools.debug.core.webkit.WebkitScope;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The IStackFrame implementation for the dartium debug elements. This stack frame element
 * represents a Dart frame.
 */
public class DartiumDebugStackFrame extends DartiumDebugElement implements IStackFrame,
    IDartStackFrame, IExceptionStackFrame, IVariableResolver, IExpressionEvaluator {
  private IThread thread;
  private WebkitCallFrame webkitFrame;

  private boolean isExceptionStackFrame;

  private VariableCollector variableCollector = VariableCollector.empty();

  private IValue classValue;
  private IValue globalScopeValue;

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
                IValue value = DartiumDebugValue.create(getTarget(), null, result.getResult());

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
    // search in locals
    for (IVariable var : getVariables()) {
      if (var.getName().equals(varName)) {
        return var;
      }
    }

    // search in instance variables
    IVariable thisVar = getThisVariable();

    if (thisVar != null) {
      IValue thisValue = thisVar.getValue();

      for (IVariable var : thisValue.getVariables()) {
        if (var.getName().equals(varName)) {
          return var;
        }
      }
    }

    // search statics
    if (getClassValue() != null) {
      for (IVariable var : getClassValue().getVariables()) {
        if (var.getName().equals(varName)) {
          return var;
        }
      }
    }

    // search globals
    if (getGlobalsScope() != null) {
      for (IVariable var : getGlobalsScope().getVariables()) {
        if (var.getName().equals(varName)) {
          return var;
        }
      }
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapterClass) {
    if (adapterClass == IThread.class) {
      return getThread();
    } else {
      return super.getAdapter(adapterClass);
    }
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
  public String getExceptionDisplayText() throws DebugException {
    DartiumDebugVariable variable = (DartiumDebugVariable) getVariables()[0];
    DartiumDebugValue exceptionValue = (DartiumDebugValue) variable.getValue();

    final String[] result = new String[1];
    final CountDownLatch latch = new CountDownLatch(1);

    exceptionValue.computeDetail(new ValueCallback() {
      @Override
      public void detailComputed(String stringValue) {
        result[0] = stringValue;

        latch.countDown();
      }
    });

    try {
      latch.await(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return "Exception: " + exceptionValue.getDisplayString();
    }

    return "Exception: " + result[0];
  }

  @Override
  public int getLineNumber() throws DebugException {
    try {
      if (getTarget().shouldUseSourceMapping() && isUsingSourceMaps()) {
        SourceMapManager.SourceLocation location = getMappedLocation();

        return WebkitLocation.webkitToElipseLine(location.line);
      } else {
        return WebkitLocation.webkitToElipseLine(webkitFrame.getLocation().getLineNumber());
      }
    } catch (Throwable t) {
      DartDebugCorePlugin.logError(t);

      return 1;
    }
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
    return DebuggerUtils.demangleVmName(webkitFrame.getFunctionName()) + "()";
  }

  @Override
  public String getSourceLocationPath_NEW(String executionContectId,
      Map<String, String> executionUrlToFileCache) {
    // TODO(scheglov) implement for the Analysis Server
    return getSourceLocationPath_OLD();
  }

  @Override
  public String getSourceLocationPath_OLD() {
    try {
      if (getTarget().shouldUseSourceMapping() && isUsingSourceMaps()) {
        return getMappedLocationPath();
      } else {
        return getActualLocationPath();
      }
    } catch (Throwable t) {
      DartDebugCorePlugin.logError(t);

      return null;
    }
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

  @Override
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
  public boolean isUsingSourceMaps() {
    return getMappedLocation() != null;
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

  @Override
  public String toString() {
    return getShortName();
  }

  protected String getActualLocationPath() {
    final String packageFragment = "/packages/";

    String scriptId = webkitFrame.getLocation().getScriptId();

    WebkitScript script = getConnection().getDebugger().getScript(scriptId);

    if (script != null) {
      String url = script.getUrl();

      if (script.isSystemScript() || script.isDataUrl() || script.isChromeExtensionUrl()) {
        return url;
      }

      // Check for http://foo/bar/web/packages/baz.dart, convert it into a package: url.
      if (url.contains(packageFragment)) {
        url = "package:" + url.substring(url.indexOf(packageFragment) + packageFragment.length());
      }

      if (url.startsWith("package:")) {
        return url;
      }

      IResource resource = getTarget().getResourceResolver().resolveUrl(url);

      if (resource != null) {
        return resource.getFullPath().toString();
      }

      try {
        return URI.create(url).getPath();
      } catch (IllegalArgumentException iae) {
        // Dartium can send us bad paths:
        // e:\b\build\slave\dartium-win-full\build ... rt\dart\CanvasRenderingContext2DImpl.dart
        DartDebugCorePlugin.logInfo("Illegal path from Dartium: " + url);
      }
    }

    return null;
  }

  protected IValue getClassValue() throws DebugException {
    if (classValue != null) {
      return classValue;
    }

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (scope.isClass()) {
        classValue = DartiumDebugValue.create(getTarget(), null, scope.getObject());
        break;
      }
    }

    return classValue;
  }

  protected IValue getGlobalsScope() throws DebugException {
    if (globalScopeValue != null) {
      return globalScopeValue;
    }

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (scope.isLibraries()) {
        globalScopeValue = DartiumDebugValue.create(getTarget(), null, scope.getObject());
        break;
      }
    }

    return globalScopeValue;
  }

  protected String getMappedLocationPath() {
    SourceMapManager.SourceLocation targetLocation = getMappedLocation();

    if (DartDebugCorePlugin.LOGGING) {
      WebkitLocation sourceLocation = webkitFrame.getLocation();
      WebkitScript script = getConnection().getDebugger().getScript(sourceLocation.getScriptId());
      String scriptPath = script == null ? "null" : script.getUrl();

      System.out.println("[" + scriptPath + "," + sourceLocation.getLineNumber() + ","
          + sourceLocation.getColumnNumber() + "] ==> mapped to " + targetLocation);
    }

    return targetLocation.file.getLocation().toPortableString();
  }

  protected IVariable getThisVariable() throws DebugException {
    for (IVariable var : getVariables()) {
      if (var instanceof DartiumDebugVariable) {
        if (((DartiumDebugVariable) var).isThisObject()) {
          return var;
        }
      }
    }

    return null;
  }

  protected WebkitCallFrame getWebkitFrame() {
    return webkitFrame;
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

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (!scope.isGlobalLike() && !scope.isInstance()) {
        remoteObjects.add(scope.getObject());
      }
    }

    variableCollector = VariableCollector.createCollector(
        getTarget(),
        thisObject,
        remoteObjects,
        null,
        exception);
  }

  private String getFileOrLibraryName() {
    String path = getSourceLocationPath_OLD();

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

  private SourceMapManager.SourceLocation getMappedLocation() {
    SourceMapManager sourceMapManager = getTarget().getSourceMapManager();

    if (sourceMapManager == null) {
      return null;
    }

    IFile file = WorkspaceSourceContainer.locatePathAsFile(getActualLocationPath());

    if (sourceMapManager.isMapSource(file)) {
      WebkitLocation location = webkitFrame.getLocation();

      return sourceMapManager.getMappingFor(
          file,
          location.getLineNumber(),
          location.getColumnNumber());
    } else {
      return null;
    }
  }
}
