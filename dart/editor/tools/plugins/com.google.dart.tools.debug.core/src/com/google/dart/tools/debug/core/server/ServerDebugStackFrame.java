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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.expr.IExpressionEvaluator;
import com.google.dart.tools.debug.core.expr.WatchExpressionResult;
import com.google.dart.tools.debug.core.util.DebuggerUtils;
import com.google.dart.tools.debug.core.util.IDartStackFrame;
import com.google.dart.tools.debug.core.util.IExceptionStackFrame;
import com.google.dart.tools.debug.core.util.IVariableResolver;

import org.eclipse.core.resources.IResource;
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The IStackFrame implementation for the VM debug elements. This stack frame element represents a
 * Dart frame.
 */
public class ServerDebugStackFrame extends ServerDebugElement implements IStackFrame,
    IDartStackFrame, IExceptionStackFrame, IVariableResolver, IExpressionEvaluator {
  private IThread thread;

  private VmCallFrame vmFrame;

  private boolean isExceptionStackFrame;

  private List<ServerDebugVariable> locals;

  private IValue classValue;

  private IVariable globalVariable;

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
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    try {
      getConnection().evaluateOnCallFrame(
          vmFrame.getIsolate(),
          vmFrame,
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

    // search in statics
    IValue classValue = getClassValue();

    if (classValue != null) {
      for (IVariable var : classValue.getVariables()) {
        if (var.getName().equals(varName)) {
          return var;
        }
      }
    }

    // search globals
    IVariable globalVar = getGlobalVariable();

    if (globalVar != null) {
      for (IVariable var : globalVar.getValue().getVariables()) {
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
    ServerDebugValue exceptionValue = (ServerDebugValue) locals.get(0).getValue();

    return "Exception: " + exceptionValue.getDisplayString();
  }

  public IValue getLibraryValue() {
    VmLibrary vmLibrary = getConnection().getLibraryPropertiesSync(
        vmFrame.getIsolate(),
        vmFrame.getLibraryId());

    if (vmLibrary == null) {
      return null;
    } else {
      return new ServerDebugValueLibrary(getTarget(), vmLibrary);
    }
  }

  @Override
  public int getLineNumber() throws DebugException {
    VmLocation location = vmFrame.getLocation();

    if (location != null) {
      return location.getLineNumber(getConnection());
    } else {
      return 0;
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
    return DebuggerUtils.demangleVmName(vmFrame.getFunctionName()) + "()";
  }

  @Override
  public String getSourceLocationPath() {
    VmLocation location = vmFrame.getLocation();

    if (location == null) {
      return null;
    }

    URI uri = URI.create(location.getUrl());

    // Resolve a package: reference.
    if (DartCore.isPackageSpec(location.getUrl())) {
      DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(
          getDebugTarget().getLaunch().getLaunchConfiguration());

      uri = resolvePackageUri(wrapper.getApplicationResource(), uri);
    }

    if (uri != null && "file".equals(uri.getScheme())) {
      return uri.getPath();
    } else {
      return "builtin:" + vmFrame.getLibraryId() + ":" + location.getUrl();
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

  /**
   * Public for testing.
   */
  public VmCallFrame getVmFrame() {
    return vmFrame;
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
  public boolean isUsingSourceMaps() {
    return false;
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

  protected void addException(VmValue exception) {
    isExceptionStackFrame = true;

    List<ServerDebugVariable> newLocals = new ArrayList<ServerDebugVariable>();

    VmVariable exceptionVariable = VmVariable.createFromException(exception);

    // Create a copy of the list, add the exception variable to the beginning.
    newLocals.add(new ServerDebugVariable(getTarget(), exceptionVariable));
    newLocals.addAll(locals);

    locals = newLocals;
  }

  protected IValue getClassValue() {
    if (classValue == null && vmFrame.hasClassId()) {
      VmClass vmClass = getConnection().getClassInfoSync(vmFrame.getIsolate(), vmFrame.getClassId());

      classValue = new ServerDebugValueClass(getTarget(), vmClass);
    }

    return classValue;
  }

  protected IVariable getGlobalVariable() throws DebugException {
    if (globalVariable == null) {
      globalVariable = ServerDebugVariable.createLibraryVariable(
          getTarget(),
          vmFrame.getIsolate(),
          vmFrame.getLibraryId());
    }

    return globalVariable;
  }

  protected IVariable getThisVariable() throws DebugException {
    for (IVariable var : getVariables()) {
      if (var instanceof ServerDebugVariable) {
        if (((ServerDebugVariable) var).isThisObject()) {
          return var;
        }
      }
    }

    return null;
  }

  private List<ServerDebugVariable> createFrom(VmCallFrame frame) {
    if (frame.getLocals() == null) {
      return Collections.emptyList();
    } else {
      List<ServerDebugVariable> variables = new ArrayList<ServerDebugVariable>();

//      // create a synthetic globals variable
//      variables.add(ServerDebugVariable.createLibraryVariable(
//          getTarget(),
//          frame.getIsolate(),
//          vmFrame.getLibraryId()));

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

  private URI resolvePackageUri(IResource resource, URI uri) {
    if (resource != null) {
      IFileInfo fileInfo = DartCore.getProjectManager().resolveUriToFileInfo(
          resource,
          uri.toString());

      if (fileInfo != null) {
        if (fileInfo.getResource() != null) {
          try {
            return new URI(
                "file",
                null,
                fileInfo.getResource().getFullPath().toPortableString(),
                null);
          } catch (URISyntaxException e) {

          }
        }

        return fileInfo.getFile().toURI();
      }
    }

    return null;
  }
}
