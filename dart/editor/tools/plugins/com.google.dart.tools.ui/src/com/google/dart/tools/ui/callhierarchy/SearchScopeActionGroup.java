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

import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.ui.IContextMenuConstants;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SearchScopeActionGroup extends ActionGroup {
  private static final String TAG_SEARCH_SCOPE_TYPE = "search_scope_type"; //$NON-NLS-1$
  private static final String TAG_SELECTED_WORKING_SET = "working_set"; //$NON-NLS-1$
  private static final String TAG_WORKING_SET_COUNT = "working_set_count"; //$NON-NLS-1$

  private static final String DIALOGSTORE_SCOPE_TYPE = "SearchScopeActionGroup.search_scope_type"; //$NON-NLS-1$
  private static final String DIALOGSTORE_SELECTED_WORKING_SET = "SearchScopeActionGroup.working_set"; //$NON-NLS-1$

  static final int SEARCH_SCOPE_TYPE_WORKSPACE = 1;
  static final int SEARCH_SCOPE_TYPE_PROJECT = 2;
  // static final int SEARCH_SCOPE_TYPE_HIERARCHY= 3;
  static final int SEARCH_SCOPE_TYPE_WORKING_SET = 4;

  private SearchScopeAction selectedAction = null;
  private String[] selectedWorkingSetNames = null;
  private CallHierarchyViewPart chvPart;
  private IDialogSettings dialogSettings;
  private SearchScopeProjectAction searchScopeProjectAction;
  private SearchScopeWorkspaceAction searchScopeWorkspaceAction;

//  private SelectWorkingSetAction selectWorkingSetAction;

  public SearchScopeActionGroup(CallHierarchyViewPart view, IDialogSettings dialogSettings) {
    this.chvPart = view;
    this.dialogSettings = dialogSettings;
    createActions();
  }

  @Override
  public void fillActionBars(IActionBars actionBars) {
    super.fillActionBars(actionBars);
    fillContextMenu(actionBars.getMenuManager());
  }

  @Override
  public void fillContextMenu(IMenuManager menu) {
    MenuManager javaSearchMM = new MenuManager(
        CallHierarchyMessages.SearchScopeActionGroup_searchScope,
        IContextMenuConstants.GROUP_SEARCH);
    javaSearchMM.setRemoveAllWhenShown(true);

    javaSearchMM.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {
        fillSearchActions(manager);
      }
    });

    fillSearchActions(javaSearchMM);
    menu.appendToGroup(IContextMenuConstants.GROUP_SEARCH, javaSearchMM);
  }

  /**
   * Fetches the full description of the scope with the appropriate include mask.
   * 
   * @param includeMask the include mask
   * @return the description of the scope with the appropriate include mask
   */
  public String getFullDescription(int includeMask) {
    if (selectedAction != null) {
      return selectedAction.getFullDescription(includeMask);
    }
    return null;
  }

  /**
   * Returns the current search scope.
   * 
   * @param includeMask the include mask
   * @return the current search scope
   */
  public SearchScope getSearchScope(int includeMask) {
    if (selectedAction != null) {
      return selectedAction.getSearchScope(includeMask);
    }

    return null;
  }

  public void restoreState(IMemento memento) {
    String[] workingSetNames = null;
    Integer scopeType = memento.getInteger(TAG_SEARCH_SCOPE_TYPE);
    if (scopeType != null) {
      if (scopeType.intValue() == SEARCH_SCOPE_TYPE_WORKING_SET) {
        Integer workingSetCount = memento.getInteger(TAG_WORKING_SET_COUNT);
        if (workingSetCount != null) {
          workingSetNames = new String[workingSetCount.intValue()];
          for (int i = 0; i < workingSetCount.intValue(); i++) {
            workingSetNames[i] = memento.getString(TAG_SELECTED_WORKING_SET + i);
          }
        }
      }
      setSelected(getSearchScopeAction(scopeType.intValue(), workingSetNames), false);
    }
  }

  public void saveState(IMemento memento) {
    int type = getSearchScopeType();
    memento.putInteger(TAG_SEARCH_SCOPE_TYPE, type);
    if (type == SEARCH_SCOPE_TYPE_WORKING_SET) {
      memento.putInteger(TAG_WORKING_SET_COUNT, selectedWorkingSetNames.length);
      for (int i = 0; i < selectedWorkingSetNames.length; i++) {
        String workingSetName = selectedWorkingSetNames[i];
        memento.putString(TAG_SELECTED_WORKING_SET + i, workingSetName);
      }
    }
  }

  protected void fillSearchActions(IMenuManager searchMM) {
    Action[] actions = getActions();

    for (int i = 0; i < actions.length; i++) {
      Action action = actions[i];

      if (action.isEnabled()) {
        searchMM.add(action);
      }
    }

    searchMM.setVisible(!searchMM.isEmpty());
  }

  protected IWorkingSet[] getActiveWorkingSets() {
    if (selectedWorkingSetNames != null) {
      return getWorkingSets(selectedWorkingSetNames);
    }

    return null;
  }

  protected CallHierarchyViewPart getView() {
    return chvPart;
  }

  protected IWorkingSetManager getWorkingSetManager() {
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();

    return workingSetManager;
  }

//  protected void setActiveWorkingSets(IWorkingSet[] sets) {
//    if (sets != null) {
//      selectedWorkingSetNames = getWorkingSetNames(sets);
//      selectedAction = new SearchScopeWorkingSetAction(this, sets, getScopeDescription(sets));
//      selectedAction.run();
//    } else {
//      selectedWorkingSetNames = null;
//      selectedAction = null;
//    }
//  }

  /**
   * Sets the new search scope type.
   * 
   * @param newSelection New action which should be the checked one (can be null iff
   *          <code>ignoreUnchecked == false</code>)
   * @param ignoreUnchecked Ignores actions which are unchecked (necessary since both the old and
   *          the new action fires).
   */
  protected void setSelected(SearchScopeAction newSelection, boolean ignoreUnchecked) {
    if (!ignoreUnchecked || newSelection.isChecked()) {
//      if (newSelection instanceof SearchScopeWorkingSetAction) {
//        selectedWorkingSetNames = getWorkingSetNames(((SearchScopeWorkingSetAction) newSelection).getWorkingSets());
//      } else {
      selectedWorkingSetNames = null;
//      }

      if (newSelection != null) {
        selectedAction = newSelection;
      } else {
        selectedAction = searchScopeWorkspaceAction;
      }

      dialogSettings.put(DIALOGSTORE_SCOPE_TYPE, getSearchScopeType());
      dialogSettings.put(DIALOGSTORE_SELECTED_WORKING_SET, selectedWorkingSetNames);
    }
  }

  private void addAction(List<Action> actions, Action action) {
    if (action == selectedAction) {
      action.setChecked(true);
    } else {
      action.setChecked(false);
    }
    actions.add(action);
  }

  private void createActions() {
    searchScopeWorkspaceAction = new SearchScopeWorkspaceAction(this);
//    fSelectWorkingSetAction = new SelectWorkingSetAction(this);
    searchScopeProjectAction = new SearchScopeProjectAction(this);

    int searchScopeType;
    try {
      searchScopeType = dialogSettings.getInt(DIALOGSTORE_SCOPE_TYPE);
    } catch (NumberFormatException e) {
      searchScopeType = SEARCH_SCOPE_TYPE_WORKSPACE;
    }
    String[] workingSetNames = dialogSettings.getArray(DIALOGSTORE_SELECTED_WORKING_SET);
    setSelected(getSearchScopeAction(searchScopeType, workingSetNames), false);
  }

  private void ensureExactlyOneCheckedAction(Action[] result) {
    int checked = getCheckedActionCount(result);
    if (checked != 1) {
      if (checked > 1) {
        for (int i = 0; i < result.length; i++) {
          Action action = result[i];
          action.setChecked(false);
        }
      }
      searchScopeWorkspaceAction.setChecked(true);
    }
  }

  private Action[] getActions() {
    List<Action> actions = new ArrayList<Action>();
//    addAction(actions, searchScopeWorkspaceAction);
//    addAction(actions, searchScopeProjectAction);
//    addAction(actions, selectWorkingSetAction);

//    Iterator<IWorkingSet[]> iter = SearchUtil.getLRUWorkingSets().sortedIterator();
//    while (iter.hasNext()) {
//      IWorkingSet[] workingSets = iter.next();
//      String description = SearchUtil.toString(workingSets);
//      SearchScopeWorkingSetAction workingSetAction = new SearchScopeWorkingSetAction(this,
//          workingSets, description);
//
//      if (isSelectedWorkingSet(workingSets)) {
//        workingSetAction.setChecked(true);
//      }
//
//      actions.add(workingSetAction);
//    }

    Action[] result = actions.toArray(new Action[actions.size()]);

    ensureExactlyOneCheckedAction(result);
    return result;
  }

  private int getCheckedActionCount(Action[] result) {
    // Ensure that exactly one action is selected
    int checked = 0;
    for (int i = 0; i < result.length; i++) {
      Action action = result[i];
      if (action.isChecked()) {
        checked++;
      }
    }
    return checked;
  }

//  private String getScopeDescription(IWorkingSet[] workingSets) {
//    return Messages.format(CallHierarchyMessages.WorkingSetScope,
//        new String[] {SearchUtil.toString(workingSets)});
//  }

  private SearchScopeAction getSearchScopeAction(int searchScopeType, String[] workingSetNames) {
    switch (searchScopeType) {
      case SEARCH_SCOPE_TYPE_WORKSPACE:
        return searchScopeWorkspaceAction;
      case SEARCH_SCOPE_TYPE_PROJECT:
        return searchScopeProjectAction;
    }
    return null;
  }

  private int getSearchScopeType() {
    if (selectedAction != null) {
      return selectedAction.getSearchScopeType();
    }
    return 0;
  }

  private String[] getWorkingSetNames(IWorkingSet[] sets) {
    String[] result = new String[sets.length];
    for (int i = 0; i < sets.length; i++) {
      result[i] = sets[i].getName();
    }
    return result;
  }

  private IWorkingSet[] getWorkingSets(String[] workingSetNames) {
    if (workingSetNames == null) {
      return null;
    }
    Set<IWorkingSet> workingSets = new HashSet<IWorkingSet>(2);
    for (int j = 0; j < workingSetNames.length; j++) {
      IWorkingSet workingSet = getWorkingSetManager().getWorkingSet(workingSetNames[j]);
      if (workingSet != null) {
        workingSets.add(workingSet);
      }
    }

    return workingSets.toArray(new IWorkingSet[workingSets.size()]);
  }

  /**
   * Determines whether the specified working sets correspond to the currently selected working
   * sets.
   * 
   * @param workingSets the array of working sets
   * @return <code>true</code> if the specified working sets correspond to the currently selected
   *         working sets
   */
//  private boolean isSelectedWorkingSet(IWorkingSet[] workingSets) {
//    if (selectedWorkingSetNames != null && selectedWorkingSetNames.length == workingSets.length) {
//      Set<String> workingSetNames = new HashSet<String>(workingSets.length);
//      for (int i = 0; i < workingSets.length; i++) {
//        workingSetNames.add(workingSets[i].getName());
//      }
//      for (int i = 0; i < selectedWorkingSetNames.length; i++) {
//        if (!workingSetNames.contains(selectedWorkingSetNames[i])) {
//          return false;
//        }
//      }
//      return true;
//    }
//    return false;
//  }
}
