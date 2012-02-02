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

  /**
   * The thread state implementation for when the VM is running.
   */
  private class RunningThreadState implements ThreadState {

    RunningThreadState() {
    }

    @Override
    public boolean canStep() {
      return false;
    }

    @Override
    public boolean canSuspend() {
      return true;
    }

    @Override
    public void dismiss() {
    }

    @Override
    public IBreakpoint[] getBreakpoints() {
      return EMPTY_BREAKPOINTS;
    }

    @Override
    public IStackFrame[] getStackFrames() {
      return EMPTY_FRAMES;
    }

    @Override
    public boolean isStepping() {
      return false;
    }

    @Override
    public boolean isSuspended() {
      return false;
    }

    @Override
    public void resume() {
      // Ignore.
    }

//    @Override
//    public void step(StepAction stepAction, int resumeReason) {
//      // Ignore.
//    }

    @Override
    public void suspend() {

    }
  }

  /**
   * The thread state implementation for when the VM is suspended.
   */
  private class SuspendedThreadState implements ThreadState {

    private DartiumDebugStackFrame[] stackFrames;

    public SuspendedThreadState() {
    }

    @Override
    public boolean canStep() {
      return true;
    }

    @Override
    public boolean canSuspend() {
      return false;
    }

    @Override
    public void dismiss() {

    }

    @Override
    public IBreakpoint[] getBreakpoints() {
      return null;
    }

    @Override
    public IStackFrame[] getStackFrames() {
      return null;
    }

    @Override
    public boolean isStepping() {
      return false;
    }

    @Override
    public boolean isSuspended() {
      return true;
    }

    @Override
    public void resume() {

    }

//    @Override
//    public void step(StepAction stepAction, int resumeReason) {
//
//    }

    @Override
    public void suspend() {
      // do nothing

    }

  }

  private static interface ThreadState {
    boolean canStep();

    boolean canSuspend();

    void dismiss();

    IBreakpoint[] getBreakpoints();

    IStackFrame[] getStackFrames();

    boolean isStepping();

    boolean isSuspended();

    void resume();

//    void step(StepAction stepAction, int resumeReason);

    void suspend();
  }

  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];

  private static final IStackFrame[] EMPTY_FRAMES = new IStackFrame[0];

  private int expectedSuspendReason = DebugEvent.UNSPECIFIED;

  private ThreadState threadState = new RunningThreadState();

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
    return threadState.canStep();
  }

  @Override
  public boolean canStepOver() {
    return threadState.canStep();
  }

  @Override
  public boolean canStepReturn() {
    return threadState.canStep();
  }

  @Override
  public boolean canSuspend() {
    return !isDisconnected() && threadState.canSuspend();
  }

  @Override
  public boolean canTerminate() {
    return getDebugTarget().canTerminate();
  }

  @Override
  public IBreakpoint[] getBreakpoints() {
    return threadState.getBreakpoints();
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
    return threadState.getStackFrames();
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
    return threadState.isStepping();
  }

  @Override
  public boolean isSuspended() {
    return !isDisconnected() && threadState.isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return getDebugTarget().isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    threadState.resume();
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
    threadState.suspend();
  }

  @Override
  public void terminate() throws DebugException {
    getDebugTarget().terminate();
  }

  protected void suspended() {
    if (threadState.isSuspended()) {
      throw new IllegalStateException("Already in suspended state");
    }
    threadState = new SuspendedThreadState();
    int suspendedDetail = DebugEvent.UNSPECIFIED;
    fireSuspendEvent(suspendedDetail);
  }

  private boolean isDisconnected() {
    return getDebugTarget().isDisconnected();
  }

}
