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
import org.eclipse.debug.core.model.ILineBreakpoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

// TODO: handle removing deleted breakpoints

/**
 * This breakpoint manager serves to off-load some of the breakpoint logic from ServerDebugTarget.
 */
class ServerBreakpointManager implements IBreakpointListener {
  private ServerDebugTarget target;

  private VmIsolate mainIsolate;

  private List<IBreakpoint> ignoredBreakpoints = new ArrayList<IBreakpoint>();

  Map<IBreakpoint, List<VmBreakpoint>> createdBreakpoints = new HashMap<IBreakpoint, List<VmBreakpoint>>();

  List<VmIsolate> liveIsolates = new ArrayList<VmIsolate>();

  public ServerBreakpointManager(ServerDebugTarget target) {
    this.target = target;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    if (supportsBreakpoint(breakpoint)) {
      addBreakpoints(Arrays.asList((DartBreakpoint) breakpoint));
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
      List<VmBreakpoint> breakpoints = createdBreakpoints.remove(breakpoint);
      if (breakpoints != null) {
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

    liveIsolates.add(mainIsolate);

    // Set up the existing breakpoints.
    addBreakpoints(mainIsolate, getSupportedBreakpoints(), true);

    DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
  }

  public void dispose() {
    if (DebugPlugin.getDefault() != null) {
      DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
    }
  }

  public void handleIsolateCreated(VmIsolate isolate) {
    if (mainIsolate == null) {
      return;
    }

    if (!liveIsolates.contains(isolate)) {
      connectToIsolate(isolate);

      liveIsolates.add(isolate);
    }
  }

  public void handleIsolateShutdown(VmIsolate isolate) {
    liveIsolates.remove(isolate);

    disconnectFromIsolate(isolate);
  }

  public boolean hasBreakpointAtLine(VmLocation location) {
    VmConnection connection = getConnection();
    int locationLine = location.getLineNumber(connection);
    for (IBreakpoint bp : createdBreakpoints.keySet()) {
      if (bp instanceof ILineBreakpoint) {
        ILineBreakpoint lineBreakpoint = (ILineBreakpoint) bp;
        try {
          if (lineBreakpoint.getLineNumber() == locationLine) {
            return true;
          }
        } catch (Throwable e) {
        }
      }
    }
    return false;
  }

  protected void addCreatedBreakpoint(DartBreakpoint breakpoint, VmBreakpoint result) {
    List<VmBreakpoint> breakpoints = createdBreakpoints.get(breakpoint);
    if (breakpoints == null) {
      breakpoints = new ArrayList<VmBreakpoint>();
      createdBreakpoints.put(breakpoint, breakpoints);
    }
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

    if (dartBreakpoint != null && isolate == mainIsolate) {
      int lineNo = bp.getLocation().getLineNumber(getConnection());

      if (lineNo != dartBreakpoint.getLine()) {
        ignoredBreakpoints.add(dartBreakpoint);

        String message = "[breakpoint in " + dartBreakpoint.getName() + " moved from line "
            + dartBreakpoint.getLine() + " to " + lineNo + "]";
        target.writeToStdout(message);

        dartBreakpoint.updateLineNumber(lineNo);
      }
    }
  }

  List<DartBreakpoint> getSupportedBreakpoints() {
    IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
        DartDebugCorePlugin.DEBUG_MODEL_ID);

    List<DartBreakpoint> bps = new ArrayList<DartBreakpoint>();

    for (IBreakpoint breakpoint : breakpoints) {
      if (target.supportsBreakpoint(breakpoint)) {
        bps.add((DartBreakpoint) breakpoint);
      }
    }

    return bps;
  }

  private void addBreakpoints(List<DartBreakpoint> breakpoints) {
    for (VmIsolate isolate : liveIsolates) {
      addBreakpoints(isolate, breakpoints, true);
    }
  }

  private void addBreakpoints(VmIsolate isolate, final List<DartBreakpoint> breakpoints,
      boolean pause) {
    boolean enabled = false;

    for (DartBreakpoint bp : breakpoints) {
      enabled |= bp.isBreakpointEnabled();
    }

    if (!enabled) {
      return;
    }

    try {
      VmInterruptResult interruptResult = pause ? getConnection().interruptConditionally(isolate)
          : VmInterruptResult.createNoopResult(getConnection());

      for (final DartBreakpoint breakpoint : breakpoints) {
        if (breakpoint.isBreakpointEnabled()) {
          IFile file = breakpoint.getFile();
          String url = null;

          if (file != null) {
            url = getAbsoluteUrlForResource(file);
          }

          int line = breakpoint.getLine();

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

          if (file != null) {
            url = getPackagesUrlForResource(file);
          }

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
        }
      }

      interruptResult.resume();
    } catch (IOException exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

  private void connectToIsolate(VmIsolate isolate) {
    try {
      VmInterruptResult interruptResult = getConnection().interruptConditionally(isolate);

      getConnection().enableAllSteppingSync(isolate);

      // Set all existing breakpoints on the new isolate.
      addBreakpoints(isolate, getSupportedBreakpoints(), false);

      interruptResult.resume();
    } catch (IOException exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

  private void disconnectFromIsolate(VmIsolate isolate) {
    for (Entry<IBreakpoint, List<VmBreakpoint>> entry : createdBreakpoints.entrySet()) {
      List<VmBreakpoint> breakpoints = entry.getValue();
      for (Iterator<VmBreakpoint> bpIter = breakpoints.iterator(); bpIter.hasNext();) {
        VmBreakpoint breakpoint = bpIter.next();
        if (breakpoint.getIsolate() == isolate) {
          bpIter.remove();
        }
      }
    }
  }

  private String getAbsoluteUrlForResource(IFile file) {
    return file.getLocation().toFile().toURI().toString();
  }

  private VmConnection getConnection() {
    return target.getConnection();
  }

  private String getPackagesUrlForResource(IFile file) {
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
