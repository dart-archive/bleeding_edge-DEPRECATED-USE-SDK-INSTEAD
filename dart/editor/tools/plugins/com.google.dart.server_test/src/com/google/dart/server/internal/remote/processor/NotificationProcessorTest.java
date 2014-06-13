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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.ElementKind;

import static com.google.dart.server.internal.remote.processor.NotificationProcessor.getElementKind;
import junit.framework.TestCase;

public class NotificationProcessorTest extends TestCase {

  public void test_getElementKind() throws Exception {
    assertSame(ElementKind.CLASS, getElementKind("CLASS"));
    assertSame(ElementKind.CLASS_TYPE_ALIAS, getElementKind("CLASS_TYPE_ALIAS"));
    assertSame(ElementKind.COMPILATION_UNIT, getElementKind("COMPILATION_UNIT"));
    assertSame(ElementKind.CONSTRUCTOR, getElementKind("CONSTRUCTOR"));
    assertSame(ElementKind.FIELD, getElementKind("FIELD"));
  }

  public void test_getErrorCode_unknown() throws Exception {
    try {
      getElementKind("UNKNOWN_ELEMENT");
      fail();
    } catch (IllegalArgumentException e) {
    }
  }
}
