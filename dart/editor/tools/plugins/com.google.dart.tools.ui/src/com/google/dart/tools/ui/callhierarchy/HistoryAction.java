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

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;

class HistoryAction extends Action {

  private static long LABEL_FLAGS = DartElementLabels.ALL_POST_QUALIFIED
      | DartElementLabels.M_PARAMETER_TYPES | DartElementLabels.M_APP_RETURNTYPE
      | DartElementLabels.T_TYPE_PARAMETERS | DartElementLabels.P_COMPRESSED
      | DartElementLabels.COLORIZE;

  static String getElementLabel(DartElement[] members) {
    switch (members.length) {
      case 0:
        Assert.isTrue(false);
        return null;

      case 1:
        return Messages.format(CallHierarchyMessages.HistoryAction_inputElements_1,
            new String[] {getShortLabel(members[0])});

      case 2:
        return Messages.format(CallHierarchyMessages.HistoryAction_inputElements_2, new String[] {
            getShortLabel(members[0]), getShortLabel(members[1])});

      default:
        return Messages.format(CallHierarchyMessages.HistoryAction_inputElements_more,
            new String[] {getShortLabel(members[0]), getShortLabel(members[1])});
    }
  }

  private static ImageDescriptor getImageDescriptor(DartElement[] members) {
    DartElementImageProvider imageProvider = new DartElementImageProvider();
    ImageDescriptor desc = imageProvider.getBaseImageDescriptor(members[0], 0);
    imageProvider.dispose();
    return desc;

  }

  private static String getShortLabel(DartElement member) {
    return DartElementLabels.getElementLabel(member, LABEL_FLAGS);
  }

  private CallHierarchyViewPart chvPart;
  private DartElement[] members;

  public HistoryAction(CallHierarchyViewPart viewPart, DartElement[] members) {
    super("", AS_RADIO_BUTTON); //$NON-NLS-1$
    chvPart = viewPart;
    this.members = members;

    String elementName = getElementLabel(members);
    setText(elementName);
    setImageDescriptor(getImageDescriptor(members));

    setDescription(Messages.format(CallHierarchyMessages.HistoryAction_description, elementName));
    setToolTipText(Messages.format(CallHierarchyMessages.HistoryAction_tooltip, elementName));

    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        DartHelpContextIds.CALL_HIERARCHY_HISTORY_ACTION);
  }

  @Override
  public void run() {
    chvPart.gotoHistoryEntry(members);
  }
}
