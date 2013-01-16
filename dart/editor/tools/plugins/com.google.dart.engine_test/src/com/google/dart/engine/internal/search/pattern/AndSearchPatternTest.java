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
package com.google.dart.engine.internal.search.pattern;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.MatchQuality;
import com.google.dart.engine.search.SearchPattern;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AndSearchPatternTest extends EngineTestCase {
  private final Element element = mock(Element.class);
  private final SearchPattern patternA = mock(SearchPattern.class);
  private final SearchPattern patternB = mock(SearchPattern.class);
  private final AndSearchPattern pattern = new AndSearchPattern(patternA, patternB);

  public void test_allExact() throws Exception {
    when(patternA.matches(element)).thenReturn(MatchQuality.EXACT);
    when(patternB.matches(element)).thenReturn(MatchQuality.EXACT);
    // validate
    assertSame(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_ExactName() throws Exception {
    when(patternA.matches(element)).thenReturn(MatchQuality.EXACT);
    when(patternB.matches(element)).thenReturn(MatchQuality.NAME);
    // validate
    assertSame(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_NameExact() throws Exception {
    when(patternA.matches(element)).thenReturn(MatchQuality.NAME);
    when(patternB.matches(element)).thenReturn(MatchQuality.EXACT);
    // validate
    assertSame(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_oneNull() throws Exception {
    when(patternA.matches(element)).thenReturn(MatchQuality.EXACT);
    when(patternB.matches(element)).thenReturn(null);
    // validate
    assertSame(null, pattern.matches(element));
  }
}
