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
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.type.FunctionType;

/**
 * The abstract class {@code ExecutableElementImpl} implements the behavior common to
 * {@code ExecutableElement}s.
 */
public abstract class ExecutableElementImpl extends ElementImpl implements ExecutableElement {
  /**
   * An array containing all of the functions defined within this executable element.
   */
  private ExecutableElement[] functions = EMPTY_ARRAY;

  /**
   * An array containing all of the labels defined within this executable element.
   */
  private LabelElement[] labels = LabelElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the local variables defined within this executable element.
   */
  private VariableElement[] localVariables = VariableElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the parameters defined by this executable element.
   */
  private ParameterElement[] parameters = ParameterElementImpl.EMPTY_ARRAY;

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
  public ExecutableElement[] getFunctions() {
    return functions;
  }

  @Override
  public LabelElement[] getLabels() {
    return labels;
  }

  @Override
  public VariableElement[] getLocalVariables() {
    return localVariables;
  }

  @Override
  public ParameterElement[] getParameters() {
    return parameters;
  }

  @Override
  public FunctionType getType() {
    return type;
  }

  /**
   * Set the functions defined within this executable element to the given functions.
   * 
   * @param functions the functions defined within this executable element
   */
  public void setFunctions(ExecutableElement[] functions) {
    for (ExecutableElement function : functions) {
      ((ExecutableElementImpl) function).setEnclosingElement(this);
    }
    this.functions = functions;
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
  public void setLocalVariables(VariableElement[] localVariables) {
    for (VariableElement variable : localVariables) {
      ((VariableElementImpl) variable).setEnclosingElement(this);
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
   * Set the type of function defined by this executable element to the given type.
   * 
   * @param type the type of function defined by this executable element
   */
  public void setType(FunctionType type) {
    this.type = type;
  }
}
