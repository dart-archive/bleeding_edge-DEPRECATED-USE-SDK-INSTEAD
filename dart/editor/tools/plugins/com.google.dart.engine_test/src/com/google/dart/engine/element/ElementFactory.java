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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.libraryIdentifier;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

/**
 * The class {@code ElementFactory} defines utility methods used to create elements for testing
 * purposes.
 */
public final class ElementFactory {
  /**
   * The element representing the class 'Object'.
   */
  private static ClassElement objectElement;

  public static ClassElement classElement(String typeName, InterfaceType superclassType,
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

  public static ClassElement classElement(String typeName, String... parameterNames) {
    return classElement(typeName, getObject().getType(), parameterNames);
  }

  public static ConstructorElement constructorElement(String name) {
    return new ConstructorElementImpl(name == null ? null : identifier(name));
  }

  public static ClassElement getObject() {
    if (objectElement == null) {
      objectElement = classElement("Object", (InterfaceType) null);
    }
    return objectElement;
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
    FileBasedSource source = new FileBasedSource(null, createFile(fileName));
    CompilationUnitElementImpl unit = new CompilationUnitElementImpl(fileName);
    unit.setSource(source);
    LibraryElementImpl library = new LibraryElementImpl(context, libraryIdentifier(libraryName));
    library.setDefiningCompilationUnit(unit);
    return library;
  }

  public static MethodElement methodElement(String methodName, Type returnType,
      Type... argumentTypes) {
    MethodElementImpl method = new MethodElementImpl(identifier(methodName));
    FunctionTypeImpl methodType = new FunctionTypeImpl(method);
    methodType.setNormalParameterTypes(argumentTypes);
    methodType.setReturnType(returnType);
    method.setType(methodType);
    return method;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ElementFactory() {
  }
}
