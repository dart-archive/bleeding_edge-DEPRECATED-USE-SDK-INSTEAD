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

/**
 * The cached data shared by all equal type parameters.
 */
public class DartTypeParameterInfo extends DeclarationElementInfo {
  /**
   * The name of the bound type, or <code>null</code> if no have a bound type.
   */
  private char[] boundName;

  /**
   * @return the name of the bound type, may be <code>null</code>.
   */
  public char[] getBoundName() {
    return boundName;
  }

  /**
   * Set the name of the bound type, may be <code>null</code>.
   */
  public void setBoundName(char[] name) {
    boundName = name;
  }
}
