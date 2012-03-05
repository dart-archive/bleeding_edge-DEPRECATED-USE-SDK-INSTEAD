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

import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.actions.OpenAction;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.util.SelectionUtil;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchSite;

/**
 * This class is used for opening the declaration of an element from the call hierarchy view.
 */
class OpenDeclarationAction extends OpenAction {

  public OpenDeclarationAction(IWorkbenchSite site) {
    super(site);
  }

  public boolean canActionBeAdded() {
    // It is safe to cast to TypeMember since the selection has already been converted
    TypeMember member = (TypeMember) SelectionUtil.getSingleElement(getSelection());

    if (member != null) {
      return true;
    }

    return false;
  }

  @Override
  public Object getElementToOpen(Object object) {
    if (object instanceof MethodWrapper) {
      return ((MethodWrapper) object).getMember();
    }
    return object;
  }

  @Override
  public ISelection getSelection() {
    return CallHierarchyUI.convertSelection(getSelectionProvider().getSelection());
  }
}
