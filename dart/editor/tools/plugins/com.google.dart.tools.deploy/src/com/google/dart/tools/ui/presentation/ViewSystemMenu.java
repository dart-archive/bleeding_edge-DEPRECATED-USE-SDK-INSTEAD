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
package com.google.dart.tools.ui.presentation;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.internal.presentations.SystemMenuClose;
import org.eclipse.ui.internal.presentations.SystemMenuDetach;
import org.eclipse.ui.internal.presentations.SystemMenuMaximize;
import org.eclipse.ui.internal.presentations.SystemMenuMinimize;
import org.eclipse.ui.internal.presentations.SystemMenuRestore;
import org.eclipse.ui.internal.presentations.SystemMenuSize;
import org.eclipse.ui.internal.presentations.UpdatingActionContributionItem;
import org.eclipse.ui.internal.presentations.util.ISystemMenu;
import org.eclipse.ui.presentations.IPresentablePart;
import org.eclipse.ui.presentations.IStackPresentationSite;

/**
 * Implements the system view context menu.
 */
@SuppressWarnings("restriction")
public class ViewSystemMenu implements ISystemMenu {
  protected MenuManager menuManager = new MenuManager();
  private SystemMenuRestore restore;
  private SystemMenuMinimize minimize;
  private SystemMenuMaximize maximize;
  private SystemMenuClose close;

  /**
   * Create the system view menu
   * 
   * @param site the associated site
   */
  public ViewSystemMenu(IStackPresentationSite site) {
    restore = new SystemMenuRestore(site);
    minimize = new SystemMenuMinimize(site);
    maximize = new SystemMenuMaximize(site);
    close = new SystemMenuClose(site);

    initialize(site);
  }

  @Override
  public void dispose() {
    menuManager.dispose();
    menuManager.removeAll();
  }

  @Override
  public void show(Control parent, Point displayCoordinates, IPresentablePart currentSelection) {
    updateActions(currentSelection);

    filterMenu();

    showMenu(parent, displayCoordinates);
  }

  protected void initialize(IStackPresentationSite site) {
    menuManager.add(new GroupMarker("restore")); //$NON-NLS-1$
    menuManager.add(new UpdatingActionContributionItem(restore));
    menuManager.add(new GroupMarker("state")); //$NON-NLS-1$
    menuManager.add(new UpdatingActionContributionItem(minimize));
    menuManager.add(new UpdatingActionContributionItem(maximize));
    menuManager.add(new Separator("close")); //$NON-NLS-1$
    menuManager.add(close);
    site.addSystemActions(menuManager);
  }

  /**
   * Test whether the given item should be filtered from the view menu.
   * 
   * @param item the item to test
   * @return <code>true</code> if this item should be filtered from view, <code>false</code>
   *         otherwise
   */
  protected boolean isFiltered(IContributionItem item) {
    if (item instanceof SystemMenuSize) {
      return true;
    }

    if (item instanceof ActionContributionItem) {
      IAction action = ((ActionContributionItem) item).getAction();

      if (action instanceof SystemMenuDetach) {
        return true;
      }
    }

    return false;
  }

  private void filterMenu() {
    //the view stack adds (unwanted) "move" and "detach" menu items and here we remove them
    for (IContributionItem item : menuManager.getItems()) {
      if (isFiltered(item)) {
        menuManager.remove(item);
      }
    }
  }

  private void showMenu(Control parent, Point displayCoordinates) {
    Menu menu = menuManager.createContextMenu(parent);
    menuManager.update(true);
    menu.setLocation(displayCoordinates.x, displayCoordinates.y);
    menu.setVisible(true);
  }

  private void updateActions(IPresentablePart currentSelection) {
    restore.update();
    minimize.update();
    maximize.update();
    close.setTarget(currentSelection);
  }

}
