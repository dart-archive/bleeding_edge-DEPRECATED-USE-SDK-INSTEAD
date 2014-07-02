/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.asserts;

import com.google.dart.engine.source.Source;
import com.google.dart.server.Element;

/**
 * A helper for validating a {@link Element}.
 */
public class ElementAssert {
  private final Element element;
  private final String description;

  public ElementAssert(Element element) {
    this.element = element;
    this.description = "target=" + element + "\n";
  }

  public ElementAssert hasLength(int expected) {
    // Element API has changed
//    assertNotNull("Length " + expected + " expected, but null found", element);
//    assertEquals(description, expected, element.getLength());
    return this;
  }

  public ElementAssert hasOffset(int expected) {
    // Element API has changed
//    assertNotNull("Offset " + expected + " expected, but null found", element);
//    assertEquals(description, expected, element.getOffset());
    return this;
  }

  public ElementAssert isIn(Source source, int expectedOffset) throws Exception {
    // Element API has changed
//    Source targetSource = element.getSource();
//    assertTrue(description + "expected to be in " + source, source.equals(targetSource));
    // check offset
    hasOffset(expectedOffset);
    return this;
  }

  public ElementAssert isIn(Source source, String search) throws Exception {
    int expectedOffset = NavigationRegionsAssert.findOffset(source, search);
    return isIn(source, expectedOffset);
  }

  public ElementAssert isInSdk() {
    // Element API has changed
//    assertTrue(description + "expected to be in SDK", element.getSource().isInSystemLibrary());
    return this;
  }
}
