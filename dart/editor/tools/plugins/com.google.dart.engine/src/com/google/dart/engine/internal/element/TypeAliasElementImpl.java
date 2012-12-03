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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.type.FunctionType;

/**
 * Instances of the class {@code TypeAliasElementImpl} implement a {@code TypeAliasElement}.
 */
public class TypeAliasElementImpl extends ElementImpl implements TypeAliasElement {
  /**
   * An array containing all of the parameters defined by this type alias.
   */
  private VariableElement[] parameters = VariableElementImpl.EMPTY_ARRAY;

  /**
   * The type of function defined by this type alias.
   */
  private FunctionType type;

  /**
   * An array containing all of the type variables defined for this type.
   */
  private TypeVariableElement[] typeVariables = TypeVariableElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of type alias elements.
   */
  public static final TypeAliasElement[] EMPTY_ARRAY = new TypeAliasElement[0];

  /**
   * Initialize a newly created type alias element to have the given name.
   * 
   * @param name the name of this element
   */
  public TypeAliasElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public ElementImpl getChild(String identifier) {
    for (VariableElement parameter : parameters) {
      if (((VariableElementImpl) parameter).getIdentifier().equals(identifier)) {
        return (VariableElementImpl) parameter;
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
  public CompilationUnitElement getEnclosingElement() {
    return (CompilationUnitElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.TYPE_ALIAS;
  }

  @Override
  public VariableElement[] getParameters() {
    return parameters;
  }

  @Override
  public FunctionType getType() {
    return type;
  }

  @Override
  public TypeVariableElement[] getTypeVariables() {
    return typeVariables;
  }

  /**
   * Set the parameters defined by this type alias to the given parameters.
   * 
   * @param parameters the parameters defined by this type alias
   */
  public void setParameters(VariableElement[] parameters) {
    if (parameters != null) {
      for (VariableElement parameter : parameters) {
        ((VariableElementImpl) parameter).setEnclosingElement(this);
      }
    }
    this.parameters = parameters;
  }

  /**
   * Set the type of function defined by this type alias to the given type.
   * 
   * @param type the type of function defined by this type alias
   */
  public void setType(FunctionType type) {
    this.type = type;
  }

  /**
   * Set the type variables defined for this type to the given variables.
   * 
   * @param typeVariables the type variables defined for this type
   */
  public void setTypeVariables(TypeVariableElement[] typeVariables) {
    for (TypeVariableElement variable : typeVariables) {
      ((TypeVariableElementImpl) variable).setEnclosingElement(this);
    }
    this.typeVariables = typeVariables;
  }
}
