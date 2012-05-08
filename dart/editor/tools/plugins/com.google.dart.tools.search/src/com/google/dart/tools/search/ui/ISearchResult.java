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

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Implementors of this interface represent the result of a search. How the results of a search are
 * structured is up to the implementor of this interface. The abstract base implementation provided
 * with {@link com.google.dart.tools.search.ui.text.AbstractTextSearchResult
 * AbstractTextSearchResult} uses a flat list of matches to represent the result of a search.
 * Subclasses of <code>SearchResultEvent</code> can be used in order to notify listeners of search
 * result changes.
 * <p>
 * To present search results to the user implementors of this interface must also provide an
 * extension for the extension point <code>com.google.dart.tools.search.searchResultViewPage</code>.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see com.google.dart.tools.search.ui.ISearchResultPage
 */
public interface ISearchResult {
  /**
   * Adds a <code>ISearchResultListener</code>. Has no effect when the listener has already been
   * added.
   * 
   * @param l the listener to be added
   */
  void addListener(ISearchResultListener l);

  /**
   * Removes a <code>ISearchResultChangedListener</code>. Has no effect when the listener hasn't
   * previously been added.
   * 
   * @param l the listener to be removed
   */
  void removeListener(ISearchResultListener l);

  /**
   * Returns a user readable label for this search result. The label is typically used in the result
   * view and should contain the search query string and number of matches.
   * 
   * @return the label for this search result
   */
  String getLabel();

  /**
   * Returns a tooltip to be used when this search result is shown in the UI.
   * 
   * @return a user readable String
   */
  String getTooltip();

  /**
   * Returns an image descriptor for the given ISearchResult.
   * 
   * @return an image representing this search result or <code>null</code>
   */
  ImageDescriptor getImageDescriptor();

  /**
   * Returns the query that produced this search result.
   * 
   * @return the query producing this result
   */
  ISearchQuery getQuery();
}
