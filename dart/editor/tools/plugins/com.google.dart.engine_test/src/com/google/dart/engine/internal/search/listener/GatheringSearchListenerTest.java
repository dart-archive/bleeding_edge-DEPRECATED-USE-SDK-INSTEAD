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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchMatch;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GatheringSearchListenerTest extends EngineTestCase {
  private final SearchMatch matchA = mock(SearchMatch.class);
  private final SearchMatch matchB = mock(SearchMatch.class);
  private final GatheringSearchListener gatheringListener = new GatheringSearchListener();

  public void test_matchFound() throws Exception {
    Element elementA = mock(Element.class);
    Element elementB = mock(Element.class);
    when(elementA.getName()).thenReturn("A");
    when(elementB.getName()).thenReturn("B");
    when(matchA.getElement()).thenReturn(elementA);
    when(matchB.getElement()).thenReturn(elementB);
    // matchB
    gatheringListener.matchFound(matchB);
    assertFalse(gatheringListener.isComplete());
    assertThat(gatheringListener.getMatches()).containsExactly(matchB);
    // matchA
    gatheringListener.matchFound(matchA);
    assertFalse(gatheringListener.isComplete());
    assertThat(gatheringListener.getMatches()).containsExactly(matchA, matchB);
  }

  public void test_searchComplete() throws Exception {
    assertFalse(gatheringListener.isComplete());
    // complete
    gatheringListener.searchComplete();
    assertTrue(gatheringListener.isComplete());
  }
}
