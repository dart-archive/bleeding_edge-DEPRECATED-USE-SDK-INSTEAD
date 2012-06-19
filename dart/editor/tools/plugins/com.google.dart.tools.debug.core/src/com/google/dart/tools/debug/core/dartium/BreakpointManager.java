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
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.util.IResourceResolver;
import com.google.dart.tools.debug.core.webkit.WebkitBreakpoint;
import com.google.dart.tools.debug.core.webkit.WebkitCallback;
import com.google.dart.tools.debug.core.webkit.WebkitLocation;
import com.google.dart.tools.debug.core.webkit.WebkitResult;
import com.google.dart.tools.debug.core.webkit.WebkitScript;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle adding a removing breakpoints to the WebKit connection for the DartiumDebugTarget class.
 */
class BreakpointManager implements IBreakpointListener {
  private DartiumDebugTarget debugTarget;
  private IResourceResolver resourceResolver;

  private List<WebkitBreakpoint> webkitBreakpoints = new ArrayList<WebkitBreakpoint>();

  public BreakpointManager(DartiumDebugTarget debugTarget, IResourceResolver resourceResolver) {
    this.debugTarget = debugTarget;
    this.resourceResolver = resourceResolver;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    if (debugTarget.supportsBreakpoint(breakpoint)) {
      try {
        addBreakpoint((DartBreakpoint) breakpoint);
      } catch (IOException exception) {
        // TODO(devoncarew): display to the user

        DartDebugCorePlugin.logError(exception);
      }
    }
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    breakpointRemoved(breakpoint, delta);
    breakpointAdded(breakpoint);
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (debugTarget.supportsBreakpoint(breakpoint)) {
      WebkitBreakpoint webkitBreakpoint = findBreakpoint((DartBreakpoint) breakpoint);

      if (webkitBreakpoint != null) {
        try {
          debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(
              webkitBreakpoint.getBreakpointId());
        } catch (IOException exception) {
          // TODO(devoncarew): display to the user

          DartDebugCorePlugin.logError(exception);
        }

        webkitBreakpoints.remove(webkitBreakpoint);
      }
    }
  }

  public void connect() throws IOException {
    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    for (IBreakpoint breakpoint : breakpoints) {
      if (debugTarget.supportsBreakpoint(breakpoint)) {
        addBreakpoint((DartBreakpoint) breakpoint);
      }
    }

    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
  }

  public void dispose() {
    // Null check for when the editor is shutting down.
    if (DebugPlugin.getDefault() != null) {
      DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
    }
  }

  public DartBreakpoint getBreakpointFor(WebkitLocation location) {
    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    WebkitScript script = debugTarget.getWebkitConnection().getDebugger().getScript(
        location.getScriptId());

    if (script == null) {
      return null;
    }

    String url = script.getUrl();
    int line = WebkitLocation.webkitToElipseLine(location.getLineNumber());

    for (IBreakpoint bp : breakpoints) {
      if (debugTarget.supportsBreakpoint(bp)) {
        DartBreakpoint breakpoint = (DartBreakpoint) bp;

        if (breakpoint.getLine() == line) {
          String bpUrl = resourceResolver.getUrlForResource(breakpoint.getFile());

          if (bpUrl.equals(url)) {
            return breakpoint;
          }
        }
      }
    }

    return null;
  }

  void handleBreakpointResolved(WebkitBreakpoint webkitBreakpoint) {
    webkitBreakpoints.add(webkitBreakpoint);
  }

  void handleGlobalObjectCleared() {
    webkitBreakpoints.clear();
  }

  private void addBreakpoint(DartBreakpoint breakpoint) throws IOException {
    if (breakpoint.isBreakpointEnabled()) {
      String url = resourceResolver.getUrlForResource(breakpoint.getFile());
      int line = WebkitLocation.eclipseToWebkitLine(breakpoint.getLine());

      debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(
          url,
          null,
          line,
          new WebkitCallback<WebkitBreakpoint>() {
            @Override
            public void handleResult(WebkitResult<WebkitBreakpoint> result) {
              // This will resolve immediately if the script is loaded in the browser. Otherwise the 
              // breakpoint info will be sent to us using the breakpoint resolved notification.
              if (result.getResult() instanceof WebkitBreakpoint) {
                webkitBreakpoints.add(result.getResult());
              }
            }
          });
    }
  }

  /**
   * Search through the webkitBreakpoints looking for one that matches the given DartBreakpoint.
   * 
   * @param breakpoint
   * @return
   */
  private WebkitBreakpoint findBreakpoint(DartBreakpoint breakpoint) {
    String url = resourceResolver.getUrlForResource(breakpoint.getFile());
    int line = WebkitLocation.eclipseToWebkitLine(breakpoint.getLine());

    for (WebkitBreakpoint webkitBreakpoint : webkitBreakpoints) {
      String scriptId = webkitBreakpoint.getLocation().getScriptId();

      WebkitScript script = debugTarget.getWebkitConnection().getDebugger().getScript(scriptId);

      if (script != null && url.equals(script.getUrl())) {
        if (webkitBreakpoint.getLocation().getLineNumber() == line) {
          return webkitBreakpoint;
        }
      }
    }

    return null;
  }

}
