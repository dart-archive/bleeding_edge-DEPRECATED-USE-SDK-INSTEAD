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

import java.util.EventObject;

/**
 * The common superclass of all events sent from <code>ISearchResults</code>. This class is supposed
 * to be subclassed to provide more specific notification.
 * 
 * @see com.google.dart.tools.search.ui.ISearchResultListener#searchResultChanged(SearchResultEvent)
 */
public abstract class SearchResultEvent extends EventObject {

  private static final long serialVersionUID = -4877459368182725252L;

  /**
   * Creates a new search result event for the given search result.
   * 
   * @param searchResult the source of the event
   */
  protected SearchResultEvent(ISearchResult searchResult) {
    super(searchResult);
  }

  /**
   * Gets the <code>ISearchResult</code> for this event.
   * 
   * @return the source of this event
   */
  public ISearchResult getSearchResult() {
    return (ISearchResult) getSource();
  }
}
