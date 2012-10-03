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
import com.google.dart.engine.internal.element.TypeElementImpl;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class InterfaceTypeImplTest extends EngineTestCase {
  public void fail_isDirectSupertypeOf_true_implicit() {
    TypeElementImpl elementA = new TypeElementImpl(identifier("Object"));
    TypeElementImpl elementB = new TypeElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_creation() {
    assertNotNull(new InterfaceTypeImpl(new TypeElementImpl(identifier("A"))));
  }

  public void test_getDynamic() {
    assertNotNull(InterfaceTypeImpl.getDynamic());
  }

  public void test_getElement() {
    TypeElementImpl typeElement = new TypeElementImpl(identifier("A"));
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);
    assertEquals(typeElement, type.getElement());
  }

  public void test_getTypeArguments() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new TypeElementImpl(identifier("A")));
    assertNull(type.getTypeArguments());
  }

  public void test_isDirectSupertypeOf_false_explicit() {
    TypeElementImpl elementA = new TypeElementImpl(identifier("A"));
    TypeElementImpl elementB = new TypeElementImpl(identifier("B"));
    TypeElementImpl elementC = new TypeElementImpl(identifier("C"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    InterfaceTypeImpl typeC = new InterfaceTypeImpl(elementC);
    elementC.setSupertype(typeB);
    assertFalse(typeA.isDirectSupertypeOf(typeC));
  }

  public void test_isDirectSupertypeOf_false_implicit() {
    TypeElementImpl elementA = new TypeElementImpl(identifier("A"));
    TypeElementImpl elementB = new TypeElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    assertFalse(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_isDirectSupertypeOf_true_explicit() {
    TypeElementImpl elementA = new TypeElementImpl(identifier("Object"));
    TypeElementImpl elementB = new TypeElementImpl(identifier("B"));
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    elementB.setSupertype(typeA);
    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_setTypeArguments() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new TypeElementImpl(identifier("A")));
    Type[] typeArguments = new Type[] {
        new InterfaceTypeImpl(new TypeElementImpl(identifier("B"))),
        new InterfaceTypeImpl(new TypeElementImpl(identifier("C"))),};
    type.setTypeArguments(typeArguments);
    assertEquals(typeArguments, type.getTypeArguments());
  }

  public void xtest_isMoreSpecificThan() {
    // TODO(brianwilkerson) Write tests for this method.
  }
}
