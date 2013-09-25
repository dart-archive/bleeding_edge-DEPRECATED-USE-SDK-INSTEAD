/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This breakpoint manager serves to off-load some of the breakpoint logic from ServerDebugTarget.
 */
class ServerBreakpointManager implements IBreakpointListener {
  private ServerDebugTarget target;

  private VmIsolate mainIsolate;

  private List<IBreakpoint> ignoredBreakpoints = new ArrayList<IBreakpoint>();

  Map<IBreakpoint, List<VmBreakpoint>> createdBreakpoints = new HashMap<IBreakpoint, List<VmBreakpoint>>();

  public ServerBreakpointManager(ServerDebugTarget target) {
    this.target = target;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    if (supportsBreakpoint(breakpoint)) {
      addBreakpoint(mainIsolate, (DartBreakpoint) breakpoint);
    }
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (ignoredBreakpoints.contains(breakpoint)) {
      ignoredBreakpoints.remove(breakpoint);
      return;
    }

    if (supportsBreakpoint(breakpoint)) {
      breakpointRemoved(breakpoint, delta);
      breakpointAdded(breakpoint);
    }
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    if (supportsBreakpoint(breakpoint)) {
      List<VmBreakpoint> breakpoints = createdBreakpoints.get(breakpoint);

      if (breakpoints != null) {
        createdBreakpoints.remove(breakpoint);

        try {
          for (VmBreakpoint bp : breakpoints) {
            getConnection().removeBreakpoint(bp.getIsolate(), bp);
          }
        } catch (IOException exception) {
          DartDebugCorePlugin.logError(exception);
        }
      }
    }
  }

  public void connect(VmIsolate mainIsolate) {
    this.mainIsolate = mainIsolate;

    // Set up the existing breakpoints.
    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    for (IBreakpoint breakpoint : breakpoints) {
      if (target.supportsBreakpoint(breakpoint)) {
        addBreakpoint(mainIsolate, (DartBreakpoint) breakpoint);
      }
    }

    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
  }

  public void dispose() {
    if (DebugPlugin.getDefault() != null) {
      DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
    }
  }

  protected void addCreatedBreakpoint(DartBreakpoint breakpoint, VmBreakpoint result) {
    if (!createdBreakpoints.containsKey(breakpoint)) {
      createdBreakpoints.put(breakpoint, new ArrayList<VmBreakpoint>());
    }

    List<VmBreakpoint> breakpoints = createdBreakpoints.get(breakpoint);
    breakpoints.add(result);
  }

  protected DartBreakpoint getDartBreakpointFor(VmBreakpoint bp) {
    for (IBreakpoint dartBreakpoint : createdBreakpoints.keySet()) {
      List<VmBreakpoint> bps = createdBreakpoints.get(dartBreakpoint);

      if (bps != null && bps.contains(bp)) {
        return (DartBreakpoint) dartBreakpoint;
      }
    }

    return null;
  }

  protected void handleBreakpointResolved(VmIsolate isolate, VmBreakpoint bp) {
    // Update the corresponding editor breakpoint if the location has changed.
    DartBreakpoint dartBreakpoint = getDartBreakpointFor(bp);

    if (dartBreakpoint != null) {
      int lineNo = bp.getLocation().getLineNumber(getConnection());
      if (lineNo != dartBreakpoint.getLine()) {
        ignoredBreakpoints.add(dartBreakpoint);

        String message = "[breakpoint in " + dartBreakpoint.getFile().getName()
            + " moved from line " + dartBreakpoint.getLine() + " to " + lineNo + "]";
        target.writeToStdout(message);

        dartBreakpoint.updateLineNumber(lineNo);
      }
    }
  }

  private void addBreakpoint(VmIsolate isolate, final DartBreakpoint breakpoint) {
    if (breakpoint.isBreakpointEnabled()) {
      String url = getAbsoluteUrlForResource(breakpoint.getFile());
      int line = breakpoint.getLine();

      try {
        VmInterruptResult interruptResult = getConnection().interruptConditionally(isolate);

        getConnection().setBreakpoint(isolate, url, line, new VmCallback<VmBreakpoint>() {
          @Override
          public void handleResult(VmResult<VmBreakpoint> result) {
            if (!result.isError()) {
              addCreatedBreakpoint(breakpoint, result.getResult());
            }
          }
        });

        url = getPubUrlForResource(breakpoint.getFile());

        if (url != null) {
          getConnection().setBreakpoint(isolate, url, line, new VmCallback<VmBreakpoint>() {
            @Override
            public void handleResult(VmResult<VmBreakpoint> result) {
              if (!result.isError()) {
                addCreatedBreakpoint(breakpoint, result.getResult());
              }
            }
          });
        }

        interruptResult.resume();
      } catch (IOException exception) {
        DartDebugCorePlugin.logError(exception);
      }
    }
  }

  private String getAbsoluteUrlForResource(IFile file) {
    return file.getLocation().toFile().toURI().toString();
  }

  private VmConnection getConnection() {
    return target.getConnection();
  }

  private String getPubUrlForResource(IFile file) {
    String locationUrl = file.getLocation().toFile().toURI().toString();

    int index = locationUrl.indexOf(DartCore.PACKAGES_DIRECTORY_URL);

    if (index != -1) {
      locationUrl = DartCore.PACKAGE_SCHEME_SPEC
          + locationUrl.substring(index + DartCore.PACKAGES_DIRECTORY_URL.length());

      return locationUrl;
    }

    index = locationUrl.lastIndexOf(DartCore.LIB_URL_PATH);

    if (index != -1) {
      String path = file.getLocation().toString();
      path = path.substring(0, path.lastIndexOf(DartCore.LIB_URL_PATH));
      File packagesDir = new File(path, DartCore.PACKAGES_DIRECTORY_NAME);

      if (packagesDir.exists()) {
        String packageName = DartCore.getSelfLinkedPackageName(file);

        if (packageName != null) {
          locationUrl = DartCore.PACKAGE_SCHEME_SPEC + packageName + "/"
              + locationUrl.substring(index + DartCore.LIB_URL_PATH.length());

          return locationUrl;
        }
      }
    }

    return null;
  }

  private boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return target.supportsBreakpoint(breakpoint);
  }

}
