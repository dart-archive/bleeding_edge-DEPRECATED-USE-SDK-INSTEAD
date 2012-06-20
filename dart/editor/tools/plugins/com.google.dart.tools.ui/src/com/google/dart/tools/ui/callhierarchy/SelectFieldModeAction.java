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

import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * Toggles how fields should be expanded: all references, read accesses, write accesses.
 */
public class SelectFieldModeAction extends Action {

  private CallHierarchyViewPart chvPart;
  private int mode;

  public SelectFieldModeAction(CallHierarchyViewPart v, int mode) {
    super(null, AS_RADIO_BUTTON);
//    if (mode == IJavaSearchConstants.REFERENCES) {
//      setText(CallHierarchyMessages.SelectFieldModeAction_all_references_label);
//      setDescription(CallHierarchyMessages.SelectFieldModeAction_all_references_description);
//    } else if (mode == IJavaSearchConstants.READ_ACCESSES) {
//      setText(CallHierarchyMessages.SelectFieldModeAction_read_accesses_label);
//      setDescription(CallHierarchyMessages.SelectFieldModeAction_read_accesses_description);
//    } else if (mode == IJavaSearchConstants.WRITE_ACCESSES) {
//      setText(CallHierarchyMessages.SelectFieldModeAction_write_accesses_label);
//      setDescription(CallHierarchyMessages.SelectFieldModeAction_write_accesses_description);
//    } else {
//      Assert.isTrue(false);
//    }
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
    chvPart.setFieldMode(mode); // will toggle the checked state
  }
}
