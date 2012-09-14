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
 * Instances of the class <code>DartFunctionInfo</code> represent the information known about a Dart
 * function.
 */
public class DartFunctionInfo extends DeclarationElementInfo {
  /**
   * The name of the return type of this method, or <code>null</code> if this method is a
   * constructor or does not have a declared return type.
   */
  private char[] returnTypeName;

  private int optionalParametersOpeningGroupChar;
  private int optionalParametersClosingGroupChar;
  private int parametersCloseParen;
  private int visibleStart;
  private int visibleEnd;

  public int getOptionalParametersClosingGroupChar() {
    return optionalParametersClosingGroupChar;
  }

  public int getOptionalParametersOpeningGroupChar() {
    return optionalParametersOpeningGroupChar;
  }

  /**
   * @return the position of parameters close parenthesis.
   */
  public int getParametersCloseParen() {
    return parametersCloseParen;
  }

  /**
   * Return the name of the return type of this method, or <code>null</code> if this method is a
   * constructor or does not have a declared return type.
   * 
   * @return the name of the return type of this method
   */
  public char[] getReturnTypeName() {
    return returnTypeName;
  }

  /**
   * @return the {@link SourceRange} in which this function is visible.
   */
  public SourceRange getVisibleRange() {
    return new SourceRangeImpl(visibleStart, visibleEnd - visibleStart + 1);
  }

  public void setOptionalParametersClosingGroupChar(int parametersOptionalClose) {
    this.optionalParametersClosingGroupChar = parametersOptionalClose;
  }

  public void setOptionalParametersOpeningGroupChar(int parametersOptionalOpen) {
    this.optionalParametersOpeningGroupChar = parametersOptionalOpen;
  }

  public void setParametersCloseParen(int parametersCloseParen) {
    this.parametersCloseParen = parametersCloseParen;
  }

  /**
   * Set the name of the return type of this method to the given name.
   * 
   * @param name the new name of the return type of this method
   */
  public void setReturnTypeName(char[] name) {
    returnTypeName = name;
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
