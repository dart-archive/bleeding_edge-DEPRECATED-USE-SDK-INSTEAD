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

public class ExactSearchPatternTest extends EngineTestCase {
  private final Element element = mock(Element.class);

  public void test_caseInsensitive_false() throws Exception {
    SearchPattern pattern = new ExactSearchPattern("HashMa", false);
    when(element.getName()).thenReturn("HashMap");
    // validate
    assertSame(null, pattern.matches(element));
  }

  public void test_caseInsensitive_true() throws Exception {
    SearchPattern pattern = new ExactSearchPattern("HashMap", false);
    when(element.getName()).thenReturn("HashMaP");
    // validate
    assertSame(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_caseSensitive_false() throws Exception {
    SearchPattern pattern = new ExactSearchPattern("HashMa", true);
    when(element.getName()).thenReturn("HashMap");
    // validate
    assertSame(null, pattern.matches(element));
  }

  public void test_caseSensitive_true() throws Exception {
    SearchPattern pattern = new ExactSearchPattern("HashMap", true);
    when(element.getName()).thenReturn("HashMap");
    // validate
    assertSame(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_nullName() throws Exception {
    SearchPattern pattern = new ExactSearchPattern("HashMap", true);
    when(element.getName()).thenReturn(null);
    // validate
    assertSame(null, pattern.matches(element));
  }
}
