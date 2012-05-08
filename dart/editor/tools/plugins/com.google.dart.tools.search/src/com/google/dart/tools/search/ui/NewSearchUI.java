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
package com.google.dart.tools.search.ui;

import com.google.dart.tools.search.internal.ui.OpenSearchDialogAction;
import com.google.dart.tools.search.internal.ui.SearchPreferencePage;
import com.google.dart.tools.search2.internal.ui.InternalSearchUI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A facade for access to the new search UI facilities.
 */
public class NewSearchUI {
  /**
   * Search Plug-in Id (value <code>"com.google.dart.tools.search"</code>).
   */
  public static final String PLUGIN_ID = "com.google.dart.tools.search"; //$NON-NLS-1$
  /**
   * Search marker type (value <code>"com.google.dart.tools.search.searchmarker"</code>).
   * 
   * @see org.eclipse.core.resources.IMarker
   */
  public static final String SEARCH_MARKER = PLUGIN_ID + ".searchmarker"; //$NON-NLS-1$
  /**
   * Id of the new Search view (value
   * <code>"com.google.dart.tools.search.ui.views.SearchView"</code>).
   */
  public static final String SEARCH_VIEW_ID = "com.google.dart.tools.search.ui.views.SearchView"; //$NON-NLS-1$

  /**
   * Id of the Search action set (value <code>"com.google.dart.tools.search.searchActionSet"</code>
   * ).
   */
  public static final String ACTION_SET_ID = PLUGIN_ID + ".searchActionSet"; //$NON-NLS-1$

  /**
   * Activates a search result view in the current workbench window page. If a search view is
   * already open in the current workbench window page, it is activated. Otherwise a new search view
   * is opened and activated.
   * 
   * @return the activate search result view or <code>null</code> if the search result view couldn't
   *         be activated
   */
  public static ISearchResultViewPart activateSearchResultView() {
    return InternalSearchUI.getInstance().getSearchViewManager().activateSearchView(false);
  }

  /**
   * Registers the given listener to receive notification of changes to queries. The listener will
   * be notified whenever a query has been added, removed, is starting or has finished. Has no
   * effect if an identical listener is already registered.
   * 
   * @param l the listener to be added
   */
  public static void addQueryListener(IQueryListener l) {
    InternalSearchUI.getInstance().addQueryListener(l);
  }

  /**
   * Returns the preference whether a search engine is allowed to report potential matches or not.
   * <p>
   * Search engines which can report inexact matches must respect this preference i.e. they should
   * not report inexact matches if this method returns <code>true</code>
   * </p>
   * 
   * @return <code>true</code> if search engine must not report inexact matches
   */
  public static boolean arePotentialMatchesIgnored() {
    return false;
//    return SearchPreferencePage.arePotentialMatchesIgnored();
  }

  /**
   * Sends a 'cancel' command to the given query running in background. The call has no effect if
   * the query is not running, not in background or is not cancelable.
   * 
   * @param query the query
   */
  public static void cancelQuery(ISearchQuery query) {
    if (query == null) {
      throw new IllegalArgumentException("query must not be null"); //$NON-NLS-1$
    }
    InternalSearchUI.getInstance().cancelSearch(query);
  }

  /**
   * Returns the ID of the default perspective.
   * <p>
   * The perspective with this ID will be used to show the Search view. If no default perspective is
   * set then the Search view will appear in the current perspective.
   * </p>
   * 
   * @return the ID of the default perspective <code>null</code> if no default perspective is set
   */
  public static String getDefaultPerspectiveId() {
    return SearchPreferencePage.getDefaultPerspectiveId();
  }

  /**
   * Returns all search queries know to the search UI (i.e. registered via <code>runQuery()</code>
   * or <code>runQueryInForeground())</code>.
   * 
   * @return all search results
   */
  public static ISearchQuery[] getQueries() {
    return InternalSearchUI.getInstance().getQueries();
  }

  /**
   * Gets the search result view shown in the current workbench window.
   * 
   * @return the search result view or <code>null</code>, if none is open in the current workbench
   *         window page
   */
  public static ISearchResultViewPart getSearchResultView() {
    return InternalSearchUI.getInstance().getSearchViewManager().getActiveSearchView();
  }

  /**
   * Returns whether the given query is currently running. Queries may be run by client request or
   * by actions in the search UI.
   * 
   * @param query the query
   * @return whether the given query is currently running
   * @see NewSearchUI#runQueryInBackground(ISearchQuery)
   * @see NewSearchUI#runQueryInForeground(IRunnableContext, ISearchQuery)
   */
  public static boolean isQueryRunning(ISearchQuery query) {
    if (query == null) {
      throw new IllegalArgumentException("query must not be null"); //$NON-NLS-1$
    }
    return InternalSearchUI.getInstance().isQueryRunning(query);
  }

  /**
   * Opens the search dialog. If <code>pageId</code> is specified and a corresponding page is found
   * then it is brought to top.
   * 
   * @param window the parent window
   * @param pageId the page to select or <code>null</code> if the best fitting page should be
   *          selected
   */
  public static void openSearchDialog(IWorkbenchWindow window, String pageId) {
    new OpenSearchDialogAction(window, pageId).run();
  }

  /**
   * Removes the given search query.
   * 
   * @param query the query to be removed
   */
  public static void removeQuery(ISearchQuery query) {
    InternalSearchUI.getInstance().removeQuery(query);
  }

  /**
   * Removes the given query listener. Does nothing if the listener is not present.
   * 
   * @param l the listener to be removed.
   */
  public static void removeQueryListener(IQueryListener l) {
    InternalSearchUI.getInstance().removeQueryListener(l);
  }

  /**
   * Returns the preference whether editors should be reused when showing search results. The goto
   * action can decide to use or ignore this preference.
   * 
   * @return <code>true</code> if editors should be reused for showing search results
   */
  public static boolean reuseEditor() {
    return SearchPreferencePage.isEditorReused();
  }

  /**
   * Runs the given search query. This method will execute the query in a background thread and not
   * block until the search is finished. Running a query adds it to the set of known queries and
   * notifies any registered {@link IQueryListener}s about the addition.
   * <p>
   * The search view that shows the result will be activated. That means a call to
   * {@link #activateSearchResultView} is not required.
   * </p>
   * 
   * @param query the query to execute. The query must be able to run in background, that means
   *          {@link ISearchQuery#canRunInBackground()} must be <code>true</code>
   * @throws IllegalArgumentException Thrown when the passed query is not able to run in background
   */
  public static void runQueryInBackground(ISearchQuery query) throws IllegalArgumentException {
    if (query == null) {
      throw new IllegalArgumentException("query must not be null"); //$NON-NLS-1$
    }
    runQueryInBackground(query, null);
  }

  /**
   * Runs the given search query. This method will execute the query in a background thread and not
   * block until the search is finished. Running a query adds it to the set of known queries and
   * notifies any registered {@link IQueryListener}s about the addition.
   * <p>
   * The result will be shown in the given search result view which will be activated. A call to to
   * {@link #activateSearchResultView} is not required.
   * </p>
   * 
   * @param query the query to execute. The query must be able to run in background, that means
   *          {@link ISearchQuery#canRunInBackground()} must be <code>true</code>
   * @param view the search result view to show the result in. If <code>null</code> is passed in,
   *          the default activation mechanism is used to open a new result view or to select the
   *          view to be reused.
   * @throws IllegalArgumentException Thrown when the passed query is not able to run in background
   */
  public static void runQueryInBackground(ISearchQuery query, ISearchResultViewPart view)
      throws IllegalArgumentException {
    if (query == null) {
      throw new IllegalArgumentException("query must not be null"); //$NON-NLS-1$
    }
    if (query.canRunInBackground()) {
      InternalSearchUI.getInstance().runSearchInBackground(query, view);
    } else {
      throw new IllegalArgumentException("Query can not be run in background"); //$NON-NLS-1$
    }
  }

  /**
   * Runs the given search query. This method will execute the query in the same thread as the
   * caller. This method blocks until the query is finished. Running a query adds it to the set of
   * known queries and notifies any registered {@link IQueryListener}s about the addition.
   * <p>
   * The result will be shown in a search view that will be activated. That means a call to
   * {@link #activateSearchResultView} is not required.
   * </p>
   * 
   * @param context the runnable context to run the query in
   * @param query the query to execute
   * @return a status indicating whether the query ran correctly, including {@link IStatus#CANCEL}
   *         to signal that the query was canceled.
   */
  public static IStatus runQueryInForeground(IRunnableContext context, ISearchQuery query) {
    if (query == null) {
      throw new IllegalArgumentException("query must not be null"); //$NON-NLS-1$
    }
    return runQueryInForeground(context, query, null);
  }

  /**
   * Runs the given search query. This method will execute the query in the same thread as the
   * caller. This method blocks until the query is finished. Running a query adds it to the set of
   * known queries and notifies any registered {@link IQueryListener}s about the addition.
   * <p>
   * The result will be shown in the given search result view which will be activated. A call to to
   * {@link #activateSearchResultView} is not required.
   * </p>
   * 
   * @param context the runnable context to run the query in
   * @param query the query to execute
   * @param view the search result view to show the result in. If <code>null</code> is passed in,
   *          the default activation mechanism is used to open a new result view or to select the
   *          view to be reused.
   * @return a status indicating whether the query ran correctly, including {@link IStatus#CANCEL}
   *         to signal that the query was canceled.
   */
  public static IStatus runQueryInForeground(IRunnableContext context, ISearchQuery query,
      ISearchResultViewPart view) {
    if (query == null) {
      throw new IllegalArgumentException("query must not be null"); //$NON-NLS-1$
    }
    return InternalSearchUI.getInstance().runSearchInForeground(context, query, view);
  }

}
