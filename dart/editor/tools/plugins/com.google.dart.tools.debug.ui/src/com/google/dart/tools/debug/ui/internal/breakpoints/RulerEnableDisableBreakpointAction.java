/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.actions.RulerBreakpointAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * The UI action class in charge of enabling and disabling breakpoints.
 */
public class RulerEnableDisableBreakpointAction extends RulerBreakpointAction implements IUpdate {

  private IBreakpoint fBreakpoint;

  /**
   * Create a new RulerEnableDisableBreakpointAction.
   * 
   * @param editor
   * @param info
   */
  public RulerEnableDisableBreakpointAction(ITextEditor editor, IVerticalRulerInfo info) {
    super(editor, info);
  }

  @Override
  public void run() {
    if (fBreakpoint != null) {
      try {
        fBreakpoint.setEnabled(!fBreakpoint.isEnabled());
      } catch (CoreException e) {
        DebugErrorHandler.errorDialog(
            getEditor().getSite().getShell(),
            Messages.breakpointAction_error,
            Messages.breakpointAction_errorTogglingBreakpoint,
            e.getStatus());
      }
    }
  }

  @Override
  public void update() {
    fBreakpoint = getBreakpoint();
    setEnabled(fBreakpoint != null);
    if (fBreakpoint != null) {
      try {
        if (fBreakpoint.isEnabled()) {
          setText(Messages.breakpointAction_disableBreakpoint);
        } else {
          setText(Messages.breakpointAction_enableBreakpoint);
        }
      } catch (CoreException e) {
      }
    } else {
      setText(Messages.breakpointAction_disableBreakpoint);
    }
  }

}
