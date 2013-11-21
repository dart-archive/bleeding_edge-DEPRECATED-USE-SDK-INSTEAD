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
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.objectinspector.ObjectInspectorView;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * An action to show the object inspector view.
 */
@SuppressWarnings("restriction")
public class ShowInspectorAction extends Action {
  private IDetailPaneContainer selectionProvider;

  public ShowInspectorAction() {
    super("Show Object Inspector", DartDebugUIPlugin.getImageDescriptor("obj16/value_show.gif"));
  }

  @Override
  public synchronized void run() {
    try {
      IStructuredSelection sel = selectionProvider.getCurrentSelection();

      if (sel != null && !sel.isEmpty()) {
        Object obj = sel.getFirstElement();

        if (obj instanceof IVariable) {
          try {
            obj = ((IVariable) obj).getValue();
          } catch (DebugException e) {

          }
        }

        if (obj instanceof IValue) {
          ObjectInspectorView.inspect((IValue) obj);
          return;
        }
      }

      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(
          "com.google.dart.tools.debug.objectInspectorView");
    } catch (PartInitException e) {
      DartUtil.logError(e);
    }
  }

  public void setSelectionProvider(IDetailPaneContainer selectionProvider) {
    this.selectionProvider = selectionProvider;
  }
}
