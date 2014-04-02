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

import com.google.dart.tools.core.NotYetImplementedException;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartDebugCorePlugin.BreakOnExceptions;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.server.VmConnection.BreakOnExceptionsType;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of IDebugTarget for Dart VM debug connections.
 */
public class ServerDebugTarget extends ServerDebugElement implements IDebugTarget, VmListener,
    IBreakpointManagerListener {

  private static ServerDebugTarget activeTarget;

  public static ServerDebugTarget getActiveTarget() {
    return activeTarget;
  }

  private static void setActiveTarget(ServerDebugTarget target) {
    activeTarget = target;
  }

  private ILaunch launch;

  private IProcess process;

  private VmConnection connection;

  private List<ServerDebugThread> threads = new ArrayList<ServerDebugThread>();

  private boolean firstBreak = true;

  private ServerBreakpointManager breakpointManager;

  // TODO(devoncarew): this "main" isolate is temporary, until the VM allows us to 
  // set wildcard breakpoints across all isolates.
  private VmIsolate mainIsolate;

  private IProject currentProject;

  public ServerDebugTarget(ILaunch launch, IProcess process, int connectionPort) {
    this(launch, process, null, connectionPort);
  }

  public ServerDebugTarget(ILaunch launch, IProcess process, String connectionHost,
      int connectionPort) {
    super(null);

    setActiveTarget(this);

    this.launch = launch;
    this.process = process;

    breakpointManager = new ServerBreakpointManager(this);

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launch.getLaunchConfiguration());

    currentProject = wrapper.getProject();

    connection = new VmConnection(connectionHost, connectionPort);
    connection.addListener(this);
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
  public void breakpointManagerEnablementChanged(boolean enabled) {
    setBreakpointsActive(enabled);
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    throw new NotYetImplementedException();
  }

  @Override
  public void breakpointResolved(VmIsolate isolate, VmBreakpoint breakpoint) {
    breakpointManager.handleBreakpointResolved(isolate, breakpoint);
  }

  @Override
  public boolean canDisconnect() {
    return false;
  }

  @Override
  public boolean canResume() {
    return false;
  }

  @Override
  public boolean canSuspend() {
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
    long timeout = 10000;

    long startTime = System.currentTimeMillis();

    try {
      sleep(50);

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
      throw new DebugException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          "Unable to connect debugger to the Dart VM: " + ioe.getMessage()));
    }
  }

  @Override
  public void connectionClosed(VmConnection connection) {
    fireTerminateEvent();
  }

  @Override
  public void connectionOpened(VmConnection connection) {
    IBreakpointManager eclipseBpManager = DebugPlugin.getDefault().getBreakpointManager();
    eclipseBpManager.addBreakpointManagerListener(this);

    if (!eclipseBpManager.isEnabled()) {
      setBreakpointsActive(false);
    }
  }

  @Override
  public void debuggerPaused(PausedReason reason, VmIsolate isolate, List<VmCallFrame> frames,
      VmValue exception) {
    boolean resumed = false;

    if (firstBreak) {
      firstBreak = false;

      // init everything
      firstIsolateInit(isolate);

      if (PausedReason.breakpoint == reason) {
        DartBreakpoint breakpoint = getBreakpointFor(frames);

        // If this is our first break, and there is no user breakpoint here, and the stop is on the
        // main() method, then resume.
        if (breakpoint == null && frames.size() > 0 && frames.get(0).isMain()) {
          resumed = true;

          try {
            getVmConnection().resume(isolate);
          } catch (IOException ioe) {
            DartDebugCorePlugin.logError(ioe);
          }
        }
      }
    }

    if (!resumed) {
      ServerDebugThread thread = findThread(isolate);

      if (thread != null) {
        if (exception != null) {
          printExceptionToStdout(exception);
        }

        thread.handleDebuggerPaused(reason, frames, exception);
      }
    }
  }

  @Override
  public void debuggerResumed(VmIsolate isolate) {
    ServerDebugThread thread = findThread(isolate);

    if (thread != null) {
      thread.handleDebuggerResumed();
    }
  }

  @Override
  public void disconnect() throws DebugException {
    throw new UnsupportedOperationException("disconnect is not supported");
  }

  @Override
  public void fireTerminateEvent() {
    setActiveTarget(null);

    dispose();

    threads.clear();

    // Check for null on system shutdown.
    if (DebugPlugin.getDefault() != null) {
      super.fireTerminateEvent();
    }
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
    return "dart";
  }

  @Override
  public IProcess getProcess() {
    return process;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    return threads.toArray(new IThread[threads.size()]);

//    if (debugThread == null) {
//      return new IThread[0];
//    } else {
//      return new IThread[] {debugThread};
//    }
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
  public void isolateCreated(VmIsolate isolate) {
    if (mainIsolate == null) {
      mainIsolate = isolate;
    }

    addThread(new ServerDebugThread(this, isolate));
  }

  @Override
  public void isolateShutdown(VmIsolate isolate) {
    removeThread(findThread(isolate));
  }

  @Override
  public boolean isSuspended() {
    // Create a copy of the threads before iterating.
    for (IThread thread : new ArrayList<IThread>(threads)) {
      if (thread.isSuspended()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isTerminated() {
    return process.isTerminated();
  }

  @Override
  public void resume() throws DebugException {

  }

  @Override
  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    if (!(breakpoint instanceof DartBreakpoint)) {
      return false;
    }

    if (currentProject == null) {
      return true;
    } else {
      return currentProject.equals(((DartBreakpoint) breakpoint).getFile().getProject());
    }
  }

  @Override
  public boolean supportsStorageRetrieval() {
    return false;
  }

  @Override
  public void suspend() throws DebugException {

  }

  @Override
  public void terminate() throws DebugException {
    process.terminate();
  }

  public void writeToStdout(String message) {
    //TODO(keertip): implement printing messages to console
    // process.getStreamMonitor().messageAdded(message);
  }

  protected ServerDebugThread findThread(VmIsolate isolate) {
    for (ServerDebugThread thread : threads) {
      if (thread.getIsolate().getId() == isolate.getId()) {
        return thread;
      }
    }

    return null;
  }

  protected DartBreakpoint getBreakpointFor(List<VmCallFrame> frames) {
    if (frames.size() == 0) {
      return null;
    }

    return getBreakpointFor(frames.get(0));
  }

  protected DartBreakpoint getBreakpointFor(VmCallFrame frame) {
    return getBreakpointFor(frame.getLocation());
  }

  // TODO(devoncarew): the concept of a main isolate needs to go away
  protected VmIsolate getMainIsolate() {
    return mainIsolate;
  }

  @Override
  protected ServerDebugTarget getTarget() {
    return this;
  }

  private void addThread(ServerDebugThread thread) {
    threads.add(thread);

    thread.fireCreationEvent();
  }

  private void dispose() {
    if (connection != null) {
      connection.handleTerminated();
    }

    breakpointManager.dispose();
  }

  private void fireStreamAppended(String message) {
    IStreamMonitor monitor = getProcess().getStreamsProxy().getOutputStreamMonitor();

    try {
      // monitor.fireStreamAppended(message);
      Method method = getMethod(monitor, "fireStreamAppended");
      method.invoke(monitor, message);
    } catch (Throwable t) {

    }
  }

  private void firstIsolateInit(VmIsolate isolate) {
    breakpointManager.connect(isolate);

    try {
      connection.enableAllSteppingSync(isolate);
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }

    // TODO(devoncarew): listen for changes to DartDebugCorePlugin.PREFS_BREAK_ON_EXCEPTIONS
    // Turn on break-on-exceptions.
    try {
      connection.setPauseOnExceptionSync(isolate, getPauseType());
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

  private DartBreakpoint getBreakpointFor(VmLocation location) {
    if (location == null) {
      return null;
    }

    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    String url = location.getUrl();
    int line = location.getLineNumber(getConnection());

    for (IBreakpoint bp : breakpoints) {
      if (getTarget().supportsBreakpoint(bp)) {
        DartBreakpoint breakpoint = (DartBreakpoint) bp;

        if (breakpoint.getLine() == line) {
          String bpUrl = breakpoint.getFile().getLocationURI().toString();

          if (bpUrl.equals(url)) {
            return breakpoint;
          }
        }
      }
    }

    return null;
  }

  private Method getMethod(Object obj, String methodName) {
    for (Method method : obj.getClass().getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        method.setAccessible(true);
        return method;
      }
    }

    return null;
  }

  private BreakOnExceptionsType getPauseType() {
    final BreakOnExceptions boe = DartDebugCorePlugin.getPlugin().getBreakOnExceptions();
    BreakOnExceptionsType pauseType = BreakOnExceptionsType.none;

    if (boe == BreakOnExceptions.uncaught) {
      pauseType = BreakOnExceptionsType.unhandled;
    } else if (boe == BreakOnExceptions.all) {
      pauseType = BreakOnExceptionsType.all;
    }

    return pauseType;
  }

  private void printExceptionToStdout(VmValue exception) {
    String text = exception.getText();

    int index = text.indexOf('\n');

    if (index != -1) {
      text = text.substring(0, index).trim();
    }

    fireStreamAppended("Breaking on exception: " + text + "\n");
  }

  private void removeThread(ServerDebugThread thread) {
    if (thread != null) {
      threads.remove(thread);

      thread.fireTerminateEvent();
    }
  }

  private void setBreakpointsActive(boolean enabled) {
    // TODO(devoncarew): we need for the command-line debugger to support enabling / disabling all
    // breakpoints.

  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {
    }
  }

}
