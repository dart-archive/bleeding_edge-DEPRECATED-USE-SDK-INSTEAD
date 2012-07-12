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
package com.google.dart.engine.internal.type;

import com.google.dart.engine.type.VoidType;

/**
 * The unique instance of the class {@code VoidTypeImpl} implements the type {@code void}.
 */
public class VoidTypeImpl extends TypeImpl implements VoidType {
  /**
   * The unique instance of this class.
   */
  private static VoidTypeImpl INSTANCE = new VoidTypeImpl(); // //$NON-NLS-1$

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static VoidTypeImpl getInstance() {
    return INSTANCE;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private VoidTypeImpl() {
    super(null, "void");
  }
}
