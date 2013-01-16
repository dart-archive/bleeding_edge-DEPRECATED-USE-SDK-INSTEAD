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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CamelCaseSearchPatternTest extends EngineTestCase {
  public void test_matchExact_samePartCount() throws Exception {
    Element element = mock(Element.class);
    when(element.getName()).thenReturn("HashMap");
    //
    CamelCaseSearchPattern pattern = new CamelCaseSearchPattern("HM", true);
    assertSame(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_matchExact_withLowerCase() throws Exception {
    Element element = mock(Element.class);
    when(element.getName()).thenReturn("HashMap");
    //
    CamelCaseSearchPattern pattern = new CamelCaseSearchPattern("HaMa", true);
    assertSame(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_matchNot_nullName() throws Exception {
    Element element = mock(Element.class);
    when(element.getName()).thenReturn(null);
    //
    CamelCaseSearchPattern pattern = new CamelCaseSearchPattern("HM", true);
    assertSame(null, pattern.matches(element));
  }

  public void test_matchNot_samePartCount() throws Exception {
    Element element = mock(Element.class);
    when(element.getName()).thenReturn("LinkedHashMap");
    //
    CamelCaseSearchPattern pattern = new CamelCaseSearchPattern("LH", true);
    assertSame(null, pattern.matches(element));
  }

  public void test_matchNot_withLowerCase() throws Exception {
    Element element = mock(Element.class);
    when(element.getName()).thenReturn("HashMap");
    //
    CamelCaseSearchPattern pattern = new CamelCaseSearchPattern("HaMu", true);
    assertSame(null, pattern.matches(element));
  }
}
