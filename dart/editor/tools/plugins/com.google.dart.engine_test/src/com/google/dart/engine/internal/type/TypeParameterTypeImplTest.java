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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.getObject;

public class TypeParameterTypeImplTest extends EngineTestCase {
  public void test_creation() {
    assertNotNull(new TypeParameterTypeImpl(new TypeParameterElementImpl(identifier("E"))));
  }

  public void test_getElement() {
    TypeParameterElementImpl element = new TypeParameterElementImpl(identifier("E"));
    TypeParameterTypeImpl type = new TypeParameterTypeImpl(element);
    assertEquals(element, type.getElement());
  }

  public void test_isMoreSpecificThan_typeArguments_dynamic() {
    TypeParameterElementImpl element = new TypeParameterElementImpl(identifier("E"));
    TypeParameterTypeImpl type = new TypeParameterTypeImpl(element);

    // E << dynamic
    assertTrue(type.isMoreSpecificThan(DynamicTypeImpl.getInstance()));
  }

  public void test_isMoreSpecificThan_typeArguments_object() {
    TypeParameterElementImpl element = new TypeParameterElementImpl(identifier("E"));
    TypeParameterTypeImpl type = new TypeParameterTypeImpl(element);

    // E << Object
    assertTrue(type.isMoreSpecificThan(getObject().getType()));
  }

  public void test_isMoreSpecificThan_typeArguments_resursive() {
    ClassElementImpl classS = classElement("A");

    TypeParameterElementImpl typeParameterU = new TypeParameterElementImpl(identifier("U"));
    TypeParameterTypeImpl typeParameterTypeU = new TypeParameterTypeImpl(typeParameterU);

    TypeParameterElementImpl typeParameterT = new TypeParameterElementImpl(identifier("T"));
    TypeParameterTypeImpl typeParameterTypeT = new TypeParameterTypeImpl(typeParameterT);

    typeParameterT.setBound(typeParameterTypeU);
    typeParameterU.setBound(typeParameterTypeU);

    // <T extends U> and <U extends T>
    // T << S
    assertFalse(typeParameterTypeT.isMoreSpecificThan(classS.getType()));
  }

  public void test_isMoreSpecificThan_typeArguments_self() {
    TypeParameterElementImpl element = new TypeParameterElementImpl(identifier("E"));
    TypeParameterTypeImpl type = new TypeParameterTypeImpl(element);

    // E << E
    assertTrue(type.isMoreSpecificThan(type));
  }

  public void test_isMoreSpecificThan_typeArguments_transitivity_interfaceTypes() {
    //  class A {}
    //  class B extends A {}
    //
    ClassElement classA = classElement("A");
    ClassElement classB = classElement("B", classA.getType());
    InterfaceType typeA = classA.getType();
    InterfaceType typeB = classB.getType();

    TypeParameterElementImpl typeParameterT = new TypeParameterElementImpl(identifier("T"));
    typeParameterT.setBound(typeB);
    TypeParameterTypeImpl typeParameterTypeT = new TypeParameterTypeImpl(typeParameterT);

    // <T extends B>
    // T << A
    assertTrue(typeParameterTypeT.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_typeArguments_transitivity_typeParameters() {
    ClassElementImpl classS = classElement("A");

    TypeParameterElementImpl typeParameterU = new TypeParameterElementImpl(identifier("U"));
    typeParameterU.setBound(classS.getType());
    TypeParameterTypeImpl typeParameterTypeU = new TypeParameterTypeImpl(typeParameterU);

    TypeParameterElementImpl typeParameterT = new TypeParameterElementImpl(identifier("T"));
    typeParameterT.setBound(typeParameterTypeU);
    TypeParameterTypeImpl typeParameterTypeT = new TypeParameterTypeImpl(typeParameterT);

    // <T extends U> and <U extends S>
    // T << S
    assertTrue(typeParameterTypeT.isMoreSpecificThan(classS.getType()));
  }

  public void test_isMoreSpecificThan_typeArguments_upperBound() {
    ClassElementImpl classS = classElement("A");

    TypeParameterElementImpl typeParameterT = new TypeParameterElementImpl(identifier("T"));
    typeParameterT.setBound(classS.getType());
    TypeParameterTypeImpl typeParameterTypeT = new TypeParameterTypeImpl(typeParameterT);

    // <T extends S>
    // T << S
    assertTrue(typeParameterTypeT.isMoreSpecificThan(classS.getType()));
  }

  public void test_substitute_equal() {
    TypeParameterElementImpl element = new TypeParameterElementImpl(identifier("E"));
    TypeParameterTypeImpl type = new TypeParameterTypeImpl(element);
    InterfaceTypeImpl argument = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    TypeParameterTypeImpl parameter = new TypeParameterTypeImpl(element);
    assertSame(argument, type.substitute(new Type[] {argument}, new Type[] {parameter}));
  }

  public void test_substitute_notEqual() {
    TypeParameterTypeImpl type = new TypeParameterTypeImpl(new TypeParameterElementImpl(
        identifier("E")));
    InterfaceTypeImpl argument = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    TypeParameterTypeImpl parameter = new TypeParameterTypeImpl(new TypeParameterElementImpl(
        identifier("F")));
    assertSame(type, type.substitute(new Type[] {argument}, new Type[] {parameter}));
  }
}
