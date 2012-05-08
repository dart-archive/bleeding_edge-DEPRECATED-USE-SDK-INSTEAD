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
package com.google.dart.tools.debug.core.chromejs;

import org.eclipse.debug.core.model.IDebugTarget;

/**
 * The IDebugTarget implementation for the Chrome debug elements. This class provides much of the
 * functionality of the Chrome debugger implementation.
 * 
 * @see IDebugTarget
 * @see StandaloneVm
 */
public class ChromeDebugTarget {
//  extends ChromeDebugElement implements IDebugTarget,
//    DebugEventListener, TabDebugEventListener {
//
//  private ChromeDebugThread javascriptThread;
//  private JavascriptVm javascriptVm;
//
//  private String debugTargetName;
//  private ILaunch launch;
//  private DartLaunchConfigWrapper launchWrapper;
//
//  private IProcess process;
//  private ScriptToFileMapping scriptToFileMapping;
//
//  private DartSourceMapping sourceMapper;
//
//  private Map<DartBreakpoint, Breakpoint> vmBreakpointMap = new HashMap<DartBreakpoint, Breakpoint>();
//
//  /**
//   * Create a new ChromeDebugTarget. This constructor is called when the debug target is a
//   * BrowserTab, as opposed to a StandaloneVm.
//   * 
//   * @param debugTargetName
//   * @param launch
//   */
//  public ChromeDebugTarget(String debugTargetName, ILaunch launch) {
//    super(null);
//
//    this.debugTargetName = debugTargetName;
//    this.launch = launch;
//
//    javascriptThread = new ChromeDebugThread(this);
//
//    launchWrapper = new DartLaunchConfigWrapper(launch.getLaunchConfiguration());
//    sourceMapper = new DartSourceMapping(launchWrapper.getProject());
//  }
//
//  /**
//   * Create a new ChromeDebugTarget.
//   * 
//   * @param debugTargetName
//   * @param launch
//   * @param standaloneVm
//   */
//  public ChromeDebugTarget(String debugTargetName, ILaunch launch, StandaloneVm standaloneVm) {
//    this(debugTargetName, launch);
//
//    this.javascriptVm = standaloneVm;
//  }
//
//  @Override
//  public void breakpointAdded(IBreakpoint bp) {
//    final DartBreakpoint breakpoint = (DartBreakpoint) bp;
//
//    IFile file = breakpoint.getFile();
//    int line = breakpoint.getLine();
//
//    SourceLocation location = sourceMapper.mapDartToJavascript(file, line);
//
//    if (location.isValid()) {
//      String scriptPath = getScriptToFileMapping().getScriptName();
//
//      if (scriptPath != null) {
//        DartDebugCorePlugin.logInfo("Adding breakpoint " + breakpoint + " ==> "
//            + getFileName(scriptPath) + ":" + location.getLine());
//
//        javascriptVm.setBreakpoint(new Breakpoint.Target.ScriptName(scriptPath),
//            location.getLine(), Breakpoint.EMPTY_VALUE, breakpoint.isBreakpointEnabled(), null,
//            new JavascriptVm.BreakpointCallback() {
//              @Override
//              public void failure(String errorMessage) {
//                reportError("Error Adding Breakpoint", errorMessage);
//              }
//
//              @Override
//              public void success(Breakpoint jsBreakpoint) {
//                vmBreakpointMap.put(breakpoint, jsBreakpoint);
//              }
//            }, null);
//      } else {
//        DartDebugCorePlugin.logInfo("Unable to map breakpoint " + breakpoint);
//      }
//    } else {
//      DartDebugCorePlugin.logInfo("Unable to map breakpoint " + breakpoint);
//    }
//  }
//
//  @Override
//  public void breakpointChanged(IBreakpoint bp, IMarkerDelta delta) {
//    if (vmBreakpointMap.containsKey(bp)) {
//      DartBreakpoint breakpoint = (DartBreakpoint) bp;
//      Breakpoint vmBreakpoint = vmBreakpointMap.get(breakpoint);
//
//      if (breakpoint.isBreakpointEnabled() != vmBreakpoint.isEnabled()) {
//        vmBreakpoint.setEnabled(breakpoint.isBreakpointEnabled());
//        vmBreakpoint.flush(null, null);
//      } else {
//        breakpointRemoved(bp, null);
//        breakpointAdded(bp);
//      }
//    }
//  }
//
//  @Override
//  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
//    DartDebugCorePlugin.logInfo("Removing breakpoint " + breakpoint);
//
//    Breakpoint vmBreakpoint = vmBreakpointMap.remove(breakpoint);
//
//    if (vmBreakpoint != null) {
//      vmBreakpoint.clear(null, null);
//    }
//  }
//
//  @Override
//  public boolean canDisconnect() {
//    return false;
//  }
//
//  @Override
//  public boolean canResume() {
//    return javascriptThread.canResume();
//  }
//
//  @Override
//  public boolean canSuspend() {
//    return javascriptThread.canSuspend();
//  }
//
//  @Override
//  public boolean canTerminate() {
//    return javascriptVm.isAttached();
//  }
//
//  @Override
//  public void closed() {
//    // nothing to do here - disconnected() is automatically called after this
//
//  }
//
//  /**
//   * Called by the launch configuration when the debug target successfully connects to a remote vm.
//   */
//  public void connected() {
//    fireCreationEvent();
//
//    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
//
//    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
//        DartDebugCorePlugin.DEBUG_MODEL_ID);
//
//    for (IBreakpoint breakpoint : breakpoints) {
//      if (supportsBreakpoint(breakpoint)) {
//        breakpointAdded(breakpoint);
//      }
//    }
//  }
//
//  @Override
//  public void disconnect() throws DebugException {
//    javascriptVm.detach();
//  }
//
//  @Override
//  public void disconnected() {
//    if (process != null) {
//      DebugPlugin.getDefault().fireDebugEventSet(
//          new DebugEvent[] {
//              new DebugEvent(process, DebugEvent.TERMINATE),
//              new DebugEvent(getDebugTarget(), DebugEvent.TERMINATE)});
//    } else {
//      DebugPlugin.getDefault().fireDebugEventSet(
//          new DebugEvent[] {new DebugEvent(getDebugTarget(), DebugEvent.TERMINATE)});
//    }
//
//    DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
//  }
//
//  @SuppressWarnings("rawtypes")
//  @Override
//  public Object getAdapter(Class adapter) {
//    if (adapter.equals(IDebugTarget.class)) {
//      return this;
//    }
//
//    return super.getAdapter(adapter);
//  }
//
//  /**
//   * Given a Javascript breakpoint, return the corresponding Dart breakpoint.
//   * 
//   * @param jsBreakpoint a Javascript breakpoint
//   * @return the Dart breakpoint corresponding to the given Javascript breakpoint
//   */
//  public DartBreakpoint getDartBreakpoint(Breakpoint jsBreakpoint) {
//    for (DartBreakpoint dartBreakpoint : vmBreakpointMap.keySet()) {
//      if (jsBreakpoint.equals(vmBreakpointMap.get(dartBreakpoint))) {
//        return dartBreakpoint;
//      }
//    }
//
//    return null;
//  }
//
//  /**
//   * @return the dart source mapper for this debug target
//   */
//  public DartSourceMapping getDartSourceMapper() {
//    return sourceMapper;
//  }
//
//  @Override
//  public DebugEventListener getDebugEventListener() {
//    return this;
//  }
//
//  @Override
//  public IDebugTarget getDebugTarget() {
//    return this;
//  }
//
//  @Override
//  public ILaunch getLaunch() {
//    return launch;
//  }
//
//  @Override
//  public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
//    throw new DebugException(new Status(DebugException.NOT_SUPPORTED,
//        DartDebugCorePlugin.PLUGIN_ID, "unsupported"));
//  }
//
//  @Override
//  public String getName() throws DebugException {
//    return debugTargetName;
//  }
//
//  @Override
//  public IProcess getProcess() {
//    return process;
//  }
//
//  @Override
//  public IThread[] getThreads() throws DebugException {
//    return new IThread[] {javascriptThread};
//  }
//
//  @Override
//  public VmStatusListener getVmStatusListener() {
//    return null;
//  }
//
//  @Override
//  public boolean hasThreads() throws DebugException {
//    return true;
//  }
//
//  @Override
//  public boolean isDisconnected() {
//    if (javascriptVm == null) {
//      return true;
//    }
//
//    return !javascriptVm.isAttached();
//  }
//
//  @Override
//  public boolean isSuspended() {
//    return javascriptThread.isSuspended();
//  }
//
//  @Override
//  public boolean isTerminated() {
//    return !javascriptVm.isAttached();
//  }
//
//  @Override
//  public void navigated(String newUrl) {
//
//  }
//
//  /**
//   * Display the given error to the user.
//   * 
//   * @param message
//   */
//  public void reportError(String message) {
//    reportError(null, message);
//  }
//
//  /**
//   * Display the given error to the user.
//   * 
//   * @param title
//   * @param message
//   */
//  public void reportError(String title, String message) {
//    DebugUIHelperFactory.getDebugUIHelper().displayError(title, message);
//
//    if (title == null) {
//      DartDebugCorePlugin.logError(message);
//    } else {
//      DartDebugCorePlugin.logError(title + ": " + message);
//    }
//  }
//
//  @Override
//  public void resume() throws DebugException {
//    javascriptThread.resume();
//  }
//
//  @Override
//  public void resumed() {
//    javascriptThread.resumed();
//  }
//
//  @Override
//  public void scriptCollected(Script script) {
//    getScriptToFileMapping().handleScriptCollected(script);
//  }
//
//  @Override
//  public void scriptContentChanged(Script script) {
//    getScriptToFileMapping().handleScriptContentChanged(script);
//  }
//
//  @Override
//  public void scriptLoaded(Script script) {
//    // This guard check is here for debugger connection bootstrapping.
//    if (javascriptVm != null) {
//      getScriptToFileMapping().handleScriptLoaded(script);
//    }
//  }
//
//  public void setBrowserTab(BrowserTab browserTab) {
//    javascriptVm = browserTab;
//  }
//
//  /**
//   * Set the Chrome IProcess that this debugger is communicating with.
//   * 
//   * @param process
//   */
//  public void setProcess(IProcess process) {
//    this.process = process;
//  }
//
//  @Override
//  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
//    return breakpoint instanceof DartBreakpoint;
//  }
//
//  @Override
//  public boolean supportsStorageRetrieval() {
//    return false;
//  }
//
//  @Override
//  public void suspend() throws DebugException {
//    javascriptThread.suspend();
//  }
//
//  @Override
//  public void suspended(DebugContext context) {
//    javascriptThread.suspended(context);
//  }
//
//  @Override
//  public void terminate() throws DebugException {
//    javascriptVm.detach();
//  }
//
//  protected JavascriptVm getJavascriptVm() {
//    return javascriptVm;
//  }
//
//  protected ScriptToFileMapping getScriptToFileMapping() {
//    if (scriptToFileMapping == null) {
//      scriptToFileMapping = new ScriptToFileMapping(this);
//    }
//
//    return scriptToFileMapping;
//  }
//
//  /**
//   * Given a path, return the last segment on it (the file name).
//   */
//  private String getFileName(String path) {
//    if (path == null) {
//      return null;
//    }
//
//    int index = path.lastIndexOf(File.separator);
//
//    if (index == -1) {
//      return path;
//    } else {
//      return path.substring(index + 1);
//    }
//  }

}
