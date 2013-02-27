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

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;

import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;

import java.util.Iterator;

class OpenLocationAction extends InstrumentedSelectionDispatchAction {
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
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    if (!checkEnabled(selection)) {
      instrumentation.metric("Problem", "Open Location not enabled for selection");
      return;
    }

    for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {

      Object item = iter.next();
      recordItem(item, instrumentation);

      boolean noError = CallHierarchyUI.openInEditor(
          item,
          getShell(),
          OpenStrategy.activateOnOpen());
      if (!noError) {
        instrumentation.metric("Problem", "CallHierachyUI-OpenInEditor reported an error");
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

  private void recordItem(Object item, InstrumentationBuilder instrumentation) {
    //TODO(lukechurch): Use the data returned by this to improve targetting once it becomes clear
    //which classes this is most often called with

    instrumentation.metric("Item-Class", item.getClass().toString());
  }
}
