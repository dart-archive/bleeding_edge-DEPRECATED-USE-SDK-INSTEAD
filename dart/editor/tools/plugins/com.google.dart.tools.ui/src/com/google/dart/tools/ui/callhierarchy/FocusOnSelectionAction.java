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

import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

class FocusOnSelectionAction extends Action {
  private CallHierarchyViewPart chvPart;

  public FocusOnSelectionAction(CallHierarchyViewPart part) {
    super(CallHierarchyMessages.FocusOnSelectionAction_focusOnSelection_text);
    chvPart = part;
    setDescription(CallHierarchyMessages.FocusOnSelectionAction_focusOnSelection_description);
    setToolTipText(CallHierarchyMessages.FocusOnSelectionAction_focusOnSelection_tooltip);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_FOCUS_ON_SELECTION_ACTION);
  }

  public boolean canActionBeAdded() {
    CompilationUnitElement[] members = getSelectedInputElements();

    if (members != null) {
      if (members.length == 1) {
        setText(Messages.format(
            CallHierarchyMessages.FocusOnSelectionAction_focusOn_text,
            BasicElementLabels.getElementName(members[0].getElementName())));
      } else {
        setText(CallHierarchyMessages.FocusOnSelectionAction_focusOn_selected);
      }
      return true;

    } else {
      return false;
    }
  }

  @Override
  public void run() {
    CompilationUnitElement[] members = getSelectedInputElements();
    if (members != null) {
      chvPart.setInputElements(members);
    }
  }

  private CompilationUnitElement[] getSelectedInputElements() {
    ISelection selection = getSelection();
    if (selection instanceof IStructuredSelection) {
      Object[] elements = ((IStructuredSelection) selection).toArray();
      CompilationUnitElement[] members = new CompilationUnitElement[elements.length];
      for (int i = 0; i < elements.length; i++) {
        Object element = elements[i];
        if (CallHierarchy.isPossibleInputElement(element)) {
          members[i] = (CompilationUnitElement) element;
        } else if (element instanceof MethodWrapper) {
          DartElement wrapped = ((MethodWrapper) element).getMember();
          if (CallHierarchy.isPossibleInputElement(wrapped)) {
            members[i] = (CompilationUnitElement) wrapped;
          } else {
            return null;
          }
        } else {
          return null;
        }
      }
      if (members.length > 0) {
        return members;
      }
    }
    return null;
  }

  private ISelection getSelection() {
    ISelectionProvider provider = chvPart.getSite().getSelectionProvider();

    if (provider != null) {
      return provider.getSelection();
    }

    return null;
  }
}
