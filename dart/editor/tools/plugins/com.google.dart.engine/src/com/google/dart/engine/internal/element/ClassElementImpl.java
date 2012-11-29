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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.internal.type.TypeImpl;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code ClassElementImpl} implement a {@code ClassElement}.
 */
public class ClassElementImpl extends ElementImpl implements ClassElement {
  /**
   * An array containing all of the accessors (getters and setters) contained in this class.
   */
  private PropertyAccessorElement[] accessors = PropertyAccessorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the constructors contained in this class.
   */
  private ConstructorElement[] constructors = ConstructorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the fields contained in this class.
   */
  private FieldElement[] fields = FieldElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the mixins that are applied to the class being extended in order to
   * derive the superclass of this class.
   */
  private Type[] mixins = TypeImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the interfaces that are implemented by this class.
   */
  private Type[] interfaces = TypeImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the methods contained in this class.
   */
  private MethodElement[] methods = MethodElementImpl.EMPTY_ARRAY;

  /**
   * The superclass of the class, or {@code null} if the class does not have an explicit superclass.
   */
  private Type supertype;

  /**
   * The type defined by the class.
   */
  private Type type;

  /**
   * An array containing all of the type variables defined for this class.
   */
  private TypeVariableElement[] typeVariables = TypeVariableElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of type elements.
   */
  public static final ClassElement[] EMPTY_ARRAY = new ClassElement[0];

  /**
   * Initialize a newly created class element to have the given name.
   * 
   * @param name the name of this element
   */
  public ClassElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public PropertyAccessorElement[] getAccessors() {
    return accessors;
  }

  @Override
  public ConstructorElement[] getConstructors() {
    return constructors;
  }

  @Override
  public FieldElement[] getFields() {
    return fields;
  }

  @Override
  public Type[] getInterfaces() {
    return interfaces;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.TYPE;
  }

  @Override
  public MethodElement[] getMethods() {
    return methods;
  }

  @Override
  public Type[] getMixins() {
    return mixins;
  }

  @Override
  public Type getSupertype() {
    return supertype;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public TypeVariableElement[] getTypeVariables() {
    return typeVariables;
  }

  @Override
  public boolean isAbstract() {
    return hasModifier(Modifier.ABSTRACT);
  }

  /**
   * Set whether this class is abstract to correspond to the given value.
   * 
   * @param isAbstract {@code true} if the class is abstract
   */
  public void setAbstract(boolean isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set the accessors contained in this class to the given accessors.
   * 
   * @param accessors the accessors contained in this class
   */
  public void setAccessors(PropertyAccessorElement[] accessors) {
    for (PropertyAccessorElement accessor : accessors) {
      ((PropertyAccessorElementImpl) accessor).setEnclosingElement(this);
    }
    this.accessors = accessors;
  }

  /**
   * Set the constructors contained in this class to the given constructors.
   * 
   * @param constructors the constructors contained in this class
   */
  public void setConstructors(ConstructorElement[] constructors) {
    for (ConstructorElement constructor : constructors) {
      ((ConstructorElementImpl) constructor).setEnclosingElement(this);
    }
    this.constructors = constructors;
  }

  /**
   * Set the fields contained in this class to the given fields.
   * 
   * @param fields the fields contained in this class
   */
  public void setFields(FieldElement[] fields) {
    for (FieldElement field : fields) {
      ((FieldElementImpl) field).setEnclosingElement(this);
    }
    this.fields = fields;
  }

  /**
   * Set the interfaces that are implemented by this class to the given types.
   * 
   * @param the interfaces that are implemented by this class
   */
  public void setInterfaces(Type[] interfaces) {
    this.interfaces = interfaces;
  }

  /**
   * Set the methods contained in this class to the given methods.
   * 
   * @param methods the methods contained in this class
   */
  public void setMethods(MethodElement[] methods) {
    for (MethodElement method : methods) {
      ((MethodElementImpl) method).setEnclosingElement(this);
    }
    this.methods = methods;
  }

  /**
   * Set the mixins that are applied to the class being extended in order to derive the superclass
   * of this class to the given types.
   * 
   * @param mixins the mixins that are applied to derive the superclass of this class
   */
  public void setMixins(Type[] mixins) {
    this.mixins = mixins;
  }

  /**
   * Set the superclass of the class to the given type.
   * 
   * @param supertype the superclass of the class
   */
  public void setSupertype(Type supertype) {
    this.supertype = supertype;
  }

  /**
   * Set the type defined by the class to the given type.
   * 
   * @param type the type defined by the class
   */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Set the type variables defined for this class to the given type variables.
   * 
   * @param typeVariables the type variables defined for this class
   */
  public void setTypeVariables(TypeVariableElement[] typeVariables) {
    for (TypeVariableElement typeVariable : typeVariables) {
      ((TypeVariableElementImpl) typeVariable).setEnclosingElement(this);
    }
    this.typeVariables = typeVariables;
  }
}
