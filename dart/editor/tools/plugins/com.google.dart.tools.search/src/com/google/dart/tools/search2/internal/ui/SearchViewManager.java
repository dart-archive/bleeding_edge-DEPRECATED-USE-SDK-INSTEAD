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
import com.google.dart.tools.search.internal.ui.util.ExceptionHandler;
import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.ISearchResultViewPart;
import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

/**
 * Manages the search view.
 */
public class SearchViewManager {

  private IQueryListener fNewQueryListener;

  public SearchViewManager(QueryManager queryManager) {
    fNewQueryListener = new IQueryListener() {

      @Override
      public void queryAdded(ISearchQuery query) {
        showNewSearchQuery(query);
      }

      @Override
      public void queryFinished(ISearchQuery query) {
      }

      @Override
      public void queryRemoved(ISearchQuery query) {
      }

      @Override
      public void queryStarting(ISearchQuery query) {
      }

    };

    queryManager.addQueryListener(fNewQueryListener);

  }

  public ISearchResultViewPart activateSearchView(boolean useForNewSearch) {
    IWorkbenchPage activePage = SearchPlugin.getActivePage();

    String defaultPerspectiveId = NewSearchUI.getDefaultPerspectiveId();
    if (defaultPerspectiveId != null) {
      IWorkbenchWindow window = activePage.getWorkbenchWindow();
      if (window != null && window.getShell() != null && !window.getShell().isDisposed()) {
        try {
          activePage = PlatformUI.getWorkbench().showPerspective(defaultPerspectiveId, window);
        } catch (WorkbenchException ex) {
          // show view in current perspective
        }
      }
    }
    if (activePage != null) {
      try {
        return (ISearchResultViewPart) activePage.showView(NewSearchUI.SEARCH_VIEW_ID);
      } catch (PartInitException ex) {
        ExceptionHandler.handle(
            ex,
            SearchMessages.Search_Error_openResultView_title,
            SearchMessages.Search_Error_openResultView_message);
      }
    }
    return null;
  }

  public void activateSearchView(ISearchResultViewPart viewPart) {
    try {
      IWorkbenchPage activePage = viewPart.getSite().getPage();
      String secondaryId = viewPart.getViewSite().getSecondaryId();
      activePage.showView(NewSearchUI.SEARCH_VIEW_ID, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
    } catch (PartInitException ex) {
      ExceptionHandler.handle(
          ex,
          SearchMessages.Search_Error_openResultView_title,
          SearchMessages.Search_Error_openResultView_message);
    }
  }

  public void dispose(QueryManager queryManager) {
    queryManager.removeQueryListener(fNewQueryListener);
  }

  public SearchView getActiveSearchView() {
    IWorkbenchPage activePage = SearchPlugin.getActivePage();
    if (activePage != null) {
      return (SearchView) activePage.findView(NewSearchUI.SEARCH_VIEW_ID);
    }
    return null;
  }

  public boolean isShown(ISearchQuery query) {
    SearchView view = getActiveSearchView();
    if (view != null) {
      ISearchResult currentSearchResult = view.getCurrentSearchResult();
      if (currentSearchResult != null && query == currentSearchResult.getQuery()) {
        return true;
      }
    }
    return false;
  }

  public void searchViewActivated(SearchView view) {

  }

  public void searchViewClosed(SearchView view) {
  }

  protected boolean showNewSearchQuery(ISearchQuery query) {
    SearchView view = getActiveSearchView();
    if (view != null) {
      view.showSearchResult(query.getSearchResult());
      return true;
    }
    return false;
  }

}
