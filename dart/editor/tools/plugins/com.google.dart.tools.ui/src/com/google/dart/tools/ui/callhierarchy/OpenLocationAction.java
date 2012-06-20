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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.ui.actions.SelectionDispatchAction;
import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;

import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkbenchSite;

import java.util.Iterator;

class OpenLocationAction extends SelectionDispatchAction {
  private CallHierarchyViewPart chvPart;

  public OpenLocationAction(CallHierarchyViewPart part, IWorkbenchSite site) {
    super(site);
    chvPart = part;
    LocationViewer viewer = chvPart.getLocationViewer();
    setText(CallHierarchyMessages.OpenLocationAction_label);
    setToolTipText(CallHierarchyMessages.OpenLocationAction_tooltip);
    setEnabled(!chvPart.getSelection().isEmpty());

    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        setEnabled(!event.getSelection().isEmpty());
      }
    });
  }

  @Override
  public ISelection getSelection() {
    return chvPart.getSelection();
  }

  @Override
  public void run(IStructuredSelection selection) {
    if (!checkEnabled(selection)) {
      return;
    }

    for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
      boolean noError = CallHierarchyUI.openInEditor(
          iter.next(),
          getShell(),
          OpenStrategy.activateOnOpen());
      if (!noError) {
        return;
      }
    }
  }

  private boolean checkEnabled(IStructuredSelection selection) {
    if (selection.isEmpty()) {
      return false;
    }

    for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
      Object element = iter.next();

      if (element instanceof MethodWrapper) {
        continue;
      } else if (element instanceof CallLocation) {
        continue;
      }

      return false;
    }

    return true;
  }
}
