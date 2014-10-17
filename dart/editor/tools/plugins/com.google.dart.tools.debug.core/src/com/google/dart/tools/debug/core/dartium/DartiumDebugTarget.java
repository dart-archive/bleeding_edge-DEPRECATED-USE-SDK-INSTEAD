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
import com.google.dart.tools.debug.core.DartDebugCorePlugin.BreakOnExceptions;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.DebugUIHelper;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.source.UriToFileResolver;
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.webkit.WebkitBreakpoint;
import com.google.dart.tools.debug.core.webkit.WebkitCallFrame;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitConnection;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.WebkitConnectionListener;
import com.google.dart.tools.debug.core.webkit.WebkitDebugger.DebuggerListenerAdapter;
import com.google.dart.tools.debug.core.webkit.WebkitDebugger.PauseOnExceptionsType;
import com.google.dart.tools.debug.core.webkit.WebkitDebugger.PausedReasonType;
import com.google.dart.tools.debug.core.webkit.WebkitDom.DomListener;
import com.google.dart.tools.debug.core.webkit.WebkitDom.InspectorListener;
import com.google.dart.tools.debug.core.webkit.WebkitLocation;
import com.google.dart.tools.debug.core.webkit.WebkitPage;
import com.google.dart.tools.debug.core.webkit.WebkitRemoteObject;
import com.google.dart.tools.debug.core.webkit.WebkitResult;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import java.io.IOException;
import java.util.List;

/**
 * The IDebugTarget implementation for the Dartium debug elements.
 */
public class DartiumDebugTarget extends DartiumDebugElement implements IDebugTarget,
    IBreakpointManagerListener {
  private static DartiumDebugTarget activeTarget;

  public static DartiumDebugTarget getActiveTarget() {
    return activeTarget;
  }

  private static void setActiveTarget(DartiumDebugTarget target) {
    activeTarget = target;
  }

  private String debugTargetName;
  private String disconnectMessage;
  private WebkitConnection connection;
  private ILaunch launch;
  private DartiumProcess process;
  private IResourceResolver resourceResolver;
  private boolean enableBreakpoints;
  private DartiumDebugThread debugThread;
  private DartBreakpointManager breakpointManager;
  private CssScriptManager cssScriptManager;
  private HtmlScriptManager htmlScriptManager;
  private DartCodeManager dartCodeManager;
  private boolean canSetScriptSource;
  private SourceMapManager sourceMapManager;
  private UriToFileResolver uriToFileResolver;

  /**
   * A copy constructor for DartiumDebugTarget.
   * 
   * @param target
   */
  public DartiumDebugTarget(DartiumDebugTarget target) {
    this(
        target.debugTargetName,
        new WebkitConnection(target.connection),
        target.launch,
        null,
        target.resourceResolver,
        target.enableBreakpoints,
        false);

    this.process = target.process;
    this.process.switchTo(this);
  }

  /**
   * @param target
   */
  public DartiumDebugTarget(String debugTargetName, WebkitConnection connection, ILaunch launch,
      Process javaProcess, IResourceResolver resourceResolver, boolean enableBreakpoints,
      boolean isRemote) {
    super(null);

    setActiveTarget(this);

    this.debugTargetName = debugTargetName;
    this.connection = connection;
    this.launch = launch;
    this.resourceResolver = resourceResolver;
    this.enableBreakpoints = enableBreakpoints;

    debugThread = new DartiumDebugThread(this);

    if (javaProcess != null || isRemote) {
      process = new DartiumProcess(this, debugTargetName, javaProcess);
    }

    if (enableBreakpoints) {
      breakpointManager = new BreakpointManager(this);
    } else {
      breakpointManager = new BreakpointManager.NullBreakpointManager();
    }

    cssScriptManager = new CssScriptManager(this);

    if (DartDebugCorePlugin.SEND_MODIFIED_HTML) {
      htmlScriptManager = new HtmlScriptManager(this);
    }

    if (DartDebugCorePlugin.SEND_MODIFIED_DART) {
      dartCodeManager = new DartCodeManager(this);
    }

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launch.getLaunchConfiguration());

    if (wrapper.getProject() != null) {
      sourceMapManager = new SourceMapManager(wrapper.getProject());
    }

    uriToFileResolver = new UriToFileResolver(launch);
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
    try {
      getConnection().getDebugger().setBreakpointsActive(enableBreakpoints && enabled);
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }
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
    setActiveTarget(null);

    breakpointManager.dispose(false);

    cssScriptManager.dispose();

    if (htmlScriptManager != null) {
      htmlScriptManager.dispose();
    }

    if (dartCodeManager != null) {
      dartCodeManager.dispose();
    }

    if (sourceMapManager != null) {
      sourceMapManager.dispose();
    }

    debugThread = null;

    // Check for null on system shutdown.
    if (DebugPlugin.getDefault() != null) {
      super.fireTerminateEvent();
    }
  }

  /**
   * @return the connection
   */
  @Override
  public WebkitConnection getConnection() {
    return connection;
  }

  @Override
  public IDebugTarget getDebugTarget() {
    return this;
  }

  public boolean getEnableBreakpoints() {
    return enableBreakpoints;
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
    if (disconnectMessage != null) {
      return debugTargetName + " <" + disconnectMessage + ">";
    } else {
      return debugTargetName;
    }
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

  public UriToFileResolver getUriToFileResolver() {
    return uriToFileResolver;
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
    return debugThread == null;
  }

  /**
   * Recycle the current Dartium debug connection; attempt to reset it to a fresh state beforehand.
   * 
   * @param url
   * @throws IOException
   */
  public void navigateToUrl(ILaunchConfiguration launchConfig, final String url,
      boolean enableBreakpoints, IResourceResolver resolver) throws IOException {
    this.resourceResolver = resolver;

    IBreakpointManager eclipseBpManager = DebugPlugin.getDefault().getBreakpointManager();

    connection.getDebugger().setBreakpointsActive(enableBreakpoints && eclipseBpManager.isEnabled());
    connection.getDebugger().setPauseOnExceptions(getPauseType());

    getConnection().getPage().navigate(url);
  }

  public void openConnection() throws IOException {
    openConnection(null, false);
  }

  public void openConnection(final String url, boolean listenForInspectorDetach) throws IOException {
    connection.addConnectionListener(new WebkitConnectionListener() {
      @Override
      public void connectionClosed(WebkitConnection connection) {
        fireTerminateEvent();
      }
    });

    connection.connect();

    process.getStreamMonitor().connectTo(connection);

    connection.getPage().addPageListener(new WebkitPage.PageListenerAdapter() {
      @Override
      public void loadEventFired(int timestamp) {
        if (htmlScriptManager != null) {
          htmlScriptManager.handleLoadEventFired();
        }
      }
    });
    connection.getPage().enable();

    connection.getCSS().enable();

    if (DartDebugCorePlugin.SEND_MODIFIED_HTML) {
      connection.getDom().addDomListener(new DomListener() {
        @Override
        public void documentUpdated() {
          if (htmlScriptManager != null) {
            htmlScriptManager.handleDocumentUpdated();
          }
        }
      });
    }

    if (listenForInspectorDetach) {
      connection.getDom().addInspectorListener(new InspectorListener() {
        @Override
        public void detached(String reason) {
          handleInspectorDetached(reason);
        }

        @Override
        public void targetCrashed() {
          handleTargetCrashed();
        }
      });
    }

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
      public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames,
          WebkitRemoteObject exception) {

        if (exception != null && !DartDebugCorePlugin.getPlugin().getBreakOnJSException()
            && isJavaScriptException(frames, exception)) {
          try {
            // Continue VM execution.
            getConnection().getDebugger().resume();
          } catch (IOException e) {

          }
        } else {
          if (exception != null) {
            printExceptionToStdout(exception);
          }

          debugThread.handleDebuggerSuspended(reason, frames, exception);
        }
      }

      @Override
      public void debuggerResumed() {
        debugThread.handleDebuggerResumed();
      }

      @Override
      public void debuggerScriptParsed(WebkitScript script) {
        checkForDebuggerExtension(script);
      }
    });
    connection.getDebugger().enable();

    IBreakpointManager eclipseBpManager = DebugPlugin.getDefault().getBreakpointManager();
    eclipseBpManager.addBreakpointManagerListener(this);

    getConnection().getDebugger().setBreakpointsActive(
        enableBreakpoints && eclipseBpManager.isEnabled());

    connection.getDebugger().canSetScriptSource(new WebkitCallback<Boolean>() {
      @Override
      public void handleResult(WebkitResult<Boolean> result) {
        if (!result.isError() && result.getResult() != null) {
          canSetScriptSource = result.getResult().booleanValue();
        }
      }
    });

    fireCreationEvent();
    process.fireCreationEvent();

    // Set our existing breakpoints and start listening for new breakpoints.
    breakpointManager.connect();

    // TODO(devoncarew): listen for changes to DartDebugCorePlugin.PREFS_BREAK_ON_EXCEPTIONS

    if (url == null) {
      connection.getDebugger().setPauseOnExceptions(getPauseType());
    } else {
      connection.getDebugger().setPauseOnExceptions(
          getPauseType(),
          createNavigateWebkitCallback(url));
    }
  }

  /**
   * Attempt to re-connect to a debug target. If successful, it will return a new
   * DartiumDebugTarget.
   * 
   * @return
   * @throws IOException
   */
  public DartiumDebugTarget reconnect() throws IOException {
    DartiumDebugTarget newTarget = new DartiumDebugTarget(this);

    newTarget.reopenConnection();

    ILaunch launch = newTarget.getLaunch();
    launch.addDebugTarget(newTarget);

    for (IDebugTarget target : launch.getDebugTargets()) {
      if (target.isTerminated()) {
        launch.removeDebugTarget(target);
      }
    }

    return newTarget;
  }

  public void reopenConnection() throws IOException {
    openConnection(null, true);
  }

  @Override
  public void resume() throws DebugException {
    debugThread.resume();
  }

  @Override
  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    if (!(breakpoint instanceof DartBreakpoint)) {
      return false;
    }

    return true;
  }

  public boolean supportsSetScriptSource() {
    return canSetScriptSource;
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
    try {
      connection.close();
    } catch (IOException e) {

    }
    process.terminate();
  }

  public void writeToStdout(String message) {
    process.getStreamMonitor().messageAdded(message);
  }

  protected WebkitCallback<Boolean> createNavigateWebkitCallback(final String url) {
    return new WebkitCallback<Boolean>() {
      @Override
      public void handleResult(WebkitResult<Boolean> result) {
        // Once all other requests have been processed, then navigate to the given url.
        try {
          if (connection.isConnected()) {
            connection.getPage().navigate(url);
          }
        } catch (IOException e) {
          DartDebugCorePlugin.logError(e);
        }
      }
    };
  }

  protected DartBreakpointManager getBreakpointManager() {
    return breakpointManager;
  }

  protected IResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  protected SourceMapManager getSourceMapManager() {
    return sourceMapManager;
  }

  protected WebkitConnection getWebkitConnection() {
    return connection;
  }

  protected void handleInspectorDetached(String reason) {
    // "replaced_with_devtools", "target_closed", ...

    final String replacedWithDevTools = "replaced_with_devtools";

    if (replacedWithDevTools.equalsIgnoreCase(reason)) {
      // When the user opens the Webkit inspector our debug connection is closed.
      // We warn the user when this happens, since it otherwise isn't apparent to them
      // when the debugger connection is closing.      
      if (enableBreakpoints) {
        // Only show this message if the user launched Dartium with debugging enabled.
        disconnectMessage = "devtools disconnect";

        DebugUIHelper.getHelper().handleDevtoolsDisconnect(this);
      }
    }
  }

  protected void handleTargetCrashed() {
    process.getStreamMonitor().messageAdded("<debug target crashed>");

    try {
      terminate();
    } catch (DebugException e) {
      DartDebugCorePlugin.logInfo(e);
    }
  }

  protected boolean isJavaScriptException(List<WebkitCallFrame> frames, WebkitRemoteObject exception) {
    if (frames.size() == 0) {
      return false;
    }

    WebkitLocation location = frames.get(0).getLocation();

    WebkitScript script = getConnection().getDebugger().getScript(location.getScriptId());
    String url = script.getUrl();

    if (url.endsWith(".dart.js") || url.endsWith(".precompiled.js")) {
      return false;
    }

    return url.endsWith(".js");
  }

  protected void printExceptionToStdout(final WebkitRemoteObject exception) {
    try {
      getConnection().getRuntime().callToString(
          exception.getObjectId(),
          new WebkitCallback<String>() {
            @Override
            public void handleResult(WebkitResult<String> result) {
              if (!result.isError()) {
                String text = result.getResult();

                if (exception != null && exception.isPrimitive()) {
                  text = exception.getValue();
                }

                if (text != null) {
                  int index = text.indexOf('\n');

                  if (index != -1) {
                    text = text.substring(0, index).trim();
                  }

                  process.getStreamMonitor().messageAdded("Breaking on exception: " + text + "\n");
                }
              }
            }
          });
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }
  }

  protected boolean shouldUseSourceMapping() {
    return DartDebugCorePlugin.getPlugin().getUseSourceMaps();
  }

  /**
   * Check for the presence of Chrome extensions content scripts. It seems like many (all?) of these
   * prevent debugging from working.
   * 
   * @param script the Debugger.scriptParsed event
   * @see dartbug.com/10298
   */
  private void checkForDebuggerExtension(WebkitScript script) {
    // {"method":"Debugger.scriptParsed","params":{"startLine":0,"libraryId":0,"endLine":154,
    //   "startColumn":0,"scriptId":"26","url":"chrome-extension://ognampngfcbddbfemdapefohjiobgbdl/data_loader.js",
    //   "isContentScript":true,"endColumn":1}}

    if (script.isContentScript() && script.isChromeExtensionUrl()) {
      DartDebugCorePlugin.logWarning("Chrome extension content script detected: " + script);

      writeToStdout("WARNING: Chrome content script extension detected. Many of these extensions "
          + "interfere with the debug\nexperience, including preventing breakpoints from working. "
          + "These extensions include but are not limited\nto SpeedTracer and the WebGL inspector. "
          + "You can disable them in Dartium via Tools > Extensions.");
      writeToStdout("(content script extension: " + script.getUrl() + ")");
    }
  }

  private PauseOnExceptionsType getPauseType() {
    if (!enableBreakpoints) {
      return PauseOnExceptionsType.none;
    }

    final BreakOnExceptions boe = DartDebugCorePlugin.getPlugin().getBreakOnExceptions();
    PauseOnExceptionsType pauseType = PauseOnExceptionsType.none;

    if (boe == BreakOnExceptions.uncaught) {
      pauseType = PauseOnExceptionsType.uncaught;
    } else if (boe == BreakOnExceptions.all) {
      pauseType = PauseOnExceptionsType.all;
    }

    return pauseType;
  }

}
