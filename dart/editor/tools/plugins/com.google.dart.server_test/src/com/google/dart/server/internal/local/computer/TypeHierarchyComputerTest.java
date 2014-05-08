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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.source.Source;
import com.google.dart.server.Element;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.SourceSet;
import com.google.dart.server.TypeHierarchyConsumer;
import com.google.dart.server.TypeHierarchyItem;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TypeHierarchyComputerTest extends AbstractLocalServerTest {
  private String contextId;
  private String code;
  private Source source;

  public void test_bad_function() throws Exception {
    createContextWithSingleSource(makeSource(//
        "fff() {}",
        ""));
    Element element = findElement("fff() {}");
    TypeHierarchyItem item = computeHierarchy(element);
    assertNull(item);
  }

  public void test_bad_recursion() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A extends B {",
        "}",
        "class B extends A {",
        "}"));
    Element elementA = findElement("A extends B {");
    Element elementB = findElement("B extends A");
    // B
    TypeHierarchyItem itemB = computeHierarchy(elementB);
    assertEquals(elementB, itemB.getClassElement());
    // A
    TypeHierarchyItem itemA = itemB.getExtendedType();
    assertEquals(elementA, itemA.getClassElement());
    assertEquals(null, itemA.getExtendedType());
  }

  public void test_class_extendedType() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "}"));
    Element elementA = findElement("A {");
    Element elementB = findElement("B extends A");
    Element elementC = findElement("C extends B");
    // C
    TypeHierarchyItem itemC = computeHierarchy(elementC);
    assertEquals("C", itemC.getName());
    assertEquals(elementC, itemC.getClassElement());
    assertNull(itemC.getMemberElement());
    assertThat(itemC.getMixedTypes()).isEmpty();
    assertThat(itemC.getImplementedTypes()).isEmpty();
    assertThat(itemC.getSubTypes()).isEmpty();
    // B
    TypeHierarchyItem itemB = itemC.getExtendedType();
    assertEquals("B", itemB.getName());
    assertEquals(elementB, itemB.getClassElement());
    assertNull(itemB.getMemberElement());
    assertThat(itemB.getMixedTypes()).isEmpty();
    assertThat(itemB.getImplementedTypes()).isEmpty();
    assertThat(itemB.getSubTypes()).isEmpty();
    // A
    TypeHierarchyItem itemA = itemB.getExtendedType();
    assertEquals("A", itemA.getName());
    assertEquals(elementA, itemA.getClassElement());
    assertNull(itemA.getMemberElement());
    assertThat(itemA.getMixedTypes()).isEmpty();
    assertThat(itemA.getImplementedTypes()).isEmpty();
    assertThat(itemA.getSubTypes()).isEmpty();
    // A extends Object
    TypeHierarchyItem itemObject = itemA.getExtendedType();
    assertEquals("Object", itemObject.getName());
    assertNull(itemObject.getMemberElement());
    assertNull(itemObject.getExtendedType());
  }

  public void test_class_implementedTypes() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class MA {",
        "}",
        "class MB {",
        "}",
        "class T implements MA, MB {",
        "}"));
    Element elementMA = findElement("MA {");
    Element elementMB = findElement("MB {");
    Element elementT = findElement("T implements");
    // T
    TypeHierarchyItem itemT = computeHierarchy(elementT);
    assertEquals("T", itemT.getName());
    assertEquals(elementT, itemT.getClassElement());
    assertNull(itemT.getMemberElement());
    assertThat(itemT.getMixedTypes()).isEmpty();
    {
      TypeHierarchyItem[] implementedTypes = itemT.getImplementedTypes();
      assertThat(implementedTypes).hasSize(2);
      // MA
      {
        TypeHierarchyItem itemMA = implementedTypes[0];
        assertEquals("MA", itemMA.getName());
        assertEquals(elementMA, itemMA.getClassElement());
      }
      // MB
      {
        TypeHierarchyItem itemMB = implementedTypes[1];
        assertEquals("MB", itemMB.getName());
        assertEquals(elementMB, itemMB.getClassElement());
      }
    }
    assertThat(itemT.getSubTypes()).isEmpty();
    // T extends Object
    TypeHierarchyItem itemObject = itemT.getExtendedType();
    assertEquals("Object", itemObject.getName());
  }

  public void test_class_mixedTypes() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class MA {",
        "}",
        "class MB {",
        "}",
        "class T extends Object with MA, MB {",
        "}"));
    Element elementMA = findElement("MA {");
    Element elementMB = findElement("MB {");
    Element elementT = findElement("T extends");
    // T
    TypeHierarchyItem itemT = computeHierarchy(elementT);
    assertEquals("T", itemT.getName());
    assertEquals(elementT, itemT.getClassElement());
    assertNull(itemT.getMemberElement());
    {
      TypeHierarchyItem[] mixedTypes = itemT.getMixedTypes();
      assertThat(mixedTypes).hasSize(2);
      // MA
      {
        TypeHierarchyItem itemMA = mixedTypes[0];
        assertEquals("MA", itemMA.getName());
        assertEquals(elementMA, itemMA.getClassElement());
      }
      // MB
      {
        TypeHierarchyItem itemMB = mixedTypes[1];
        assertEquals("MB", itemMB.getName());
        assertEquals(elementMB, itemMB.getClassElement());
      }
    }
    assertThat(itemT.getImplementedTypes()).isEmpty();
    assertThat(itemT.getSubTypes()).isEmpty();
    // T extends Object
    TypeHierarchyItem itemObject = itemT.getExtendedType();
    assertEquals("Object", itemObject.getName());
  }

  public void test_class_subTypes() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "}",
        "class B extends A {",
        "}",
        "class C extends A {",
        "}",
        "class D extends C {",
        "}"));
    Element elementA = findElement("A {");
    Element elementB = findElement("B extends A");
    Element elementC = findElement("C extends A");
    Element elementD = findElement("D extends C");
    // A
    TypeHierarchyItem itemA = computeHierarchy(elementA);
    assertEquals(elementA, itemA.getClassElement());
    // A sub types
    {
      TypeHierarchyItem[] subTypesA = itemA.getSubTypes();
      assertThat(subTypesA).hasSize(2);
      {
        TypeHierarchyItem itemB = subTypesA[0];
        assertEquals(elementB, itemB.getClassElement());
        assertThat(itemB.getSubTypes()).isEmpty();
      }
      {
        TypeHierarchyItem itemC = subTypesA[1];
        assertEquals(elementC, itemC.getClassElement());
        // C sub types
        TypeHierarchyItem[] subTypesC = itemC.getSubTypes();
        assertThat(subTypesC).hasSize(1);
        {
          TypeHierarchyItem itemD = subTypesC[0];
          assertEquals(elementD, itemD.getClassElement());
          assertThat(itemD.getSubTypes()).isEmpty();
        }
      }
    }
  }

  public void test_member_getter() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  get test => null; // in A",
        "}",
        "class B extends A {",
        "  get test => null; // in B",
        "}",
        "class C extends B {",
        "}",
        "class D extends C {",
        "  get test => null; // in D",
        "}"));
    Element elementA = findElement("A {");
    Element elementB = findElement("B extends");
    Element elementC = findElement("C extends");
    Element elementD = findElement("D extends");
    Element elementAt = findElement("test => null; // in A");
    Element elementBt = findElement("test => null; // in B");
    Element elementDt = findElement("test => null; // in D");
    // B
    TypeHierarchyItem itemB = computeHierarchy(elementBt);
    assertEquals("B", itemB.getName());
    assertEquals(elementB, itemB.getClassElement());
    assertEquals(elementBt, itemB.getMemberElement());
    TypeHierarchyItem[] subTypesB = itemB.getSubTypes();
    assertThat(subTypesB).hasSize(1);
    // A
    TypeHierarchyItem itemA = itemB.getExtendedType();
    assertEquals(elementA, itemA.getClassElement());
    assertEquals(elementAt, itemA.getMemberElement());
    // C
    TypeHierarchyItem itemC = subTypesB[0];
    assertEquals(elementC, itemC.getClassElement());
    assertEquals(null, itemC.getMemberElement());
    TypeHierarchyItem[] subTypesC = itemC.getSubTypes();
    assertThat(subTypesC).hasSize(1);
    // D
    TypeHierarchyItem itemD = subTypesC[0];
    assertEquals(elementD, itemD.getClassElement());
    assertEquals(elementDt, itemD.getMemberElement());
    assertThat(itemD.getSubTypes()).isEmpty();
  }

  public void test_member_method() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  test() {} // in A",
        "}",
        "class B extends A {",
        "  test() {} // in B",
        "}",
        "class C extends B {",
        "}",
        "class D extends C {",
        "  test() {} // in D",
        "}"));
    Element elementA = findElement("A {");
    Element elementB = findElement("B extends");
    Element elementC = findElement("C extends");
    Element elementD = findElement("D extends");
    Element elementAt = findElement("test() {} // in A");
    Element elementBt = findElement("test() {} // in B");
    Element elementDt = findElement("test() {} // in D");
    // B
    TypeHierarchyItem itemB = computeHierarchy(elementBt);
    assertEquals("B", itemB.getName());
    assertEquals(elementB, itemB.getClassElement());
    assertEquals(elementBt, itemB.getMemberElement());
    TypeHierarchyItem[] subTypesB = itemB.getSubTypes();
    assertThat(subTypesB).hasSize(1);
    // A
    TypeHierarchyItem itemA = itemB.getExtendedType();
    assertEquals(elementA, itemA.getClassElement());
    assertEquals(elementAt, itemA.getMemberElement());
    // C
    TypeHierarchyItem itemC = subTypesB[0];
    assertEquals(elementC, itemC.getClassElement());
    assertEquals(null, itemC.getMemberElement());
    TypeHierarchyItem[] subTypesC = itemC.getSubTypes();
    assertThat(subTypesC).hasSize(1);
    // D
    TypeHierarchyItem itemD = subTypesC[0];
    assertEquals(elementD, itemD.getClassElement());
    assertEquals(elementDt, itemD.getMemberElement());
    assertThat(itemD.getSubTypes()).isEmpty();
  }

  public void test_member_operator() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  operator ==(x) => null; // in A",
        "}",
        "class B extends A {",
        "  operator ==(x) => null; // in B",
        "}",
        "class C extends B {",
        "}",
        "class D extends C {",
        "  operator ==(x) => null; // in D",
        "}"));
    Element elementA = findElement("A {");
    Element elementB = findElement("B extends");
    Element elementC = findElement("C extends");
    Element elementD = findElement("D extends");
    Element elementAt = findElement("==(x) => null; // in A");
    Element elementBt = findElement("==(x) => null; // in B");
    Element elementDt = findElement("==(x) => null; // in D");
    // B
    TypeHierarchyItem itemB = computeHierarchy(elementBt);
    assertEquals("B", itemB.getName());
    assertEquals(elementB, itemB.getClassElement());
    assertEquals(elementBt, itemB.getMemberElement());
    TypeHierarchyItem[] subTypesB = itemB.getSubTypes();
    assertThat(subTypesB).hasSize(1);
    // A
    TypeHierarchyItem itemA = itemB.getExtendedType();
    assertEquals(elementA, itemA.getClassElement());
    assertEquals(elementAt, itemA.getMemberElement());
    // C
    TypeHierarchyItem itemC = subTypesB[0];
    assertEquals(elementC, itemC.getClassElement());
    assertEquals(null, itemC.getMemberElement());
    TypeHierarchyItem[] subTypesC = itemC.getSubTypes();
    assertThat(subTypesC).hasSize(1);
    // D
    TypeHierarchyItem itemD = subTypesC[0];
    assertEquals(elementD, itemD.getClassElement());
    assertEquals(elementDt, itemD.getMemberElement());
    assertThat(itemD.getSubTypes()).isEmpty();
  }

  public void test_member_setter() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  set test(x) {} // in A",
        "}",
        "class B extends A {",
        "  set test(x) {} // in B",
        "}",
        "class C extends B {",
        "}",
        "class D extends C {",
        "  set test(x) {} // in D",
        "}"));
    Element elementA = findElement("A {");
    Element elementB = findElement("B extends");
    Element elementC = findElement("C extends");
    Element elementD = findElement("D extends");
    Element elementAt = findElement("test(x) {} // in A");
    Element elementBt = findElement("test(x) {} // in B");
    Element elementDt = findElement("test(x) {} // in D");
    // B
    TypeHierarchyItem itemB = computeHierarchy(elementBt);
    assertEquals("B", itemB.getName());
    assertEquals(elementB, itemB.getClassElement());
    assertEquals(elementBt, itemB.getMemberElement());
    TypeHierarchyItem[] subTypesB = itemB.getSubTypes();
    assertThat(subTypesB).hasSize(1);
    // A
    TypeHierarchyItem itemA = itemB.getExtendedType();
    assertEquals(elementA, itemA.getClassElement());
    assertEquals(elementAt, itemA.getMemberElement());
    // C
    TypeHierarchyItem itemC = subTypesB[0];
    assertEquals(elementC, itemC.getClassElement());
    assertEquals(null, itemC.getMemberElement());
    TypeHierarchyItem[] subTypesC = itemC.getSubTypes();
    assertThat(subTypesC).hasSize(1);
    // D
    TypeHierarchyItem itemD = subTypesC[0];
    assertEquals(elementD, itemD.getClassElement());
    assertEquals(elementDt, itemD.getMemberElement());
    assertThat(itemD.getSubTypes()).isEmpty();
  }

  private TypeHierarchyItem computeHierarchy(Element element) {
    final TypeHierarchyItem[] result = {null};
    final CountDownLatch latch = new CountDownLatch(1);
    server.computeTypeHierarchy(contextId, element, new TypeHierarchyConsumer() {
      @Override
      public void computedHierarchy(TypeHierarchyItem target) {
        result[0] = target;
        latch.countDown();
      }
    });
    Uninterruptibles.awaitUninterruptibly(latch, 600, TimeUnit.SECONDS);
    return result[0];
  }

  private void createContextWithSingleSource(String code) {
    this.contextId = createContext("test");
    this.code = code;
    this.source = addSource(contextId, "/test.dart", code);
  }

  private Element findElement(int offset) {
    // ensure navigation data
    server.subscribe(
        contextId,
        ImmutableMap.of(NotificationKind.NAVIGATION, SourceSet.EXPLICITLY_ADDED));
    server.test_waitForWorkerComplete();
    // find navigation reqion with Element
    NavigationRegion[] regions = serverListener.getNavigationRegions(contextId, source);
    for (NavigationRegion navigationRegion : regions) {
      if (navigationRegion.containsInclusive(offset)) {
        return navigationRegion.getTargets()[0];
      }
    }
    // not found
    fail("Not found element at " + offset);
    return null;
  }

  private Element findElement(String search) {
    int offset = findOffset(search);
    return findElement(offset);
  }

  /**
   * @return the offset of given "search" string in {@link #code}. Fails test if not found.
   */
  private int findOffset(String search) {
    int offset = code.indexOf(search);
    assertThat(offset).describedAs(code).isNotEqualTo(-1);
    return offset;
  }
}
