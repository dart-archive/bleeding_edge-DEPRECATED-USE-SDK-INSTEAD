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

import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.resolver.TypeResolverVisitor;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code FunctionTypeAliasElementImpl} implement a
 * {@code FunctionTypeAliasElement}.
 * 
 * @coverage dart.engine.element
 */
public class FunctionTypeAliasElementImpl extends ElementImpl implements FunctionTypeAliasElement {
  /**
   * An array containing all of the parameters defined by this type alias.
   */
  private ParameterElement[] parameters = ParameterElementImpl.EMPTY_ARRAY;

  /**
   * The return type defined by this type alias.
   */
  private Type returnType;

  /**
   * The type of function defined by this type alias.
   */
  private FunctionType type;

  /**
   * An array containing all of the type parameters defined for this type.
   */
  private TypeParameterElement[] typeParameters = TypeParameterElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of type alias elements.
   */
  public static final FunctionTypeAliasElement[] EMPTY_ARRAY = new FunctionTypeAliasElement[0];

  /**
   * Initialize a newly created type alias element to have the given name.
   * 
   * @param name the name of this element
   */
  public FunctionTypeAliasElementImpl(Identifier name) {
    super(name);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitFunctionTypeAliasElement(this);
  }

  @Override
  public ElementImpl getChild(String identifier) {
    for (VariableElement parameter : parameters) {
      if (((VariableElementImpl) parameter).getIdentifier().equals(identifier)) {
        return (VariableElementImpl) parameter;
      }
    }
    for (TypeParameterElement typeParameter : typeParameters) {
      if (((TypeParameterElementImpl) typeParameter).getIdentifier().equals(identifier)) {
        return (TypeParameterElementImpl) typeParameter;
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
    return ElementKind.FUNCTION_TYPE_ALIAS;
  }

  @Override
  public FunctionTypeAlias getNode() throws AnalysisException {
    return getNodeMatching(FunctionTypeAlias.class);
  }

  @Override
  public ParameterElement[] getParameters() {
    return parameters;
  }

  @Override
  public Type getReturnType() {
    return returnType;
  }

  @Override
  public FunctionType getType() {
    return type;
  }

  @Override
  public TypeParameterElement[] getTypeParameters() {
    return typeParameters;
  }

  /**
   * Set the parameters defined by this type alias to the given parameters.
   * 
   * @param parameters the parameters defined by this type alias
   */
  public void setParameters(ParameterElement[] parameters) {
    if (parameters != null) {
      for (ParameterElement parameter : parameters) {
        ((ParameterElementImpl) parameter).setEnclosingElement(this);
      }
    }
    this.parameters = parameters;
  }

  /**
   * Set the return type defined by this type alias.
   * 
   * @param returnType the return type defined by this type alias
   */
  public void setReturnType(Type returnType) {
    this.returnType = returnType;
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
   * Set the type parameters defined for this type to the given parameters.
   * 
   * @param typeParameters the type parameters defined for this type
   */
  public void setTypeParameters(TypeParameterElement[] typeParameters) {
    for (TypeParameterElement typeParameter : typeParameters) {
      ((TypeParameterElementImpl) typeParameter).setEnclosingElement(this);
    }
    this.typeParameters = typeParameters;
  }

  /**
   * Set the parameters defined by this type alias to the given parameters without becoming the
   * parent of the parameters. This should only be used by the {@link TypeResolverVisitor} when
   * creating a synthetic type alias.
   * 
   * @param parameters the parameters defined by this type alias
   */
  public void shareParameters(ParameterElement[] parameters) {
    this.parameters = parameters;
  }

  /**
   * Set the type parameters defined for this type to the given parameters without becoming the
   * parent of the parameters. This should only be used by the {@link TypeResolverVisitor} when
   * creating a synthetic type alias.
   * 
   * @param typeParameters the type parameters defined for this type
   */
  public void shareTypeParameters(TypeParameterElement[] typeParameters) {
    this.typeParameters = typeParameters;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(parameters, visitor);
    safelyVisitChildren(typeParameters, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append("typedef ");
    builder.append(getDisplayName());
    int typeParameterCount = typeParameters.length;
    if (typeParameterCount > 0) {
      builder.append("<");
      for (int i = 0; i < typeParameterCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        ((TypeParameterElementImpl) typeParameters[i]).appendTo(builder);
      }
      builder.append(">");
    }
    builder.append("(");
    int parameterCount = parameters.length;
    for (int i = 0; i < parameterCount; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      ((ParameterElementImpl) parameters[i]).appendTo(builder);
    }
    builder.append(")");
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.getReturnType());
    }
  }
}
