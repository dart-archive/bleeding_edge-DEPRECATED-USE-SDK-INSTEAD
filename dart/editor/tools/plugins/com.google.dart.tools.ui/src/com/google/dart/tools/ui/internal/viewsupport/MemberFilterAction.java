/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.actions.MemberFilterActionGroup;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * Action used to enable / disable method filter properties
 */
public class MemberFilterAction extends Action {

  private int fFilterProperty;
  private MemberFilterActionGroup fFilterActionGroup;

  public MemberFilterAction(MemberFilterActionGroup actionGroup, String title, int property,
      String contextHelpId, boolean initValue) {
    super(title);
    fFilterActionGroup = actionGroup;
    fFilterProperty = property;

    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, contextHelpId);

    setChecked(initValue);
  }

  /**
   * Returns this action's filter property.
   */
  public int getFilterProperty() {
    return fFilterProperty;
  }

  /*
   * @see Action#actionPerformed
   */
  @Override
  public void run() {
    fFilterActionGroup.setMemberFilter(fFilterProperty, isChecked());
  }

}
