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

package com.google.dart.server.internal.local.asserts;

import com.google.dart.engine.source.Source;
import com.google.dart.server.NavigationTarget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A helper for validating a {@link NavigationTarget}.
 */
public class NavigationTargetAssert {
  private final NavigationTarget target;
  private final String description;

  public NavigationTargetAssert(NavigationTarget target) {
    this.target = target;
    this.description = "target=" + target + "\n";
  }

  public NavigationTargetAssert hasLength(int expected) {
    assertNotNull("Length " + expected + " expected, but null found", target);
    assertEquals(description, expected, target.getLength());
    return this;
  }

  public NavigationTargetAssert hasOffset(int expected) {
    assertNotNull("Offset " + expected + " expected, but null found", target);
    assertEquals(description, expected, target.getOffset());
    return this;
  }

  public NavigationTargetAssert isIn(Source source, int expectedOffset) throws Exception {
    Source targetSource = target.getSource();
    assertTrue(description + "expected to be in " + source, source.equals(targetSource));
    // check offset
    hasOffset(expectedOffset);
    return this;
  }

  public NavigationTargetAssert isIn(Source source, String search) throws Exception {
    int expectedOffset = NavigationRegionsAssert.findOffset(source, search);
    return isIn(source, expectedOffset);
  }

  public NavigationTargetAssert isInSdk() {
    assertTrue(description + "expected to be in SDK", target.getSource().isInSystemLibrary());
    return this;
  }
}
