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

import com.google.dart.engine.AnalysisEngine;
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
import com.google.dart.engine.element.TypeParameterElement;
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
import com.google.dart.engine.internal.element.TypeParameterElementImpl;

import java.util.ArrayList;

/**
 * Instances of the class {@code ElementHolder} hold on to elements created while traversing an AST
 * structure so that they can be accessed when creating their enclosing element.
 * 
 * @coverage dart.engine.resolver
 */
public class ElementHolder {
  private ArrayList<PropertyAccessorElement> accessors;
  private ArrayList<ConstructorElement> constructors;
  private ArrayList<ClassElement> enums;
  private ArrayList<FieldElement> fields;
  private ArrayList<FunctionElement> functions;
  private ArrayList<LabelElement> labels;
  private ArrayList<VariableElement> localVariables;
  private ArrayList<MethodElement> methods;
  private ArrayList<ParameterElement> parameters;
  private ArrayList<TopLevelVariableElement> topLevelVariables;
  private ArrayList<ClassElement> types;
  private ArrayList<FunctionTypeAliasElement> typeAliases;
  private ArrayList<TypeParameterElement> typeParameters;

  /**
   * Initialize a newly created element holder.
   */
  public ElementHolder() {
    super();
  }

  public void addAccessor(PropertyAccessorElement element) {
    if (accessors == null) {
      accessors = new ArrayList<PropertyAccessorElement>();
    }
    accessors.add(element);
  }

  public void addConstructor(ConstructorElement element) {
    if (constructors == null) {
      constructors = new ArrayList<ConstructorElement>();
    }
    constructors.add(element);
  }

  public void addEnum(ClassElement element) {
    if (enums == null) {
      enums = new ArrayList<ClassElement>();
    }
    enums.add(element);
  }

  public void addField(FieldElement element) {
    if (fields == null) {
      fields = new ArrayList<FieldElement>();
    }
    fields.add(element);
  }

  public void addFunction(FunctionElement element) {
    if (functions == null) {
      functions = new ArrayList<FunctionElement>();
    }
    functions.add(element);
  }

  public void addLabel(LabelElement element) {
    if (labels == null) {
      labels = new ArrayList<LabelElement>();
    }
    labels.add(element);
  }

  public void addLocalVariable(LocalVariableElement element) {
    if (localVariables == null) {
      localVariables = new ArrayList<VariableElement>();
    }
    localVariables.add(element);
  }

  public void addMethod(MethodElement element) {
    if (methods == null) {
      methods = new ArrayList<MethodElement>();
    }
    methods.add(element);
  }

  public void addParameter(ParameterElement element) {
    if (parameters == null) {
      parameters = new ArrayList<ParameterElement>();
    }
    parameters.add(element);
  }

  public void addTopLevelVariable(TopLevelVariableElement element) {
    if (topLevelVariables == null) {
      topLevelVariables = new ArrayList<TopLevelVariableElement>();
    }
    topLevelVariables.add(element);
  }

  public void addType(ClassElement element) {
    if (types == null) {
      types = new ArrayList<ClassElement>();
    }
    types.add(element);
  }

  public void addTypeAlias(FunctionTypeAliasElement element) {
    if (typeAliases == null) {
      typeAliases = new ArrayList<FunctionTypeAliasElement>();
    }
    typeAliases.add(element);
  }

  public void addTypeParameter(TypeParameterElement element) {
    if (typeParameters == null) {
      typeParameters = new ArrayList<TypeParameterElement>();
    }
    typeParameters.add(element);
  }

  public PropertyAccessorElement[] getAccessors() {
    if (accessors == null) {
      return PropertyAccessorElementImpl.EMPTY_ARRAY;
    }
    PropertyAccessorElement[] result = accessors.toArray(new PropertyAccessorElement[accessors.size()]);
    accessors = null;
    return result;
  }

  public ConstructorElement[] getConstructors() {
    if (constructors == null) {
      return ConstructorElementImpl.EMPTY_ARRAY;
    }
    ConstructorElement[] result = constructors.toArray(new ConstructorElement[constructors.size()]);
    constructors = null;
    return result;
  }

  public ClassElement[] getEnums() {
    if (enums == null) {
      return ClassElementImpl.EMPTY_ARRAY;
    }
    ClassElement[] result = enums.toArray(new ClassElement[enums.size()]);
    enums = null;
    return result;
  }

  public FieldElement getField(String fieldName) {
    if (fields == null) {
      return null;
    }
    for (FieldElement field : fields) {
      if (field.getName().equals(fieldName)) {
        return field;
      }
    }
    return null;
  }

  public FieldElement[] getFields() {
    if (fields == null) {
      return FieldElementImpl.EMPTY_ARRAY;
    }
    FieldElement[] result = fields.toArray(new FieldElement[fields.size()]);
    fields = null;
    return result;
  }

  public FieldElement[] getFieldsWithoutFlushing() {
    if (fields == null) {
      return FieldElementImpl.EMPTY_ARRAY;
    }
    FieldElement[] result = fields.toArray(new FieldElement[fields.size()]);
    return result;
  }

  public FunctionElement[] getFunctions() {
    if (functions == null) {
      return FunctionElementImpl.EMPTY_ARRAY;
    }
    FunctionElement[] result = functions.toArray(new FunctionElement[functions.size()]);
    functions = null;
    return result;
  }

  public LabelElement[] getLabels() {
    if (labels == null) {
      return LabelElementImpl.EMPTY_ARRAY;
    }
    LabelElement[] result = labels.toArray(new LabelElement[labels.size()]);
    labels = null;
    return result;
  }

  public LocalVariableElement[] getLocalVariables() {
    if (localVariables == null) {
      return LocalVariableElementImpl.EMPTY_ARRAY;
    }
    LocalVariableElement[] result = localVariables.toArray(new LocalVariableElement[localVariables.size()]);
    localVariables = null;
    return result;
  }

  public MethodElement[] getMethods() {
    if (methods == null) {
      return MethodElementImpl.EMPTY_ARRAY;
    }
    MethodElement[] result = methods.toArray(new MethodElement[methods.size()]);
    methods = null;
    return result;
  }

  public ParameterElement[] getParameters() {
    if (parameters == null) {
      return ParameterElementImpl.EMPTY_ARRAY;
    }
    ParameterElement[] result = parameters.toArray(new ParameterElement[parameters.size()]);
    parameters = null;
    return result;
  }

  public TopLevelVariableElement getTopLevelVariable(String variableName) {
    if (topLevelVariables == null) {
      return null;
    }
    for (TopLevelVariableElement variable : topLevelVariables) {
      if (variable.getName().equals(variableName)) {
        return variable;
      }
    }
    return null;
  }

  public TopLevelVariableElement[] getTopLevelVariables() {
    if (topLevelVariables == null) {
      return TopLevelVariableElementImpl.EMPTY_ARRAY;
    }
    TopLevelVariableElement[] result = topLevelVariables.toArray(new TopLevelVariableElement[topLevelVariables.size()]);
    topLevelVariables = null;
    return result;
  }

  public FunctionTypeAliasElement[] getTypeAliases() {
    if (typeAliases == null) {
      return FunctionTypeAliasElementImpl.EMPTY_ARRAY;
    }
    FunctionTypeAliasElement[] result = typeAliases.toArray(new FunctionTypeAliasElement[typeAliases.size()]);
    typeAliases = null;
    return result;
  }

  public TypeParameterElement[] getTypeParameters() {
    if (typeParameters == null) {
      return TypeParameterElementImpl.EMPTY_ARRAY;
    }
    TypeParameterElement[] result = typeParameters.toArray(new TypeParameterElement[typeParameters.size()]);
    typeParameters = null;
    return result;
  }

  public ClassElement[] getTypes() {
    if (types == null) {
      return ClassElementImpl.EMPTY_ARRAY;
    }
    ClassElement[] result = types.toArray(new ClassElement[types.size()]);
    types = null;
    return result;
  }

  public void validate() {
    StringBuilder builder = new StringBuilder();
    if (accessors != null) {
      builder.append(accessors.size());
      builder.append(" accessors");
    }
    if (constructors != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(constructors.size());
      builder.append(" constructors");
    }
    if (fields != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(fields.size());
      builder.append(" fields");
    }
    if (functions != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(functions.size());
      builder.append(" functions");
    }
    if (labels != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(labels.size());
      builder.append(" labels");
    }
    if (localVariables != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(localVariables.size());
      builder.append(" local variables");
    }
    if (methods != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(methods.size());
      builder.append(" methods");
    }
    if (parameters != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(parameters.size());
      builder.append(" parameters");
    }
    if (topLevelVariables != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(topLevelVariables.size());
      builder.append(" top-level variables");
    }
    if (types != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(types.size());
      builder.append(" types");
    }
    if (typeAliases != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(typeAliases.size());
      builder.append(" type aliases");
    }
    if (typeParameters != null) {
      if (builder.length() > 0) {
        builder.append("; ");
      }
      builder.append(typeParameters.size());
      builder.append(" type parameters");
    }
    if (builder.length() > 0) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Failed to capture elements: " + builder.toString());
    }
  }
}
