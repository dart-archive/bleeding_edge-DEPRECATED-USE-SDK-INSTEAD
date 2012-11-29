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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class InterfaceTypeImplTest extends EngineTestCase {
  public void fail_isDirectSupertypeOf_true_implicit() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("Object"));
    ClassElementImpl elementB = new ClassElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void fail_isMoreSpecificThan_covariance() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("A"));
    ClassElementImpl elementI = new ClassElementImpl(identifier("I"));
    ClassElementImpl elementJ = new ClassElementImpl(identifier("J"));
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeAJ = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeI = new InterfaceTypeImpl(elementI);
    InterfaceTypeImpl typeJ = new InterfaceTypeImpl(elementJ);
    elementJ.setSupertype(typeI);
    typeAI.setTypeArguments(new Type[] {typeI});
    typeAJ.setTypeArguments(new Type[] {typeJ});
    assertTrue(typeAJ.isMoreSpecificThan(typeAI));
  }

  public void fail_isMoreSpecificThan_directSupertype_implicit() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("Object"));
    ClassElementImpl elementB = new ClassElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    assertTrue(typeB.isMoreSpecificThan(typeA));
  }

  public void test_creation() {
    assertNotNull(new InterfaceTypeImpl(new ClassElementImpl(identifier("A"))));
  }

  public void test_getDynamic() {
    assertNotNull(InterfaceTypeImpl.getDynamic());
  }

  public void test_getElement() {
    ClassElementImpl typeElement = new ClassElementImpl(identifier("A"));
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);
    assertEquals(typeElement, type.getElement());
  }

  public void test_getTypeArguments() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    assertNull(type.getTypeArguments());
  }

  public void test_isDirectSupertypeOf_false_explicit() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("A"));
    ClassElementImpl elementB = new ClassElementImpl(identifier("B"));
    ClassElementImpl elementC = new ClassElementImpl(identifier("C"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    InterfaceTypeImpl typeC = new InterfaceTypeImpl(elementC);
    elementC.setSupertype(typeB);
    assertFalse(typeA.isDirectSupertypeOf(typeC));
  }

  public void test_isDirectSupertypeOf_false_implicit() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("A"));
    ClassElementImpl elementB = new ClassElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    assertFalse(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_isDirectSupertypeOf_true_explicit() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("A"));
    ClassElementImpl elementB = new ClassElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    elementB.setSupertype(typeA);
    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_isMoreSpecificThan_bottom() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    assertTrue(BottomTypeImpl.getInstance().isMoreSpecificThan(type));
  }

  public void test_isMoreSpecificThan_directSupertype_explicit() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("A"));
    ClassElementImpl elementB = new ClassElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    elementB.setSupertype(typeA);
    assertTrue(typeB.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_dynamic() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    assertTrue(type.isMoreSpecificThan(InterfaceTypeImpl.getDynamic()));
  }

  public void test_isMoreSpecificThan_indirectSupertype() {
    ClassElementImpl elementA = new ClassElementImpl(identifier("A"));
    ClassElementImpl elementB = new ClassElementImpl(identifier("B"));
    ClassElementImpl elementC = new ClassElementImpl(identifier("C"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    InterfaceTypeImpl typeC = new InterfaceTypeImpl(elementC);
    elementB.setSupertype(typeA);
    elementC.setSupertype(typeB);
    assertTrue(typeC.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_same() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    assertTrue(type.isMoreSpecificThan(type));
  }

  public void test_setTypeArguments() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    Type[] typeArguments = new Type[] {
        new InterfaceTypeImpl(new ClassElementImpl(identifier("B"))),
        new InterfaceTypeImpl(new ClassElementImpl(identifier("C"))),};
    type.setTypeArguments(typeArguments);
    assertEquals(typeArguments, type.getTypeArguments());
  }
}
