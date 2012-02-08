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

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

import java.io.IOException;

/**
 * Handle adding a removing breakpoints to the Webkit connection for the DartiumDebugTarget class.
 */
class BreakpointManager implements IBreakpointListener {
  private DartiumDebugTarget debugTarget;

  public BreakpointManager(DartiumDebugTarget debugTarget) {
    this.debugTarget = debugTarget;
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    if (debugTarget.supportsBreakpoint(breakpoint)) {
      try {
        addBreakpoint((DartBreakpoint) breakpoint);
      } catch (IOException exception) {
        // TODO: display to the user

        DartDebugCorePlugin.logError(exception);
      }
    }
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    // TODO:

  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    // TODO:

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
    DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
  }

  private void addBreakpoint(DartBreakpoint breakpoint) throws IOException {
    debugTarget.getWebkitConnection().getDebugger().setBreakpointByUrl(null,
        breakpoint.getUrlRegex(), breakpoint.getLine(), null);
  }

}
