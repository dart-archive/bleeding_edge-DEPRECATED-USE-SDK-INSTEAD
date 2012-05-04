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
package com.google.dart.tools.core.generator;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This class contains utility methods that some generators need in this package. These methods were
 * copied out of <code>com.google.dart.compiler.parser.DartScanner</code> so that we could implement
 * {@link #validateIdentifier()}.
 * <p>
 * TODO the private methods in DartScanner should be accessible so that we don't make copies here.
 */
public class DartIdentifierUtil {

  /**
   * Given a string, return an equivalent string that is a valid Dart identifier.
   * 
   * @param str
   * @return
   */
  public static String createValidIdentifier(String str) {
    if (str.length() == 0) {
      return str;
    }

    StringBuilder builder = new StringBuilder();
    boolean nextIsCaps = false;

    if (Character.isJavaIdentifierStart(str.charAt(0))) {
      builder.append(str.charAt(0));
    }

    str = str.substring(1);

    for (char c : str.toCharArray()) {
      if (Character.isJavaIdentifierPart(c)) {
        if (nextIsCaps) {
          builder.append(Character.toUpperCase(c));
        } else {
          builder.append(c);
        }
        nextIsCaps = false;
      } else if (c == '-' || c == '_') {
        nextIsCaps = true;
      } else {
        nextIsCaps = false;
      }
    }

    return builder.toString();
  }

  public static IStatus validateIdentifier(final String str) {
    // TODO extract the strings in this method
    // return error status if str is null or empty
    if (str == null || str.length() == 0) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "This Dart identifier is empty.");
    }
    // return error for the first character
    if (!isIdentifierStart(str.charAt(0))) {
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          "The first character of this Dart identifer is not a valid character, Dart identifiers "
              + "must start with an alphabetic character, '_' or '$'.");
    }
    // return error for any non-first characters
    for (int i = 1; i < str.length(); i++) {
      if (!isIdentifierPart(str.charAt(i))) {
        return new Status(
            IStatus.ERROR,
            DartCore.PLUGIN_ID,
            "This Dart identifer has an invalid character, identifiers "
                + "must include only alphanumeric characters, '_' or '$'.");
      }
    }
    return Status.OK_STATUS;
  }

  private static boolean isDecimalDigit(int c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isIdentifierPart(int c) {
    return isIdentifierStart(c) || isDecimalDigit(c);
  }

  private static boolean isIdentifierStart(int c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_') || (c == '$');
  }
}
