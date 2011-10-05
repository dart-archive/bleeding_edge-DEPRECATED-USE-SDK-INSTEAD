/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.internal;

import com.google.dart.tools.core.utilities.compiler.DartSourceMapping;
import com.google.dart.tools.core.utilities.compiler.SourceLocation;

import org.chromium.sdk.CallFrame;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsVariable;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The IStackFrame implementation for the Chrome debug elements. This stack frame element can
 * represent either a Dart or a JavaScript frame.
 */
public class ChromeDebugStackFrame extends ChromeDebugElement implements IStackFrame {
  private ChromeDebugStackFrame parentFrame;
  private CallFrame jsCallframe;
  private IThread thread;
  private ChromeDebugVariable[] variables;
  private Map<String, String> previousVariableStates;

  /**
   * Create a new ChromeDebugStackFrame.
   * 
   * @param target
   * @param thread
   */
  public ChromeDebugStackFrame(IDebugTarget target, IThread thread,
      ChromeDebugStackFrame parentFrame, CallFrame jsCallframe) {
    super(target);

    this.thread = thread;
    this.jsCallframe = jsCallframe;
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

  @Override
  public int getCharEnd() throws DebugException {
    return -1;
  }

  @Override
  public int getCharStart() throws DebugException {
    return -1;
  }

  /**
   * Return the source location (file and line number) for the corresponding Dart file. The
   * SourceLocation is be not be {@see SourceLocation#isValid()} if no mapping could be found.
   * 
   * @return the source location (file and line number) for the Dart source
   */
  public SourceLocation getDartSourceLocation() {
    int lineNumber = getJavaScriptLineNumber();
    int column = getJavaScriptColumnIndex();

    ChromeDebugTarget debugTarget = (ChromeDebugTarget) getDebugTarget();

    String filePath = debugTarget.getScriptToFileMapping().mapFromScriptToFileName(
        jsCallframe.getScript());

    DartSourceMapping sourceMapper = debugTarget.getDartSourceMapper();

    return sourceMapper.mapJavaScriptToDart(filePath, lineNumber, column);
  }

  public String getFrameId() {
    if (parentFrame == null) {
      return jsCallframe.getFunctionName();
    } else {
      return parentFrame.getFrameId() + "." + jsCallframe.getFunctionName();
    }
  }

  /**
   * Return the source location (file and line number) for the corresponding JavaScript file.
   * 
   * @return the source location (file and line number) for the JavaScript source
   */
  public SourceLocation getJSSourceLocation() {
    int lineNumber = getJavaScriptLineNumber();

    ChromeDebugTarget debugTarget = (ChromeDebugTarget) getDebugTarget();

    String filePath = debugTarget.getScriptToFileMapping().mapFromScriptToFileName(
        jsCallframe.getScript());

    return new SourceLocation(new Path(filePath), lineNumber);
  }

  @Override
  public int getLineNumber() throws DebugException {
    int dartSourceLine = getDartSourceLocation().getLine();

    if (dartSourceLine != -1) {
      return dartSourceLine;
    } else {
      return getJavaScriptLineNumber();
    }
  }

  @Override
  public String getName() throws DebugException {
    String name = jsCallframe.getFunctionName();
    String demangledName = CompilerUtilities.demangleFunctionName(name);

    if (demangledName != null) {
      name = demangledName;
    }

    if (!name.endsWith(")")) {
      name += "()";
    }

    SourceLocation sourceLocation = getDartSourceLocation();

    if (sourceLocation.getLine() == -1) {
      // If we can't resolve the Dart source, use the Javascript location.
      sourceLocation = getJSSourceLocation();

      name += " [" + sourceLocation.getFileName() + ":" + sourceLocation.getLine() + "]";

      return name;
    } else {
      return name;
    }
  }

  @Override
  public IRegisterGroup[] getRegisterGroups() throws DebugException {
    return null;
  }

  @Override
  public IThread getThread() {
    return thread;
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    if (variables == null) {
      variables = getVariablesImpl();
    }

    return variables;
  }

  @Override
  public boolean hasRegisterGroups() throws DebugException {
    return false;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return true;
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
   * @return the column for the current javascript execution location
   */
  protected int getJavaScriptColumnIndex() {
    return jsCallframe.getStatementStartPosition().getColumn();
  }

  /**
   * @return the line for the current javascript execution location
   */
  protected int getJavaScriptLineNumber() {
    return jsCallframe.getStatementStartPosition().getLine();
  }

  Map<String, String> getPreviousVariableStates() {
    return previousVariableStates;
  }

  void setPreviousVariableStates(Map<String, String> frameVarStates) {
    previousVariableStates = frameVarStates;
  }

  void storeVariableStates(Map<String, String> frameVarStates) throws DebugException {
    if (variables != null) {
      for (ChromeDebugVariable variable : variables) {
        variable.storeValue(frameVarStates);
      }
    }
  }

  private ChromeDebugVariable[] getVariablesImpl() throws DebugException {
    List<? extends JsScope> variableScopes = jsCallframe.getVariableScopes();

    List<ChromeDebugVariable> variables = new ArrayList<ChromeDebugVariable>();

    JsVariable receiverVariable = jsCallframe.getReceiverVariable();

    if (receiverVariable != null) {
      variables.add(new ChromeDebugVariable(getDebugTarget(), receiverVariable));
    }

    for (JsScope varScope : variableScopes) {
      if (varScope.getType() != JsScope.Type.GLOBAL) {
        for (JsVariable jsVariable : varScope.getVariables()) {
          if (CompilerUtilities.isDartLocalVariable(jsVariable.getName())) {
            ChromeDebugVariable variable = new ChromeDebugVariable(getDebugTarget(), jsVariable);

            if (previousVariableStates != null) {
              variable.setPreviousValues(previousVariableStates);
            }

            variables.add(variable);
          }
        }
      }
    }

    return variables.toArray(new ChromeDebugVariable[variables.size()]);
  }

}
