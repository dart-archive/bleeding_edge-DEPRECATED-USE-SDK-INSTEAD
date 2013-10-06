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
package com.google.dart.engine.resolver;

import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.internal.resolver.MemberMap;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.element.ElementFactory.methodElement;

import junit.framework.TestCase;

public class MemberMapTest extends TestCase {

  /**
   * The null type.
   */
  private InterfaceType nullType;

  @Override
  public void setUp() {
    nullType = new TestTypeProvider().getNullType();
  }

  public void test_MemberMap_copyConstructor() {
    MethodElement m1 = methodElement("m1", nullType);
    MethodElement m2 = methodElement("m2", nullType);
    MethodElement m3 = methodElement("m3", nullType);

    MemberMap map = new MemberMap();
    map.put(m1.getName(), m1);
    map.put(m2.getName(), m2);
    map.put(m3.getName(), m3);

    MemberMap copy = new MemberMap(map);
    assertEquals(map.getSize(), copy.getSize());
    assertEquals(m1, copy.get(m1.getName()));
    assertEquals(m2, copy.get(m2.getName()));
    assertEquals(m3, copy.get(m3.getName()));
  }

  public void test_MemberMap_override() {
    MethodElement m1 = methodElement("m", nullType);
    MethodElement m2 = methodElement("m", nullType);

    MemberMap map = new MemberMap();
    map.put(m1.getName(), m1);
    map.put(m2.getName(), m2);

    assertEquals(1, map.getSize());
    assertEquals(m2, map.get("m"));
  }

  public void test_MemberMap_put() {
    MethodElement m1 = methodElement("m1", nullType);

    MemberMap map = new MemberMap();
    assertEquals(0, map.getSize());
    map.put(m1.getName(), m1);
    assertEquals(1, map.getSize());
    assertEquals(m1, map.get("m1"));
  }

}
