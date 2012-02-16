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
import com.google.dart.tools.debug.core.webkit.WebkitDebugger.PausedReasonType;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The IThread implementation for the Dartium debug elements.
 */
public class DartiumDebugThread extends DartiumDebugElement implements IThread {
  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];

  private static final IStackFrame[] EMPTY_FRAMES = new IStackFrame[0];

  private int expectedSuspendReason = DebugEvent.UNSPECIFIED;

  private boolean suspended;
  private IStackFrame[] suspendedFrames = EMPTY_FRAMES;
  private IBreakpoint[] suspendedBreakpoints = EMPTY_BREAKPOINTS;

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
    return DartDebugCorePlugin.VM_SUPPORTS_STEPPING && isSuspended();
  }

  @Override
  public boolean canStepOver() {
    return DartDebugCorePlugin.VM_SUPPORTS_STEPPING && isSuspended();
  }

  @Override
  public boolean canStepReturn() {
    // aka stepOut
    return DartDebugCorePlugin.VM_SUPPORTS_STEPPING && isSuspended();
  }

  @Override
  public boolean canSuspend() {
    return DartDebugCorePlugin.VM_SUPPORTS_PAUSING && !isTerminated() && !isSuspended();
  }

  @Override
  public boolean canTerminate() {
    return getDebugTarget().canTerminate();
  }

  @Override
  public IBreakpoint[] getBreakpoints() {
    return suspendedBreakpoints;
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
    return expectedSuspendReason == DebugEvent.STEP_INTO
        || expectedSuspendReason == DebugEvent.STEP_OVER
        || expectedSuspendReason == DebugEvent.STEP_RETURN;
  }

  @Override
  public boolean isSuspended() {
    return suspended;
  }

  @Override
  public boolean isTerminated() {
    return getDebugTarget().isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    try {
      getConnection().getDebugger().resume();
    } catch (IOException exception) {
      // TODO(devoncarew): expected resume reason?
      //expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void stepInto() throws DebugException {
    expectedSuspendReason = DebugEvent.STEP_END;

    try {
      getConnection().getDebugger().stepInto();
    } catch (IOException exception) {
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void stepOver() throws DebugException {
    expectedSuspendReason = DebugEvent.STEP_END;

    try {
      getConnection().getDebugger().stepOver();
    } catch (IOException exception) {
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void stepReturn() throws DebugException {
    // aka stepOut
    expectedSuspendReason = DebugEvent.STEP_END;

    try {
      getConnection().getDebugger().stepOut();
    } catch (IOException exception) {
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  /**
   * Causes this element to suspend its execution, generating a <code>SUSPEND</code> event. Has no
   * effect on an already suspended element. Implementations may be blocking or non-blocking.
   * 
   * @exception DebugException on failure. Reasons include: <br>
   *              TARGET_REQUEST_FAILED - The request failed in the target <br>
   *              NOT_SUPPORTED - The capability is not supported by the target
   */
  @Override
  public void suspend() throws DebugException {
    expectedSuspendReason = DebugEvent.CLIENT_REQUEST;

    try {
      getConnection().getDebugger().pause();
    } catch (IOException exception) {
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void terminate() throws DebugException {
    getDebugTarget().terminate();
  }

  protected void handleDebuggerSuspended(PausedReasonType reason, List<WebkitCallFrame> webkitFrames) {
    int suspendReason = DebugEvent.BREAKPOINT;

    if (expectedSuspendReason != DebugEvent.UNSPECIFIED) {
      suspendReason = expectedSuspendReason;
    }

    suspended = true;

    // TODO(devoncarew): can we fill in the suspendedBreakpoints list?

    suspendedFrames = createFrames(webkitFrames);

    fireSuspendEvent(suspendReason);
  }

  void handleDebuggerResumed() {
    // clear data
    suspended = false;
    suspendedFrames = EMPTY_FRAMES;
    suspendedBreakpoints = EMPTY_BREAKPOINTS;

    // send event
    // TODO(devoncarew): step events
    fireResumeEvent(DebugEvent.UNSPECIFIED);
  }

  private DebugException createDebugException(IOException exception) {
    return new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
        exception.getMessage(), exception));
  }

  private IStackFrame[] createFrames(List<WebkitCallFrame> webkitFrames) {
    List<IStackFrame> frames = new ArrayList<IStackFrame>();

    for (WebkitCallFrame webkitFrame : webkitFrames) {
      DartiumDebugStackFrame frame = new DartiumDebugStackFrame(getTarget(), this, webkitFrame);

      frames.add(frame);
    }

    return frames.toArray(new IStackFrame[frames.size()]);
  }

  private boolean isDisconnected() {
    return getDebugTarget().isDisconnected();
  }

}
