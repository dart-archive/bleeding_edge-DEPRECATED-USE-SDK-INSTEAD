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
package com.google.dart.tools.core.utilities.general;

import junit.framework.TestCase;

import org.junit.Assert;

public class StringUtilitiesTest extends TestCase {

  public void test_findCommonOverlap() {
    assertEquals(0, StringUtilities.findCommonOverlap("", "abcd"));
    assertEquals(3, StringUtilities.findCommonOverlap("abc", "abcd"));
    assertEquals(0, StringUtilities.findCommonOverlap("123456", "abcd"));
    assertEquals(3, StringUtilities.findCommonOverlap("123456xxx", "xxxabcd"));
  }

  public void test_findCommonPrefix() {
    assertEquals(0, StringUtilities.findCommonPrefix("abc", "xyz"));
    assertEquals(4, StringUtilities.findCommonPrefix("1234abcdef", "1234xyz"));
    assertEquals(4, StringUtilities.findCommonPrefix("1234", "1234xyz"));
  }

  public void test_findCommonSuffix() {
    assertEquals(0, StringUtilities.findCommonSuffix("abc", "xyz"));
    assertEquals(4, StringUtilities.findCommonSuffix("abcdef1234", "xyz1234"));
    assertEquals(4, StringUtilities.findCommonSuffix("1234", "xyz1234"));
  }

  public void test_StringUtilities_endsWithIgnoreCase() {
    // null cases
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase(null, null));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("", null));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase(null, ""));

    // trivial case: string is non-null and suffix is ""
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("", ""));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("a", ""));

    // non-trivial cases: ignore case not needed
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "abcd"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "bcd"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "cd"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "d"));

    // non-trivial cases: ignore case needed
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "ABCD"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "BCD"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "CD"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("abcd", "D"));

    // non-trivial cases: additional tests which tests the method in the way that the editor will
    // use this method
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.html", ".html"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.HTML", ".html"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.HtMl", ".html"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.hTmL", ".html"));

    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.dart", ".dart"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.DART", ".dart"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.DaRt", ".dart"));
    Assert.assertTrue(StringUtilities.endsWithIgnoreCase("name.dArT", ".dart"));

    // non-trivial cases again, but with an incorrect suffix
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.htm", ".html"));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.HTM", ".html"));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.HtM", ".html"));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.hTm", ".html"));

    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.dar", ".dart"));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.DAR", ".dart"));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.DaR", ".dart"));
    Assert.assertFalse(StringUtilities.endsWithIgnoreCase("name.dAr", ".dart"));
  }

  public void test_StringUtilities_parseArgumentString() {
    Assert.assertArrayEquals(
        new String[] {"one", "two"},
        StringUtilities.parseArgumentString("one two"));

    Assert.assertArrayEquals(
        new String[] {"one", "two two"},
        StringUtilities.parseArgumentString("one \"two two\""));

    Assert.assertArrayEquals(
        new String[] {"one", "arg='two two'"},
        StringUtilities.parseArgumentString("one arg='two two'"));
  }
}
