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

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;

public class ServerDebugTarget extends ServerDebugElement implements IDebugTarget, VmListener {
  private ILaunch launch;
  private IProcess process;
  private int connectionPort;

  private VmConnection connection;

  public ServerDebugTarget(ILaunch launch, IProcess process, int connectionPort) {
    super(null);

    this.launch = launch;
    this.process = process;
    this.connectionPort = connectionPort;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    // TODO(devoncarew):
    if (supportsBreakpoint(breakpoint)) {

    }
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (supportsBreakpoint(breakpoint)) {
      breakpointRemoved(breakpoint, delta);
      breakpointAdded(breakpoint);
    }
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    // TODO(devoncarew):
    if (supportsBreakpoint(breakpoint)) {

    }

  }

  @Override
  public boolean canDisconnect() {
    return false;
  }

  @Override
  public boolean canResume() {
    // TODO(devoncarew):

    return false;
  }

  @Override
  public boolean canSuspend() {
    // TODO(devoncarew): implement this

    return false;
  }

  @Override
  public boolean canTerminate() {
    return process.canTerminate();
  }

  /**
   * Connect to the debugger instance at connectionPort port.
   * 
   * @throws DebugException
   */
  public void connect() throws DebugException {
    long timeout = 2000;

    connection = new VmConnection(connectionPort);
    connection.addListener(this);

    long startTime = System.currentTimeMillis();

    try {
      while (true) {
        try {
          connection.connect();

          break;
        } catch (IOException ioe) {
          if (process.isTerminated()) {
            throw ioe;
          }

          if (System.currentTimeMillis() > startTime + timeout) {
            throw ioe;
          } else {
            sleep(25);
          }
        }
      }
    } catch (IOException ioe) {
      DartDebugCorePlugin.logError(ioe);

      throw new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
          "Unable to connect debugger to the Dart VM: " + ioe.getMessage()));
    }
  }

  @Override
  public void disconnect() throws DebugException {
    throw new UnsupportedOperationException("disconnect is not supported");
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
    return "dart";
  }

  @Override
  public IProcess getProcess() {
    return process;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    // TODO(devoncarew):

    return null;
  }

  public VmConnection getVmConnection() {
    return connection;
  }

  @Override
  public boolean hasThreads() throws DebugException {
    return true;
  }

  @Override
  public boolean isDisconnected() {
    return process.isTerminated();
  }

  @Override
  public boolean isSuspended() {
    // TODO(devoncarew):

    return false;
  }

  @Override
  public boolean isTerminated() {
    return process.isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    try {
      connection.sendSimpleCommand("resume");
    } catch (IOException exception) {
      throw createDebugException(exception);
    }
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
    try {
      connection.sendSimpleCommand("pause");
    } catch (IOException exception) {
      throw createDebugException(exception);
    }
  }

  @Override
  public void terminate() throws DebugException {
    process.terminate();
  }

  private DebugException createDebugException(IOException exception) {
    return new DebugException(new Status(IStatus.ERROR, DartDebugCorePlugin.PLUGIN_ID,
        exception.getMessage(), exception));
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {
    }
  }

}
