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
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.type.InterfaceType;

import java.util.HashSet;

/**
 * Instances of the class {@code ClassElementImpl} implement a {@code ClassElement}.
 * 
 * @coverage dart.engine.element
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
  private InterfaceType[] mixins = InterfaceTypeImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the interfaces that are implemented by this class.
   */
  private InterfaceType[] interfaces = InterfaceTypeImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the methods contained in this class.
   */
  private MethodElement[] methods = MethodElementImpl.EMPTY_ARRAY;

  /**
   * The superclass of the class, or {@code null} if the class does not have an explicit superclass.
   */
  private InterfaceType supertype;

  /**
   * The type defined by the class.
   */
  private InterfaceType type;

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
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitClassElement(this);
  }

  @Override
  public PropertyAccessorElement[] getAccessors() {
    return accessors;
  }

  @Override
  public InterfaceType[] getAllSupertypes() {
    HashSet<InterfaceType> list = new HashSet<InterfaceType>();
    collectAllSupertypes(list);
    return list.toArray(new InterfaceType[list.size()]);
  }

  @Override
  public ElementImpl getChild(String identifier) {
    //
    // The casts in this method are safe because the set methods would have thrown a CCE if any of
    // the elements in the arrays were not of the expected types.
    //
    for (PropertyAccessorElement accessor : accessors) {
      if (((PropertyAccessorElementImpl) accessor).getIdentifier().equals(identifier)) {
        return (PropertyAccessorElementImpl) accessor;
      }
    }
    for (ConstructorElement constructor : constructors) {
      if (((ConstructorElementImpl) constructor).getIdentifier().equals(identifier)) {
        return (ConstructorElementImpl) constructor;
      }
    }
    for (FieldElement field : fields) {
      if (((FieldElementImpl) field).getIdentifier().equals(identifier)) {
        return (FieldElementImpl) field;
      }
    }
    for (MethodElement method : methods) {
      if (((MethodElementImpl) method).getIdentifier().equals(identifier)) {
        return (MethodElementImpl) method;
      }
    }
    for (TypeVariableElement typeVariable : typeVariables) {
      if (((TypeVariableElementImpl) typeVariable).getIdentifier().equals(identifier)) {
        return (TypeVariableElementImpl) typeVariable;
      }
    }
    return null;
  }

  @Override
  public ConstructorElement[] getConstructors() {
    return constructors;
  }

  /**
   * Given some name, this returns the {@link FieldElement} with the matching name, if there is no
   * such field, then {@code null} is returned.
   * 
   * @param name some name to lookup a field element with
   * @return the matching field element, or {@code null} if no such element was found
   */
  public FieldElement getField(String name) {
    for (FieldElement fieldElement : fields) {
      if (name.equals(fieldElement.getName())) {
        return fieldElement;
      }
    }
    return null;
  }

  @Override
  public FieldElement[] getFields() {
    return fields;
  }

  /**
   * Return the element representing the getter with the given name that is declared in this class,
   * or {@code null} if this class does not declare a getter with the given name.
   * 
   * @param getterName the name of the getter to be returned
   * @return the getter declared in this class with the given name
   */
  public PropertyAccessorElement getGetter(String getterName) {
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isGetter() && accessor.getName().equals(getterName)) {
        return accessor;
      }
    }
    return null;
  }

  @Override
  public InterfaceType[] getInterfaces() {
    return interfaces;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.CLASS;
  }

  /**
   * Return the element representing the method with the given name that is declared in this class,
   * or {@code null} if this class does not declare a method with the given name.
   * 
   * @param methodName the name of the method to be returned
   * @return the method declared in this class with the given name
   */
  public MethodElement getMethod(String methodName) {
    for (MethodElement method : methods) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    return null;
  }

  @Override
  public MethodElement[] getMethods() {
    return methods;
  }

  @Override
  public InterfaceType[] getMixins() {
    return mixins;
  }

  @Override
  public ConstructorElement getNamedConstructor(String name) {
    for (ConstructorElement element : getConstructors()) {
      String elementName = element.getName();
      if (elementName != null && elementName.equals(name)) {
        return element;
      }
    }
    return null;
  }

  /**
   * Return the element representing the setter with the given name that is declared in this class,
   * or {@code null} if this class does not declare a setter with the given name.
   * 
   * @param setterName the name of the getter to be returned
   * @return the setter declared in this class with the given name
   */
  public PropertyAccessorElement getSetter(String setterName) {
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isSetter() && accessor.getName().equals(setterName)) {
        return accessor;
      }
    }
    return null;
  }

  @Override
  public InterfaceType getSupertype() {
    return supertype;
  }

  @Override
  public InterfaceType getType() {
    return type;
  }

  @Override
  public TypeVariableElement[] getTypeVariables() {
    return typeVariables;
  }

  @Override
  public ConstructorElement getUnnamedConstructor() {
    for (ConstructorElement element : getConstructors()) {
      String name = element.getName();
      if (name == null || name.isEmpty()) {
        return element;
      }
    }
    return null;
  }

  @Override
  public boolean isAbstract() {
    return hasModifier(Modifier.ABSTRACT);
  }

  @Override
  public boolean isTypedef() {
    return hasModifier(Modifier.TYPEDEF);
  }

  @Override
  public boolean isValidMixin() {
    return hasModifier(Modifier.MIXIN);
  }

  @Override
  public PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library) {
    PropertyAccessorElement element = getGetter(getterName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    for (InterfaceType mixin : mixins) {
      ClassElement mixinElement = mixin.getElement();
      if (mixinElement != null) {
        element = ((ClassElementImpl) mixinElement).getGetter(getterName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
    }
    if (supertype != null) {
      ClassElement supertypeElement = supertype.getElement();
      if (supertypeElement != null) {
        element = supertypeElement.lookUpGetter(getterName, library);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
    }
    return null;
  }

  @Override
  public MethodElement lookUpMethod(String methodName, LibraryElement library) {
    MethodElement element = getMethod(methodName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    for (InterfaceType mixin : mixins) {
      ClassElement mixinElement = mixin.getElement();
      if (mixinElement != null) {
        element = ((ClassElementImpl) mixinElement).getMethod(methodName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
    }
    if (supertype != null) {
      ClassElement supertypeElement = supertype.getElement();
      if (supertypeElement != null) {
        element = supertypeElement.lookUpMethod(methodName, library);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
    }
    return null;
  }

  @Override
  public PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library) {
    PropertyAccessorElement element = getSetter(setterName);
    if (element != null && element.isAccessibleIn(library)) {
      return element;
    }
    for (InterfaceType mixin : mixins) {
      ClassElement mixinElement = mixin.getElement();
      if (mixinElement != null) {
        element = ((ClassElementImpl) mixinElement).getSetter(setterName);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
    }
    if (supertype != null) {
      ClassElement supertypeElement = supertype.getElement();
      if (supertypeElement != null) {
        element = supertypeElement.lookUpSetter(setterName, library);
        if (element != null && element.isAccessibleIn(library)) {
          return element;
        }
      }
    }
    return null;
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
  public void setInterfaces(InterfaceType[] interfaces) {
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
  public void setMixins(InterfaceType[] mixins) {
    this.mixins = mixins;
  }

  /**
   * Set the superclass of the class to the given type.
   * 
   * @param supertype the superclass of the class
   */
  public void setSupertype(InterfaceType supertype) {
    this.supertype = supertype;
  }

  /**
   * Set the type defined by the class to the given type.
   * 
   * @param type the type defined by the class
   */
  public void setType(InterfaceType type) {
    this.type = type;
  }

  /**
   * Set whether this class is defined by a typedef construct to correspond to the given value.
   * 
   * @param isTypedef {@code true} if the class is defined by a typedef construct
   */
  public void setTypedef(boolean isTypedef) {
    setModifier(Modifier.TYPEDEF, isTypedef);
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

  /**
   * Set whether this class is a valid mixin to correspond to the given value.
   * 
   * @param isValidMixin {@code true} if this class can be used as a mixin
   */
  public void setValidMixin(boolean isValidMixin) {
    setModifier(Modifier.MIXIN, isValidMixin);
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(accessors, visitor);
    safelyVisitChildren(constructors, visitor);
    safelyVisitChildren(fields, visitor);
    safelyVisitChildren(methods, visitor);
    safelyVisitChildren(typeVariables, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    String name = getName();
    if (name == null) {
      builder.append("{unnamed class}");
    } else {
      builder.append(name);
    }
    int variableCount = typeVariables.length;
    if (variableCount > 0) {
      builder.append("<");
      for (int i = 0; i < variableCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        ((TypeVariableElementImpl) typeVariables[i]).appendTo(builder);
      }
      builder.append(">");
    }
  }

  private void collectAllSupertypes(HashSet<InterfaceType> list) {
    if (supertype == null || list.contains(supertype)) {
      return;
    }
    list.add(supertype);
    ((ClassElementImpl) supertype.getElement()).collectAllSupertypes(list);
    for (InterfaceType type : getInterfaces()) {
      if (!list.contains(type)) {
        list.add(type);
        ((ClassElementImpl) type.getElement()).collectAllSupertypes(list);
      }
    }
    for (InterfaceType type : getMixins()) {
      if (!list.contains(type)) {
        list.add(type);
      }
    }
  }
}
