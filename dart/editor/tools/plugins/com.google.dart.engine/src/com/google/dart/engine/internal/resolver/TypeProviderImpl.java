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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code TypeProviderImpl} provide access to types defined by the language
 * by looking for those types in the element model for the core library.
 * 
 * @coverage dart.engine.resolver
 */
public class TypeProviderImpl implements TypeProvider {
  /**
   * The type representing the built-in type 'bool'.
   */
  private InterfaceType boolType;

  /**
   * The type representing the type 'bottom'.
   */
  private Type bottomType;

  /**
   * The type representing the built-in type 'double'.
   */
  private InterfaceType doubleType;

  /**
   * The type representing the built-in type 'Deprecated'.
   */
  private InterfaceType deprecatedType;

  /**
   * The type representing the built-in type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The type representing the built-in type 'Function'.
   */
  private InterfaceType functionType;

  /**
   * The type representing the built-in type 'int'.
   */
  private InterfaceType intType;

  /**
   * The type representing the built-in type 'List'.
   */
  private InterfaceType listType;

  /**
   * The type representing the built-in type 'Map'.
   */
  private InterfaceType mapType;

  /**
   * The type representing the type 'Null'.
   */
  private InterfaceType nullType;

  /**
   * The type representing the built-in type 'num'.
   */
  private InterfaceType numType;

  /**
   * The type representing the built-in type 'Object'.
   */
  private InterfaceType objectType;

  /**
   * The type representing the built-in type 'StackTrace'.
   */
  private InterfaceType stackTraceType;

  /**
   * The type representing the built-in type 'String'.
   */
  private InterfaceType stringType;

  /**
   * The type representing the built-in type 'Symbol'.
   */
  private InterfaceType symbolType;

  /**
   * The type representing the built-in type 'Type'.
   */
  private InterfaceType typeType;

  /**
   * Initialize a newly created type provider to provide the types defined in the given library.
   * 
   * @param coreLibrary the element representing the core library (dart:core).
   */
  public TypeProviderImpl(LibraryElement coreLibrary) {
    initializeFrom(coreLibrary);
  }

  @Override
  public InterfaceType getBoolType() {
    return boolType;
  }

  @Override
  public Type getBottomType() {
    return bottomType;
  }

  @Override
  public InterfaceType getDeprecatedType() {
    return deprecatedType;
  }

  @Override
  public InterfaceType getDoubleType() {
    return doubleType;
  }

  @Override
  public Type getDynamicType() {
    return dynamicType;
  }

  @Override
  public InterfaceType getFunctionType() {
    return functionType;
  }

  @Override
  public InterfaceType getIntType() {
    return intType;
  }

  @Override
  public InterfaceType getListType() {
    return listType;
  }

  @Override
  public InterfaceType getMapType() {
    return mapType;
  }

  @Override
  public InterfaceType getNullType() {
    return nullType;
  }

  @Override
  public InterfaceType getNumType() {
    return numType;
  }

  @Override
  public InterfaceType getObjectType() {
    return objectType;
  }

  @Override
  public InterfaceType getStackTraceType() {
    return stackTraceType;
  }

  @Override
  public InterfaceType getStringType() {
    return stringType;
  }

  @Override
  public InterfaceType getSymbolType() {
    return symbolType;
  }

  @Override
  public InterfaceType getTypeType() {
    return typeType;
  }

  /**
   * Return the type with the given name from the given namespace, or {@code null} if there is no
   * class with the given name.
   * 
   * @param namespace the namespace in which to search for the given name
   * @param typeName the name of the type being searched for
   * @return the type that was found
   */
  private InterfaceType getType(Namespace namespace, String typeName) {
    Element element = namespace.get(typeName);
    if (element == null) {
      AnalysisEngine.getInstance().getLogger().logInformation("No definition of type " + typeName);
      return null;
    }
    return ((ClassElement) element).getType();
  }

  /**
   * Initialize the types provided by this type provider from the given library.
   * 
   * @param library the library containing the definitions of the core types
   */
  private void initializeFrom(LibraryElement library) {
    Namespace namespace = new NamespaceBuilder().createPublicNamespaceForLibrary(library);
    boolType = getType(namespace, "bool");
    bottomType = BottomTypeImpl.getInstance();
    deprecatedType = getType(namespace, "Deprecated");
    doubleType = getType(namespace, "double");
    dynamicType = DynamicTypeImpl.getInstance();
    functionType = getType(namespace, "Function");
    intType = getType(namespace, "int");
    listType = getType(namespace, "List");
    mapType = getType(namespace, "Map");
    nullType = getType(namespace, "Null");
    numType = getType(namespace, "num");
    objectType = getType(namespace, "Object");
    stackTraceType = getType(namespace, "StackTrace");
    stringType = getType(namespace, "String");
    symbolType = getType(namespace, "Symbol");
    typeType = getType(namespace, "Type");
  }
}
