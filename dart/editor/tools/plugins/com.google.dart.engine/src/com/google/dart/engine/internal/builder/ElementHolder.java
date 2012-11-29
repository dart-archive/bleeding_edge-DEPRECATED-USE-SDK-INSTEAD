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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;

import java.util.ArrayList;

/**
 * Instances of the class {@code ElementHolder} hold on to elements created while traversing an AST
 * structure so that they can be accessed when creating their enclosing element.
 */
public class ElementHolder {
  private ArrayList<PropertyAccessorElement> accessors = new ArrayList<PropertyAccessorElement>();
  private ArrayList<ConstructorElement> constructors = new ArrayList<ConstructorElement>();
  private ArrayList<FieldElement> fields = new ArrayList<FieldElement>();
  private ArrayList<FunctionElement> functions = new ArrayList<FunctionElement>();
  private ArrayList<LabelElement> labels = new ArrayList<LabelElement>();
  private ArrayList<MethodElement> methods = new ArrayList<MethodElement>();
  private ArrayList<TypeAliasElement> typeAliases = new ArrayList<TypeAliasElement>();
  private ArrayList<ParameterElement> parameters = new ArrayList<ParameterElement>();
  private ArrayList<ClassElement> types = new ArrayList<ClassElement>();
  private ArrayList<TypeVariableElement> typeVariables = new ArrayList<TypeVariableElement>();
  private ArrayList<VariableElement> variables = new ArrayList<VariableElement>();

  /**
   * Initialize a newly created element holder.
   */
  public ElementHolder() {
    super();
  }

  public void addAccessor(PropertyAccessorElement element) {
    accessors.add(element);
  }

  public void addConstructor(ConstructorElement element) {
    constructors.add(element);
  }

  public void addField(FieldElement element) {
    fields.add(element);
  }

  public void addFunction(FunctionElement element) {
    functions.add(element);
  }

  public void addLabel(LabelElement element) {
    labels.add(element);
  }

  public void addMethod(MethodElement element) {
    methods.add(element);
  }

  public void addParameter(ParameterElement element) {
    parameters.add(element);
  }

  public void addType(ClassElement element) {
    types.add(element);
  }

  public void addTypeAlias(TypeAliasElement element) {
    typeAliases.add(element);
  }

  public void addTypeVariable(TypeVariableElement element) {
    typeVariables.add(element);
  }

  public void addVariable(VariableElement element) {
    variables.add(element);
  }

  public PropertyAccessorElement[] getAccessors() {
    return accessors.toArray(new PropertyAccessorElement[accessors.size()]);
  }

  public ConstructorElement[] getConstructors() {
    return constructors.toArray(new ConstructorElement[constructors.size()]);
  }

  public FieldElement getField(String fieldName) {
    for (FieldElement field : fields) {
      if (field.getName().equals(fieldName)) {
        return field;
      }
    }
    return null;
  }

  public FieldElement[] getFields() {
    return fields.toArray(new FieldElement[fields.size()]);
  }

  public FunctionElement[] getFunctions() {
    return functions.toArray(new FunctionElement[functions.size()]);
  }

  public LabelElement[] getLabels() {
    return labels.toArray(new LabelElement[labels.size()]);
  }

  public MethodElement[] getMethods() {
    return methods.toArray(new MethodElement[methods.size()]);
  }

  public ParameterElement[] getParameters() {
    return parameters.toArray(new ParameterElement[parameters.size()]);
  }

  public TypeAliasElement[] getTypeAliases() {
    return typeAliases.toArray(new TypeAliasElement[typeAliases.size()]);
  }

  public ClassElement[] getTypes() {
    return types.toArray(new ClassElement[types.size()]);
  }

  public TypeVariableElement[] getTypeVariables() {
    return typeVariables.toArray(new TypeVariableElement[typeVariables.size()]);
  }

  public VariableElement[] getVariables() {
    return variables.toArray(new VariableElement[variables.size()]);
  }
}
