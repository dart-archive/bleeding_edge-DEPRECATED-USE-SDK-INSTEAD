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
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.breakpoints.DartBreakpoint;
import com.google.dart.tools.debug.core.server.VmConnection.BreakpointResolvedCallback;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This breakpoint manager serves to off-load some of the breakpoint logic from ServerDebugTarget.
 */
class ServerBreakpointManager implements IBreakpointListener {

  private class BreakpointCallback implements BreakpointResolvedCallback {
    private DartBreakpoint dartBreakpoint;

    public BreakpointCallback(DartBreakpoint dartBreakpoint) {
      this.dartBreakpoint = dartBreakpoint;
    }

    @Override
    public void handleResolved(VmBreakpoint bp) {
      if (bp.getLocation() != null) {
        final String url = getAbsoluteUrlForResource(dartBreakpoint.getFile());
        final String pubUrl = getPubUrlForResource(dartBreakpoint.getFile());

        if (bp.getLocation().getUrl().equals(url)
            || (pubUrl != null && bp.getLocation().getUrl().equals(pubUrl))) {
          if (bp.getLocation().getLineNumber(getConnection()) != dartBreakpoint.getLine()) {
            ignoredBreakpoints.add(dartBreakpoint);

            dartBreakpoint.updateLineNumber(bp.getLocation().getLineNumber(getConnection()));
          }
        }
      }
    }
  }

  private ServerDebugTarget target;

  private VmIsolate mainIsolate;

  private List<IBreakpoint> ignoredBreakpoints = new ArrayList<IBreakpoint>();

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
      VmBreakpoint vmBreakpoint = findBreakpoint((DartBreakpoint) breakpoint);
      VmBreakpoint pubBreakoint = findPubBreakpoint((DartBreakpoint) breakpoint);

      try {
        if (vmBreakpoint != null) {
          getConnection().removeBreakpoint(mainIsolate, vmBreakpoint);
        }

        if (pubBreakoint != null) {
          getConnection().removeBreakpoint(mainIsolate, pubBreakoint);
        }
      } catch (IOException exception) {
        DartDebugCorePlugin.logError(exception);
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

  private void addBreakpoint(VmIsolate isolate, DartBreakpoint breakpoint) {
    if (breakpoint.isBreakpointEnabled()) {
      String url = getAbsoluteUrlForResource(breakpoint.getFile());
      int line = breakpoint.getLine();

      try {
        getConnection().setBreakpointSync(isolate, url, line, new BreakpointCallback(breakpoint));

        url = getPubUrlForResource(breakpoint.getFile());

        if (url != null) {
          getConnection().setBreakpointSync(isolate, url, line, new BreakpointCallback(breakpoint));
        }
      } catch (IOException exception) {
        DartDebugCorePlugin.logError(exception);
      }
    }
  }

  private VmBreakpoint findBreakpoint(DartBreakpoint breakpoint) {
    final String url = getAbsoluteUrlForResource(breakpoint.getFile());
    final int line = breakpoint.getLine();

    for (VmBreakpoint bp : getConnection().getBreakpoints()) {
      if (line == bp.getLocation().getLineNumber(getConnection())
          && NetUtils.compareUrls(url, bp.getLocation().getUrl())) {
        return bp;
      }
    }
    return null;
  }

  private VmBreakpoint findPubBreakpoint(DartBreakpoint breakpoint) {
    final int line = breakpoint.getLine();
    final String pubUrl = getPubUrlForResource(breakpoint.getFile());

    if (pubUrl != null) {
      for (VmBreakpoint bp : getConnection().getBreakpoints()) {
        if (line == bp.getLocation().getLineNumber(getConnection())
            && NetUtils.compareUrls(pubUrl, bp.getLocation().getUrl())) {
          return bp;
        }
      }
    }
    return null;
  }

  private String getAbsoluteUrlForResource(IFile file) {
    return file.getLocation().toFile().toURI().toString();
  }

  private VmConnection getConnection() {
    return target.getConnection();
  }

  private String getPubUrlForResource(IFile file) {
    String locationUrl = file.getLocation().toFile().toURI().toString();

    int index = locationUrl.indexOf(DartCore.PACKAGES_DIRECTORY_PATH);

    if (index != -1) {
      locationUrl = DartCore.PACKAGE_SCHEME_SPEC
          + locationUrl.substring(index + DartCore.PACKAGES_DIRECTORY_PATH.length());

      return locationUrl;
    }

    index = locationUrl.lastIndexOf(DartCore.LIB_DIRECTORY_PATH);

    if (index != -1) {
      String path = file.getLocation().toString();
      path = path.substring(0, path.lastIndexOf(DartCore.LIB_DIRECTORY_PATH));
      File packagesDir = new File(path, DartCore.PACKAGES_DIRECTORY_NAME);

      if (packagesDir.exists()) {
        String packageName = DartCore.getSelfLinkedPackageName(file);

        if (packageName != null) {
          locationUrl = DartCore.PACKAGE_SCHEME_SPEC + packageName + File.separator
              + locationUrl.substring(index + DartCore.LIB_DIRECTORY_PATH.length());

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
