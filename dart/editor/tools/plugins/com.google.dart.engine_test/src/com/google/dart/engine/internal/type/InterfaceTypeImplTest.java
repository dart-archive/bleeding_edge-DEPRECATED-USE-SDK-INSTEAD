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
import com.google.dart.engine.element.ElementFactory;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.classElement;

import java.util.Set;

public class InterfaceTypeImplTest extends EngineTestCase {

  public void test_computeLongestInheritancePathToObject_multipleInterfacePaths() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    ClassElementImpl elementD = (ClassElementImpl) classElement("D");
    ClassElementImpl elementE = (ClassElementImpl) classElement("E");
    elementB.setInterfaces(new InterfaceType[] {elementA.getType()});
    elementC.setInterfaces(new InterfaceType[] {elementA.getType()});
    elementD.setInterfaces(new InterfaceType[] {elementC.getType()});
    elementE.setInterfaces(new InterfaceType[] {elementB.getType(), elementD.getType()});
    // assertion: even though the longest path to Object for typeB is 2, and typeE implements typeB,
    // the longest path for typeE is 4 since it also implements typeD
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementB.getType()));
    assertEquals(4, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementE.getType()));
  }

  public void test_computeLongestInheritancePathToObject_multipleSuperclassPaths() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementA.getType());
    ClassElement elementD = classElement("D", elementC.getType());
    ClassElementImpl elementE = (ClassElementImpl) classElement("E", elementB.getType());
    elementE.setInterfaces(new InterfaceType[] {elementD.getType()});
    // assertion: even though the longest path to Object for typeB is 2, and typeE extends typeB,
    // the longest path for typeE is 4 since it also implements typeD
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementB.getType()));
    assertEquals(4, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementE.getType()));
  }

  public void test_computeLongestInheritancePathToObject_object() {
    ClassElement elementA = classElement("A");
    InterfaceType object = elementA.getSupertype();
    assertEquals(0, InterfaceTypeImpl.computeLongestInheritancePathToObject(object));
  }

  public void test_computeLongestInheritancePathToObject_singleInterfacePath() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    elementB.setInterfaces(new InterfaceType[] {elementA.getType()});
    elementC.setInterfaces(new InterfaceType[] {elementB.getType()});
    assertEquals(1, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementA.getType()));
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementB.getType()));
    assertEquals(3, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementC.getType()));
  }

  public void test_computeLongestInheritancePathToObject_singleSuperclassPath() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementB.getType());
    assertEquals(1, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementA.getType()));
    assertEquals(2, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementB.getType()));
    assertEquals(3, InterfaceTypeImpl.computeLongestInheritancePathToObject(elementC.getType()));
  }

  public void test_computeSuperinterfaceSet_multipleInterfacePaths() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    ClassElementImpl elementD = (ClassElementImpl) classElement("D");
    ClassElementImpl elementE = (ClassElementImpl) classElement("E");
    elementB.setInterfaces(new InterfaceType[] {elementA.getType()});
    elementC.setInterfaces(new InterfaceType[] {elementA.getType()});
    elementD.setInterfaces(new InterfaceType[] {elementC.getType()});
    elementE.setInterfaces(new InterfaceType[] {elementB.getType(), elementD.getType()});
    // D
    Set<InterfaceType> superinterfacesOfD = InterfaceTypeImpl.computeSuperinterfaceSet(elementD.getType());
    assertNotNull(superinterfacesOfD);
    assertTrue(superinterfacesOfD.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfD.contains(elementA.getType()));
    assertTrue(superinterfacesOfD.contains(elementC.getType()));
    assertEquals(3, superinterfacesOfD.size());
    // E
    Set<InterfaceType> superinterfacesOfE = InterfaceTypeImpl.computeSuperinterfaceSet(elementE.getType());
    assertNotNull(superinterfacesOfE);
    assertTrue(superinterfacesOfE.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfE.contains(elementA.getType()));
    assertTrue(superinterfacesOfE.contains(elementB.getType()));
    assertTrue(superinterfacesOfE.contains(elementC.getType()));
    assertTrue(superinterfacesOfE.contains(elementD.getType()));
    assertEquals(5, superinterfacesOfE.size());
  }

  public void test_computeSuperinterfaceSet_multipleSuperclassPaths() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementA.getType());
    ClassElement elementD = classElement("D", elementC.getType());
    ClassElementImpl elementE = (ClassElementImpl) classElement("E", elementB.getType());
    elementE.setInterfaces(new InterfaceType[] {elementD.getType()});
    // D
    Set<InterfaceType> superinterfacesOfD = InterfaceTypeImpl.computeSuperinterfaceSet(elementD.getType());
    assertNotNull(superinterfacesOfD);
    assertTrue(superinterfacesOfD.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfD.contains(elementA.getType()));
    assertTrue(superinterfacesOfD.contains(elementC.getType()));
    assertEquals(3, superinterfacesOfD.size());
    // E
    Set<InterfaceType> superinterfacesOfE = InterfaceTypeImpl.computeSuperinterfaceSet(elementE.getType());
    assertNotNull(superinterfacesOfE);
    assertTrue(superinterfacesOfE.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfE.contains(elementA.getType()));
    assertTrue(superinterfacesOfE.contains(elementB.getType()));
    assertTrue(superinterfacesOfE.contains(elementC.getType()));
    assertTrue(superinterfacesOfE.contains(elementD.getType()));
    assertEquals(5, superinterfacesOfE.size());
  }

  public void test_computeSuperinterfaceSet_singleInterfacePath() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    elementB.setInterfaces(new InterfaceType[] {elementA.getType()});
    elementC.setInterfaces(new InterfaceType[] {elementB.getType()});
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(elementA.getType());
    assertNotNull(superinterfacesOfA);
    assertTrue(superinterfacesOfA.contains(ElementFactory.getObject().getType()));
    assertEquals(1, superinterfacesOfA.size());
    // B
    Set<InterfaceType> superinterfacesOfB = InterfaceTypeImpl.computeSuperinterfaceSet(elementB.getType());
    assertNotNull(superinterfacesOfB);
    assertTrue(superinterfacesOfB.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfB.contains(elementA.getType()));
    assertEquals(2, superinterfacesOfB.size());
    // C
    Set<InterfaceType> superinterfacesOfC = InterfaceTypeImpl.computeSuperinterfaceSet(elementC.getType());
    assertNotNull(superinterfacesOfC);
    assertTrue(superinterfacesOfC.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfC.contains(elementA.getType()));
    assertTrue(superinterfacesOfC.contains(elementB.getType()));
    assertEquals(3, superinterfacesOfC.size());
  }

  public void test_computeSuperinterfaceSet_singleSuperclassPath() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementB.getType());
    // A
    Set<InterfaceType> superinterfacesOfA = InterfaceTypeImpl.computeSuperinterfaceSet(elementA.getType());
    assertNotNull(superinterfacesOfA);
    assertTrue(superinterfacesOfA.contains(ElementFactory.getObject().getType()));
    assertEquals(1, superinterfacesOfA.size());
    // B
    Set<InterfaceType> superinterfacesOfB = InterfaceTypeImpl.computeSuperinterfaceSet(elementB.getType());
    assertNotNull(superinterfacesOfB);
    assertTrue(superinterfacesOfB.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfB.contains(elementA.getType()));
    assertEquals(2, superinterfacesOfB.size());
    // C
    Set<InterfaceType> superinterfacesOfC = InterfaceTypeImpl.computeSuperinterfaceSet(elementC.getType());
    assertNotNull(superinterfacesOfC);
    assertTrue(superinterfacesOfC.contains(ElementFactory.getObject().getType()));
    assertTrue(superinterfacesOfC.contains(elementA.getType()));
    assertTrue(superinterfacesOfC.contains(elementB.getType()));
    assertEquals(3, superinterfacesOfC.size());
  }

  public void test_creation() {
    assertNotNull(new InterfaceTypeImpl(new ClassElementImpl(identifier("A"))));
  }

  public void test_getElement() {
    ClassElementImpl typeElement = new ClassElementImpl(identifier("A"));
    InterfaceTypeImpl type = new InterfaceTypeImpl(typeElement);
    assertEquals(typeElement, type.getElement());
  }

  public void test_getLeastUpperBound_directInterfaceCase() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementB.setInterfaces(new InterfaceType[] {typeA});
    elementC.setInterfaces(new InterfaceType[] {typeB});
    assertEquals(typeB, typeB.getLeastUpperBound(typeC));
    assertEquals(typeB, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_directSubclassCase() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C", elementB.getType());
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    assertEquals(typeB, typeB.getLeastUpperBound(typeC));
    assertEquals(typeB, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_functionType() {
    Type interfaceType = classElement("A").getType();
    FunctionTypeImpl functionType = new FunctionTypeImpl(new FunctionElementImpl(identifier("f")));
    assertNull(interfaceType.getLeastUpperBound(functionType));
  }

  public void test_getLeastUpperBound_mixinCase() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementA.getType());
    ClassElementImpl elementD = (ClassElementImpl) classElement("D", elementB.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeC = elementC.getType();
    InterfaceType typeD = elementD.getType();
    elementD.setMixins(new InterfaceType[] {
        classElement("M").getType(), classElement("N").getType(), classElement("O").getType(),
        classElement("P").getType()});
    assertEquals(typeA, typeD.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeD));
  }

  public void test_getLeastUpperBound_null() {
    Type interfaceType = classElement("A").getType();
    assertNull(interfaceType.getLeastUpperBound(null));
  }

  public void test_getLeastUpperBound_object() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    Type typeObject = typeA.getElement().getSupertype();

    // assert that object does not have a super type
    assertNull(((ClassElement) typeObject.getElement()).getSupertype());

    // assert that both A and B have the same super type of Object
    assertEquals(typeObject, typeB.getElement().getSupertype());

    // finally, assert that the only least upper bound of A and B is Object
    assertEquals(typeObject, typeA.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_self() {
    ClassElement elementA = classElement("A");
    InterfaceType typeA = elementA.getType();
    assertEquals(typeA, typeA.getLeastUpperBound(typeA));
  }

  public void test_getLeastUpperBound_sharedSuperclass1() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C", elementA.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperclass2() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C", elementA.getType());
    ClassElementImpl elementD = (ClassElementImpl) classElement("D", elementC.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeD = elementD.getType();
    assertEquals(typeA, typeB.getLeastUpperBound(typeD));
    assertEquals(typeA, typeD.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperclass3() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C", elementB.getType());
    ClassElementImpl elementD = (ClassElementImpl) classElement("D", elementB.getType());
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    InterfaceType typeD = elementD.getType();
    assertEquals(typeB, typeC.getLeastUpperBound(typeD));
    assertEquals(typeB, typeD.getLeastUpperBound(typeC));
  }

  public void test_getLeastUpperBound_sharedSuperclass4() {
    ClassElement elementA = classElement("A");
    ClassElement elementA2 = classElement("A2");
    ClassElement elementA3 = classElement("A3");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C", elementA.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeA2 = elementA2.getType();
    InterfaceType typeA3 = elementA3.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementB.setInterfaces(new InterfaceType[] {typeA2});
    elementC.setInterfaces(new InterfaceType[] {typeA3});
    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperinterface1() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementB.setInterfaces(new InterfaceType[] {typeA});
    elementC.setInterfaces(new InterfaceType[] {typeA});
    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperinterface2() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    ClassElementImpl elementD = (ClassElementImpl) classElement("D");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    InterfaceType typeD = elementD.getType();
    elementB.setInterfaces(new InterfaceType[] {typeA});
    elementC.setInterfaces(new InterfaceType[] {typeA});
    elementD.setInterfaces(new InterfaceType[] {typeC});
    assertEquals(typeA, typeB.getLeastUpperBound(typeD));
    assertEquals(typeA, typeD.getLeastUpperBound(typeB));
  }

  public void test_getLeastUpperBound_sharedSuperinterface3() {
    ClassElementImpl elementA = (ClassElementImpl) classElement("A");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    ClassElementImpl elementD = (ClassElementImpl) classElement("D");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    InterfaceType typeD = elementD.getType();
    elementB.setInterfaces(new InterfaceType[] {typeA});
    elementC.setInterfaces(new InterfaceType[] {typeB});
    elementD.setInterfaces(new InterfaceType[] {typeB});
    assertEquals(typeB, typeC.getLeastUpperBound(typeD));
    assertEquals(typeB, typeD.getLeastUpperBound(typeC));
  }

  public void test_getLeastUpperBound_sharedSuperinterface4() {
    ClassElement elementA = classElement("A");
    ClassElement elementA2 = classElement("A2");
    ClassElement elementA3 = classElement("A3");
    ClassElementImpl elementB = (ClassElementImpl) classElement("B");
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeA2 = elementA2.getType();
    InterfaceType typeA3 = elementA3.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementB.setInterfaces(new InterfaceType[] {typeA, typeA2});
    elementC.setInterfaces(new InterfaceType[] {typeA, typeA3});
    assertEquals(typeA, typeB.getLeastUpperBound(typeC));
    assertEquals(typeA, typeC.getLeastUpperBound(typeB));
  }

  public void test_getTypeArguments() {
    InterfaceTypeImpl type = new InterfaceTypeImpl(new ClassElementImpl(identifier("A")));
    assertLength(0, type.getTypeArguments());
  }

  public void test_isDirectSupertypeOf_false() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B");
    ClassElement elementC = classElement("C", elementB.getType());
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeC = new InterfaceTypeImpl(elementC);
    assertFalse(typeA.isDirectSupertypeOf(typeC));
  }

  public void test_isDirectSupertypeOf_true() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    InterfaceTypeImpl typeA = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeB = new InterfaceTypeImpl(elementB);
    assertTrue(typeA.isDirectSupertypeOf(typeB));
  }

  public void test_isMoreSpecificThan_bottom() {
    Type type = classElement("A").getType();
    assertTrue(BottomTypeImpl.getInstance().isMoreSpecificThan(type));
  }

  public void test_isMoreSpecificThan_covariance() {
    ClassElement elementA = classElement("A", "E");
    ClassElement elementI = classElement("I");
    ClassElement elementJ = classElement("J", elementI.getType());
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeAJ = new InterfaceTypeImpl(elementA);
    typeAI.setTypeArguments(new Type[] {elementI.getType()});
    typeAJ.setTypeArguments(new Type[] {elementJ.getType()});
    assertTrue(typeAJ.isMoreSpecificThan(typeAI));
    assertFalse(typeAI.isMoreSpecificThan(typeAJ));
  }

  public void test_isMoreSpecificThan_directSupertype() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    assertTrue(typeB.isMoreSpecificThan(typeA));
    // the opposite test tests a different branch in isMoreSpecificThan()
    assertFalse(typeA.isMoreSpecificThan(typeB));
  }

  public void test_isMoreSpecificThan_dynamic() {
    InterfaceType type = classElement("A").getType();
    assertTrue(type.isMoreSpecificThan(DynamicTypeImpl.getInstance()));
  }

  public void test_isMoreSpecificThan_indirectSupertype() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementB.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeC = elementC.getType();
    assertTrue(typeC.isMoreSpecificThan(typeA));
  }

  public void test_isMoreSpecificThan_self() {
    InterfaceType type = classElement("A").getType();
    assertTrue(type.isMoreSpecificThan(type));
  }

  public void test_isSubtypeOf_directSubtype() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    assertTrue(typeB.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeB));
  }

  public void test_isSubtypeOf_dynamic() {
    ClassElement elementA = classElement("A");
    InterfaceType typeA = elementA.getType();
    Type dynamicType = DynamicTypeImpl.getInstance();
    assertFalse(dynamicType.isSubtypeOf(typeA));
    assertTrue(typeA.isSubtypeOf(dynamicType));
  }

  public void test_isSubtypeOf_indirectSubtype() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementB.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeC = elementC.getType();
    assertTrue(typeC.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_interface() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    InterfaceType typeObject = elementA.getSupertype();
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementC.setInterfaces(new InterfaceType[] {typeB});

    assertTrue(typeC.isSubtypeOf(typeB));
    assertTrue(typeC.isSubtypeOf(typeObject));
    assertTrue(typeC.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_mixins() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    InterfaceType typeObject = elementA.getSupertype();
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementC.setMixins(new InterfaceType[] {typeB});

    assertTrue(typeC.isSubtypeOf(typeB));
    assertTrue(typeC.isSubtypeOf(typeObject));
    assertFalse(typeC.isSubtypeOf(typeA));
    assertFalse(typeA.isSubtypeOf(typeC));
  }

  public void test_isSubtypeOf_object() {
    ClassElement elementA = classElement("A");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeObject = elementA.getSupertype();
    assertTrue(typeA.isSubtypeOf(typeObject));
    assertFalse(typeObject.isSubtypeOf(typeA));
  }

  public void test_isSubtypeOf_self() {
    ClassElement elementA = classElement("A");
    InterfaceType typeA = elementA.getType();
    assertTrue(typeA.isSubtypeOf(typeA));
  }

  public void test_isSubtypeOf_typeArguments() {
    ClassElement elementA = classElement("A", "E");
    ClassElement elementI = classElement("I");
    ClassElement elementJ = classElement("J", elementI.getType());
    ClassElement elementK = classElement("K");
    InterfaceType typeA = elementA.getType();
    InterfaceTypeImpl typeAI = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeAJ = new InterfaceTypeImpl(elementA);
    InterfaceTypeImpl typeAK = new InterfaceTypeImpl(elementA);
    typeAI.setTypeArguments(new Type[] {elementI.getType()});
    typeAJ.setTypeArguments(new Type[] {elementJ.getType()});
    typeAK.setTypeArguments(new Type[] {elementK.getType()});

    // A<J> <: A<I> since J <: I
    assertTrue(typeAJ.isSubtypeOf(typeAI));
    assertFalse(typeAI.isSubtypeOf(typeAJ));

    // A<I> <: A<I> since I <: I
    assertTrue(typeAI.isSubtypeOf(typeAI));

    // A <: A<I> and A <: A<J>
    assertTrue(typeA.isSubtypeOf(typeAI));
    assertTrue(typeA.isSubtypeOf(typeAJ));

    // A<I> <: A and A<J> <: A
    assertTrue(typeAI.isSubtypeOf(typeA));
    assertTrue(typeAJ.isSubtypeOf(typeA));

    // A<I> !<: A<K> and A<K> !<: A<I>
    assertFalse(typeAI.isSubtypeOf(typeAK));
    assertFalse(typeAK.isSubtypeOf(typeAI));
  }

  public void test_isSupertypeOf_directSupertype() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    assertFalse(typeB.isSupertypeOf(typeA));
    assertTrue(typeA.isSupertypeOf(typeB));
  }

  public void test_isSupertypeOf_dynamic() {
    ClassElement elementA = classElement("A");
    InterfaceType typeA = elementA.getType();
    Type dynamicType = DynamicTypeImpl.getInstance();
    assertTrue(dynamicType.isSupertypeOf(typeA));
    assertFalse(typeA.isSupertypeOf(dynamicType));
  }

  public void test_isSupertypeOf_indirectSupertype() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElement elementC = classElement("C", elementB.getType());
    InterfaceType typeA = elementA.getType();
    InterfaceType typeC = elementC.getType();
    assertFalse(typeC.isSupertypeOf(typeA));
    assertTrue(typeA.isSupertypeOf(typeC));
  }

  public void test_isSupertypeOf_interface() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    InterfaceType typeObject = elementA.getSupertype();
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementC.setInterfaces(new InterfaceType[] {typeB});

    assertTrue(typeB.isSupertypeOf(typeC));
    assertTrue(typeObject.isSupertypeOf(typeC));
    assertTrue(typeA.isSupertypeOf(typeC));
    assertFalse(typeC.isSupertypeOf(typeA));
  }

  public void test_isSupertypeOf_mixins() {
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B", elementA.getType());
    ClassElementImpl elementC = (ClassElementImpl) classElement("C");
    InterfaceType typeObject = elementA.getSupertype();
    InterfaceType typeA = elementA.getType();
    InterfaceType typeB = elementB.getType();
    InterfaceType typeC = elementC.getType();
    elementC.setMixins(new InterfaceType[] {typeB});

    assertTrue(typeB.isSupertypeOf(typeC));
    assertTrue(typeObject.isSupertypeOf(typeC));
    assertFalse(typeA.isSupertypeOf(typeC));
    assertFalse(typeC.isSupertypeOf(typeA));
  }

  public void test_isSupertypeOf_object() {
    ClassElement elementA = classElement("A");
    InterfaceType typeA = elementA.getType();
    InterfaceType typeObject = elementA.getSupertype();
    assertFalse(typeA.isSupertypeOf(typeObject));
    assertTrue(typeObject.isSupertypeOf(typeA));
  }

  public void test_isSupertypeOf_self() {
    ClassElement elementA = classElement("A");
    InterfaceType typeA = elementA.getType();
    assertTrue(typeA.isSupertypeOf(typeA));
  }

  public void test_setTypeArguments() {
    InterfaceTypeImpl type = (InterfaceTypeImpl) classElement("A").getType();
    Type[] typeArguments = new Type[] {
        new InterfaceTypeImpl(classElement("B")), new InterfaceTypeImpl(classElement("C")),};
    type.setTypeArguments(typeArguments);
    assertEquals(typeArguments, type.getTypeArguments());
  }

  public void test_substitute_equal() {
    ClassElementImpl classElement = new ClassElementImpl(identifier("A"));
    TypeVariableElementImpl parameterElement = new TypeVariableElementImpl(identifier("E"));
    //classElement.setTypeVariables(new TypeVariableElement[] {parameterElement});

    InterfaceTypeImpl type = new InterfaceTypeImpl(classElement);
    TypeVariableTypeImpl parameter = new TypeVariableTypeImpl(parameterElement);
    type.setTypeArguments(new Type[] {parameter});

    InterfaceTypeImpl argumentType = new InterfaceTypeImpl(new ClassElementImpl(identifier("B")));

    InterfaceType result = type.substitute(new Type[] {argumentType}, new Type[] {parameter});
    assertEquals(classElement, result.getElement());
    Type[] resultArguments = result.getTypeArguments();
    assertLength(1, resultArguments);
    assertEquals(argumentType, resultArguments[0]);
  }

  public void test_substitute_notEqual() {
    ClassElementImpl classElement = new ClassElementImpl(identifier("A"));
    TypeVariableElementImpl parameterElement = new TypeVariableElementImpl(identifier("E"));
    //classElement.setTypeVariables(new TypeVariableElement[] {parameterElement});

    InterfaceTypeImpl type = new InterfaceTypeImpl(classElement);
    TypeVariableTypeImpl parameter = new TypeVariableTypeImpl(parameterElement);
    type.setTypeArguments(new Type[] {parameter});

    InterfaceTypeImpl argumentType = new InterfaceTypeImpl(new ClassElementImpl(identifier("B")));
    TypeVariableTypeImpl parameterType = new TypeVariableTypeImpl(new TypeVariableElementImpl(
        identifier("F")));

    InterfaceType result = type.substitute(new Type[] {argumentType}, new Type[] {parameterType});
    assertEquals(classElement, result.getElement());
    Type[] resultArguments = result.getTypeArguments();
    assertLength(1, resultArguments);
    assertEquals(parameter, resultArguments[0]);
  }
}
