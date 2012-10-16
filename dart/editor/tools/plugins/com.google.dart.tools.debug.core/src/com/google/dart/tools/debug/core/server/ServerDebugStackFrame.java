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

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.expr.IExpressionEvaluator;
import com.google.dart.tools.debug.core.expr.WatchExpressionResult;
import com.google.dart.tools.debug.core.source.ISourceLookup;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IExceptionStackFrame;
import com.google.dart.tools.debug.core.util.IVariableResolver;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The IStackFrame implementation for the VM debug elements. This stack frame element represents a
 * Dart frame.
 */
public class ServerDebugStackFrame extends ServerDebugElement implements IStackFrame,
    ISourceLookup, IExceptionStackFrame, IVariableResolver, IExpressionEvaluator {
  private IThread thread;
  private VmCallFrame vmFrame;
  private boolean isExceptionStackFrame;
  private List<ServerDebugVariable> locals;

  public ServerDebugStackFrame(IDebugTarget target, IThread thread, VmCallFrame vmFrame) {
    super(target);

    this.thread = thread;
    this.vmFrame = vmFrame;
    this.locals = createFrom(vmFrame);
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
  public void evaluateExpression(String expression, IWatchExpressionListener listener) {
    // TODO(devoncarew): implement this when the command-line debugger supports expression evaluation

    listener.watchEvaluationFinished(WatchExpressionResult.noOp(expression));
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
    return "Exception: " + ((ServerDebugValue) locals.get(0).getValue()).getDisplayString();
  }

  @Override
  public int getLineNumber() throws DebugException {
    return vmFrame.getLocation().getLineNumber();
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
    return DebuggerUtils.demanglePrivateName(vmFrame.getFunctionName()) + "()";
  }

  @Override
  public String getSourceLocationPath() {
    URI uri = URI.create(vmFrame.getLocation().getUrl());

    // Resolve a package: reference.
    if (PackageLibraryManager.isPackageUri(uri)) {
      DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(
          getDebugTarget().getLaunch().getLaunchConfiguration());
      IResource resource = wrapper.getApplicationResource();
      if (resource != null) {
        uri = PackageLibraryManagerProvider.getPackageLibraryManager(
            resource.getLocation().toFile()).resolvePackageUri(vmFrame.getLocation().getUrl());
      } else {
        uri = PackageLibraryManagerProvider.getPackageLibraryManager().resolvePackageUri(
            vmFrame.getLocation().getUrl());
      }
    }

    if ("file".equals(uri.getScheme())) {
      return uri.getPath();
    } else if (PackageLibraryManager.isDartUri(uri)) {
      return uri.toString();
    } else {
      return "builtin:" + vmFrame.getLibraryId() + ":" + vmFrame.getLocation().getUrl();
    }
  }

  @Override
  public IThread getThread() {
    return thread;
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    return locals.toArray(new IVariable[locals.size()]);
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
    return DebuggerUtils.isPrivateName(vmFrame.getFunctionName());
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

  protected void addException(VmValue exception) {
    isExceptionStackFrame = true;

    List<ServerDebugVariable> newLocals = new ArrayList<ServerDebugVariable>();

    VmVariable exceptionVariable = VmVariable.createFromException(exception);

    // Create a copy of the list, add the exception variable to the beginning.
    newLocals.add(new ServerDebugVariable(getTarget(), exceptionVariable));
    newLocals.addAll(locals);

    locals = newLocals;
  }

  private List<ServerDebugVariable> createFrom(VmCallFrame frame) {
    if (frame.getLocals() == null) {
      return Collections.emptyList();
    } else {
      List<ServerDebugVariable> variables = new ArrayList<ServerDebugVariable>();

      // create a synthetic globals variable
      variables.add(ServerDebugVariable.createLibraryVariable(
          getTarget(),
          frame.getIsolate(),
          vmFrame.getLibraryId()));

      for (VmVariable var : frame.getLocals()) {
        ServerDebugVariable serverVariable = new ServerDebugVariable(getTarget(), var);

        variables.add(serverVariable);
      }

      return variables;
    }
  }

  private String getFileOrLibraryName() {
    VmLocation location = vmFrame.getLocation();

    if (location != null) {
      String url = location.getUrl();

      int index = url.lastIndexOf('/');

      if (index != -1) {
        return url.substring(index + 1);
      } else {
        return url;
      }
    }

    return null;
  }

}
