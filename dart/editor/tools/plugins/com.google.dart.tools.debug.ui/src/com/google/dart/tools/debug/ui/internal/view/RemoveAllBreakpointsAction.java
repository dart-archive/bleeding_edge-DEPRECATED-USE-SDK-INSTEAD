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

package com.google.dart.tools.debug.ui.internal.view;

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;

/**
 * An action to remove all breakpoints in the workspace.
 */
public class RemoveAllBreakpointsAction extends Action implements IBreakpointListener {

  public RemoveAllBreakpointsAction() {
    super("Remove All Breakpoints");

    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/rem_all_brk.gif"));

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
  public void run() {
    final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

    try {
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(
          false,
          false,
          new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException,
                InterruptedException {
              deleteBreakpoints(shell, monitor);
            }
          });
    } catch (InvocationTargetException e) {
      DartUtil.logError(e);
    } catch (InterruptedException e) {
      DartUtil.logError(e);
    }
  }

  protected void deleteBreakpoints(Shell shell, IProgressMonitor monitor) {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    IBreakpoint[] breakpoints = breakpointManager.getBreakpoints();

    if (breakpoints.length > 0) {
      try {
        DebugUITools.deleteBreakpoints(breakpoints, shell, monitor);
      } catch (CoreException exception) {
        DebugErrorHandler.errorDialog(
            shell,
            "Error Deleting Breakpoints",
            exception.toString(),
            exception);
      }
    }
  }

  protected void dispose() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    breakpointManager.removeBreakpointListener(this);
  }

  protected void init() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    breakpointManager.addBreakpointListener(this);

    updateEnablement();
  }

  private void updateEnablement() {
    IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
    IBreakpoint[] breakpoints = breakpointManager.getBreakpoints();

    setEnabled(breakpoints.length > 0);
  }

}
