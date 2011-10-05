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
package com.google.dart.tools.core.model;

/**
 * The interface <code>Field</code> defines the behavior of objects representing fields defined in
 * types.
 */
public interface Field extends TypeMember {
  /**
   * Return the name of the type of this field, or <code>null</code> if this field does not have a
   * declared type.
   * 
   * @return the name of the type of this field
   * @throws DartModelException if the type of this field cannot be accessed
   */
  public String getTypeName() throws DartModelException;

  /**
   * Return <code>true</code> if this field is declared to be constant.
   * 
   * @return <code>true</code> if this field is const
   */
  public boolean isConstant();

  /**
   * Return <code>true</code> if this field is declared to be final.
   * 
   * @return <code>true</code> if this field is final
   */
  public boolean isFinal();
}
