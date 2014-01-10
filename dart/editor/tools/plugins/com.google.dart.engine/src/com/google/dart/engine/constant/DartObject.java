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
package com.google.dart.engine.constant;

import com.google.dart.engine.type.InterfaceType;

import java.math.BigInteger;

/**
 * The interface {@code DartObject} defines the behavior of objects that represent the state of a
 * Dart object.
 */
public interface DartObject {
  /**
   * Return the boolean value of this object, or {@code null} if either the value of this object is
   * not known or this object is not of type 'bool'.
   * 
   * @return the boolean value of this object
   */
  public Boolean getBoolValue();

  /**
   * Return the floating point value of this object, or {@code null} if either the value of this
   * object is not known or this object is not of type 'double'.
   * 
   * @return the floating point value of this object
   */
  public Double getDoubleValue();

  /**
   * Return the integer value of this object, or {@code null} if either the value of this object is
   * not known or this object is not of type 'int'.
   * 
   * @return the integer value of this object
   */
  public BigInteger getIntValue();

  /**
   * Return the string value of this object, or {@code null} if either the value of this object is
   * not known or this object is not of type 'String'.
   * 
   * @return the string value of this object
   */
  public String getStringValue();

  /**
   * Return the run-time type of this object.
   * 
   * @return the run-time type of this object
   */
  public InterfaceType getType();

  /**
   * Return this object's value if it can be represented exactly, or {@code null} if either the
   * value cannot be represented exactly or if the value is {@code null}. Clients should use
   * {@link #hasExactValue()} to distinguish between these two cases.
   * 
   * @return this object's value
   */
  public Object getValue();

  /**
   * Return {@code true} if this object's value can be represented exactly.
   * 
   * @return {@code true} if this object's value can be represented exactly
   */
  public boolean hasExactValue();

  /**
   * Return {@code true} if this object represents the value 'false'.
   * 
   * @return {@code true} if this object represents the value 'false'
   */
  public boolean isFalse();

  /**
   * Return {@code true} if this object represents the value 'null'.
   * 
   * @return {@code true} if this object represents the value 'null'
   */
  public boolean isNull();

  /**
   * Return {@code true} if this object represents the value 'true'.
   * 
   * @return {@code true} if this object represents the value 'true'
   */
  public boolean isTrue();
}
