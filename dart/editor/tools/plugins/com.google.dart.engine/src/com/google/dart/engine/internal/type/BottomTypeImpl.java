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

import com.google.dart.engine.type.Type;

/**
 * The unique instance of the class {@code BottomTypeImpl} implements the type {@code bottom}.
 */
public class BottomTypeImpl extends TypeImpl {
  /**
   * The unique instance of this class.
   */
  private static BottomTypeImpl INSTANCE = new BottomTypeImpl(); //$NON-NLS-1$

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static BottomTypeImpl getInstance() {
    return INSTANCE;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private BottomTypeImpl() {
    super(null, "bottom");
  }

  @Override
  public boolean isMoreSpecificThan(Type type) {
    return true;
  }

  @Override
  public boolean isSubtypeOf(Type type) {
    // bottom is a subtype of all types
    return true;
  }
}
