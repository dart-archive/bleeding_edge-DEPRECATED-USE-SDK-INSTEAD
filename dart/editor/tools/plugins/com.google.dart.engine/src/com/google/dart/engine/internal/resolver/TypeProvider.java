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

import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * The interface {@code TypeProvider} defines the behavior of objects that provide access to types
 * defined by the language.
 * 
 * @coverage dart.engine.resolver
 */
public interface TypeProvider {
  /**
   * Return the type representing the built-in type 'bool'.
   * 
   * @return the type representing the built-in type 'bool'
   */
  public InterfaceType getBoolType();

  /**
   * Return the type representing the type 'bottom'.
   * 
   * @return the type representing the type 'bottom'
   */
  public Type getBottomType();

  /**
   * Return the type representing the built-in type 'Deprecated'.
   * 
   * @return the type representing the built-in type 'Deprecated'
   */
  public InterfaceType getDeprecatedType();

  /**
   * Return the type representing the built-in type 'double'.
   * 
   * @return the type representing the built-in type 'double'
   */
  public InterfaceType getDoubleType();

  /**
   * Return the type representing the built-in type 'dynamic'.
   * 
   * @return the type representing the built-in type 'dynamic'
   */
  public Type getDynamicType();

  /**
   * Return the type representing the built-in type 'Function'.
   * 
   * @return the type representing the built-in type 'Function'
   */
  public InterfaceType getFunctionType();

  /**
   * Return the type representing the built-in type 'int'.
   * 
   * @return the type representing the built-in type 'int'
   */
  public InterfaceType getIntType();

  /**
   * Return the type representing the built-in type 'List'.
   * 
   * @return the type representing the built-in type 'List'
   */
  public InterfaceType getListType();

  /**
   * Return the type representing the built-in type 'Map'.
   * 
   * @return the type representing the built-in type 'Map'
   */
  public InterfaceType getMapType();

  /**
   * Return the type representing the built-in type 'Null'.
   * 
   * @return the type representing the built-in type 'null'
   */
  public InterfaceType getNullType();

  /**
   * Return the type representing the built-in type 'num'.
   * 
   * @return the type representing the built-in type 'num'
   */
  public InterfaceType getNumType();

  /**
   * Return the type representing the built-in type 'Object'.
   * 
   * @return the type representing the built-in type 'Object'
   */
  public InterfaceType getObjectType();

  /**
   * Return the type representing the built-in type 'StackTrace'.
   * 
   * @return the type representing the built-in type 'StackTrace'
   */
  public InterfaceType getStackTraceType();

  /**
   * Return the type representing the built-in type 'String'.
   * 
   * @return the type representing the built-in type 'String'
   */
  public InterfaceType getStringType();

  /**
   * Return the type representing the built-in type 'Symbol'.
   * 
   * @return the type representing the built-in type 'Symbol'
   */
  public InterfaceType getSymbolType();

  /**
   * Return the type representing the built-in type 'Type'.
   * 
   * @return the type representing the built-in type 'Type'
   */
  public InterfaceType getTypeType();
}
