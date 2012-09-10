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
 * Instances of the class <code>DartMethodInfo</code> maintain the cached data shared by all equal
 * methods.
 */
public class DartMethodInfo extends DeclarationElementInfo {
  private int parametersCloseParen;
  /**
   * A flag indicating whether this method is a constructor.
   */
  private boolean isConstructor;

  /**
   * A flag indicating whether this method is implicitly defined.
   */
  private boolean isImplicit;

  /**
   * The name of the return type of this method, or <code>null</code> if this method is a
   * constructor or does not have a declared return type.
   */
  private char[] returnTypeName;

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
   * Return <code>true</code> if this method is a constructor.
   * 
   * @return <code>true</code> if this method is a constructor
   */
  public boolean isConstructor() {
    return isConstructor;
  }

  /**
   * Return <code>true</code> if this method represents an implicitly defined method. At the moment
   * the only implicitly defined methods are zero-argument constructors in classes that have no
   * explicitly defined constructors.
   * 
   * @return <code>true</code> if this method represents an implicitly defined method
   */
  public boolean isImplicit() {
    return isImplicit;
  }

  /**
   * Set whether this method is a constructor to the given value.
   * 
   * @param constructor <code>true</code> if this method is a constructor
   */
  public void setConstructor(boolean constructor) {
    isConstructor = constructor;
  }

  /**
   * Set whether this method represents an implicitly defined method to the given value. At the
   * moment the only implicitly defined methods are zero-argument constructors in classes that have
   * no explicitly defined constructors.
   * 
   * @param implicit <code>true</code> if this method represents an implicitly defined method
   */
  public void setImplicit(boolean implicit) {
    isImplicit = implicit;
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
}
