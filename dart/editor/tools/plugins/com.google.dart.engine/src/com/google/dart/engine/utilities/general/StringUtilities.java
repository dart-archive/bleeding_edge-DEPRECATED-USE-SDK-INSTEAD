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
package com.google.dart.engine.utilities.general;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * The class {@code StringUtilities} defines utility methods for strings.
 * 
 * @coverage dart.engine.utilities
 */
public final class StringUtilities {
  public static final String[] EMPTY_ARRAY = new String[0];

  /**
   * The {@link Interner} instance to use for {@link #intern(String)}.
   */
  private static final Interner<String> INTERNER = Interners.newWeakInterner();

  /**
   * Returns a canonical representation for the given {@link String}.
   * 
   * @return the given {@link String} or its canonical representation.
   */
  public static String intern(String str) {
    if (str == null) {
      return null;
    }
    str = new String(str);
    return INTERNER.intern(str);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private StringUtilities() {
  }
}
