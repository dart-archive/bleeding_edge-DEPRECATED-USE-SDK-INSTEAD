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
package com.google.dart.tools.search.ui.text;

import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.SearchResultEvent;

/**
 * An event indicating that all matches have been removed from a
 * <code>AbstractTextSearchResult</code>.
 * <p>
 * Clients may instantiate or subclass this class.
 * </p>
 */
public class RemoveAllEvent extends SearchResultEvent {
  private static final long serialVersionUID = 6009335074727417445L;

  /**
   * A constructor
   * 
   * @param searchResult the search result this event is about
   */
  public RemoveAllEvent(ISearchResult searchResult) {
    super(searchResult);
  }
}
