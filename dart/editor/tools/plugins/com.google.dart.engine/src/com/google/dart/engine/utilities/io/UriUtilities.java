/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.utilities.io;

import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.UriBasedDirective;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The class {@code UriUtilities} implements utility methods used to manipulate URIs.
 * 
 * @coverage dart.engine.utilities
 */
public final class UriUtilities {

  /**
   * Validation codes returned by {@link UriUtilities#validate(StringLiteral)}.
   */
  public enum UriValidationCode {
    INVALID_URI,
    URI_WITH_INTERPOLATION,
    URI_WITH_DART_EXT_SCHEME,
  }

  /**
   * The prefix of a URI using the {@code dart-ext} scheme to reference a native code library.
   */
  private static final String DART_EXT_SCHEME = "dart-ext:";

  /**
   * Convert from a non-URI encoded string to a URI encoded one.
   * 
   * @return the URI encoded input string
   */
  public static String encode(String str) {
    StringBuilder builder = new StringBuilder(str.length() * 2);

    for (char c : str.toCharArray()) {
      switch (c) {
        case '%':
        case '?':
        case ';':
        case '#':
        case '"':
        case '\'':
        case '<':
        case '>':
        case ' ':
          // ' ' ==> "%20"
          builder.append('%');
          builder.append(Integer.toHexString(c));
          break;
        default:
          builder.append(c);
          break;
      }
    }

    return builder.toString();
  }

  /**
   * Validate the given directive, but do not check for existance.
   * 
   * @param directive the URI based directive, not {@code null}
   * @return a code indicating the problem if there is one, or {@code null} no problem
   */
  public static UriValidationCode validate(UriBasedDirective directive) {
    StringLiteral uriLiteral = directive.getUri();
    if (uriLiteral instanceof StringInterpolation) {
      return UriValidationCode.URI_WITH_INTERPOLATION;
    }
    String uriContent = directive.getUriContent();
    if (uriContent == null) {
      return UriValidationCode.INVALID_URI;
    }
    if (directive instanceof ImportDirective && uriContent.startsWith(DART_EXT_SCHEME)) {
      return UriValidationCode.URI_WITH_DART_EXT_SCHEME;
    }
    try {
      new URI(encode(uriContent));
    } catch (URISyntaxException exception) {
      return UriValidationCode.INVALID_URI;
    }
    return null;
  }

  /**
   * Disallow the creation of instances of this class.
   */
  private UriUtilities() {
  }
}
