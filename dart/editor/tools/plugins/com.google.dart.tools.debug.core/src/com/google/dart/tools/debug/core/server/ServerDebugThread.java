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

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import java.util.List;

/**
 * The IDebugTarget implementation for the VM debug elements.
 */
public class ServerDebugThread extends ServerDebugElement implements IThread {
  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];

  private static final ServerDebugStackFrame[] EMPTY_FRAMES = new ServerDebugStackFrame[0];

  private ServerDebugStackFrame[] suspendedFrames = EMPTY_FRAMES;

  /**
   * @param target
   */
  public ServerDebugThread(IDebugTarget target, List<VmCallFrame> frames) {
    super(target);

    suspendedFrames = createFrames(frames);
  }

  @Override
  public boolean canResume() {
    return getTarget().canResume();
  }

  @Override
  public boolean canStepInto() {
    // TODO(devoncarew):

    return false;
  }

  @Override
  public boolean canStepOver() {
    // TODO(devoncarew):

    return false;
  }

  @Override
  public boolean canStepReturn() {
    // TODO(devoncarew):

    return false;
  }

  @Override
  public boolean canSuspend() {
    return getTarget().canSuspend();
  }

  @Override
  public boolean canTerminate() {
    return getTarget().canTerminate();
  }

  @Override
  public IBreakpoint[] getBreakpoints() {
    return EMPTY_BREAKPOINTS;
  }

  @Override
  public String getName() throws DebugException {
    return "dart-0";
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
    // TODO(devoncarew):

    return false;
  }

  @Override
  public boolean isSuspended() {
    return getTarget().isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return getTarget().isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    getTarget().resume();
  }

  @Override
  public void stepInto() throws DebugException {
    // TODO(devoncarew):

  }

  @Override
  public void stepOver() throws DebugException {
    // TODO(devoncarew):

  }

  @Override
  public void stepReturn() throws DebugException {
    // TODO(devoncarew):

  }

  @Override
  public void suspend() throws DebugException {
    getTarget().suspend();
  }

  @Override
  public void terminate() throws DebugException {
    getTarget().terminate();
  }

  private ServerDebugStackFrame[] createFrames(List<VmCallFrame> frames) {
    ServerDebugStackFrame[] result = new ServerDebugStackFrame[frames.size()];

    for (int i = 0; i < result.length; i++) {
      result[i] = new ServerDebugStackFrame(getTarget(), this, frames.get(i));
    }

    return result;
  }

}
