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
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CountingSearchListenerTest extends EngineTestCase {
  public void test_matchFound() throws Exception {
    SearchListener listener = mock(SearchListener.class);
    SearchMatch match = mock(SearchMatch.class);
    SearchListener countingListener = new CountingSearchListener(2, listener);
    // "match" should be passed to "listener"
    countingListener.matchFound(match);
    verify(listener).matchFound(match);
    verifyNoMoreInteractions(listener);
  }

  public void test_searchComplete() throws Exception {
    SearchListener listener = mock(SearchListener.class);
    SearchListener countingListener = new CountingSearchListener(2, listener);
    // complete 2 -> 1
    countingListener.searchComplete();
    verifyZeroInteractions(listener);
    // complete 2 -> 0
    countingListener.searchComplete();
    verify(listener).searchComplete();
  }

  public void test_searchComplete_zero() throws Exception {
    SearchListener listener = mock(SearchListener.class);
    new CountingSearchListener(0, listener);
    // complete at 0
    verify(listener).searchComplete();
  }
}
