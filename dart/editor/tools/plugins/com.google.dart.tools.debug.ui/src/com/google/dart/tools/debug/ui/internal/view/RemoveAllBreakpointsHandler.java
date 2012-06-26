/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.view;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * A command handler to remove all workspace breakpoints.
 */
public class RemoveAllBreakpointsHandler extends AbstractHandler implements IBreakpointListener {
  private RemoveAllBreakpointsAction action;

  /**
   * Create a new RemoveAllBreakpointsHandler.
   */
  public RemoveAllBreakpointsHandler() {
    init();
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    updateEnablement();
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    updateEnablement();
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    updateEnablement();
  }

  @Override
  public void dispose() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    breakpointManager.removeBreakpointListener(this);
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    getRemoveBreakpointsAction().run();

    return null;
  }

  protected void init() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    breakpointManager.addBreakpointListener(this);

    updateEnablement();
  }

  private RemoveAllBreakpointsAction getRemoveBreakpointsAction() {
    if (action == null) {
      action = new RemoveAllBreakpointsAction();
    }

    return action;
  }

  private void updateEnablement() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    IBreakpoint[] breakpoints = breakpointManager.getBreakpoints();

    setBaseEnabled(breakpoints.length > 0);
  }

}
