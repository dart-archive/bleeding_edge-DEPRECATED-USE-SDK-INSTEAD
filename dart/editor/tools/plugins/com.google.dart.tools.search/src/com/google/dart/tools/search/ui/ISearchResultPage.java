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

import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Interface to be implemented by contributors to the extension point
 * <code>com.google.dart.tools.search.searchResultViewPages</code>. A <code>ISearchResultPage</code>
 * is used to show the search results for a particular class of <code>ISearchResult</code> (as
 * specified in the <code>searchResultClass</code> attribute of the extension point) in the search
 * result view. <br>
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface ISearchResultPage extends IPageBookViewPage {
  /**
   * Returns an object representing the current user interface state of the page. For example, the
   * current selection in a viewer. The UI state will be later passed into the
   * <code>setInput()</code> method when the currently shown <code>ISearchResult</code> is shown
   * again.
   * 
   * @return an object representing the UI state of this page
   */
  Object getUIState();

  /**
   * Sets the search result to be shown in this search results page. Implementers should restore UI
   * state (e.g. selection) from the previously saved <code>uiState</code> object.
   * 
   * @param search the search result to be shown or <code>null</code> to clear the page.
   * @param uiState the previously saved UI state
   * @see ISearchResultPage#getUIState()
   */
  void setInput(ISearchResult search, Object uiState);

  /**
   * Sets the search view this search results page is shown in. This method will be called before
   * the page is shown for the first time (i.e. before the page control is created).
   * 
   * @param part the parent search view
   */
  void setViewPart(ISearchResultViewPart part);

  /**
   * Restores the page state. Note that this applies only to state that is saved across sessions.
   * 
   * @param memento a memento to restore the page state from or <code>null</code> if no previous
   *          state was saved
   * @see #setInput(ISearchResult, Object)
   */
  void restoreState(IMemento memento);

  /**
   * Saves the page state in a memento. Note that this applies to state that should persist across
   * sessions.
   * 
   * @param memento a memento to receive the object state
   * @see #getUIState()
   */
  void saveState(IMemento memento);

  /**
   * Sets the id for this page. This method will be called before any other initialization is done.
   * 
   * @param id the id for this page
   */
  void setID(String id);

  /**
   * Returns the id set via <code>setID</code>.
   * 
   * @return the id of this page
   */
  String getID();

  /**
   * Returns a user readable label for this search result page. The label will be used to describe
   * the contents for the page to the user (for example it will be displayed in the search view
   * title bar). To take an example from file search, a label might read like this: 'test' - 896
   * matches in workspace.
   * 
   * @return the user readable label for this search result page
   */
  String getLabel();
}
