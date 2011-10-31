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
package com.google.dart.tools.core.utilities.general;

public final class StringUtilities {

  /**
   * This method is equivalent to {@link String#endsWith(String)} with the exception that
   * {@link String#equalsIgnoreCase(String)} is used, instead of {@link String#equals(Object)}.
   * 
   * @see String#endsWith(String)
   * @param str the String to test
   * @param suffix the suffix
   * @return <code>true</code> if the passes string ends with the passed suffix, ignoring cases
   */
  public static boolean endsWithIgnoreCase(String str, String suffix) {
    // if one of the two inputs is null, return false.
    if (str == null || suffix == null) {
      return false;
    }
    int strLength = str.length();
    int suffixLength = suffix.length();
    // cover the trivial case where the suffix has no length
    if (suffixLength == 0) {
      return true;
    }
    // if the string is shorter than the suffix, return false
    if (strLength < suffixLength) {
      return false;
    }
    String strSuffix = str.substring(strLength - suffixLength);
    return strSuffix.equalsIgnoreCase(suffix);
  }

  private StringUtilities() {

  }

}
