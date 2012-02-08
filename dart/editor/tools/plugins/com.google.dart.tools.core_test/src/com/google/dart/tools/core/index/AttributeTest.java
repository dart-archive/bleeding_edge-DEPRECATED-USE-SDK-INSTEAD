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
package com.google.dart.tools.core.index;

import junit.framework.TestCase;

public class AttributeTest extends TestCase {
  public void test_Attribute_getIdentifier() {
    String identifier = "can-return-null";
    Attribute attribute = Attribute.getAttribute(identifier);
    assertEquals(identifier, attribute.getIdentifier());
  }

  public void test_Attribute_getRelationship() {
    String firstId = "can-return-null";
    String secondId = "must-not-be-null";
    Attribute first = Attribute.getAttribute(firstId);
    assertNotNull(first);
    assertSame(first, Attribute.getAttribute(firstId));
    Attribute second = Attribute.getAttribute(secondId);
    assertNotNull(second);
    assertNotSame(first, second);
  }
}
