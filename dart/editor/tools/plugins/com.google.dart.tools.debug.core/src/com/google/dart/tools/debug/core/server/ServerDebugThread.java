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
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.server.VmListener.PausedReason;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;
import java.util.List;

/**
 * The IDebugTarget implementation for the VM debug elements.
 */
public class ServerDebugThread extends ServerDebugElement implements IThread {
  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];

  private static final ServerDebugStackFrame[] EMPTY_FRAMES = new ServerDebugStackFrame[0];

  private ServerDebugStackFrame[] suspendedFrames = EMPTY_FRAMES;
  private IBreakpoint[] suspendedBreakpoints = EMPTY_BREAKPOINTS;

  private boolean suspended;
  private int expectedSuspendReason = DebugEvent.UNSPECIFIED;
  private int expectedResumeReason = DebugEvent.UNSPECIFIED;

  private VmIsolate vmIsolate;

  /**
   * @param target
   */
  public ServerDebugThread(IDebugTarget target) {
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
    return isSuspended();
  }

  @Override
  public boolean canSuspend() {
    return DartDebugCorePlugin.VM_SUPPORTS_PAUSING && !isTerminated() && !isSuspended();
  }

  @Override
  public boolean canTerminate() {
    return getTarget().canTerminate();
  }

  @Override
  public IBreakpoint[] getBreakpoints() {
    return suspendedBreakpoints;
  }

  @Override
  public String getName() throws DebugException {
    String name = vmIsolate == null ? "isolate-0" : vmIsolate.getName();

    return name + (isSuspended() ? " [suspended]" : "");
  }

  @Override
  public int getPriority() throws DebugException {
    return 0;
  }

  @Override
  public IStackFrame[] getStackFrames() throws DebugException {
    return suspendedFrames;
  }

  @Override
  public IStackFrame getTopStackFrame() throws DebugException {
    IStackFrame[] frames = getStackFrames();

    return frames.length > 0 ? frames[0] : null;
  }

  @Override
  public boolean hasStackFrames() throws DebugException {
    return isSuspended();
  }

  @Override
  public boolean isStepping() {
    return expectedResumeReason == DebugEvent.STEP_INTO
        || expectedResumeReason == DebugEvent.STEP_OVER
        || expectedResumeReason == DebugEvent.STEP_RETURN
        || expectedSuspendReason == DebugEvent.STEP_END;
  }

  @Override
  public boolean isSuspended() {
    return suspended;
  }

  @Override
  public boolean isTerminated() {
    return getTarget().isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    try {
      expectedResumeReason = DebugEvent.UNSPECIFIED;

      getConnection().resume();
    } catch (IOException exception) {
      throw createDebugException(exception);
    }
  }

  @Override
  public void stepInto() throws DebugException {
    expectedResumeReason = DebugEvent.STEP_END;
    expectedSuspendReason = DebugEvent.STEP_INTO;

    try {
      getConnection().stepInto();
    } catch (IOException exception) {
      expectedResumeReason = DebugEvent.UNSPECIFIED;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void stepOver() throws DebugException {
    expectedResumeReason = DebugEvent.STEP_END;
    expectedSuspendReason = DebugEvent.STEP_OVER;

    try {
      getConnection().stepOver();
    } catch (IOException exception) {
      expectedResumeReason = DebugEvent.UNSPECIFIED;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void stepReturn() throws DebugException {
    expectedResumeReason = DebugEvent.STEP_END;
    expectedSuspendReason = DebugEvent.STEP_RETURN;

    try {
      getConnection().stepOut();
    } catch (IOException exception) {
      expectedResumeReason = DebugEvent.UNSPECIFIED;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void suspend() throws DebugException {
    try {
      getConnection().interrupt(getIsolateId());
    } catch (IOException ioe) {
      throw createDebugException(ioe);
    }
  }

  @Override
  public void terminate() throws DebugException {
    getTarget().terminate();
  }

  protected void handleDebuggerPaused(PausedReason dbgReason, VmIsolate isolate,
      List<VmCallFrame> frames, VmValue exception) {
    // TODO(devoncarew): we won't change the isolate value when we have real isolate support
    this.vmIsolate = isolate;

    int reason = DebugEvent.BREAKPOINT;

    if (expectedSuspendReason != DebugEvent.UNSPECIFIED) {
      reason = expectedSuspendReason;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;
    } else {
      DartBreakpoint breakpoint = getTarget().getBreakpointFor(frames);

      if (breakpoint != null) {
        suspendedBreakpoints = new IBreakpoint[] {breakpoint};
        reason = DebugEvent.BREAKPOINT;
      }
    }

    suspended = true;

    suspendedFrames = createFrames(frames, exception);

    fireSuspendEvent(reason);
  }

  void handleDebuggerResumed() {
    // clear data
    suspended = false;
    suspendedFrames = EMPTY_FRAMES;
    suspendedBreakpoints = EMPTY_BREAKPOINTS;

    // send event
    int reason = expectedResumeReason;
    expectedResumeReason = DebugEvent.UNSPECIFIED;

    fireResumeEvent(reason);
  }

  private DebugException createDebugException(IOException exception) {
    return new DebugException(new Status(
        IStatus.ERROR,
        DartDebugCorePlugin.PLUGIN_ID,
        exception.getMessage(),
        exception));
  }

  private ServerDebugStackFrame[] createFrames(List<VmCallFrame> frames, VmValue exception) {
    ServerDebugStackFrame[] result = new ServerDebugStackFrame[frames.size()];

    for (int i = 0; i < result.length; i++) {
      result[i] = new ServerDebugStackFrame(getTarget(), this, frames.get(i));

      if (i == 0 && exception != null) {
        result[i].addException(exception);
      }
    }

    return result;
  }

  private int getIsolateId() {
    if (vmIsolate != null) {
      return vmIsolate.getId();
    }

    return -1;
  }

  private boolean isDisconnected() {
    return getDebugTarget().isDisconnected();
  }

}
