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
package com.google.dart.engine.search;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.source.SourceRange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchMatchTest extends EngineTestCase {
  private final Element element = mock(Element.class);

  public void test_comparatorByName() throws Exception {
    Element elementA = mock(Element.class);
    Element elementB = mock(Element.class);
    when(elementA.getName()).thenReturn("A");
    when(elementB.getName()).thenReturn("B");
    SearchMatch matchA = new SearchMatch(
        MatchQuality.EXACT,
        MatchKind.TYPE_REFERENCE,
        elementA,
        new SourceRange(10, 5));
    SearchMatch matchB = new SearchMatch(
        MatchQuality.EXACT,
        MatchKind.TYPE_REFERENCE,
        elementB,
        new SourceRange(20, 10));
    // compare
    assertEquals(-1, SearchMatch.SORT_BY_ELEMENT_NAME.compare(matchA, matchB));
    assertEquals(0, SearchMatch.SORT_BY_ELEMENT_NAME.compare(matchA, matchA));
    assertEquals(1, SearchMatch.SORT_BY_ELEMENT_NAME.compare(matchB, matchA));
  }

  public void test_equals() throws Exception {
    Element elementA = mock(Element.class);
    Element elementB = mock(Element.class);
    when(elementA.getName()).thenReturn("A");
    when(elementB.getName()).thenReturn("B");
    SearchMatch matchA = new SearchMatch(
        MatchQuality.EXACT,
        MatchKind.TYPE_REFERENCE,
        elementA,
        new SourceRange(10, 5));
    // not SearchMatch
    assertFalse(matchA.equals(null));
    // same object
    assertTrue(matchA.equals(matchA));
    // same properties
    {
      SearchMatch matchB = new SearchMatch(
          MatchQuality.EXACT,
          MatchKind.TYPE_REFERENCE,
          elementA,
          new SourceRange(10, 5));
      assertTrue(matchA.equals(matchB));
      // change "qualified"
      matchB.setQualified(true);
      assertFalse(matchA.equals(matchB));
    }
  }

  public void test_new() throws Exception {
    SearchMatch match = new SearchMatch(
        MatchQuality.EXACT,
        MatchKind.TYPE_REFERENCE,
        element,
        new SourceRange(10, 5));
    assertSame(MatchQuality.EXACT, match.getQuality());
    assertSame(MatchKind.TYPE_REFERENCE, match.getKind());
    assertEquals(element, match.getElement());
    assertEquals(new SourceRange(10, 5), match.getSourceRange());
    // defaults
    assertEquals(null, match.getImportPrefix());
    assertEquals(false, match.isQualified());
    // toString()
    assertEquals("SearchMatch(kind=TYPE_REFERENCE, quality=EXACT, element=null, "
        + "range=[offset=10, length=5], qualified=false)", match.toString());
  }

  public void test_setImportPrefix() throws Exception {
    SearchMatch match = new SearchMatch(
        MatchQuality.EXACT,
        MatchKind.TYPE_REFERENCE,
        element,
        new SourceRange(10, 5));
    match.setImportPrefix("prf");
    assertEquals("prf", match.getImportPrefix());
    // toString()
    assertEquals("SearchMatch(kind=TYPE_REFERENCE, quality=EXACT, element=null, "
        + "range=[offset=10, length=5], qualified=false, importPrefix=prf)", match.toString());
  }

  public void test_setQualified() throws Exception {
    SearchMatch match = new SearchMatch(
        MatchQuality.EXACT,
        MatchKind.TYPE_REFERENCE,
        element,
        new SourceRange(10, 5));
    match.setQualified(true);
    assertEquals(true, match.isQualified());
    // toString()
    assertEquals("SearchMatch(kind=TYPE_REFERENCE, quality=EXACT, element=null, "
        + "range=[offset=10, length=5], qualified=true)", match.toString());
  }
}
