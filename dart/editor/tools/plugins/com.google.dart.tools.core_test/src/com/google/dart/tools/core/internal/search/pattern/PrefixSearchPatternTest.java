/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.search.pattern;

import com.google.dart.tools.core.mock.MockDartElement;
import com.google.dart.tools.core.search.MatchQuality;

import junit.framework.TestCase;

public class PrefixSearchPatternTest extends TestCase {
  public void test_PrefixSearchPattern_matches_exact() {
    String name = "Array";
    PrefixSearchPattern pattern = new PrefixSearchPattern(name, false);
    MockDartElement element = new MockDartElement(name);
    assertEquals(MatchQuality.EXACT, pattern.matches(element));
  }

  public void test_PrefixSearchPattern_matches_false() {
    PrefixSearchPattern pattern = new PrefixSearchPattern("Array", false);
    MockDartElement element = new MockDartElement("IArray");
    assertNull(pattern.matches(element));
  }

  public void test_PrefixSearchPattern_matches_prefix() {
    PrefixSearchPattern pattern = new PrefixSearchPattern("Array", false);
    MockDartElement element = new MockDartElement("ArrayImpl");
    assertEquals(MatchQuality.EXACT, pattern.matches(element));
  }
}
