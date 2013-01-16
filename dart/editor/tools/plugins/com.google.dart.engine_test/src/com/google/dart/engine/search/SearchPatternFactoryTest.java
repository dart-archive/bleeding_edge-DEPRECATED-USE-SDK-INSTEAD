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
import com.google.dart.engine.internal.search.pattern.AndSearchPattern;
import com.google.dart.engine.internal.search.pattern.CamelCaseSearchPattern;
import com.google.dart.engine.internal.search.pattern.ExactSearchPattern;
import com.google.dart.engine.internal.search.pattern.OrSearchPattern;
import com.google.dart.engine.internal.search.pattern.PrefixSearchPattern;
import com.google.dart.engine.internal.search.pattern.RegularExpressionSearchPattern;
import com.google.dart.engine.internal.search.pattern.WildcardSearchPattern;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SearchPatternFactoryTest extends EngineTestCase {
  SearchPattern patternA = mock(SearchPattern.class);
  SearchPattern patternB = mock(SearchPattern.class);

  public void test_createAndPattern() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createAndPattern(patternA, patternB);
    assertThat(pattern).isInstanceOf(AndSearchPattern.class);
  }

  public void test_createAndPattern_single() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createAndPattern(patternA);
    assertSame(patternA, pattern);
  }

  public void test_createCamelCasePattern() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createCamelCasePattern("LHM", true);
    assertThat(pattern).isInstanceOf(CamelCaseSearchPattern.class);
  }

  public void test_createExactPattern() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createExactPattern("HashMap", false);
    assertThat(pattern).isInstanceOf(ExactSearchPattern.class);
  }

  public void test_createOrPattern() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createOrPattern(patternA, patternB);
    assertThat(pattern).isInstanceOf(OrSearchPattern.class);
  }

  public void test_createOrPattern_single() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createOrPattern(patternA);
    assertSame(patternA, pattern);
  }

  public void test_createPrefixPattern() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createPrefixPattern("HashMap", false);
    assertThat(pattern).isInstanceOf(PrefixSearchPattern.class);
  }

  public void test_createRegularExpressionPattern() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createRegularExpressionPattern("H.*Ma[a-z]", true);
    assertThat(pattern).isInstanceOf(RegularExpressionSearchPattern.class);
  }

  public void test_createWildcardPattern() throws Exception {
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern("H*Ma?", true);
    assertThat(pattern).isInstanceOf(WildcardSearchPattern.class);
  }
}
