/*
 * Copyright (c) 2011, the Dart project authors.
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

/**
 * Instances of the class <code>DartFunctionInfo</code> represent the information known about a Dart
 * function.
 */
public class DartFunctionInfo extends SourceElementWithChildrenInfo {
  /**
   * The name of the return type of this method, or <code>null</code> if this method is a
   * constructor or does not have a declared return type.
   */
  private char[] returnTypeName;

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
   * Set the name of the return type of this method to the given name.
   * 
   * @param name the new name of the return type of this method
   */
  public void setReturnTypeName(char[] name) {
    returnTypeName = name;
  }
}
