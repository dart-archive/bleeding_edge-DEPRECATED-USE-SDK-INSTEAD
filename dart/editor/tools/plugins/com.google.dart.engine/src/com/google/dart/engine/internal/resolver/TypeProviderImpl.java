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
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.resolver.scope.Namespace;
import com.google.dart.engine.resolver.scope.NamespaceBuilder;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code TypeProviderImpl} provide access to types defined by the language
 * by looking for those types in the element model for the core library.
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
   * The type representing the built-in type 'dynamic'.
   */
  private Type dynamicType;

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

  /**
   * Return the type representing the built-in type 'bool'.
   * 
   * @return the type representing the built-in type 'bool'
   */
  @Override
  public InterfaceType getBoolType() {
    return boolType;
  }

  /**
   * Return the type representing the type 'bottom'.
   * 
   * @return the type representing the type 'bottom'
   */
  @Override
  public Type getBottomType() {
    return bottomType;
  }

  /**
   * Return the type representing the built-in type 'double'.
   * 
   * @return the type representing the built-in type 'double'
   */
  @Override
  public InterfaceType getDoubleType() {
    return doubleType;
  }

  /**
   * Return the type representing the built-in type 'dynamic'.
   * 
   * @return the type representing the built-in type 'dynamic'
   */
  @Override
  public Type getDynamicType() {
    return dynamicType;
  }

  /**
   * Return the type representing the built-in type 'int'.
   * 
   * @return the type representing the built-in type 'int'
   */
  @Override
  public InterfaceType getIntType() {
    return intType;
  }

  /**
   * Return the type representing the built-in type 'List'.
   * 
   * @return the type representing the built-in type 'List'
   */
  @Override
  public InterfaceType getListType() {
    return listType;
  }

  /**
   * Return the type representing the built-in type 'Map'.
   * 
   * @return the type representing the built-in type 'Map'
   */
  @Override
  public InterfaceType getMapType() {
    return mapType;
  }

  /**
   * Return the type representing the built-in type 'Object'.
   * 
   * @return the type representing the built-in type 'Object'
   */
  @Override
  public InterfaceType getObjectType() {
    return objectType;
  }

  /**
   * Return the type representing the built-in type 'StackTrace'.
   * 
   * @return the type representing the built-in type 'StackTrace'
   */
  @Override
  public InterfaceType getStackTraceType() {
    return stackTraceType;
  }

  /**
   * Return the type representing the built-in type 'String'.
   * 
   * @return the type representing the built-in type 'String'
   */
  @Override
  public InterfaceType getStringType() {
    return stringType;
  }

  /**
   * Return the type representing the built-in type 'Type'.
   * 
   * @return the type representing the built-in type 'Type'
   */
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
    Namespace namespace = new NamespaceBuilder().createPublicNamespace(library);
    boolType = getType(namespace, "bool");
    bottomType = BottomTypeImpl.getInstance();
    doubleType = getType(namespace, "double");
    dynamicType = DynamicTypeImpl.getInstance();
    intType = getType(namespace, "int");
    listType = getType(namespace, "List");
    mapType = getType(namespace, "Map");
    objectType = getType(namespace, "Object");
    stackTraceType = getType(namespace, "StackTrace");
    stringType = getType(namespace, "String");
    typeType = getType(namespace, "Type");
  }
}
