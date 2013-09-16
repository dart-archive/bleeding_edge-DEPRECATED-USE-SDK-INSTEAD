/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.actions;

import com.ibm.icu.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

public class MenuBuilder {

  protected Comparator comparator = new Comparator() {

    public int compare(Object o1, Object o2) {
      return Collator.getInstance().compare(getSortKey(o1), getSortKey(o2));
    }

    protected String getSortKey(Object o) {
      String result = ""; //$NON-NLS-1$
      if (o instanceof IAction) {
        IAction action = (IAction) o;
        result = action.getText();
      }
      // else if (o instanceof MenuData)
      // {
      // result = "z" + ((MenuData)o).name;
      // }
      return result;
    }
  };

  protected void createAlphebeticalGrouping(IMenuManager menu, List actionList) {
    Object[] array = actionList.toArray();
    if (array.length > 0) {
      Arrays.sort(array, comparator);
    }

    int groupSize = 15;
    int minGroupSize = 5;
    int numberOfGroups = (array.length / groupSize)
        + ((array.length % groupSize > minGroupSize) ? 1 : 0);

    for (int i = 0; i < numberOfGroups; i++) {
      boolean isLastGroup = (i == (numberOfGroups - 1));
      int firstIndex = i * groupSize;
      int lastIndex = isLastGroup ? array.length - 1 : i * groupSize + groupSize - 1;
      Action firstAction = (Action) array[firstIndex];
      Action lastAction = (Action) array[lastIndex];
      MenuManager submenu = new MenuManager(firstAction.getText() + " - " + lastAction.getText()); //$NON-NLS-1$
      menu.add(submenu);
      for (int j = firstIndex; j <= lastIndex; j++) {
        submenu.add((Action) array[j]);
      }
    }
  }

  public void populateMenu(IMenuManager menu, List actionList, boolean createTiered) {
    // sort the actions
    if (actionList.size() < 25) {
      Object[] array = actionList.toArray();
      if (array.length > 0) {
        Arrays.sort(array, comparator);
      }
      for (int i = 0; i < array.length; i++) {
        menu.add((Action) array[i]);
      }
    } else {
      createAlphebeticalGrouping(menu, actionList);
    }
  }

  /*
   * protected void createPropertyGrouping(IMenuManager menu, List actionList) { MenuDataTable
   * menuDataTable = new MenuDataTable();
   * 
   * for (Iterator i = actionList.iterator(); i.hasNext(); ) { String groupName = null; Action
   * action = (Action)i.next(); if (action instanceof NodeAction) { groupName =
   * ((NodeAction)action).getGroupName(); } if (groupName == null) { groupName = ""; } MenuData
   * menuData = menuDataTable.lookupOrCreate(groupName, ""); menuData.childList.add(action); }
   * populateMenu(menu, menuDataTable.getRoot()); }
   * 
   * 
   * protected void populateMenu(MenuManager menuManager, MenuData menuData) { for (Iterator i =
   * menuData.childList.iterator(); i.hasNext(); ) { Object o = i.next(); if (o instanceof Action) {
   * menuManager.add((Action)o); } else if (o instanceof MenuData) { MenuData childMenuData =
   * (MenuData)o; MenuManager childMenuManager = new MenuManager(childMenuData.name);
   * menuManager.add(childMenuManager); populateMenu(childMenuManager, childMenuData); } } }
   * 
   * 
   * public MenuDataTable { protected Hashtable table = new Hashtable(); protected MenuData root;
   * 
   * public MenuDataTable() { root = lookupOrCreateMenuData(null, null); }
   * 
   * protected MenuData lookupMenuData(String name) { String key = name != null ? name : ""; return
   * (MenuData)menuDataTable.get(key); }
   * 
   * protected MenuData lookupOrCreateMenuData(String name, String parentName) { String key = name
   * != null ? name : ""; MenuData menuData = (MenuData)menuDataTable.get(key); if (menuData ==
   * null) { menuData = new MenuData(name, parentName); menuDataTable.put(key, menuData); } return
   * menuData; }
   * 
   * public MenuData getRoot() { return root; } }
   * 
   * 
   * protected class MenuData { public String name; public String parentName; public List childList
   * = new Vector();
   * 
   * MenuData(String name, String parentName) { this.name = name; this.parentName = parentName; }
   * 
   * protected void sort() { Object[] array = childList.toArray(); if (array.length > 0 ) {
   * Arrays.sort(array, comparator); } childList = Arrays.asList(array);
   * 
   * for (Iterator i = childList.iterator(); i.hasNext(); ) { Object o = i.next(); if (o instanceof
   * MenuData) { ((MenuData)o).sort(); } } } }
   */
}
