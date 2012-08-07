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
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.internal.type.TypeImpl;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code TypeElementImpl} implement a {@code TypeElement}.
 */
public class TypeElementImpl extends ElementImpl implements TypeElement {
  /**
   * An array containing all of the accessors (getters and setters) contained in this type.
   */
  private PropertyAccessorElement[] accessors = PropertyAccessorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the constructors contained in this type.
   */
  private ConstructorElement[] constructors = ConstructorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the fields contained in this type.
   */
  private FieldElement[] fields = FieldElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the interfaces that are implemented or extended by this type.
   */
  private Type[] interfaces = TypeImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the methods contained in this type.
   */
  private MethodElement[] methods = MethodElementImpl.EMPTY_ARRAY;

  /**
   * The superclass of the class, or {@code null} if either the class does not have an explicit
   * superclass or if this type represents an interface.
   */
  private Type supertype;

  /**
   * An array containing all of the type variables defined for this type.
   */
  private TypeVariableElement[] typeVariables = TypeVariableElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of type elements.
   */
  public static final TypeElement[] EMPTY_ARRAY = new TypeElement[0];

  /**
   * Initialize a newly created type element to have the given name.
   * 
   * @param name the name of this element
   */
  public TypeElementImpl(Identifier name) {
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
  public Type getSupertype() {
    return supertype;
  }

  @Override
  public TypeVariableElement[] getTypeVariables() {
    return typeVariables;
  }

  /**
   * Return {@code true} if this type is abstract. A type is abstract if it is an interface, it has
   * an explicit {@code abstract} modifier, or it has an abstract method. Note, that this definition
   * of <i>abstract</i> is different from <i>has unimplemented members</i>.
   * 
   * @return {@code true} if this type is abstract
   */
  @Override
  public boolean isAbstract() {
    return hasModifier(Modifier.ABSTRACT);
  }

  /**
   * Set whether this type is abstract to correspond to the given value.
   * 
   * @param isAbstract {@code true} if the type is abstract
   */
  public void setAbstract(boolean isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set the accessors contained in this type to the given accessors.
   * 
   * @param accessors the accessors contained in this type
   */
  public void setAccessors(PropertyAccessorElement[] accessors) {
    for (PropertyAccessorElement accessor : accessors) {
      ((PropertyAccessorElementImpl) accessor).setEnclosingElement(this);
    }
    this.accessors = accessors;
  }

  /**
   * Set the constructors contained in this type to the given constructors.
   * 
   * @param constructors the constructors contained in this type
   */
  public void setConstructors(ConstructorElement[] constructors) {
    for (ConstructorElement constructor : constructors) {
      ((ConstructorElementImpl) constructor).setEnclosingElement(this);
    }
    this.constructors = constructors;
  }

  /**
   * Set the fields contained in this type to the given fields.
   * 
   * @param fields the fields contained in this type
   */
  public void setFields(FieldElement[] fields) {
    for (FieldElement field : fields) {
      ((FieldElementImpl) field).setEnclosingElement(this);
    }
    this.fields = fields;
  }

  /**
   * Set the interfaces that are implemented or extended by this type to the given types.
   * 
   * @param the interfaces that are implemented or extended by this type
   */
  public void setInterfaces(Type[] interfaces) {
    this.interfaces = interfaces;
  }

  /**
   * Set the methods contained in this type to the given methods.
   * 
   * @param methods the methods contained in this type
   */
  public void setMethods(MethodElement[] methods) {
    for (MethodElement method : methods) {
      ((MethodElementImpl) method).setEnclosingElement(this);
    }
    this.methods = methods;
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
   * Set the type variables defined for this type to the given type variables.
   * 
   * @param typeVariables the type variables defined for this type
   */
  public void setTypeVariables(TypeVariableElement[] typeVariables) {
    for (TypeVariableElement typeVariable : typeVariables) {
      ((TypeVariableElementImpl) typeVariable).setEnclosingElement(this);
    }
    this.typeVariables = typeVariables;
  }
}
