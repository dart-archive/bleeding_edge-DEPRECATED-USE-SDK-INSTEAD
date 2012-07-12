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

package com.google.dart.tools.debug.ui.internal.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.actions.breakpoints.RetargetBreakpointAction;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

@SuppressWarnings("restriction")
public class RetargetToggleBreakpointAction extends RetargetBreakpointAction {

  @Override
  protected boolean canPerformAction(Object target, ISelection selection, IWorkbenchPart part) {
    if (target instanceof IToggleBreakpointsTargetExtension) {
      IToggleBreakpointsTargetExtension ext = (IToggleBreakpointsTargetExtension) target;
      return ext.canToggleBreakpoints(part, selection);
    } else {
      return ((IToggleBreakpointsTarget) target).canToggleLineBreakpoints(part, selection);
    }
  }

  @Override
  protected String getOperationUnavailableMessage() {
    return "The operation is unavailable on the current selection. Please place the cursor in a valid location for a breakpoint.";
  }

  @Override
  protected void performAction(Object target, ISelection selection, IWorkbenchPart part)
      throws CoreException {
    if (target instanceof IToggleBreakpointsTargetExtension) {
      IToggleBreakpointsTargetExtension ext = (IToggleBreakpointsTargetExtension) target;
      ext.toggleBreakpoints(part, selection);
    } else {
      ((IToggleBreakpointsTarget) target).toggleLineBreakpoints(part, selection);
    }
  }

}
