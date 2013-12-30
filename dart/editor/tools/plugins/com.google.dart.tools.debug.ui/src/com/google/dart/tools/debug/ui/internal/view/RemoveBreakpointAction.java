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

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * A selection dependent action to remove a breakpoint.
 */
public class RemoveBreakpointAction extends SelectionProviderAction {
  private IBreakpoint breakpoint;

  protected RemoveBreakpointAction(ISelectionProvider provider) {
    super(provider, "Remove Breakpoint");

    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/rem_co.gif"));
    setEnabled(false);
  }

  @Override
  public void run() {
    if (breakpoint != null) {
      try {
        breakpoint.delete();
      } catch (CoreException e) {
        DartDebugUIPlugin.logError(e);
      }
    }
  }

  @Override
  public void selectionChanged(IStructuredSelection sel) {
    if (sel.size() == 1) {
      breakpoint = (IBreakpoint) DebugPlugin.getAdapter(sel.getFirstElement(), IBreakpoint.class);
    } else {
      breakpoint = null;
    }

    setEnabled(breakpoint != null);
  }
}
