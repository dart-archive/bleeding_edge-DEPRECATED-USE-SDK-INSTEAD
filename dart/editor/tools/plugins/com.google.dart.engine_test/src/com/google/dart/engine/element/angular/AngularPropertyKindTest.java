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
package com.google.dart.engine.element.angular;

import com.google.dart.engine.EngineTestCase;

public class AngularPropertyKindTest extends EngineTestCase {
  public void test_ATTR() {
    AngularPropertyKind kind = AngularPropertyKind.ATTR;
    assertFalse(kind.callsGetter());
    assertTrue(kind.callsSetter());
  }

  public void test_CALLBACK() {
    AngularPropertyKind kind = AngularPropertyKind.CALLBACK;
    assertFalse(kind.callsGetter());
    assertTrue(kind.callsSetter());
  }

  public void test_ONE_WAY() {
    AngularPropertyKind kind = AngularPropertyKind.ONE_WAY;
    assertFalse(kind.callsGetter());
    assertTrue(kind.callsSetter());
  }

  public void test_ONE_WAY_ONE_TIME() {
    AngularPropertyKind kind = AngularPropertyKind.ONE_WAY_ONE_TIME;
    assertFalse(kind.callsGetter());
    assertTrue(kind.callsSetter());
  }

  public void test_TWO_WAY() {
    AngularPropertyKind kind = AngularPropertyKind.TWO_WAY;
    assertTrue(kind.callsGetter());
    assertTrue(kind.callsSetter());
  }
}
