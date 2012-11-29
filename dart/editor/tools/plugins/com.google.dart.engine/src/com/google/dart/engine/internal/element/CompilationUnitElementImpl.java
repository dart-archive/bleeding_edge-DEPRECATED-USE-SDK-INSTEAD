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

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code CompilationUnitElementImpl} implement a
 * {@link CompilationUnitElement}.
 */
public class CompilationUnitElementImpl extends ElementImpl implements CompilationUnitElement {
  /**
   * An array containing all of the top-level accessors (getters and setters) contained in this
   * compilation unit.
   */
  private PropertyAccessorElement[] accessors = PropertyAccessorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the fields contained in this compilation unit.
   */
  private FieldElement[] fields = FieldElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the top-level functions contained in this compilation unit.
   */
  private FunctionElement[] functions = FunctionElementImpl.EMPTY_ARRAY;

  /**
   * The source that corresponds to this compilation unit.
   */
  private Source source;

  /**
   * An array containing all of the type aliases contained in this compilation unit.
   */
  private TypeAliasElement[] typeAliases = TypeAliasElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the types contained in this compilation unit.
   */
  private ClassElement[] types = ClassElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of compilation unit elements.
   */
  public static final CompilationUnitElement[] EMPTY_ARRAY = new CompilationUnitElement[0];

  /**
   * Initialize a newly created compilation unit element to have the given name.
   * 
   * @param name the name of this element
   */
  public CompilationUnitElementImpl(String name) {
    // Compilation units do not contain their own declaration.
    super(name, -1);
  }

  @Override
  public boolean equals(Object object) {
    return this.getClass() == object.getClass()
        && source.equals(((CompilationUnitElementImpl) object).getSource());
  }

  @Override
  public PropertyAccessorElement[] getAccessors() {
    return accessors;
  }

  @Override
  public LibraryElement getEnclosingElement() {
    return (LibraryElement) super.getEnclosingElement();
  }

  @Override
  public FieldElement[] getFields() {
    return fields;
  }

  @Override
  public FunctionElement[] getFunctions() {
    return functions;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.COMPILATION_UNIT;
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public TypeAliasElement[] getTypeAliases() {
    return typeAliases;
  }

  @Override
  public ClassElement[] getTypes() {
    return types;
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }

  /**
   * Set the top-level accessors (getters and setters) contained in this compilation unit to the
   * given accessors.
   * 
   * @param the top-level accessors (getters and setters) contained in this compilation unit
   */
  public void setAccessors(PropertyAccessorElement[] accessors) {
    for (PropertyAccessorElement accessor : accessors) {
      ((PropertyAccessorElementImpl) accessor).setEnclosingElement(this);
    }
    this.accessors = accessors;
  }

  /**
   * Set the fields contained in this compilation unit to the given fields.
   * 
   * @param fields the fields contained in this compilation unit
   */
  public void setFields(FieldElement[] fields) {
    for (FieldElement field : fields) {
      ((FieldElementImpl) field).setEnclosingElement(this);
    }
    this.fields = fields;
  }

  /**
   * Set the top-level functions contained in this compilation unit to the given functions.
   * 
   * @param functions the top-level functions contained in this compilation unit
   */
  public void setFunctions(FunctionElement[] functions) {
    for (FunctionElement function : functions) {
      ((FunctionElementImpl) function).setEnclosingElement(this);
    }
    this.functions = functions;
  }

  /**
   * Set the source that corresponds to this compilation unit to the given source.
   * 
   * @param source the source that corresponds to this compilation unit
   */
  public void setSource(Source source) {
    this.source = source;
  }

  /**
   * Set the type aliases contained in this compilation unit to the given type aliases.
   * 
   * @param typeAliases the type aliases contained in this compilation unit
   */
  public void setTypeAliases(TypeAliasElement[] typeAliases) {
    for (TypeAliasElement typeAlias : typeAliases) {
      ((TypeAliasElementImpl) typeAlias).setEnclosingElement(this);
    }
    this.typeAliases = typeAliases;
  }

  /**
   * Set the types contained in this compilation unit to the given types.
   * 
   * @param types types contained in this compilation unit
   */
  public void setTypes(ClassElement[] types) {
    for (ClassElement type : types) {
      ((ClassElementImpl) type).setEnclosingElement(this);
    }
    this.types = types;
  }
}
