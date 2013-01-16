/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.search.listener;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FilterSearchListenerTest extends EngineTestCase {
  private final SearchListener listener = mock(SearchListener.class);
  private final SearchMatch match = mock(SearchMatch.class);
  private final SearchFilter filter = mock(SearchFilter.class);
  private final SearchListener filteredListener = new FilteredSearchListener(filter, listener);

  public void test_matchFound_filterFalse() throws Exception {
    when(filter.passes(match)).thenReturn(false);
    // "match" should be passed to "listener"
    filteredListener.matchFound(match);
    verifyNoMoreInteractions(listener);
  }

  public void test_matchFound_filterTrue() throws Exception {
    when(filter.passes(match)).thenReturn(true);
    // "match" should be passed to "listener"
    filteredListener.matchFound(match);
    verify(listener).matchFound(match);
    verifyNoMoreInteractions(listener);
  }

  public void test_searchComplete() throws Exception {
    filteredListener.searchComplete();
    verify(listener).searchComplete();
    verifyNoMoreInteractions(listener);
  }
}
