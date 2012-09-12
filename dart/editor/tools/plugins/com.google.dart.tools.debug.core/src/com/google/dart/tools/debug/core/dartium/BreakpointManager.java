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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handle adding a removing breakpoints to the WebKit connection for the DartiumDebugTarget class.
 */
class BreakpointManager implements IBreakpointListener {
  private DartiumDebugTarget debugTarget;
  private IResourceResolver resourceResolver;

  /** Maps from webkit breakpointIds to DartBreakpoints. */
  private Map<String, DartBreakpoint> breakpointMap = new HashMap<String, DartBreakpoint>();

  private List<WebkitBreakpoint> webkitBreakpoints = new ArrayList<WebkitBreakpoint>();

  private List<IBreakpoint> ignoredBreakpoints = new ArrayList<IBreakpoint>();

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
        if (!debugTarget.isTerminated()) {
          DartDebugCorePlugin.logError(exception);
        }
      }
    }
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (ignoredBreakpoints.contains(breakpoint)) {
      ignoredBreakpoints.remove(breakpoint);
      return;
    }

    if (debugTarget.supportsBreakpoint(breakpoint)) {
      breakpointRemoved(breakpoint, delta);
      breakpointAdded(breakpoint);
    }
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (debugTarget.supportsBreakpoint(breakpoint)) {
      String breakpointId = getWebkitId((DartBreakpoint) breakpoint);

      if (breakpointId != null) {
        try {
          debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(breakpointId);
        } catch (IOException exception) {
          if (!debugTarget.isTerminated()) {
            DartDebugCorePlugin.logError(exception);
          }
        }

        WebkitBreakpoint bp = findBreakpoint(breakpointId);

        if (bp != null) {
          webkitBreakpoints.remove(bp);
        }
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

  public void dispose(boolean deleteAll) {
    // Null check for when the editor is shutting down.
    if (DebugPlugin.getDefault() != null) {
      if (deleteAll) {
        try {
          for (String breakpointId : breakpointMap.keySet()) {
            debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(breakpointId);
          }
        } catch (IOException exception) {
          if (!debugTarget.isTerminated()) {
            DartDebugCorePlugin.logError(exception);
          }
        }
      }

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

    DartBreakpoint breakpoint = breakpointMap.get(webkitBreakpoint.getBreakpointId());

    if (breakpoint != null) {
      int eclipseLine = WebkitLocation.webkitToElipseLine(webkitBreakpoint.getLocation().getLineNumber());

      if (breakpoint.getLine() != eclipseLine) {
        ignoredBreakpoints.add(breakpoint);

        breakpoint.updateLineNumber(eclipseLine);
      }
    }
  }

  void handleGlobalObjectCleared() {
    webkitBreakpoints.clear();
  }

  private void addBreakpoint(final DartBreakpoint breakpoint) throws IOException {
    if (breakpoint.isBreakpointEnabled()) {
      // String url = resourceResolver.getUrlForResource(breakpoint.getFile());
      String regex = breakpoint.getFile().getFullPath().toPortableString();

      int line = WebkitLocation.eclipseToWebkitLine(breakpoint.getLine());

      debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(
          null,
          regex,
          line,
          new WebkitCallback<String>() {
            @Override
            public void handleResult(WebkitResult<String> result) {
              if (!result.isError()) {
                breakpointMap.put(result.getResult(), breakpoint);
              }
            }
          });
    }
  }

  private WebkitBreakpoint findBreakpoint(String breakpointId) {
    for (WebkitBreakpoint bp : webkitBreakpoints) {
      if (breakpointId.equals(bp.getBreakpointId())) {
        return bp;
      }
    }

    return null;
  }

  private String getWebkitId(DartBreakpoint breakpoint) {
    for (String breakpointId : breakpointMap.keySet()) {
      if (breakpointMap.get(breakpointId) == breakpoint) {
        return breakpointId;
      }
    }

    return null;
  }

}
