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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * The abstract class {@code ExecutableElementImpl} implements the behavior common to
 * {@code ExecutableElement}s.
 * 
 * @coverage dart.engine.element
 */
public abstract class ExecutableElementImpl extends ElementImpl implements ExecutableElement {
  /**
   * An array containing all of the functions defined within this executable element.
   */
  private FunctionElement[] functions = FunctionElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the labels defined within this executable element.
   */
  private LabelElement[] labels = LabelElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the local variables defined within this executable element.
   */
  private LocalVariableElement[] localVariables = LocalVariableElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the parameters defined by this executable element.
   */
  private ParameterElement[] parameters = ParameterElementImpl.EMPTY_ARRAY;

  /**
   * The return type defined by this executable element.
   */
  private Type returnType;

  /**
   * The type of function defined by this executable element.
   */
  private FunctionType type;

  /**
   * An empty array of executable elements.
   */
  public static final ExecutableElement[] EMPTY_ARRAY = new ExecutableElement[0];

  /**
   * Initialize a newly created executable element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public ExecutableElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created executable element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public ExecutableElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public ElementImpl getChild(String identifier) {
    for (ExecutableElement function : functions) {
      if (((ExecutableElementImpl) function).getIdentifier().equals(identifier)) {
        return (ExecutableElementImpl) function;
      }
    }
    for (LabelElement label : labels) {
      if (((LabelElementImpl) label).getIdentifier().equals(identifier)) {
        return (LabelElementImpl) label;
      }
    }
    for (VariableElement variable : localVariables) {
      if (((VariableElementImpl) variable).getIdentifier().equals(identifier)) {
        return (VariableElementImpl) variable;
      }
    }
    for (ParameterElement parameter : parameters) {
      if (((ParameterElementImpl) parameter).getIdentifier().equals(identifier)) {
        return (ParameterElementImpl) parameter;
      }
    }
    return null;
  }

  @Override
  public FunctionElement[] getFunctions() {
    return functions;
  }

  @Override
  public LabelElement[] getLabels() {
    return labels;
  }

  @Override
  public LocalVariableElement[] getLocalVariables() {
    return localVariables;
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
  public boolean isAsynchronous() {
    return hasModifier(Modifier.ASYNCHRONOUS);
  }

  @Override
  public boolean isGenerator() {
    return hasModifier(Modifier.GENERATOR);
  }

  @Override
  public boolean isOperator() {
    return false;
  }

  @Override
  public boolean isSynchronous() {
    return !hasModifier(Modifier.ASYNCHRONOUS);
  }

  /**
   * Set whether this method's body is asynchronous to correspond to the given value.
   * 
   * @param isAsynchronous {@code true} if the method's body is asynchronous
   */
  public void setAsynchronous(boolean isAsynchronous) {
    setModifier(Modifier.ASYNCHRONOUS, isAsynchronous);
  }

  /**
   * Set the functions defined within this executable element to the given functions.
   * 
   * @param functions the functions defined within this executable element
   */
  public void setFunctions(FunctionElement[] functions) {
    for (FunctionElement function : functions) {
      ((FunctionElementImpl) function).setEnclosingElement(this);
    }
    this.functions = functions;
  }

  /**
   * Set whether this method's body is a generator to correspond to the given value.
   * 
   * @param isGenerator {@code true} if the method's body is a generator
   */
  public void setGenerator(boolean isGenerator) {
    setModifier(Modifier.GENERATOR, isGenerator);
  }

  /**
   * Set the labels defined within this executable element to the given labels.
   * 
   * @param labels the labels defined within this executable element
   */
  public void setLabels(LabelElement[] labels) {
    for (LabelElement label : labels) {
      ((LabelElementImpl) label).setEnclosingElement(this);
    }
    this.labels = labels;
  }

  /**
   * Set the local variables defined within this executable element to the given variables.
   * 
   * @param localVariables the local variables defined within this executable element
   */
  public void setLocalVariables(LocalVariableElement[] localVariables) {
    for (LocalVariableElement variable : localVariables) {
      ((LocalVariableElementImpl) variable).setEnclosingElement(this);
    }
    this.localVariables = localVariables;
  }

  /**
   * Set the parameters defined by this executable element to the given parameters.
   * 
   * @param parameters the parameters defined by this executable element
   */
  public void setParameters(ParameterElement[] parameters) {
    for (ParameterElement parameter : parameters) {
      ((ParameterElementImpl) parameter).setEnclosingElement(this);
    }
    this.parameters = parameters;
  }

  /**
   * Set the return type defined by this executable element.
   * 
   * @param returnType the return type defined by this executable element
   */
  public void setReturnType(Type returnType) {
    this.returnType = returnType;
  }

  /**
   * Set the type of function defined by this executable element to the given type.
   * 
   * @param type the type of function defined by this executable element
   */
  public void setType(FunctionType type) {
    this.type = type;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(functions, visitor);
    safelyVisitChildren(labels, visitor);
    safelyVisitChildren(localVariables, visitor);
    safelyVisitChildren(parameters, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    if (getKind() != ElementKind.GETTER) {
      builder.append("(");
      String closing = null;
      ParameterKind kind = ParameterKind.REQUIRED;
      int parameterCount = parameters.length;
      for (int i = 0; i < parameterCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        ParameterElementImpl parameter = (ParameterElementImpl) parameters[i];
        ParameterKind parameterKind = parameter.getParameterKind();
        if (parameterKind != kind) {
          if (closing != null) {
            builder.append(closing);
          }
          if (parameterKind == ParameterKind.POSITIONAL) {
            builder.append("[");
            closing = "]";
          } else if (parameterKind == ParameterKind.NAMED) {
            builder.append("{");
            closing = "}";
          } else {
            closing = null;
          }
        }
        kind = parameterKind;
        parameter.appendToWithoutDelimiters(builder);
      }
      if (closing != null) {
        builder.append(closing);
      }
      builder.append(")");
    }
    if (type != null) {
      builder.append(Element.RIGHT_ARROW);
      builder.append(type.getReturnType());
    }
  }
}
