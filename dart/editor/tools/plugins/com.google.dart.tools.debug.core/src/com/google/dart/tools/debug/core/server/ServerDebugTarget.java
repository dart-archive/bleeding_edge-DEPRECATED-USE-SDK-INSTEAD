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
import com.google.dart.tools.debug.core.util.NetUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;
import java.util.List;

/**
 * An implementation of IDebugTarget for Dart VM debug connections.
 */
public class ServerDebugTarget extends ServerDebugElement implements IDebugTarget, VmListener {
  private ILaunch launch;
  private IProcess process;
  private int connectionPort;

  private VmConnection connection;

  private ServerDebugThread debugThread = new ServerDebugThread(this);

  public ServerDebugTarget(ILaunch launch, IProcess process, int connectionPort) {
    super(null);

    this.launch = launch;
    this.process = process;
    this.connectionPort = connectionPort;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    if (supportsBreakpoint(breakpoint)) {
      addBreakpoint((DartBreakpoint) breakpoint);
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
    if (supportsBreakpoint(breakpoint)) {
      VmBreakpoint vmBreakpoint = findBreakpoint((DartBreakpoint) breakpoint);

      if (vmBreakpoint != null) {
        try {
          getConnection().removeBreakpoint(vmBreakpoint);
        } catch (IOException exception) {
          // TODO(devoncarew): display to the user

          DartDebugCorePlugin.logError(exception);
        }
      }
    }
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

      throw new DebugException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Unable to connect debugger to the Dart VM: " + ioe.getMessage()));
    }

    // Set up the existing breakpoints.
    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    for (IBreakpoint breakpoint : breakpoints) {
      if (supportsBreakpoint(breakpoint)) {
        addBreakpoint((DartBreakpoint) breakpoint);
      }
    }

    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);

    // TODO(devoncarew): temporary - this is just here to exercise the getLibraryURLs call
    try {
      connection.getLibraryURLs(new VmCallback<List<String>>() {
        @Override
        public void handleResult(VmResult<List<String>> result) {
          if (result.isError()) {
            System.out.println("lib result error: " + result.getError());
          } else {
            for (String lib : result.getResult()) {
              System.out.println("lib: " + lib);
            }
          }
        }
      });
    } catch (IOException ioe) {

    }

    // TODO(devoncarew): this it temporarily commented out to test the paused event functionality
    //resume();
  }

  @Override
  public void debuggerPaused(List<VmCallFrame> frames) {
    debugThread.handleDebuggerPaused(frames);
  }

  @Override
  public void debuggerResumed() {
    debugThread.handleDebuggerResumed();
  }

  @Override
  public void disconnect() throws DebugException {
    throw new UnsupportedOperationException("disconnect is not supported");
  }

  @Override
  public void fireTerminateEvent() {
    dispose();

    debugThread = null;

    super.fireTerminateEvent();
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
    return new IThread[] {debugThread};
  }

  public VmConnection getVmConnection() {
    return connection;
  }

  @Override
  public boolean hasThreads() throws DebugException {
    return !isTerminated();
  }

  @Override
  public boolean isDisconnected() {
    return process.isTerminated();
  }

  @Override
  public boolean isSuspended() {
    return debugThread == null ? false : debugThread.isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return process.isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    try {
      connection.resume();
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
      connection.pause();
    } catch (IOException exception) {
      throw createDebugException(exception);
    }
  }

  @Override
  public void terminate() throws DebugException {
    process.terminate();
  }

  @Override
  protected ServerDebugTarget getTarget() {
    return this;
  }

  private void addBreakpoint(DartBreakpoint breakpoint) {
    if (breakpoint.isBreakpointEnabled()) {
      String url = getUrlForResource(breakpoint.getFile());
      int line = breakpoint.getLine();

      try {
        getConnection().setBreakpoint(url, line);
      } catch (IOException exception) {
        // TODO(devoncarew): display to the user

        DartDebugCorePlugin.logError(exception);
      }
    }
  }

  private DebugException createDebugException(IOException exception) {
    return new DebugException(new Status(
        IStatus.ERROR,
        DartDebugCorePlugin.PLUGIN_ID,
        exception.getMessage(),
        exception));
  }

  private void dispose() {
    if (DebugPlugin.getDefault() != null) {
      DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
    }
  }

  private VmBreakpoint findBreakpoint(DartBreakpoint breakpoint) {
    final String url = getUrlForResource(breakpoint.getFile());
    final int line = breakpoint.getLine();

    for (VmBreakpoint bp : getConnection().getBreakpoints()) {
      if (line == bp.getLocation().getLineNumber()
          && url.equals(NetUtils.compareUrls(url, bp.getLocation().getUrl()))) {
        return bp;
      }
    }

    return null;
  }

  private String getUrlForResource(IFile file) {
    return file.getLocation().toFile().toURI().toString();
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {
    }
  }

}
