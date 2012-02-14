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
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.webkit.WebkitBreakpoint;
import com.google.dart.tools.debug.core.webkit.WebkitCallFrame;
import com.google.dart.tools.debug.core.webkit.WebkitConnection;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.WebkitConnectionListener;
import com.google.dart.tools.debug.core.webkit.WebkitDebugger.DebuggerListenerAdapter;
import com.google.dart.tools.debug.core.webkit.WebkitDebugger.PausedReasonType;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;
import java.util.List;

/**
 * The IDebugTarget implementation for the Dartium debug elements.
 */
public class DartiumDebugTarget extends DartiumDebugElement implements IDebugTarget {
  private String debugTargetName;
  private WebkitConnection connection;
  private ILaunch launch;
  private DartiumProcess process;
  private DartiumDebugThread debugThread;
  private DartiumStreamMonitor outputStreamMonitor;
  private BreakpointManager breakpointManager;

  /**
   * @param target
   */
  public DartiumDebugTarget(String debugTargetName, WebkitConnection connection, ILaunch launch,
      Process javaProcess, IResourceResolver resourceResolver) {
    super(null);

    this.debugTargetName = debugTargetName;
    this.connection = connection;
    this.launch = launch;

    debugThread = new DartiumDebugThread(this);
    process = new DartiumProcess(this, javaProcess);
    outputStreamMonitor = new DartiumStreamMonitor();
    breakpointManager = new BreakpointManager(this, resourceResolver);
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
    return debugThread == null ? false : debugThread.canResume();
  }

  @Override
  public boolean canSuspend() {
    return debugThread == null ? false : debugThread.canSuspend();
  }

  @Override
  public boolean canTerminate() {
    return connection.isConnected();
  }

  @Override
  public void disconnect() throws DebugException {
    throw new UnsupportedOperationException("disconnect is not supported");
  }

  @Override
  public void fireTerminateEvent() {
    breakpointManager.dispose();

    debugThread = null;

    super.fireTerminateEvent();
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
  public String getName() {
    return debugTargetName;
  }

  @Override
  public IProcess getProcess() {
    return process;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    if (debugThread != null) {
      return new IThread[] {debugThread};
    } else {
      return new IThread[0];
    }
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
    return debugThread == null ? false : debugThread.isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return process.isTerminated();
  }

  public void openConnection(String url) throws IOException {
    connection.addConnectionListener(new WebkitConnectionListener() {
      @Override
      public void connectionClosed(WebkitConnection connection) {
        fireTerminateEvent();
      }
    });

    connection.connect();

    connection.getConsole().addConsoleListener(outputStreamMonitor);
    connection.getConsole().enable();

    connection.getDebugger().addDebuggerListener(new DebuggerListenerAdapter() {
      @Override
      public void debuggerBreakpointResolved(WebkitBreakpoint breakpoint) {
        breakpointManager.handleBreakpointResolved(breakpoint);
      }

      @Override
      public void debuggerGlobalObjectCleared() {
        breakpointManager.handleGlobalObjectCleared();
      }

      @Override
      public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames, Object data) {
        debugThread.handleDebuggerSuspended(reason, frames);
      }

      @Override
      public void debuggerResumed() {
        debugThread.handleDebuggerResumed();
      }
    });
    connection.getDebugger().enable();

    fireCreationEvent();
    process.fireCreationEvent();

    if (DartDebugCorePlugin.ENABLE_DEBUGGING) {
      // Set our existing breakpoints, and start listening for new breakpoints.
      breakpointManager.connect();

      // TODO(devoncarew): the VM does not yet support this, and we'd want a way to expose this in
      // the UI as an toggle.
      //connection.getDebugger().setPauseOnExceptions(PauseOnExceptionsType.uncaught);

      connection.getPage().navigate(url);
    }

  }

  @Override
  public void resume() throws DebugException {
    debugThread.resume();
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

  @Override
  public void terminate() throws DebugException {
    process.terminate();
  }

  protected WebkitConnection getWebkitConnection() {
    return connection;
  }

  IStreamMonitor getOutputStreamMonitor() {
    return outputStreamMonitor;
  }

}
