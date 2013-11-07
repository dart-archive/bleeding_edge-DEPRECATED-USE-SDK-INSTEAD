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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.element.ElementFactory.classElement;

public class VoidTypeImplTest extends EngineTestCase {

  /**
   * Reference {code VoidTypeImpl.getInstance()}.
   */
  final Type voidType = VoidTypeImpl.getInstance();

  public void test_isMoreSpecificThan_void_A() {
    ClassElement classA = classElement("A");
    assertFalse(voidType.isMoreSpecificThan(classA.getType()));
  }

  public void test_isMoreSpecificThan_void_dynamic() {
    assertTrue(voidType.isMoreSpecificThan(DynamicTypeImpl.getInstance()));
  }

  public void test_isMoreSpecificThan_void_void() {
    assertTrue(voidType.isMoreSpecificThan(voidType));
  }

  public void test_isSubtypeOf_void_A() {
    ClassElement classA = classElement("A");
    assertFalse(voidType.isSubtypeOf(classA.getType()));
  }

  public void test_isSubtypeOf_void_dynamic() {
    assertTrue(voidType.isSubtypeOf(DynamicTypeImpl.getInstance()));
  }

  public void test_isSubtypeOf_void_void() {
    assertTrue(voidType.isSubtypeOf(voidType));
  }

  public void test_isVoid() {
    assertTrue(voidType.isVoid());
  }
}
