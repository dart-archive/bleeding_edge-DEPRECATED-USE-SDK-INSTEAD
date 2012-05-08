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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;

import java.util.ArrayList;

/**
 * Invoke the resource creation wizard selection Wizard. This action will retarget to the active
 * view.
 */
class ShowSearchHistoryDialogAction extends Action {
  private SearchView fSearchView;

  /*
   * Create a new instance of this class
   */
  public ShowSearchHistoryDialogAction(SearchView searchView) {
    super(SearchMessages.ShowSearchesAction_label);
    setToolTipText(SearchMessages.ShowSearchesAction_tooltip);
    fSearchView = searchView;
  }

  @Override
  public void run() {
    ISearchQuery[] queries = NewSearchUI.getQueries();

    ArrayList<ISearchResult> input = new ArrayList<ISearchResult>();
    for (int j = 0; j < queries.length; j++) {
      ISearchResult search = queries[j].getSearchResult();
      input.add(search);
    }

    SearchHistorySelectionDialog dlg = new SearchHistorySelectionDialog(
        SearchPlugin.getActiveWorkbenchShell(),
        input);

    ISearchResult current = fSearchView.getCurrentSearchResult();
    if (current != null) {
      Object[] selected = new Object[1];
      selected[0] = current;
      dlg.setInitialSelections(selected);
    }
    if (dlg.open() == Window.OK) {
      Object[] result = dlg.getResult();
      if (result != null && result.length == 1) {
        ISearchResult searchResult = (ISearchResult) result[0];
        InternalSearchUI.getInstance().showSearchResult(fSearchView, searchResult, false);
      }
    }

  }
}
