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

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;

/**
 * The action to pin the Call Hierarchy view.
 */
class PinCallHierarchyViewAction extends Action {
  private CallHierarchyViewPart chvPart = null;

  /**
   * Constructs a 'Pin Call Hierarchy view' action.
   * 
   * @param view the Call Hierarchy view
   */
  public PinCallHierarchyViewAction(CallHierarchyViewPart view) {
    super(CallHierarchyMessages.PinCallHierarchyViewAction_label, IAction.AS_CHECK_BOX);
    setToolTipText(CallHierarchyMessages.PinCallHierarchyViewAction_tooltip);
    DartPluginImages.setLocalImageDescriptors(this, "pin_view.gif"); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_PIN_VIEW_ACTION);
    chvPart = view;
  }

  @Override
  public void run() {
    chvPart.setPinned(isChecked());
  }
}
