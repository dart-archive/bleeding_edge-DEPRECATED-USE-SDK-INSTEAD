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

// TODO remove
public class SearchUtil {
//
//  // LRU working sets
//  public static int LRU_WORKINGSET_LIST_SIZE = 3;
//  private static LRUWorkingSetsList fgLRUWorkingSets;
//  // Settings store
//  private static final String DIALOG_SETTINGS_KEY = "CallHierarchySearchScope"; //$NON-NLS-1$
//  private static final String STORE_LRU_WORKING_SET_NAMES = "lastUsedWorkingSetNames"; //$NON-NLS-1$
//  private static IDialogSettings fgSettingsStore;
//
//  public static LRUWorkingSetsList getLRUWorkingSets() {
//    if (SearchUtil.fgLRUWorkingSets == null) {
//      restoreState();
//    }
//    return SearchUtil.fgLRUWorkingSets;
//  }
//
//  public static String toString(IWorkingSet[] workingSets) {
//    Arrays.sort(workingSets, new WorkingSetComparator());
//    String result = ""; //$NON-NLS-1$
//    if (workingSets != null && workingSets.length > 0) {
//      boolean firstFound = false;
//      for (int i = 0; i < workingSets.length; i++) {
//        String workingSetName = BasicElementLabels.getWorkingSetLabel(workingSets[i]);
//        if (firstFound) {
//          result = Messages.format(CallHierarchyMessages.SearchUtil_workingSetConcatenation,
//              new String[] {result, workingSetName});
//        } else {
//          result = workingSetName;
//          firstFound = true;
//        }
//      }
//    }
//    return result;
//  }
//
//  /**
//   * Updates the LRU list of working sets.
//   * 
//   * @param workingSets the workings sets to be added to the LRU list
//   */
//  public static void updateLRUWorkingSets(IWorkingSet[] workingSets) {
//    if (workingSets == null || workingSets.length < 1) {
//      return;
//    }
//
//    SearchUtil.getLRUWorkingSets().add(workingSets);
//    SearchUtil.saveState();
//  }
//
//  static void restoreState() {
//    SearchUtil.fgLRUWorkingSets = new LRUWorkingSetsList(SearchUtil.LRU_WORKINGSET_LIST_SIZE);
//    SearchUtil.fgSettingsStore = DartToolsPlugin.getDefault().getDialogSettings().getSection(
//        SearchUtil.DIALOG_SETTINGS_KEY);
//    if (SearchUtil.fgSettingsStore == null) {
//      SearchUtil.fgSettingsStore = DartToolsPlugin.getDefault().getDialogSettings().addNewSection(
//          SearchUtil.DIALOG_SETTINGS_KEY);
//    }
//
//    boolean foundLRU = false;
//    for (int i = SearchUtil.LRU_WORKINGSET_LIST_SIZE - 1; i >= 0; i--) {
//      String[] lruWorkingSetNames = SearchUtil.fgSettingsStore.getArray(SearchUtil.STORE_LRU_WORKING_SET_NAMES
//          + i);
//      if (lruWorkingSetNames != null) {
//        Set<IWorkingSet> workingSets = new HashSet<IWorkingSet>(2);
//        for (int j = 0; j < lruWorkingSetNames.length; j++) {
//          IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(
//              lruWorkingSetNames[j]);
//          if (workingSet != null) {
//            workingSets.add(workingSet);
//          }
//        }
//        foundLRU = true;
//        if (!workingSets.isEmpty()) {
//          SearchUtil.fgLRUWorkingSets.add(workingSets.toArray(new IWorkingSet[workingSets.size()]));
//        }
//      }
//    }
//    if (!foundLRU) {
//      // try old preference format
//      restoreFromOldFormat();
//    }
//  }
//
//  private static void restoreFromOldFormat() {
//    SearchUtil.fgLRUWorkingSets = new LRUWorkingSetsList(SearchUtil.LRU_WORKINGSET_LIST_SIZE);
//    SearchUtil.fgSettingsStore = JavaPlugin.getDefault().getDialogSettings().getSection(
//        SearchUtil.DIALOG_SETTINGS_KEY);
//    if (SearchUtil.fgSettingsStore == null) {
//      SearchUtil.fgSettingsStore = DartToolsPlugin.getDefault().getDialogSettings().addNewSection(
//          SearchUtil.DIALOG_SETTINGS_KEY);
//    }
//
//    boolean foundLRU = false;
//    String[] lruWorkingSetNames = SearchUtil.fgSettingsStore.getArray(SearchUtil.STORE_LRU_WORKING_SET_NAMES);
//    if (lruWorkingSetNames != null) {
//      for (int i = lruWorkingSetNames.length - 1; i >= 0; i--) {
//        IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(
//            lruWorkingSetNames[i]);
//        if (workingSet != null) {
//          foundLRU = true;
//          SearchUtil.fgLRUWorkingSets.add(new IWorkingSet[] {workingSet});
//        }
//      }
//    }
//    if (foundLRU) {
//      // save in new format
//      saveState();
//    }
//  }
//
//  private static void saveState() {
//    IWorkingSet[] workingSets;
//    Iterator<IWorkingSet[]> iter = SearchUtil.fgLRUWorkingSets.iterator();
//    int i = 0;
//    while (iter.hasNext()) {
//      workingSets = iter.next();
//      String[] names = new String[workingSets.length];
//      for (int j = 0; j < workingSets.length; j++) {
//        names[j] = workingSets[j].getName();
//      }
//      SearchUtil.fgSettingsStore.put(SearchUtil.STORE_LRU_WORKING_SET_NAMES + i, names);
//      i++;
//    }
//  }
}
