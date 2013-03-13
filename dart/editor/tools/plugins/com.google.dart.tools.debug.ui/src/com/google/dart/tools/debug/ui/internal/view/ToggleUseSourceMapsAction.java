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

package com.google.dart.tools.debug.ui.internal.view;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;

/**
 * Toggle the source mapping feature. When off, the debugger does not try and use source maps to
 * determine the execution point.
 */
public class ToggleUseSourceMapsAction extends Action {
  private IViewPart launchView;

  public ToggleUseSourceMapsAction(IViewPart launchView) {
    super("Use Source Maps", IAction.AS_CHECK_BOX);

    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/map_file.png"));

    setChecked(DartDebugCorePlugin.getPlugin().getUseSourceMaps());

    this.launchView = launchView;
  }

  @Override
  public void run() {
    DartDebugCorePlugin.getPlugin().setUseSourceMaps(isChecked());

    // send a refresh to the UI

    ISelection sel = launchView.getSite().getSelectionProvider().getSelection();

    if (!sel.isEmpty() && sel instanceof IStructuredSelection) {
      IStructuredSelection selection = (IStructuredSelection) sel;

      Object obj = selection.getFirstElement();

      if (obj instanceof IStackFrame) {
        IStackFrame frame = (IStackFrame) obj;

        // TODO(devoncarew: this does not work great - it does not refresh the stack view
        fireEvent(new DebugEvent(frame, DebugEvent.SUSPEND, DebugEvent.BREAKPOINT));
      }
    }
  }

  private void fireEvent(DebugEvent event) {
    DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
  }

}
