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

import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.TransferData;

import java.lang.reflect.Member;
import java.util.List;

class CallHierarchyTransferDropAdapter /* extends ViewerInputDropAdapter */{

  @SuppressWarnings("unused")
  private CallHierarchyViewPart fCallHierarchyViewPart;

  public CallHierarchyTransferDropAdapter(CallHierarchyViewPart viewPart, StructuredViewer viewer) {
//    super(viewer);
    fCallHierarchyViewPart = viewPart;
  }

  public boolean performDrop(Object data) {
//    setSelectionFeedbackEnabled(false);
//    setExpandEnabled(false);
//
//    if (getCurrentTarget() != null) {
//      return super.performDrop(data);
//    }
//
//    Object input = getInputElement(getSelection());
//    if (input != null) {
//      doInputView(input);
//      return true;
//    }
//    return super.performDrop(data);
    return false;
  }

  protected int determineOperation(Object target, int operation, TransferData transferType,
      int operations) {
//    setSelectionFeedbackEnabled(false);
//    setExpandEnabled(false);
//
//    initializeSelection();
//
//    if (target != null) {
//      return super.determineOperation(target, operation, transferType, operations);
//    } else if (getInputElement(getSelection()) != null) {
//      setSelectionFeedbackEnabled(false);
//      setExpandEnabled(false);
//      return operation == DND.DROP_DEFAULT || operation == DND.DROP_MOVE ? DND.DROP_LINK
//          : operation;
//    } else {
//      return DND.DROP_NONE;
//    }
    return 0;
  }

  protected void doInputView(Object inputElements) {
//    TypeMember[] newElements = (TypeMember[]) inputElements;
//    TypeMember[] oldInput = fCallHierarchyViewPart.getInputElements();
//    boolean noInput = oldInput != null && oldInput.length > 0;
//    if (getCurrentOperation() == DND.DROP_LINK || !noInput) {
//      fCallHierarchyViewPart.setInputElements(newElements);
//      return;
//    }
//    if (newElements != null && newElements.length > 0) {
//      fCallHierarchyViewPart.addInputElements(newElements);
//    }
  }

  protected Object getInputElement(ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      List<?> elements = ((IStructuredSelection) selection).toList();
      if (CallHierarchy.arePossibleInputElements(elements)) {
        return elements.toArray(new Member[elements.size()]);
      }
    }
    return null;
  }
}
