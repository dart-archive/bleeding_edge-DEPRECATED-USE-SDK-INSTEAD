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

}
