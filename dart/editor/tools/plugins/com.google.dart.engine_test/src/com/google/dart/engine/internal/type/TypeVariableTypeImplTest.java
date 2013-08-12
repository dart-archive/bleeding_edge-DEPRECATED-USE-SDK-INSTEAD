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
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.getObject;

public class TypeVariableTypeImplTest extends EngineTestCase {
  public void fail_isMoreSpecificThan_typeArguments_object() {
    TypeVariableElementImpl element = new TypeVariableElementImpl(identifier("E"));
    TypeVariableTypeImpl type = new TypeVariableTypeImpl(element);

    // E << Object
    assertTrue(type.isMoreSpecificThan(getObject().getType()));
  }

  public void fail_isMoreSpecificThan_typeArguments_self() {
    TypeVariableElementImpl element = new TypeVariableElementImpl(identifier("E"));
    TypeVariableTypeImpl type = new TypeVariableTypeImpl(element);

    // E << E
    assertTrue(type.isMoreSpecificThan(type));
  }

  public void test_creation() {
    assertNotNull(new TypeVariableTypeImpl(new TypeVariableElementImpl(identifier("E"))));
  }

  public void test_getElement() {
    TypeVariableElementImpl element = new TypeVariableElementImpl(identifier("E"));
    TypeVariableTypeImpl type = new TypeVariableTypeImpl(element);
    assertEquals(element, type.getElement());
  }

  public void test_isMoreSpecificThan_typeArguments_upperBound() {
    ClassElementImpl classS = classElement("A");

    TypeVariableElementImpl typeVarT = new TypeVariableElementImpl(identifier("T"));
    typeVarT.setBound(classS.getType());
    TypeVariableTypeImpl typeVarTypeT = new TypeVariableTypeImpl(typeVarT);

    // <T extends S>
    // T << S
    assertTrue(typeVarTypeT.isMoreSpecificThan(classS.getType()));
  }

  public void test_substitute_equal() {
    TypeVariableElementImpl element = new TypeVariableElementImpl(identifier("E"));
    TypeVariableTypeImpl type = new TypeVariableTypeImpl(element);
    InterfaceTypeImpl argument = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    TypeVariableTypeImpl parameter = new TypeVariableTypeImpl(element);
    assertSame(argument, type.substitute(new Type[] {argument}, new Type[] {parameter}));
  }

  public void test_substitute_notEqual() {
    TypeVariableTypeImpl type = new TypeVariableTypeImpl(new TypeVariableElementImpl(
        identifier("E")));
    InterfaceTypeImpl argument = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    TypeVariableTypeImpl parameter = new TypeVariableTypeImpl(new TypeVariableElementImpl(
        identifier("F")));
    assertSame(type, type.substitute(new Type[] {argument}, new Type[] {parameter}));
  }
}
