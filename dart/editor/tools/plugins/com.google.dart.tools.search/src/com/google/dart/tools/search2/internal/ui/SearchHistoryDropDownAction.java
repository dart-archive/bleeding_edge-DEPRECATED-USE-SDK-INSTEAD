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

import com.google.dart.tools.search.internal.ui.SearchPluginImages;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.NewSearchUI;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

class SearchHistoryDropDownAction extends Action implements IMenuCreator {

  private class ShowSearchFromHistoryAction extends Action {
    private ISearchResult fSearch;

    public ShowSearchFromHistoryAction(ISearchResult search) {
      super("", AS_RADIO_BUTTON); //$NON-NLS-1$
      fSearch = search;

      String label = escapeAmp(search.getLabel());
      if (InternalSearchUI.getInstance().isQueryRunning(search.getQuery())) {
        label = MessageFormat.format(
            SearchMessages.SearchDropDownAction_running_message,
            new Object[] {label});
      }
      // fix for bug 38049
      if (label.indexOf('@') >= 0) {
        label += '@';
      }
      setText(label);
      setImageDescriptor(search.getImageDescriptor());
      setToolTipText(search.getTooltip());
    }

    @Override
    public void run() {
      runIfChecked(false);
    }

    @Override
    public void runWithEvent(Event event) {
      runIfChecked(event.stateMask == SWT.CTRL);
    }

    private String escapeAmp(String label) {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < label.length(); i++) {
        char ch = label.charAt(i);
        buf.append(ch);
        if (ch == '&') {
          buf.append('&');
        }
      }
      return buf.toString();
    }

    private void runIfChecked(boolean openNewSearchView) {
      if (isChecked()) {
        InternalSearchUI.getInstance().showSearchResult(fSearchView, fSearch, openNewSearchView);
      }
    }
  }

  public static final int RESULTS_IN_DROP_DOWN = 10;

  private Menu fMenu;
  private SearchView fSearchView;

  public SearchHistoryDropDownAction(SearchView searchView) {
    setText(SearchMessages.SearchDropDownAction_label);
    setToolTipText(SearchMessages.SearchDropDownAction_tooltip);
    SearchPluginImages.setImageDescriptors(
        this,
        SearchPluginImages.T_LCL,
        SearchPluginImages.IMG_LCL_SEARCH_HISTORY);
    fSearchView = searchView;
    setMenuCreator(this);
  }

  @Override
  public void dispose() {
    disposeMenu();
  }

  @Override
  public Menu getMenu(Control parent) {
    ISearchResult currentSearch = fSearchView.getCurrentSearchResult();
    disposeMenu();

    fMenu = new Menu(parent);

    ISearchQuery[] searches = NewSearchUI.getQueries();
    if (searches.length > 0) {
      for (int i = 0; i < searches.length; i++) {
        ISearchResult search = searches[i].getSearchResult();
        ShowSearchFromHistoryAction action = new ShowSearchFromHistoryAction(search);
        action.setChecked(search.equals(currentSearch));
        addActionToMenu(fMenu, action);
      }
      new MenuItem(fMenu, SWT.SEPARATOR);
      addActionToMenu(fMenu, new ShowSearchHistoryDialogAction(fSearchView));
      addActionToMenu(fMenu, new RemoveAllSearchesAction());
    }
    return fMenu;
  }

  @Override
  public Menu getMenu(Menu parent) {
    return null;
  }

  @Override
  public void run() {
    new ShowSearchHistoryDialogAction(fSearchView).run();
  }

  public void updateEnablement() {
    boolean hasQueries = InternalSearchUI.getInstance().getSearchManager().hasQueries();
    setEnabled(hasQueries);
  }

  protected void addActionToMenu(Menu parent, Action action) {
    ActionContributionItem item = new ActionContributionItem(action);
    item.fill(parent, -1);
  }

  void disposeMenu() {
    if (fMenu != null) {
      fMenu.dispose();
    }
  }
}
