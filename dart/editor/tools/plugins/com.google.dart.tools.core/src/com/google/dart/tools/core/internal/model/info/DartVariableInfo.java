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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>DartVariableInfo</code> maintain the cached data shared by all equal
 * variables.
 */
public class DartVariableInfo extends DeclarationElementInfo {
  /**
   * The name of the declared type of this variable, or <code>null</code> if this variable does not
   * have a declared type. In the case where the type is a function type, this method will return a
   * string that contains only the types of the function's parameters and not the parameter names.
   */
  private char[] typeName;

  /**
   * The name of the declared type of this variable, or <code>null</code> if this variable does not
   * have a declared type. In the case where the type is a function type, this method will return a
   * string that contains both the types and names of the function's parameters.
   */
  private char[] fullTypeName;

  /**
   * A flag indicating whether this variable is a parameter.
   */
  private boolean isParameter;

  private int visibleStart;
  private int visibleEnd;

  /**
   * Return the name of the declared type of this variable, or <code>null</code> if this variable
   * does not have a declared type. In the case where the type is a function type, this method will
   * return a string that contains both the types and names of the function's parameters.
   * 
   * @return the name of the declared type of this variable
   */
  public char[] getFullTypeName() {
    return fullTypeName;
  }

  /**
   * Return the name of the declared type of this variable, or <code>null</code> if this variable
   * does not have a declared type. In the case where the type is a function type, this method will
   * return a string that contains only the types of the function's parameters and not the parameter
   * names.
   * 
   * @return the name of the declared type of this variable
   */
  public char[] getTypeName() {
    return typeName;
  }

  /**
   * @return the {@link SourceRange} in which this variable is visible.
   */
  public SourceRange getVisibleRange() {
    return new SourceRangeImpl(visibleStart, visibleEnd - visibleStart + 1);
  }

  /**
   * Return <code>true</code> if this variable is a parameter.
   * 
   * @return <code>true</code> if this variable is a parameter
   */
  public boolean isParameter() {
    return isParameter;
  }

  /**
   * Set the full name of the declared type of this variable to the given name.
   * 
   * @return the full name of the declared type of this variable
   */
  public void setFullTypeName(char[] name) {
    fullTypeName = name;
  }

  /**
   * Set whether this variable is a parameter to match the given value.
   * 
   * @param parameter <code>true</code> if this variable is a parameter
   */
  public void setParameter(boolean parameter) {
    isParameter = parameter;
  }

  /**
   * Set the name of the declared type of this variable to the given name
   * 
   * @param name the new name of the declared type of this variable
   */
  public void setTypeName(char[] name) {
    typeName = name;
  }

  /**
   * Sets the end of source range in which this variable is visible.
   */
  public void setVisibleEnd(int visibleEnd) {
    this.visibleEnd = visibleEnd;
  }

  /**
   * Sets the start of source range in which this variable is visible.
   */
  public void setVisibleStart(int visibleStart) {
    this.visibleStart = visibleStart;
  }
}
