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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * Toggles the call direction of the call hierarchy (i.e. toggles between showing callers and
 * callees.)
 */
class ToggleCallModeAction extends Action {

  private CallHierarchyViewPart chvPart;
  private int mode;

  public ToggleCallModeAction(CallHierarchyViewPart v, int mode) {
    super("", AS_RADIO_BUTTON); //$NON-NLS-1$
    if (mode == CallHierarchyViewPart.CALL_MODE_CALLERS) {
      setText(CallHierarchyMessages.ToggleCallModeAction_callers_label);
      setDescription(CallHierarchyMessages.ToggleCallModeAction_callers_description);
      setToolTipText(CallHierarchyMessages.ToggleCallModeAction_callers_tooltip);
      DartPluginImages.setLocalImageDescriptors(this, "ch_callers.gif"); //$NON-NLS-1$
    } else if (mode == CallHierarchyViewPart.CALL_MODE_CALLEES) {
      setText(CallHierarchyMessages.ToggleCallModeAction_callees_label);
      setDescription(CallHierarchyMessages.ToggleCallModeAction_callees_description);
      setToolTipText(CallHierarchyMessages.ToggleCallModeAction_callees_tooltip);
      DartPluginImages.setLocalImageDescriptors(this, "ch_callees.gif"); //$NON-NLS-1$
    } else {
      Assert.isTrue(false);
    }
    this.chvPart = v;
    this.mode = mode;
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_TOGGLE_CALL_MODE_ACTION);
  }

  public int getMode() {
    return mode;
  }

  @Override
  public void run() {
    chvPart.setCallMode(mode); // will toggle the checked state
  }
}
