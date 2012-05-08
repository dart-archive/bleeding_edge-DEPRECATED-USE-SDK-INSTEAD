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
import com.google.dart.tools.debug.core.webkit.WebkitCallFrame;
import com.google.dart.tools.debug.core.webkit.WebkitLocation;
import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;
import com.google.dart.tools.debug.core.webkit.WebkitScope;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The IStackFrame implementation for the dartium debug elements. This stack frame element
 * represents a Dart frame.
 */
public class DartiumDebugStackFrame extends DartiumDebugElement implements IStackFrame {
  private IThread thread;
  private WebkitCallFrame webkitFrame;
  private VariableCollector variableCollector = VariableCollector.empty();

  public DartiumDebugStackFrame(IDebugTarget target, IThread thread, WebkitCallFrame webkitFrame) {
    super(target);

    this.thread = thread;
    this.webkitFrame = webkitFrame;

    fillInDartiumVariables();
  }

  @Override
  public boolean canResume() {
    return getThread().canResume();
  }

  @Override
  public boolean canStepInto() {
    return getThread().canStepInto();
  }

  @Override
  public boolean canStepOver() {
    return getThread().canStepOver();
  }

  @Override
  public boolean canStepReturn() {
    return getThread().canStepReturn();
  }

  @Override
  public boolean canSuspend() {
    return getThread().canSuspend();
  }

  @Override
  public boolean canTerminate() {
    return getThread().canTerminate();
  }

  public DartiumDebugVariable findVariable(String varName) throws DebugException {

    IVariable[] variables = getVariables();
    for (int i = 0; i < variables.length; i++) {
      DartiumDebugVariable var = (DartiumDebugVariable) variables[i];
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
  public int getLineNumber() throws DebugException {
    return WebkitLocation.webkitToElipseLine(webkitFrame.getLocation().getLineNumber());
  }

  @Override
  public String getName() throws DebugException {
    return webkitFrame.getFunctionName() + "()";
  }

  @Override
  public IRegisterGroup[] getRegisterGroups() throws DebugException {
    return new IRegisterGroup[0];
  }

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
  public boolean hasRegisterGroups() throws DebugException {
    return false;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return getVariables().length > 0;
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

  private void fillInDartiumVariables() {
    List<WebkitRemoteObject> remoteObjects = new ArrayList<WebkitRemoteObject>();

    WebkitRemoteObject thisObject = null;

    if (!webkitFrame.isStaticMethod()) {
      thisObject = webkitFrame.getThisObject();
    }

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (!scope.isGlobal()) {
        remoteObjects.add(scope.getObject());
      }
    }

    variableCollector = VariableCollector.createCollector(getTarget(), thisObject, remoteObjects);
  }

}
