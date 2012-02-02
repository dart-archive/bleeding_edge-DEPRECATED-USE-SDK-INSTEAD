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
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

/**
 * The IDebugTarget implementation for the Dartium debug elements.
 */
public class DartiumDebugTarget extends DartiumDebugElement implements IDebugTarget {

  private String debugTargetName;
  private ILaunch launch;
  private IProcess process;
  private DartiumDebugThread debugThread;

  /**
   * @param target
   */
  public DartiumDebugTarget(String debugTargetName, ILaunch launch) {
    super(null);
    this.debugTargetName = debugTargetName;
    this.launch = launch;
    debugThread = new DartiumDebugThread(this);
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    throw new NotYetImplementedException();
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    throw new NotYetImplementedException();
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    throw new NotYetImplementedException();
  }

  @Override
  public boolean canDisconnect() {
    return false;
  }

  @Override
  public boolean canResume() {
    return debugThread.canResume();
  }

  @Override
  public boolean canSuspend() {
    return debugThread.canSuspend();
  }

  @Override
  public boolean canTerminate() {
    throw new NotYetImplementedException();
//    return false;
  }

  @Override
  public void disconnect() throws DebugException {
    throw new NotYetImplementedException();

  }

  @Override
  public IDebugTarget getDebugTarget() {
    return this;
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
    return null;
  }

  @Override
  public String getName() throws DebugException {
    return debugTargetName;
  }

  @Override
  public IProcess getProcess() {
    return process;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    return new IThread[] {debugThread};
  }

  @Override
  public boolean hasThreads() throws DebugException {
    return true;
  }

  @Override
  public boolean isDisconnected() {
    return false;
  }

  @Override
  public boolean isSuspended() {
    return debugThread.isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public void resume() throws DebugException {
    debugThread.resume();
  }

  public void setProcess(IProcess process) {
    this.process = process;
  }

  @Override
  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return breakpoint instanceof DartBreakpoint;
  }

  @Override
  public boolean supportsStorageRetrieval() {
    return false;
  }

  @Override
  public void suspend() throws DebugException {
    debugThread.suspend();
  }

  public void suspended() {
    debugThread.suspended();
  }

  @Override
  public void terminate() throws DebugException {
    throw new NotYetImplementedException();
  }

}
