/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.element;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.ExportElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.TopLevelVariableElementImpl;
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.source.NonExistingSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;

/**
 * The class {@code ElementFactory} defines utility methods used to create elements for testing
 * purposes. The elements that are created are complete in the sense that as much of the element
 * model as can be created, given the provided information, has been created.
 */
public final class ElementFactory {
  /**
   * The element representing the class 'Object'.
   */
  private static ClassElementImpl objectElement;

  public static ClassElementImpl classElement(String typeName, InterfaceType superclassType,
      String... parameterNames) {
    ClassElementImpl element = new ClassElementImpl(typeName, 0);
    element.setSupertype(superclassType);
    InterfaceTypeImpl type = new InterfaceTypeImpl(element);
    element.setType(type);

    int count = parameterNames.length;
    if (count > 0) {
      TypeParameterElementImpl[] typeParameters = new TypeParameterElementImpl[count];
      TypeParameterTypeImpl[] typeParameterTypes = new TypeParameterTypeImpl[count];
      for (int i = 0; i < count; i++) {
        TypeParameterElementImpl typeParameter = new TypeParameterElementImpl(parameterNames[i], 0);
        typeParameters[i] = typeParameter;
        typeParameterTypes[i] = new TypeParameterTypeImpl(typeParameter);
        typeParameter.setType(typeParameterTypes[i]);
      }
      element.setTypeParameters(typeParameters);
      type.setTypeArguments(typeParameterTypes);
    }

    return element;
  }

  public static ClassElementImpl classElement(String typeName, String... parameterNames) {
    return classElement(typeName, getObjectType(), parameterNames);
  }

  public static CompilationUnitElementImpl compilationUnit(String fileName) {
    Source source = new NonExistingSource(fileName, UriKind.FILE_URI);
    CompilationUnitElementImpl unit = new CompilationUnitElementImpl(fileName);
    unit.setSource(source);
    return unit;
  }

  public static ConstructorElementImpl constructorElement(ClassElement definingClass, String name,
      boolean isConst, Type... argumentTypes) {
    Type type = definingClass.getType();
    ConstructorElementImpl constructor = name == null ? new ConstructorElementImpl("", -1)
        : new ConstructorElementImpl(name, 0);
    constructor.setConst(isConst);
    int count = argumentTypes.length;
    ParameterElement[] parameters = new ParameterElement[count];
    for (int i = 0; i < count; i++) {
      ParameterElementImpl parameter = new ParameterElementImpl("a" + i, i);
      parameter.setType(argumentTypes[i]);
      parameter.setParameterKind(ParameterKind.REQUIRED);
      parameters[i] = parameter;
    }
    constructor.setParameters(parameters);
    constructor.setReturnType(type);

    FunctionTypeImpl constructorType = new FunctionTypeImpl(constructor);
    constructor.setType(constructorType);

    return constructor;
  }

  public static ConstructorElementImpl constructorElement(ClassElement definingClass, String name,
      Type... argumentTypes) {
    return constructorElement(definingClass, name, false, argumentTypes);
  }

  public static ExportElementImpl exportFor(LibraryElement exportedLibrary,
      NamespaceCombinator... combinators) {
    ExportElementImpl spec = new ExportElementImpl();
    spec.setExportedLibrary(exportedLibrary);
    spec.setCombinators(combinators);
    return spec;
  }

  public static FieldElementImpl fieldElement(String name, boolean isStatic, boolean isFinal,
      boolean isConst, Type type) {
    FieldElementImpl field = new FieldElementImpl(name, 0);
    field.setConst(isConst);
    field.setFinal(isFinal);
    field.setStatic(isStatic);
    field.setType(type);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
    getter.setGetter(true);
    getter.setStatic(isStatic);
    getter.setSynthetic(true);
    getter.setVariable(field);
    getter.setReturnType(type);
    field.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getter.setType(getterType);

    if (!isConst && !isFinal) {
      PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(field);
      setter.setSetter(true);
      setter.setStatic(isStatic);
      setter.setSynthetic(true);
      setter.setVariable(field);
      setter.setParameters(new ParameterElement[] {requiredParameter("_" + name, type)});
      setter.setReturnType(VoidTypeImpl.getInstance());
      setter.setType(new FunctionTypeImpl(setter));
      field.setSetter(setter);
    }

    return field;
  }

  public static FieldFormalParameterElementImpl fieldFormalParameter(Identifier name) {
    return new FieldFormalParameterElementImpl(name);
  }

  public static FunctionElementImpl functionElement(String functionName) {
    return functionElement(functionName, null, null, null, null);
  }

  public static FunctionElementImpl functionElement(String functionName, ClassElement returnElement) {
    return functionElement(functionName, returnElement, null, null);
  }

  public static FunctionElementImpl functionElement(String functionName,
      ClassElement returnElement, ClassElement[] normalParameters, ClassElement[] optionalParameters) {
    // We don't create parameter elements because we don't have parameter names
    FunctionElementImpl functionElement = new FunctionElementImpl(functionName, 0);
    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);
    functionElement.setType(functionType);
    // return type
    if (returnElement == null) {
      functionElement.setReturnType(VoidTypeImpl.getInstance());
    } else {
      functionElement.setReturnType(returnElement.getType());
    }
    // parameters
    int normalCount = normalParameters == null ? 0 : normalParameters.length;
    int optionalCount = optionalParameters == null ? 0 : optionalParameters.length;
    int totalCount = normalCount + optionalCount;
    ParameterElement[] parameters = new ParameterElement[totalCount];
    for (int i = 0; i < totalCount; i++) {
      ParameterElementImpl parameter = new ParameterElementImpl("a" + i, i);
      if (i < normalCount) {
        parameter.setType(normalParameters[i].getType());
        parameter.setParameterKind(ParameterKind.REQUIRED);
      } else {
        parameter.setType(optionalParameters[i - normalCount].getType());
        parameter.setParameterKind(ParameterKind.POSITIONAL);
      }
      parameters[i] = parameter;
    }
    functionElement.setParameters(parameters);
    // done
    return functionElement;
  }

  public static FunctionElementImpl functionElement(String functionName,
      ClassElement returnElement, ClassElement[] normalParameters, String[] names,
      ClassElement[] namedParameters) {
    FunctionElementImpl functionElement = new FunctionElementImpl(functionName, 0);
    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);
    functionElement.setType(functionType);
    // parameters
    int normalCount = normalParameters == null ? 0 : normalParameters.length;
    int nameCount = names == null ? 0 : names.length;
    int typeCount = namedParameters == null ? 0 : namedParameters.length;
    if (names != null && nameCount != typeCount) {
      throw new IllegalStateException(
          "The passed String[] and ClassElement[] arrays had different lengths.");
    }
    int totalCount = normalCount + nameCount;
    ParameterElement[] parameters = new ParameterElement[totalCount];
    for (int i = 0; i < totalCount; i++) {
      if (i < normalCount) {
        ParameterElementImpl parameter = new ParameterElementImpl("a" + i, i);
        parameter.setType(normalParameters[i].getType());
        parameter.setParameterKind(ParameterKind.REQUIRED);
        parameters[i] = parameter;
      } else {
        ParameterElementImpl parameter = new ParameterElementImpl(names[i - normalCount], i);
        parameter.setType(namedParameters[i - normalCount].getType());
        parameter.setParameterKind(ParameterKind.NAMED);
        parameters[i] = parameter;
      }
    }
    functionElement.setParameters(parameters);
    // return type
    if (returnElement == null) {
      functionElement.setReturnType(VoidTypeImpl.getInstance());
    } else {
      functionElement.setReturnType(returnElement.getType());
    }
    return functionElement;
  }

  public static FunctionElementImpl functionElement(String functionName,
      ClassElement[] normalParameters) {
    return functionElement(functionName, null, normalParameters, null);
  }

  public static FunctionElementImpl functionElement(String functionName,
      ClassElement[] normalParameters, ClassElement[] optionalParameters) {
    return functionElement(functionName, null, normalParameters, optionalParameters);
  }

  public static FunctionElementImpl functionElement(String functionName,
      ClassElement[] normalParameters, String[] names, ClassElement[] namedParameters) {
    return functionElement(functionName, null, normalParameters, names, namedParameters);
  }

  public static FunctionElementImpl functionElementWithParameters(String functionName,
      Type returnType, ParameterElement... parameters) {
    FunctionElementImpl functionElement = new FunctionElementImpl(functionName, 0);
    functionElement.setReturnType(returnType == null ? VoidTypeImpl.getInstance() : returnType);
    functionElement.setParameters(parameters);

    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);
    functionElement.setType(functionType);
    return functionElement;
  }

  public static ClassElementImpl getObject() {
    if (objectElement == null) {
      objectElement = classElement("Object", (InterfaceType) null);
    }
    return objectElement;
  }

  public static InterfaceType getObjectType() {
    return getObject().getType();
  }

  public static PropertyAccessorElementImpl getterElement(String name, boolean isStatic, Type type) {
    FieldElementImpl field = new FieldElementImpl(name, -1);
    field.setStatic(isStatic);
    field.setSynthetic(true);
    field.setType(type);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
    getter.setGetter(true);
    getter.setStatic(isStatic);
    getter.setVariable(field);
    getter.setReturnType(type);
    field.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getter.setType(getterType);

    return getter;
  }

  public static HtmlElementImpl htmlUnit(AnalysisContext context, String fileName) {
    Source source = new NonExistingSource(fileName, UriKind.FILE_URI);
    HtmlElementImpl unit = new HtmlElementImpl(context, fileName);
    unit.setSource(source);
    return unit;
  }

  public static ImportElementImpl importFor(LibraryElement importedLibrary, PrefixElement prefix,
      NamespaceCombinator... combinators) {
    ImportElementImpl spec = new ImportElementImpl(0);
    spec.setImportedLibrary(importedLibrary);
    spec.setPrefix(prefix);
    spec.setCombinators(combinators);
    return spec;
  }

  public static LibraryElementImpl library(AnalysisContext context, String libraryName) {
    String fileName = "/" + libraryName + ".dart";
    CompilationUnitElementImpl unit = compilationUnit(fileName);
    LibraryElementImpl library = new LibraryElementImpl(context, libraryName, 0);
    library.setDefiningCompilationUnit(unit);
    return library;
  }

  public static LocalVariableElementImpl localVariableElement(Identifier name) {
    return new LocalVariableElementImpl(name);
  }

  public static LocalVariableElementImpl localVariableElement(String name) {
    return new LocalVariableElementImpl(name, 0);
  }

  public static MethodElementImpl methodElement(String methodName, Type returnType,
      Type... argumentTypes) {
    MethodElementImpl method = new MethodElementImpl(methodName, 0);

    int count = argumentTypes.length;
    ParameterElement[] parameters = new ParameterElement[count];
    for (int i = 0; i < count; i++) {
      ParameterElementImpl parameter = new ParameterElementImpl("a" + i, i);
      parameter.setType(argumentTypes[i]);
      parameter.setParameterKind(ParameterKind.REQUIRED);
      parameters[i] = parameter;
    }
    method.setParameters(parameters);
    method.setReturnType(returnType);

    FunctionTypeImpl methodType = new FunctionTypeImpl(method);
    method.setType(methodType);
    return method;
  }

  public static MethodElementImpl methodElementWithParameters(String methodName,
      Type[] typeArguments, Type returnType, ParameterElement... parameters) {
    MethodElementImpl method = new MethodElementImpl(methodName, 0);
    method.setParameters(parameters);
    method.setReturnType(returnType);

    FunctionTypeImpl methodType = new FunctionTypeImpl(method);
    methodType.setTypeArguments(typeArguments);
    method.setType(methodType);
    return method;
  }

  public static ParameterElementImpl namedParameter(String name) {
    ParameterElementImpl parameter = new ParameterElementImpl(name, 0);
    parameter.setParameterKind(ParameterKind.NAMED);
    return parameter;
  }

  public static ParameterElementImpl namedParameter(String name, Type type) {
    ParameterElementImpl parameter = new ParameterElementImpl(name, 0);
    parameter.setParameterKind(ParameterKind.NAMED);
    parameter.setType(type);
    return parameter;
  }

  public static ParameterElementImpl positionalParameter(String name) {
    ParameterElementImpl parameter = new ParameterElementImpl(name, 0);
    parameter.setParameterKind(ParameterKind.POSITIONAL);
    return parameter;
  }

  public static ParameterElementImpl positionalParameter(String name, Type type) {
    ParameterElementImpl parameter = new ParameterElementImpl(name, 0);
    parameter.setParameterKind(ParameterKind.POSITIONAL);
    parameter.setType(type);
    return parameter;
  }

  public static PrefixElementImpl prefix(String name) {
    return new PrefixElementImpl(name, 0);
  }

  public static ParameterElementImpl requiredParameter(String name) {
    ParameterElementImpl parameter = new ParameterElementImpl(name, 0);
    parameter.setParameterKind(ParameterKind.REQUIRED);
    return parameter;
  }

  public static ParameterElementImpl requiredParameter(String name, Type type) {
    ParameterElementImpl parameter = new ParameterElementImpl(name, 0);
    parameter.setParameterKind(ParameterKind.REQUIRED);
    parameter.setType(type);
    return parameter;
  }

  public static PropertyAccessorElementImpl setterElement(String name, boolean isStatic, Type type) {
    FieldElementImpl field = new FieldElementImpl(name, -1);
    field.setStatic(isStatic);
    field.setSynthetic(true);
    field.setType(type);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
    getter.setGetter(true);
    getter.setStatic(isStatic);
    getter.setVariable(field);
    getter.setReturnType(type);
    field.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getter.setType(getterType);

    ParameterElementImpl parameter = requiredParameter("a", type);
    PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(field);
    setter.setSetter(true);
    setter.setStatic(isStatic);
    setter.setSynthetic(true);
    setter.setVariable(field);
    setter.setParameters(new ParameterElement[] {parameter});
    setter.setReturnType(VoidTypeImpl.getInstance());
    setter.setType(new FunctionTypeImpl(setter));
    field.setSetter(setter);

    return setter;
  }

  public static TopLevelVariableElementImpl topLevelVariableElement(Identifier name) {
    return new TopLevelVariableElementImpl(name);
  }

  public static TopLevelVariableElementImpl topLevelVariableElement(String name) {
    TopLevelVariableElementImpl element = new TopLevelVariableElementImpl(name, -1);
    element.setSynthetic(true);
    return element;
  }

  public static TopLevelVariableElementImpl topLevelVariableElement(String name, boolean isConst,
      boolean isFinal, Type type) {
    TopLevelVariableElementImpl variable = new TopLevelVariableElementImpl(name, -1);
    variable.setConst(isConst);
    variable.setFinal(isFinal);
    variable.setSynthetic(true);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(variable);
    getter.setGetter(true);
    getter.setStatic(true);
    getter.setSynthetic(true);
    getter.setVariable(variable);
    getter.setReturnType(type);
    variable.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getter.setType(getterType);

    if (!isFinal) {
      PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(variable);
      setter.setSetter(true);
      setter.setStatic(true);
      setter.setSynthetic(true);
      setter.setVariable(variable);
      setter.setParameters(new ParameterElement[] {requiredParameter("_" + name, type)});
      setter.setReturnType(VoidTypeImpl.getInstance());
      setter.setType(new FunctionTypeImpl(setter));
      variable.setSetter(setter);
    }

    return variable;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ElementFactory() {
  }
}
