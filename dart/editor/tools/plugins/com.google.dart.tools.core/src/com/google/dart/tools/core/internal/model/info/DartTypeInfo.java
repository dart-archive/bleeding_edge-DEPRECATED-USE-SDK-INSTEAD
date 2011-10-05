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
 * Instances of the class <code>DartTypeInfo</code> maintain the cached data shared by all equal
 * types.
 */
public class DartTypeInfo extends DeclarationElementInfo {
  /**
   * An array containing the names of the interfaces that are implemented by this type.
   */
  private char[][] interfaceNames;

  /**
   * A flag indicating whether this object represents an interface.
   */
  private boolean isInterface;

  /**
   * The name of the superclass of this type, or <code>null</code> if this type does not have an
   * explicitly declared superclass.
   */
  private char[] superclassName;

  /**
   * Return an array containing the names of the interfaces that are implemented by this type.
   * 
   * @return an array containing the names of the interfaces that are implemented by this type
   */
  public char[][] getInterfaceNames() {
    return interfaceNames;
  }

  /**
   * Return the name of the superclass of this type, or <code>null</code> if this type does not have
   * an explicitly declared superclass.
   * 
   * @return the name of the superclass of this type
   */
  public char[] getSuperclassName() {
    return superclassName;
  }

  /**
   * Return <code>true</code> if this type is an interface.
   * 
   * @return <code>true</code> if this type is an interface
   */
  public boolean isInterface() {
    return isInterface;
  }

  /**
   * Set the names of the interfaces that are implemented by this type to those in the given array.
   * 
   * @param names the names of the interfaces that are implemented by this type
   */
  public void setInterfaceNames(char[][] names) {
    interfaceNames = names;
  }

  /**
   * Set whether this type is an interface.
   * 
   * @param isInterface <code>true</code> if this type is an interface
   */
  public void setIsInterface(boolean isInterface) {
    this.isInterface = isInterface;
  }

  /**
   * Set the name of the superclass of this type to the given name.
   * 
   * @param name the new name of the superclass of this type
   */
  public void setSuperclassName(char[] name) {
    superclassName = name;
  }
}
