/*
 * Copyright (c) 2012, the Dart project authors.
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

public class SourceUtilitiesTest extends TestCase {
  public void test_SourceUtilities_findInsertionPointForSource_empty() {
    assertInsertionPointForSource("", "");
  }

  public void test_SourceUtilities_findInsertionPointForSource_firstOfKind() {
    assertInsertionPointForSource("#library(\"x\");\r", "\rclass X {}");
  }

  public void test_SourceUtilities_findInsertionPointForSource_nextOfKind() {
    assertInsertionPointForSource("#library(\"x\");\r\r#source(\"remote.dart\");\r", "\rclass X {}");
  }

  public void test_SourceUtilities_lineSeparator() {
    assertNotNull(SourceUtilities.LINE_SEPARATOR);
  }

  private void assertInsertionPointForSource(String prefix, String suffix) {
    assertEquals(prefix.length(), SourceUtilities.findInsertionPointForSource(
        prefix + suffix,
        SourceUtilities.SOURCE_DIRECTIVE,
        "local.dart"));
  }
}
