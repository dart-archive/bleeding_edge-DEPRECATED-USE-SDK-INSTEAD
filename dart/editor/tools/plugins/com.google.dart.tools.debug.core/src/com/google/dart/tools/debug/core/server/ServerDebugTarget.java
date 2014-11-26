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
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.source.UriToFileResolver;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
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

  private VmIsolate currentIsolate;

  @SuppressWarnings("unused")
  private IProject currentProject;

  private UriToFileResolver uriToFileResolver;

  private IEclipsePreferences preferences;
  private IPreferenceChangeListener preferenceListener;

  public ServerDebugTarget(ILaunch launch, IProcess process, int connectionPort) {
    this(launch, process, null, connectionPort);
  }

  public ServerDebugTarget(ILaunch launch, IProcess process, String connectionHost,
      int connectionPort) {
    super(null);

    setActiveTarget(this);

    this.launch = launch;
    this.process = process;

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launch.getLaunchConfiguration());

    currentProject = wrapper.getProject();
    uriToFileResolver = new UriToFileResolver(launch);

    connection = new VmConnection(connectionHost, connectionPort);
    connection.addListener(this);

    breakpointManager = new ServerBreakpointManager(this);

    // listen for preference changes
    preferences = DartDebugCorePlugin.getPlugin().getPrefs();
    preferenceListener = new IPreferenceChangeListener() {
      @Override
      public void preferenceChange(PreferenceChangeEvent event) {
        handlePreferenceChange(event);
      }
    };
    preferences.addPreferenceChangeListener(preferenceListener);
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
    currentIsolate = isolate;

    boolean resumed = false;

    if (firstBreak) {
      firstBreak = false;

      // Init everything.
      firstIsolateInit(isolate);
    }

    if (PausedReason.breakpoint == reason) {
      //DartBreakpoint breakpoint = getBreakpointFor(frames);

      // If this is our first break then resume.
      if (isolate.isFirstBreak()) {
        isolate.setFirstBreak(false);
        breakpointManager.handleIsolateCreated(isolate);
        if (!hasBreakpointAtTopFrame(frames)) {
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

  public UriToFileResolver getUriToFileResolver() {
    return uriToFileResolver;
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
    addThread(new ServerDebugThread(this, isolate));

    try {
      connection.enableAllSteppingSync(isolate);
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }

    breakpointManager.setPauseOnExceptionSync(isolate);
  }

  @Override
  public void isolateShutdown(VmIsolate isolate) {
    removeThread(findThread(isolate));

    breakpointManager.handleIsolateShutdown(isolate);
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
    // Any breakpoint, we don't know in general case if some project is used or not.
    // E.g. when package root is used, we might end up referencing a project,
    // not an external resource.
    return true;
//    if (!(breakpoint instanceof DartBreakpoint)) {
//      return false;
//    }
//    DartBreakpoint dartBreakpoint = (DartBreakpoint) breakpoint;
//    IFile file = dartBreakpoint.getFile();
//    // external file?
//    if (currentProject == null || file == null) {
//      return true;
//    }
//    // check the project
//    IProject project = file.getProject();
//    // file from the same project, or a Pub package project
//    if (currentProject.equals(project)) {
//      return true;
//    }
//    // OK, a Pub package project
//    if (PubCacheManager_NEW.isPubCacheProject(project)) {
//      return true;
//    }
//    // a breakpoint from another project
//    return false;
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
    uriToFileResolver.dispose();
  }

  public void writeToStdout(String message) {
    fireStreamAppended(message + "\n");
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

  protected VmIsolate getCurrentIsolate() {
    return currentIsolate;
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
    preferences.removePreferenceChangeListener(preferenceListener);

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
      if (method != null) {
        method.invoke(monitor, message);
      }
    } catch (Throwable t) {
      DartDebugCorePlugin.logInfo(message);
    }
  }

  private void firstIsolateInit(VmIsolate isolate) {
    breakpointManager.connect(isolate);
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
          IFile file = breakpoint.getFile();
          if (file != null) {
            String bpUrl = file.getLocationURI().toString();

            if (bpUrl.equals(url)) {
              return breakpoint;
            }
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

  private void handlePreferenceChange(PreferenceChangeEvent event) {
    String key = event.getKey();
    if (DartDebugCorePlugin.PREFS_BREAK_ON_EXCEPTIONS.equals(key)) {
      breakpointManager.setPauseOnExceptionForLiveIsolates();
    }
  }

  private boolean hasBreakpointAtTopFrame(List<VmCallFrame> frames) {
    VmCallFrame frame = frames.get(0);
    VmLocation location = frame.getLocation();
    return breakpointManager.hasBreakpointAtLine(location);
  }

  private void printExceptionToStdout(VmValue exception) {
    String text = exception.getText();

    int index = text.indexOf('\n');

    if (index != -1) {
      text = text.substring(0, index).trim();
    }

    fireStreamAppended("Breaking on exception: " + text);
    try {
      VmIsolate isolate = exception.getIsolate();
      connection.evaluateObject(isolate, exception, "toString()", new VmCallback<VmValue>() {
        @Override
        public void handleResult(VmResult<VmValue> result) {
          if (result.isError()) {
            fireStreamAppended("\n");
          } else {
            String desc = result.getResult().getText();
            desc = StringUtils.removeStart(desc, "\"");
            desc = StringUtils.removeEnd(desc, "\"");
            fireStreamAppended(": " + desc + "\n");
          }
        }
      });
    } catch (IOException e) {
    }
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
