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
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;

import java.util.Arrays;

class HistoryDropDownAction extends Action implements IMenuCreator {

  private static class ClearHistoryAction extends Action {

    /**
     * Creates a clear history action.
     * 
     * @param view the Call Hierarchy view part
     */
    public ClearHistoryAction(CallHierarchyViewPart view) {
      super(CallHierarchyMessages.HistoryDropDownAction_clearhistory_label);
    }

    @Override
    public void run() {
      CallHierarchyUI.getDefault().clearHistory();
    }
  }

  public static final int RESULTS_IN_DROP_DOWN = 10;
  private CallHierarchyViewPart chvPart;
  private Menu menu;

  public HistoryDropDownAction(CallHierarchyViewPart view) {
    chvPart = view;
    menu = null;
    setToolTipText(CallHierarchyMessages.HistoryDropDownAction_tooltip);
    DartPluginImages.setLocalImageDescriptors(this, "history_list.gif"); //$NON-NLS-1$

    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        DartHelpContextIds.CALL_HIERARCHY_HISTORY_DROP_DOWN_ACTION);

    setMenuCreator(this);
  }

  @Override
  public void dispose() {
    chvPart = null;

    if (menu != null) {
      menu.dispose();
      menu = null;
    }
  }

  @Override
  public Menu getMenu(Control parent) {
    if (menu != null) {
      menu.dispose();
    }
    menu = new Menu(parent);
    DartElement[][] elements = chvPart.getHistoryEntries();
    addEntries(menu, elements);
    new MenuItem(menu, SWT.SEPARATOR);
    addActionToMenu(menu, new HistoryListAction(chvPart));
    addActionToMenu(menu, new ClearHistoryAction(chvPart));
    return menu;
  }

  @Override
  public Menu getMenu(Menu parent) {
    return null;
  }

  @Override
  public void run() {
    new HistoryListAction(chvPart).run();
  }

  protected void addActionToMenu(Menu parent, Action action) {
    ActionContributionItem item = new ActionContributionItem(action);
    item.fill(parent, -1);
  }

  private boolean addEntries(Menu menu, DartElement[][] elements) {
    boolean checked = false;

    int min = Math.min(elements.length, RESULTS_IN_DROP_DOWN);

    for (int i = 0; i < min; i++) {
      HistoryAction action = new HistoryAction(chvPart, elements[i]);
      action.setChecked(Arrays.equals(elements[i], chvPart.getInputElements()));
      checked = checked || action.isChecked();
      addActionToMenu(menu, action);
    }

    return checked;
  }
}
