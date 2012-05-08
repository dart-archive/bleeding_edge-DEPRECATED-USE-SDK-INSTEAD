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

import org.eclipse.jface.action.IMenuManager;

import org.eclipse.ui.IViewPart;

/**
 * <p>
 * Interface for the search result view. The search result view is responsible for managing the set
 * of search result and delegates display of search results to the appropriate
 * <code>ISearchResultPage</code>. Clients may access the search result view via the
 * <code>NewSearchUI</code> facade class.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients
 * </p>
 * 
 * @see NewSearchUI#activateSearchResultView()
 * @see NewSearchUI#getSearchResultView()
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISearchResultViewPart extends IViewPart {
  /**
   * Search result pages should call this method to have the search results view contribute to their
   * context menus.
   * 
   * @param menuManager the menu manager the search result view should contribute to
   */
  void fillContextMenu(IMenuManager menuManager);

  /**
   * Returns the <code>ISearchResultPage</code> currently shown in this search view. Returns
   * <code>null</code> if no page is currently shown.
   * 
   * @return the active <code>ISearchResultPage</code> or <code>null</code>
   */
  ISearchResultPage getActivePage();

  /**
   * Requests that the search view updates the label it is showing for search result pages.
   * Typically, a search result page will call this method when the search result it's displaying is
   * updated.
   * 
   * @see ISearchResultPage#getLabel()
   */
  void updateLabel();
}
