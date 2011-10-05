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
package com.google.dart.tools.core.utilities.compiler;

/**
 * Utility methods for Dart JavaScript
 */
public class DartJsUtilities {
  private static final String $DART = "$Dart";
  private static final String NATIVE_ = "native_";

  /**
   * Given a mangled JavaScript function name, return a Dart <typeName>.<methodName> or
   * <code>null</code> if it cannot be decoded.
   */
  public static String demangleJsFunctionName(String functName) {
    final String systemTypeName = "<system>";

    // Extract the type name
    String typeName = null;
    int start = 0;
    int end = 0;
    if (functName.startsWith("$")) {
      typeName = systemTypeName;
      end = 1;
    } else {
      if (functName.startsWith(NATIVE_)) {
        start = NATIVE_.length();
      }
      if (start < functName.length() && functName.charAt(start) == '_') {
        start++;
      }
      end = functName.indexOf($DART, start);
      if (end == -1) {
        end = functName.indexOf('$', start);
        if (end == -1) {
          end = functName.indexOf('_', start);
        }
      }
      if (end != -1) {
        typeName = functName.substring(start, end);

        // Check for appname$typename
        if (typeName.indexOf('$') != -1) {
          String[] strs = typeName.split("\\$");

          typeName = strs[strs.length - 1];
        }

        if (!isValidIdentifier(typeName)) {
          typeName = null;
        }
      } else {
        typeName = systemTypeName;
        end = 0;
      }
    }

    // Extract the method name
    String methodName = null;
    start = end;
    if (functName.substring(start).startsWith($DART)) {
      start += $DART.length();
      if (start == functName.length()) {
        methodName = "<constructor>";
      }
    }
    if (methodName == null) {
      while (start < functName.length()) {
        char ch = functName.charAt(start);
        if (ch == '$' || ch == '_' || ch == '.') {
          start++;
        } else {
          break;
        }
      }
      end = functName.indexOf('$', start);
      if (end == -1) {
        end = functName.indexOf('_', start);
        if (end == -1) {
          end = functName.length();
        }
      }
      methodName = functName.substring(start, end);
      if (methodName.length() == 0) {
        methodName = null;
      } else if (methodName.equals("operator")) {
        methodName = typeName;
        typeName = systemTypeName;
      } else if (!isValidIdentifier(methodName)) {
        methodName = null;
      }
    }

    if (methodName == null || typeName == null) {
      return null;
    }

    return typeName + "." + methodName;
  }

  static boolean isValidIdentifier(String typeName) {
    if (typeName == null || typeName.length() == 0) {
      return false;
    }
    if (!isIdentifierStart(typeName.charAt(0))) {
      return false;
    }
    for (int i = 1; i < typeName.length(); i++) {
      if (!isIdentifierPart(typeName.charAt(i))) {
        return false;
      }
    }
    return true;
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

  // No instances
  private DartJsUtilities() {
  }
}
