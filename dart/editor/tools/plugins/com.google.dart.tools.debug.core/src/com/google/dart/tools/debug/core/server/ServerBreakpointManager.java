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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartDebugCorePlugin.BreakOnExceptions;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.server.VmConnection.BreakOnExceptionsType;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: handle removing deleted breakpoints

/**
 * This breakpoint manager serves to off-load some of the breakpoint logic from ServerDebugTarget.
 */
class ServerBreakpointManager implements IBreakpointListener {
  private class SetBreakpointHelper {
    final int NUM_URLS = 2;

    final VmIsolate isolate;
    final DartBreakpoint breakpoint;
    final int line;
    final AtomicInteger numErrors = new AtomicInteger();

    public SetBreakpointHelper(VmIsolate isolate, DartBreakpoint breakpoint, int line) {
      this.isolate = isolate;
      this.breakpoint = breakpoint;
      this.line = line;
    }

    void setBreakpoint(String url) throws IOException {
      // fail if no URL
      if (url == null) {
        if (numErrors.incrementAndGet() == NUM_URLS) {
          target.writeToStdout("No valid URL for: " + breakpoint);
        }
        return;
      }
      // try to set
      connection.setBreakpoint(isolate, url, line, new VmCallback<VmBreakpoint>() {
        @Override
        public void handleResult(VmResult<VmBreakpoint> result) {
          if (result.isError()) {
            if (numErrors.incrementAndGet() == NUM_URLS) {
              String error = result.getError();
              target.writeToStdout(error);
            }
          } else {
            addCreatedBreakpoint(breakpoint, result.getResult());
          }
        }
      });
    }
  }

  private final ServerDebugTarget target;
  private final VmConnection connection;

  private VmIsolate mainIsolate;

  private List<IBreakpoint> ignoredBreakpoints = new ArrayList<IBreakpoint>();

  Map<IBreakpoint, List<VmBreakpoint>> createdBreakpoints = new HashMap<IBreakpoint, List<VmBreakpoint>>();

  List<VmIsolate> liveIsolates = new ArrayList<VmIsolate>();

  public ServerBreakpointManager(ServerDebugTarget target) {
    this.target = target;
    this.connection = target.getConnection();
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
            connection.removeBreakpoint(bp.getIsolate(), bp);
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
    String locationUrl = location.getUrl();
    if (locationUrl == null) {
      return false;
    }
    // prepare line (requires lines table)
    int locationLine = location.getLineNumber(connection);
    // check every breakpoint's line + file
    for (IBreakpoint _bp : createdBreakpoints.keySet()) {
      if (_bp instanceof DartBreakpoint) {
        DartBreakpoint bp = (DartBreakpoint) _bp;
        if (bp.getLine() == locationLine) {
          IFile bpFile = bp.getFile();
          if (bpFile != null) {
            URI bpUri = bpFile.getLocationURI();
            String bpUriString = bpUri.toString();
            if (bpUri != null && bpUriString.equals(locationUrl)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public void setPauseOnExceptionForLiveIsolates() {
    BreakOnExceptionsType pauseType = getPauseType();
    for (VmIsolate isolate : liveIsolates) {
      try {
        connection.setPauseOnException(isolate, pauseType);
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);
      }
    }
  }

  public void setPauseOnExceptionSync(VmIsolate isolate) {
    BreakOnExceptionsType pauseType = getPauseType();
    try {
      connection.setPauseOnExceptionSync(isolate, pauseType);
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
    }
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
      int lineNo = bp.getLocation().getLineNumber(connection);

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
      VmInterruptResult interruptResult = pause ? connection.interruptConditionally(isolate)
          : VmInterruptResult.createNoopResult(connection);

      for (DartBreakpoint breakpoint : breakpoints) {
        if (breakpoint.isBreakpointEnabled()) {
          int line = breakpoint.getLine();
          SetBreakpointHelper helper = new SetBreakpointHelper(isolate, breakpoint, line);
          // file path
          String filePath = breakpoint.getActualFilePath();
          if (filePath == null) {
            continue;
          }
          // try package: URL
          {
            String url = getPackagesUrlForFilePath(filePath);
            helper.setBreakpoint(url);
          }
          // try file:// URL
          {
            String url = getAbsoluteUrlForFilePath(filePath);
            helper.setBreakpoint(url);
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
      VmInterruptResult interruptResult = connection.interruptConditionally(isolate);

      connection.enableAllSteppingSync(isolate);

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

  private String getAbsoluteUrlForFilePath(String filePath) {
    return new File(filePath).toURI().toString();
  }

  private String getPackagesUrlForFilePath(String filePath) {
    return target.getUriToFileResolver().getUriForPath(filePath);
  }

  private BreakOnExceptionsType getPauseType() {
    BreakOnExceptions boe = DartDebugCorePlugin.getPlugin().getBreakOnExceptions();
    BreakOnExceptionsType pauseType = BreakOnExceptionsType.none;

    if (boe == BreakOnExceptions.uncaught) {
      pauseType = BreakOnExceptionsType.unhandled;
    } else if (boe == BreakOnExceptions.all) {
      pauseType = BreakOnExceptionsType.all;
    }

    return pauseType;
  }

  private boolean supportsBreakpoint(IBreakpoint breakpoint) {
    return target.supportsBreakpoint(breakpoint);
  }
}
