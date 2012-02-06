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

import com.google.dart.tools.core.NotYetImplementedException;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

/**
 * The IThread implementation for the Dartium debug elements.
 */
public class DartiumDebugThread extends DartiumDebugElement implements IThread {

  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];

  private static final IStackFrame[] EMPTY_FRAMES = new IStackFrame[0];

  private int expectedSuspendReason = DebugEvent.UNSPECIFIED;

  private IBreakpoint[] suspendBreakpoints = EMPTY_BREAKPOINTS;

  /**
   * @param target
   */
  public DartiumDebugThread(IDebugTarget target) {
    super(target);
  }

  @Override
  public boolean canResume() {
    return !isDisconnected() && isSuspended();
  }

  @Override
  public boolean canStepInto() {
    return isSuspended();
  }

  @Override
  public boolean canStepOver() {
    return isSuspended();
  }

  @Override
  public boolean canStepReturn() {
    // stepOut
    return isSuspended();
  }

  @Override
  public boolean canSuspend() {
    return !isTerminated() && !isSuspended();
  }

  @Override
  public boolean canTerminate() {
    return getDebugTarget().canTerminate();
  }

  @Override
  public IBreakpoint[] getBreakpoints() {
    throw new NotYetImplementedException();
  }

  @Override
  public String getName() throws DebugException {
    return "Call Stack";
  }

  @Override
  public int getPriority() throws DebugException {
    return 0;
  }

  @Override
  public IStackFrame[] getStackFrames() throws DebugException {
    throw new NotYetImplementedException();
  }

  @Override
  public IStackFrame getTopStackFrame() throws DebugException {
    throw new NotYetImplementedException();
  }

  @Override
  public boolean hasStackFrames() throws DebugException {
    return isSuspended();
  }

  @Override
  public boolean isStepping() {
    throw new NotYetImplementedException();
  }

  @Override
  public boolean isSuspended() {
    // TODO(devoncarew): implement

    return false;
  }

  @Override
  public boolean isTerminated() {
    return getDebugTarget().isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    throw new NotYetImplementedException();
  }

  @Override
  public void stepInto() throws DebugException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stepOver() throws DebugException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stepReturn() throws DebugException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void suspend() throws DebugException {
    throw new NotYetImplementedException();
  }

  @Override
  public void terminate() throws DebugException {
    getDebugTarget().terminate();
  }

  protected void suspended() {
    throw new NotYetImplementedException();

//    if (threadState.isSuspended()) {
//      throw new IllegalStateException("Already in suspended state");
//    }
//    threadState = new SuspendedThreadState();
//    int suspendedDetail = DebugEvent.UNSPECIFIED;
//    fireSuspendEvent(suspendedDetail);
  }

  private boolean isDisconnected() {
    return getDebugTarget().isDisconnected();
  }

}
