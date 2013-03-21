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

import com.google.dart.tools.core.DartCore;
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

  private static String PACKAGES_DIRECTORY_PATH = "/packages/";
  private static String LIB_DIRECTORY_PATH = "/lib/";

  private DartiumDebugTarget debugTarget;
  private IResourceResolver resourceResolver;

  private Map<IBreakpoint, List<String>> breakpointToIdMap = new HashMap<IBreakpoint, List<String>>();
  private Map<String, DartBreakpoint> breakpointsToUpdateMap = new HashMap<String, DartBreakpoint>();

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
    // We generate this change event in the handleBreakpointResolved() method - ignore one
    // instance of the event.
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
      List<String> breakpointIds = breakpointToIdMap.get(breakpoint);

      if (breakpointIds != null) {
        for (String breakpointId : breakpointIds) {
          breakpointsToUpdateMap.remove(breakpointId);

          try {
            debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(breakpointId);
          } catch (IOException exception) {
            if (!debugTarget.isTerminated()) {
              DartDebugCorePlugin.logError(exception);
            }
          }
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
          for (List<String> ids : breakpointToIdMap.values()) {
            if (ids != null) {
              for (String id : ids) {
                debugTarget.getWebkitConnection().getDebugger().removeBreakpoint(id);
              }
            }
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

  void addToBreakpointMap(IBreakpoint breakpoint, String id, boolean trackChanges) {
    synchronized (breakpointToIdMap) {
      if (breakpointToIdMap.get(breakpoint) == null) {
        breakpointToIdMap.put(breakpoint, new ArrayList<String>());
      }

      breakpointToIdMap.get(breakpoint).add(id);

      if (trackChanges) {
        breakpointsToUpdateMap.put(id, (DartBreakpoint) breakpoint);
      }
    }
  }

  void handleBreakpointResolved(WebkitBreakpoint webkitBreakpoint) {
    DartBreakpoint breakpoint = breakpointsToUpdateMap.get(webkitBreakpoint.getBreakpointId());

    if (breakpoint != null) {
      int eclipseLine = WebkitLocation.webkitToElipseLine(webkitBreakpoint.getLocation().getLineNumber());

      if (breakpoint.getLine() != eclipseLine) {
        ignoredBreakpoints.add(breakpoint);

        breakpoint.updateLineNumber(eclipseLine);
      }
    }
  }

  void handleGlobalObjectCleared() {

  }

  private void addBreakpoint(final DartBreakpoint breakpoint) throws IOException {
    if (breakpoint.isBreakpointEnabled()) {
      String regex = resourceResolver.getUrlRegexForResource(breakpoint.getFile());

      int packagesIndex = regex.indexOf(PACKAGES_DIRECTORY_PATH);
      int libIndex = regex.indexOf(LIB_DIRECTORY_PATH);

      if (packagesIndex != -1) {
        regex = regex.substring(packagesIndex);
      } else if (libIndex != -1) {
        // check if source is located in the "lib" directory and if there is a link to it from the 
        // packages directory breakpoint should be /packages/...
        String packageName = DartCore.getSelfLinkedPackageName(breakpoint.getFile());

        if (packageName != null) {
          regex = PACKAGES_DIRECTORY_PATH + packageName + "/"
              + regex.substring(libIndex + LIB_DIRECTORY_PATH.length());
        }
      }

      int line = WebkitLocation.eclipseToWebkitLine(breakpoint.getLine());

      debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(
          null,
          regex,
          line,
          -1,
          new WebkitCallback<String>() {
            @Override
            public void handleResult(WebkitResult<String> result) {
              if (!result.isError()) {
                addToBreakpointMap(breakpoint, result.getResult(), true);
              }
            }
          });

      // Handle source mapped breakpoints - set an additional breakpoint if the file is under
      // source mapping.
      SourceMapManager sourceMapManager = debugTarget.getSourceMapManager();

      if (sourceMapManager.isMapTarget(breakpoint.getFile())) {
        SourceMapManager.SourceLocation location = sourceMapManager.getReverseMappingFor(
            breakpoint.getFile(),
            line);

        if (location != null) {
          String mappedRegex = resourceResolver.getUrlRegexForResource(location.getFile());

          if (DartDebugCorePlugin.LOGGING) {
            System.out.println("breakpoint [" + regex + "," + line + ",-1] ==> mapped to ["
                + mappedRegex + "," + location.getLine() + "," + location.getColumn() + "]");
          }

          debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(
              null,
              mappedRegex,
              location.getLine(),
              location.getColumn(),
              new WebkitCallback<String>() {
                @Override
                public void handleResult(WebkitResult<String> result) {
                  if (!result.isError()) {
                    addToBreakpointMap(breakpoint, result.getResult(), false);
                  }
                }
              });
        }
      }
    }
  }
}
