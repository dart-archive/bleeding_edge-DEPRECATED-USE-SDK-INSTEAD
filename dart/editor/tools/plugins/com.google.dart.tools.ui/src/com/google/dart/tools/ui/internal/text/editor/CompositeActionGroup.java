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
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class CompositeActionGroup extends ActionGroup {

  private ActionGroup[] fGroups;

  public CompositeActionGroup() {
  }

  public CompositeActionGroup(ActionGroup[] groups) {
    setGroups(groups);
  }

  public void addGroup(ActionGroup group) {
    if (fGroups == null) {
      fGroups = new ActionGroup[] {group};
    } else {
      ActionGroup[] newGroups = new ActionGroup[fGroups.length + 1];
      System.arraycopy(fGroups, 0, newGroups, 0, fGroups.length);
      newGroups[fGroups.length] = group;
      fGroups = newGroups;
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    if (fGroups == null) {
      return;
    }
    for (int i = 0; i < fGroups.length; i++) {
      fGroups[i].dispose();
    }
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    if (fGroups == null) {
      return;
    }
    for (int i = 0; i < fGroups.length; i++) {
      fGroups[i].fillActionBars(actionBars);
    }
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    super.fillContextMenu(menu);
    if (fGroups == null) {
      return;
    }
    for (int i = 0; i < fGroups.length; i++) {
      fGroups[i].fillContextMenu(menu);
    }
  }

  public ActionGroup get(int index) {
    if (fGroups == null) {
      return null;
    }
    return fGroups[index];
  }

  @Override
  public void setContext(ActionContext context) {
    super.setContext(context);
    if (fGroups == null) {
      return;
    }
    for (int i = 0; i < fGroups.length; i++) {
      fGroups[i].setContext(context);
    }
  }

  @Override
  public void updateActionBars() {
    super.updateActionBars();
    if (fGroups == null) {
      return;
    }
    for (int i = 0; i < fGroups.length; i++) {
      fGroups[i].updateActionBars();
    }
  }

  protected void setGroups(ActionGroup[] groups) {
    Assert.isTrue(fGroups == null);
    Assert.isNotNull(groups);
    fGroups = groups;
  }
}
