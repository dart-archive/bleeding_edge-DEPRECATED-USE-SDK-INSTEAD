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
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PrefixElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.TopLevelVariableElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.libraryIdentifier;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.util.LinkedHashMap;

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
    ClassElementImpl element = new ClassElementImpl(identifier(typeName));
    element.setSupertype(superclassType);
    InterfaceTypeImpl type = new InterfaceTypeImpl(element);
    element.setType(type);

    int count = parameterNames.length;
    if (count > 0) {
      TypeVariableElementImpl[] typeVariables = new TypeVariableElementImpl[count];
      TypeVariableTypeImpl[] typeArguments = new TypeVariableTypeImpl[count];
      for (int i = 0; i < count; i++) {
        TypeVariableElementImpl variable = new TypeVariableElementImpl(
            identifier(parameterNames[i]));
        typeVariables[i] = variable;
        typeArguments[i] = new TypeVariableTypeImpl(variable);
        variable.setType(typeArguments[i]);
      }
      element.setTypeVariables(typeVariables);
      type.setTypeArguments(typeArguments);
    }

    return element;
  }

  public static ClassElementImpl classElement(String typeName, String... parameterNames) {
    return classElement(typeName, getObject().getType(), parameterNames);
  }

  public static ConstructorElementImpl constructorElement(String name) {
    return new ConstructorElementImpl(name == null ? null : identifier(name));
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
    FieldElementImpl field = new FieldElementImpl(identifier(name));
    field.setConst(isConst);
    field.setFinal(isFinal);
    field.setStatic(isStatic);
    field.setType(type);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
    getter.setGetter(true);
    getter.setSynthetic(true);
    field.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getterType.setReturnType(type);
    getter.setType(getterType);

    if (!isConst && !isFinal) {
      PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(field);
      setter.setSetter(true);
      setter.setSynthetic(true);
      field.setSetter(setter);

      FunctionTypeImpl setterType = new FunctionTypeImpl(getter);
      setterType.setNormalParameterTypes(new Type[] {type});
      setterType.setReturnType(VoidTypeImpl.getInstance());
      setter.setType(setterType);
    }

    return field;
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
    FunctionElementImpl functionElement = new FunctionElementImpl(identifier(functionName));
    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);
    functionElement.setType(functionType);
    // return type
    if (returnElement != null) {
      functionType.setReturnType(returnElement.getType());
    }
    // normal parameters
    int count = normalParameters == null ? 0 : normalParameters.length;
    if (count > 0) {
      InterfaceType[] normalParameterTypes = new InterfaceType[count];
      for (int i = 0; i < count; i++) {
        normalParameterTypes[i] = normalParameters[i].getType();
      }
      functionType.setNormalParameterTypes(normalParameterTypes);
    }
    // optional parameters
    count = optionalParameters == null ? 0 : optionalParameters.length;
    if (count > 0) {
      InterfaceType[] optionalParameterTypes = new InterfaceType[count];
      for (int i = 0; i < count; i++) {
        optionalParameterTypes[i] = optionalParameters[i].getType();
      }
      functionType.setOptionalParameterTypes(optionalParameterTypes);
    }
    return functionElement;
  }

  public static FunctionElementImpl functionElement(String functionName,
      ClassElement returnElement, ClassElement[] normalParameters, String[] names,
      ClassElement[] namedParameters) {
    // We don't create parameter elements because we don't have parameter names for non-named parameters
    FunctionElementImpl functionElement = new FunctionElementImpl(identifier(functionName));
    FunctionTypeImpl functionType = new FunctionTypeImpl(functionElement);
    functionElement.setType(functionType);
    // return type
    if (returnElement != null) {
      functionType.setReturnType(returnElement.getType());
    }
    // normal parameters
    int count = normalParameters == null ? 0 : normalParameters.length;
    if (count > 0) {
      InterfaceType[] normalParameterTypes = new InterfaceType[count];
      for (int i = 0; i < count; i++) {
        normalParameterTypes[i] = normalParameters[i].getType();
      }
      functionType.setNormalParameterTypes(normalParameterTypes);
    }
    // named parameters
    if (names != null && names.length > 0 && names.length == namedParameters.length) {
      LinkedHashMap<String, Type> map = new LinkedHashMap<String, Type>();
      for (int i = 0; i < names.length; i++) {
        map.put(names[i], namedParameters[i].getType());
      }
      functionType.setNamedParameterTypes(map);
    } else if (names != null) {
      throw new IllegalStateException(
          "The passed String[] and ClassElement[] arrays had different lengths.");
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

  public static ClassElementImpl getObject() {
    if (objectElement == null) {
      objectElement = classElement("Object", (InterfaceType) null);
    }
    return objectElement;
  }

  public static PropertyAccessorElementImpl getterElement(String name, boolean isStatic, Type type) {
    FieldElementImpl field = new FieldElementImpl(identifier(name));
    field.setStatic(isStatic);
    field.setSynthetic(true);
    field.setType(type);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
    getter.setGetter(true);
    field.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getterType.setReturnType(type);
    getter.setType(getterType);

    return getter;
  }

  public static ImportElementImpl importFor(LibraryElement importedLibrary, PrefixElement prefix,
      NamespaceCombinator... combinators) {
    ImportElementImpl spec = new ImportElementImpl();
    spec.setImportedLibrary(importedLibrary);
    spec.setPrefix(prefix);
    spec.setCombinators(combinators);
    return spec;
  }

  public static LibraryElementImpl library(AnalysisContext context, String libraryName) {
    String fileName = libraryName + ".dart";
    FileBasedSource source = new FileBasedSource(
        context.getSourceFactory().getContentCache(),
        createFile(fileName));
    CompilationUnitElementImpl unit = new CompilationUnitElementImpl(fileName);
    unit.setSource(source);
    LibraryElementImpl library = new LibraryElementImpl(context, libraryIdentifier(libraryName));
    library.setDefiningCompilationUnit(unit);
    return library;
  }

  public static LocalVariableElementImpl localVariableElement(Identifier name) {
    return new LocalVariableElementImpl(name);
  }

  public static LocalVariableElementImpl localVariableElement(String name) {
    return new LocalVariableElementImpl(identifier(name));
  }

  public static MethodElementImpl methodElement(String methodName, Type returnType,
      Type... argumentTypes) {
    MethodElementImpl method = new MethodElementImpl(identifier(methodName));

    int count = argumentTypes.length;
    ParameterElement[] parameters = new ParameterElement[count];
    for (int i = 0; i < count; i++) {
      ParameterElementImpl parameter = new ParameterElementImpl(identifier("a" + i));
      parameter.setType(argumentTypes[i]);
      parameter.setParameterKind(ParameterKind.REQUIRED);
      parameters[i] = parameter;
    }
    method.setParameters(parameters);

    FunctionTypeImpl methodType = new FunctionTypeImpl(method);
    methodType.setNormalParameterTypes(argumentTypes);
    methodType.setReturnType(returnType);
    method.setType(methodType);
    return method;
  }

  public static ParameterElementImpl namedParameter(String name) {
    ParameterElementImpl parameter = new ParameterElementImpl(identifier(name));
    parameter.setParameterKind(ParameterKind.NAMED);
    return parameter;
  }

  public static ParameterElementImpl positionalParameter(String name) {
    ParameterElementImpl parameter = new ParameterElementImpl(identifier(name));
    parameter.setParameterKind(ParameterKind.POSITIONAL);
    return parameter;
  }

  public static PrefixElementImpl prefix(String name) {
    return new PrefixElementImpl(identifier(name));
  }

  public static ParameterElementImpl requiredParameter(String name) {
    ParameterElementImpl parameter = new ParameterElementImpl(identifier(name));
    parameter.setParameterKind(ParameterKind.REQUIRED);
    return parameter;
  }

  public static PropertyAccessorElementImpl setterElement(String name, boolean isStatic, Type type) {
    FieldElementImpl field = new FieldElementImpl(identifier(name));
    field.setStatic(isStatic);
    field.setSynthetic(true);
    field.setType(type);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
    getter.setGetter(true);
    field.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getterType.setReturnType(type);
    getter.setType(getterType);

    PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(field);
    setter.setSetter(true);
    setter.setSynthetic(true);
    field.setSetter(setter);

    FunctionTypeImpl setterType = new FunctionTypeImpl(getter);
    setterType.setNormalParameterTypes(new Type[] {type});
    setterType.setReturnType(VoidTypeImpl.getInstance());
    setter.setType(setterType);

    return setter;
  }

  public static TopLevelVariableElementImpl topLevelVariableElement(Identifier name) {
    return new TopLevelVariableElementImpl(name);
  }

  public static TopLevelVariableElementImpl topLevelVariableElement(String name) {
    return new TopLevelVariableElementImpl(name);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ElementFactory() {
  }
}
