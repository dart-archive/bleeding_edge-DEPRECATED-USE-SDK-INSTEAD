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
package com.google.dart.tools.core.utilities.general;

import junit.framework.TestCase;

public class CharOperationTest extends TestCase {
  public void test_camelCaseMatch_firstCharacterMismatch() {
    assertFalse(CharOperation.camelCaseMatch("LM".toCharArray(), "HashMap".toCharArray()));
  }

  public void test_camelCaseMatch_lowerCharacterMismatch() {
    assertFalse(CharOperation.camelCaseMatch("HashMup".toCharArray(), "HashMap".toCharArray()));
  }

  public void test_camelCaseMatch_nameTooShort_lowerCases() {
    assertFalse(CharOperation.camelCaseMatch("HMaps".toCharArray(), "HashMap".toCharArray()));
  }

  public void test_camelCaseMatch_nameTooShort_upperCases() {
    assertFalse(CharOperation.camelCaseMatch("HML".toCharArray(), "HashMap".toCharArray()));
  }

  public void test_camelCaseMatch_nullName() {
    assertFalse(CharOperation.camelCaseMatch("".toCharArray(), null));
  }

  public void test_camelCaseMatch_nullPattern() {
    assertTrue(CharOperation.camelCaseMatch(null, "justAnything".toCharArray()));
  }

  public void test_camelCaseMatch_onlyUpper_exactNumber() {
    assertTrue(CharOperation.camelCaseMatch("HM".toCharArray(), "HashMap".toCharArray()));
  }

  public void test_camelCaseMatch_upperCharacterMismatch() {
    assertFalse(CharOperation.camelCaseMatch("HT".toCharArray(), "HashMap".toCharArray()));
  }

  public void test_camelCaseMatch_withDigits() {
    assertTrue(CharOperation.camelCaseMatch("S2H".toCharArray(), "String2Hash".toCharArray()));
  }

  public void test_camelCaseMatch_withDigits_ignored() {
    assertTrue(CharOperation.camelCaseMatch("SH".toCharArray(), "String2Hash".toCharArray()));
  }

  public void test_camelCaseMatch3_nullName() {
    assertFalse(CharOperation.camelCaseMatch("".toCharArray(), null, true));
  }

  public void test_camelCaseMatch3_nullPattern() {
    assertTrue(CharOperation.camelCaseMatch(null, "justAnything".toCharArray(), true));
  }

  public void test_camelCaseMatch3_onlyUpper_exactNumber() {
    assertTrue(CharOperation.camelCaseMatch("HM".toCharArray(), "HashMap".toCharArray(), true));
  }

  public void test_camelCaseMatch3_onlyUpper_lessThanNumber() {
    assertFalse(CharOperation.camelCaseMatch(
        "LH".toCharArray(),
        "LinkedHashMap".toCharArray(),
        true));
  }

  public void test_camelCaseMatch6_onlyUpper_exactNumber() {
    assertTrue(CharOperation.camelCaseMatch(
        "HM".toCharArray(),
        0,
        -1,
        "HashMap".toCharArray(),
        0,
        -1));
  }

  public void test_camelCaseMatch7_emptyNameRange() {
    assertFalse(CharOperation.camelCaseMatch(
        "HM".toCharArray(),
        0,
        -1,
        "HashMap".toCharArray(),
        2,
        0,
        true));
  }

  public void test_camelCaseMatch7_emptyPatternRange_emptyNameRange() {
    assertTrue(CharOperation.camelCaseMatch(
        "HM".toCharArray(),
        2,
        0,
        "HashMap".toCharArray(),
        2,
        0,
        true));
  }

  public void test_camelCaseMatch7_emptyPatternRange_notEmptyNameRange() {
    assertFalse(CharOperation.camelCaseMatch(
        "HM".toCharArray(),
        2,
        0,
        "HashMap".toCharArray(),
        0,
        -1,
        true));
  }

  public void test_camelCaseMatch7_negativeLength() {
    assertTrue(CharOperation.camelCaseMatch(
        "HM".toCharArray(),
        0,
        -1,
        "HashMap".toCharArray(),
        0,
        -1,
        true));
  }

  public void test_camelCaseMatch7_nullName() {
    assertFalse(CharOperation.camelCaseMatch("".toCharArray(), 0, -1, null, 0, -1, true));
  }

  public void test_camelCaseMatch7_nullPattern() {
    assertTrue(CharOperation.camelCaseMatch(null, 0, -1, "justAnything".toCharArray(), 0, -1, true));
  }

  public void test_match3_notPattern_caseMismatch_caseInsensitive() throws Exception {
    assertTrue(CharOperation.match("abc".toCharArray(), "AbC".toCharArray(), false));
  }

  public void test_match3_notPattern_caseMismatch_caseSensitive() throws Exception {
    assertFalse(CharOperation.match("abc".toCharArray(), "AbC".toCharArray(), true));
  }

  public void test_match3_notPattern_equals() throws Exception {
    assertTrue(CharOperation.match("abc".toCharArray(), "abc".toCharArray(), true));
  }

  public void test_match3_notPattern_nameTooShort() throws Exception {
    assertFalse(CharOperation.match("abcd".toCharArray(), "abc".toCharArray(), true));
  }

  public void test_match3_nullName() throws Exception {
    assertFalse(CharOperation.match("*".toCharArray(), null, true));
  }

  public void test_match3_nullPattern() throws Exception {
    assertTrue(CharOperation.match(null, "anything".toCharArray(), true));
  }

  public void test_match3_question() throws Exception {
    assertTrue(CharOperation.match("ab?d".toCharArray(), "abcd".toCharArray(), true));
  }

  public void test_match3_star_anyName() throws Exception {
    assertTrue(CharOperation.match("*".toCharArray(), "abc".toCharArray(), true));
  }

  public void test_match3_star_suffixMatch() throws Exception {
    assertTrue(CharOperation.match("*D".toCharArray(), "abcD".toCharArray(), true));
  }

  public void test_match3_star_suffixMismatch() throws Exception {
    assertFalse(CharOperation.match("*D".toCharArray(), "abcE".toCharArray(), true));
  }

  public void test_match3_star2_noSuffix() throws Exception {
    assertTrue(CharOperation.match("*D*".toCharArray(), "abcDefg".toCharArray(), true));
  }

  public void test_match3_star2_suffixMatch() throws Exception {
    assertTrue(CharOperation.match("*D*H".toCharArray(), "abcDefgH".toCharArray(), true));
  }

  public void test_match7_notPattern_equals() throws Exception {
    assertTrue(CharOperation.match("abc".toCharArray(), 0, -1, "abc".toCharArray(), 0, -1, true));
  }

  public void test_match7_nullName() throws Exception {
    assertFalse(CharOperation.match("*".toCharArray(), 0, -1, null, 0, -1, true));
  }

  public void test_match7_nullPattern() throws Exception {
    assertTrue(CharOperation.match(null, 0, -1, "anything".toCharArray(), 0, -1, true));
  }
}
