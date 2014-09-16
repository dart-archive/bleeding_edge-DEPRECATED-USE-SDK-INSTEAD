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

package com.google.dart.engine.internal.type;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.UnionType;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.functionElement;

import java.util.Set;

public class UnionTypeImplTest extends EngineTestCase {
  private ClassElement classA;
  private InterfaceType typeA;
  private ClassElement classB;
  private InterfaceType typeB;

  private Type uA;
  private Type uB;
  private Type uAB;
  private Type uBA;

  private Type[] us;

  public void test_emptyUnionsNotAllowed() {
    try {
      UnionTypeImpl.union();
    } catch (IllegalArgumentException e) {
      return;
    }
    fail("Expected illegal argument exception.");
  }

  public void test_equality_beingASubtypeOfAnElementIsNotSufficient() {
    // Non-equal if some elements are different
    assertFalse(uAB.equals(uA));
  }

  public void test_equality_insertionOrderDoesntMatter() {
    // Insertion order doesn't matter, only sets of elements
    assertTrue(uAB.equals(uBA));
    assertTrue(uBA.equals(uAB));
  }

  public void test_equality_reflexivity() {
    for (Type u : us) {
      assertTrue(u.equals(u));
    }
  }

  public void test_equality_singletonsCollapse() {
    assertTrue(typeA.equals(uA));
    assertTrue(uA.equals(typeA));
  }

  public void test_isMoreSpecificThan_allElementsOnLHSAreSubtypesOfSomeElementOnRHS() {
    // Unions are subtypes when all elements are subtypes
    assertTrue(uAB.isMoreSpecificThan(uA));
    assertTrue(uAB.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_element() {
    // Elements of union are sub types
    assertTrue(typeA.isMoreSpecificThan(uAB));
    assertTrue(typeB.isMoreSpecificThan(uAB));
  }

  public void test_isMoreSpecificThan_notSubtypeOfAnyElement() {
    // Types that are not subtypes of elements are not subtypes
    assertFalse(typeA.isMoreSpecificThan(uB));
  }

  public void test_isMoreSpecificThan_reflexivity() {
    for (Type u : us) {
      assertTrue(u.isMoreSpecificThan(u));
    }
  }

  // This tests the less strict (more unsound) subtype semantics for union types.
  // It will break if we change to the more strict definition of subtyping.
  public void test_isMoreSpecificThan_someElementOnLHSIsNotASubtypeOfAnyElementOnRHS() {
    // Unions are subtypes when some element is a subtype
    assertTrue(uAB.isMoreSpecificThan(uB));
    assertTrue(uAB.isMoreSpecificThan(typeB));
  }

  public void test_isMoreSpecificThan_subtypeOfSomeElement() {
    // Subtypes of elements are sub types
    assertTrue(typeB.isMoreSpecificThan(uA));
  }

  public void test_isSubtypeOf_allElementsOnLHSAreSubtypesOfSomeElementOnRHS() {
    // Unions are subtypes when all elements are subtypes
    assertTrue(uAB.isSubtypeOf(uA));
    assertTrue(uAB.isSubtypeOf(typeA));
  }

  public void test_isSubtypeOf_element() {
    // Elements of union are sub types
    assertTrue(typeA.isSubtypeOf(uAB));
    assertTrue(typeB.isSubtypeOf(uAB));
  }

  public void test_isSubtypeOf_notSubtypeOfAnyElement() {
    // Types that are not subtypes of elements are not subtypes
    assertFalse(typeA.isSubtypeOf(uB));
  }

  public void test_isSubtypeOf_reflexivity() {
    for (Type u : us) {
      assertTrue(u.isSubtypeOf(u));
    }
  }

  // This tests the less strict (more unsound) subtype semantics for union types.
  // It will break if we change to the more strict definition of subtyping.
  public void test_isSubtypeOf_someElementOnLHSIsNotASubtypeOfAnyElementOnRHS() {
    // Unions are subtypes when some element is a subtype
    assertTrue(uAB.isSubtypeOf(uB));
    assertTrue(uAB.isSubtypeOf(typeB));
  }

  public void test_isSubtypeOf_subtypeOfSomeElement() {
    // Subtypes of elements are sub types
    assertTrue(typeB.isSubtypeOf(uA));
  }

  public void test_nestedUnionsCollapse() {
    UnionType u = (UnionType) UnionTypeImpl.union(uAB, typeA);
    for (Type t : u.getElements()) {
      if (t instanceof UnionType) {
        fail("Expected only non-union types but found " + t + "!");
      }
    }
  }

  public void test_noLossage() {
    UnionType u = (UnionType) UnionTypeImpl.union(typeA, typeB, typeB, typeA, typeB, typeB);
    Set<Type> elements = u.getElements();
    assertTrue(elements.contains(typeA));
    assertTrue(elements.contains(typeB));
    assertTrue(elements.size() == 2);
  }

  public void test_substitute() {
    // Based on [InterfaceTypeImplTest.test_substitute_equal].
    ClassElement classAE = classElement("A", "E");
    InterfaceType typeAE = classAE.getType();
    Type[] args = {typeB};
    Type[] params = {classAE.getTypeParameters()[0].getType()};
    Type typeAESubbed = typeAE.substitute(args, params);

    assertFalse(typeAE.equals(typeAESubbed));
    assertEquals(
        UnionTypeImpl.union(typeA, typeAE).substitute(args, params),
        UnionTypeImpl.union(typeA, typeAESubbed));
  }

  public void test_toString_pair() {
    String s = uAB.toString();
    assertTrue(s.equals("{A,B}") || s.equals("{B,A}"));
    assertEquals(s, uAB.getDisplayName());
  }

  public void test_toString_singleton() {
    // Singleton unions collapse to the the single type.
    assertEquals("A", uA.toString());
  }

  public void test_unionTypeIsLessSpecificThan_function() {
    // Based on [FunctionTypeImplTest.test_isAssignableTo_normalAndPositionalArgs].
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    Type uAT = UnionTypeImpl.union(uA, t);
    assertTrue(t.isMoreSpecificThan(uAT));
    assertFalse(t.isMoreSpecificThan(uAB));
  }

  public void test_unionTypeIsSuperTypeOf_function() {
    // Based on [FunctionTypeImplTest.test_isAssignableTo_normalAndPositionalArgs].
    ClassElement a = classElement("A");
    FunctionType t = functionElement("t", null, new ClassElement[] {a}).getType();
    Type uAT = UnionTypeImpl.union(uA, t);
    assertTrue(t.isSubtypeOf(uAT));
    assertFalse(t.isSubtypeOf(uAB));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    classA = classElement("A");
    typeA = classA.getType();
    classB = classElement("B", typeA);
    typeB = classB.getType();

    uA = UnionTypeImpl.union(typeA);
    uB = UnionTypeImpl.union(typeB);
    uAB = UnionTypeImpl.union(typeA, typeB);
    uBA = UnionTypeImpl.union(typeB, typeA);

    us = new Type[] {uA, uB, uAB, uBA};
  }
}
