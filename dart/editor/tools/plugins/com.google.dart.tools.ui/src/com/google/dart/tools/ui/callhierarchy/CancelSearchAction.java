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
import org.eclipse.ui.PlatformUI;

/**
 * This class is copied from the org.eclipse.search2.internal.ui.CancelSearchAction class.
 */
public class CancelSearchAction extends Action {

  private CallHierarchyViewPart view;

  public CancelSearchAction(CallHierarchyViewPart view) {
    super(CallHierarchyMessages.CancelSearchAction_label);
    this.view = view;
    setToolTipText(CallHierarchyMessages.CancelSearchAction_tooltip);
    DartPluginImages.setLocalImageDescriptors(this, "ch_cancel.gif"); //$NON-NLS-1$

    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        this,
        DartHelpContextIds.CALL_HIERARCHY_CANCEL_SEARCH_ACTION);
  }

  @Override
  public void run() {
    view.cancelJobs();
  }
}
