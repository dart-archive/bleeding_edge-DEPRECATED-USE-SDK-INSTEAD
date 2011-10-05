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
 * Instances of the class <code>DartFieldInfo</code> maintain the cached data shared by all equal
 * fields.
 */
public class DartFieldInfo extends DeclarationElementInfo {
  /**
   * The name of the declared type of this field, or <code>null</code> if this field does not have a
   * declared type.
   */
  private char[] typeName;

  /**
   * Return the name of the declared type of this field, or <code>null</code> if this field does not
   * have a declared type.
   * 
   * @return the name of the declared type of this field
   */
  public char[] getTypeName() {
    return typeName;
  }

  /**
   * Set the name of the declared type of this field to the given name
   * 
   * @param name the new name of the declared type of this field
   */
  public void setTypeName(char[] name) {
    typeName = name;
  }
}
