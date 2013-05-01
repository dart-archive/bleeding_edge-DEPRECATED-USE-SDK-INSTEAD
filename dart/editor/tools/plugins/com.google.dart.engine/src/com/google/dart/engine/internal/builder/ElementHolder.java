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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.FunctionTypeAliasElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.TopLevelVariableElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;

import java.util.ArrayList;

/**
 * Instances of the class {@code ElementHolder} hold on to elements created while traversing an AST
 * structure so that they can be accessed when creating their enclosing element.
 * 
 * @coverage dart.engine.resolver
 */
public class ElementHolder {
  private ArrayList<PropertyAccessorElement> accessors = new ArrayList<PropertyAccessorElement>();
  private ArrayList<ConstructorElement> constructors = new ArrayList<ConstructorElement>();
  private ArrayList<FieldElement> fields = new ArrayList<FieldElement>();
  private ArrayList<FunctionElement> functions = new ArrayList<FunctionElement>();
  private ArrayList<LabelElement> labels = new ArrayList<LabelElement>();
  private ArrayList<VariableElement> localVariables = new ArrayList<VariableElement>();
  private ArrayList<MethodElement> methods = new ArrayList<MethodElement>();
  private ArrayList<FunctionTypeAliasElement> typeAliases = new ArrayList<FunctionTypeAliasElement>();
  private ArrayList<ParameterElement> parameters = new ArrayList<ParameterElement>();
  private ArrayList<VariableElement> topLevelVariables = new ArrayList<VariableElement>();
  private ArrayList<ClassElement> types = new ArrayList<ClassElement>();
  private ArrayList<TypeVariableElement> typeVariables = new ArrayList<TypeVariableElement>();

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

  public void addLocalVariable(LocalVariableElement element) {
    localVariables.add(element);
  }

  public void addMethod(MethodElement element) {
    methods.add(element);
  }

  public void addParameter(ParameterElement element) {
    parameters.add(element);
  }

  public void addTopLevelVariable(TopLevelVariableElement element) {
    topLevelVariables.add(element);
  }

  public void addType(ClassElement element) {
    types.add(element);
  }

  public void addTypeAlias(FunctionTypeAliasElement element) {
    typeAliases.add(element);
  }

  public void addTypeVariable(TypeVariableElement element) {
    typeVariables.add(element);
  }

  public PropertyAccessorElement[] getAccessors() {
    if (accessors.isEmpty()) {
      return PropertyAccessorElementImpl.EMPTY_ARRAY;
    }
    return accessors.toArray(new PropertyAccessorElement[accessors.size()]);
  }

  public ConstructorElement[] getConstructors() {
    if (constructors.isEmpty()) {
      return ConstructorElementImpl.EMPTY_ARRAY;
    }
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
    if (fields.isEmpty()) {
      return FieldElementImpl.EMPTY_ARRAY;
    }
    return fields.toArray(new FieldElement[fields.size()]);
  }

  public FunctionElement[] getFunctions() {
    if (functions.isEmpty()) {
      return FunctionElementImpl.EMPTY_ARRAY;
    }
    return functions.toArray(new FunctionElement[functions.size()]);
  }

  public LabelElement[] getLabels() {
    if (labels.isEmpty()) {
      return LabelElementImpl.EMPTY_ARRAY;
    }
    return labels.toArray(new LabelElement[labels.size()]);
  }

  public LocalVariableElement[] getLocalVariables() {
    if (localVariables.isEmpty()) {
      return LocalVariableElementImpl.EMPTY_ARRAY;
    }
    return localVariables.toArray(new LocalVariableElement[localVariables.size()]);
  }

  public MethodElement[] getMethods() {
    if (methods.isEmpty()) {
      return MethodElementImpl.EMPTY_ARRAY;
    }
    return methods.toArray(new MethodElement[methods.size()]);
  }

  public ParameterElement[] getParameters() {
    if (parameters.isEmpty()) {
      return ParameterElementImpl.EMPTY_ARRAY;
    }
    return parameters.toArray(new ParameterElement[parameters.size()]);
  }

  public TopLevelVariableElement[] getTopLevelVariables() {
    if (topLevelVariables.isEmpty()) {
      return TopLevelVariableElementImpl.EMPTY_ARRAY;
    }
    return topLevelVariables.toArray(new TopLevelVariableElement[topLevelVariables.size()]);
  }

  public FunctionTypeAliasElement[] getTypeAliases() {
    if (typeAliases.isEmpty()) {
      return FunctionTypeAliasElementImpl.EMPTY_ARRAY;
    }
    return typeAliases.toArray(new FunctionTypeAliasElement[typeAliases.size()]);
  }

  public ClassElement[] getTypes() {
    if (types.isEmpty()) {
      return ClassElementImpl.EMPTY_ARRAY;
    }
    return types.toArray(new ClassElement[types.size()]);
  }

  public TypeVariableElement[] getTypeVariables() {
    if (typeVariables.isEmpty()) {
      return TypeVariableElementImpl.EMPTY_ARRAY;
    }
    return typeVariables.toArray(new TypeVariableElement[typeVariables.size()]);
  }
}
