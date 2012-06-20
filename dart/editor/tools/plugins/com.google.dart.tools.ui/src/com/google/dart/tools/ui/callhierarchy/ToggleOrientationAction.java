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
 * Toggles the orientation of the layout of the call hierarchy
 */
class ToggleOrientationAction extends Action {

  private CallHierarchyViewPart chvPart;
  private int actionOrientation;

  public ToggleOrientationAction(CallHierarchyViewPart v, int orientation) {
    super("", AS_RADIO_BUTTON); //$NON-NLS-1$
    if (orientation == CallHierarchyViewPart.VIEW_ORIENTATION_HORIZONTAL) {
      setText(CallHierarchyMessages.ToggleOrientationAction_horizontal_label);
      setDescription(CallHierarchyMessages.ToggleOrientationAction_horizontal_description);
      setToolTipText(CallHierarchyMessages.ToggleOrientationAction_horizontal_tooltip);
      DartPluginImages.setLocalImageDescriptors(this, "th_horizontal.gif"); //$NON-NLS-1$
    } else if (orientation == CallHierarchyViewPart.VIEW_ORIENTATION_VERTICAL) {
      setText(CallHierarchyMessages.ToggleOrientationAction_vertical_label);
      setDescription(CallHierarchyMessages.ToggleOrientationAction_vertical_description);
      setToolTipText(CallHierarchyMessages.ToggleOrientationAction_vertical_tooltip);
      DartPluginImages.setLocalImageDescriptors(this, "th_vertical.gif"); //$NON-NLS-1$
    } else if (orientation == CallHierarchyViewPart.VIEW_ORIENTATION_AUTOMATIC) {
      setText(CallHierarchyMessages.ToggleOrientationAction_automatic_label);
      setDescription(CallHierarchyMessages.ToggleOrientationAction_automatic_description);
      setToolTipText(CallHierarchyMessages.ToggleOrientationAction_automatic_tooltip);
      DartPluginImages.setLocalImageDescriptors(this, "th_automatic.gif"); //$NON-NLS-1$
    } else if (orientation == CallHierarchyViewPart.VIEW_ORIENTATION_SINGLE) {
      setText(CallHierarchyMessages.ToggleOrientationAction_single_label);
      setDescription(CallHierarchyMessages.ToggleOrientationAction_single_description);
      setToolTipText(CallHierarchyMessages.ToggleOrientationAction_single_tooltip);
      DartPluginImages.setLocalImageDescriptors(this, "th_single.gif"); //$NON-NLS-1$
    } else {
      Assert.isTrue(false);
    }
    this.chvPart = v;
    this.actionOrientation = orientation;
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_TOGGLE_ORIENTATION_ACTION);
  }

  public int getOrientation() {
    return actionOrientation;
  }

  @Override
  public void run() {
    if (isChecked()) {
      chvPart.orientation = actionOrientation;
      chvPart.computeOrientation();
    }
  }

}
