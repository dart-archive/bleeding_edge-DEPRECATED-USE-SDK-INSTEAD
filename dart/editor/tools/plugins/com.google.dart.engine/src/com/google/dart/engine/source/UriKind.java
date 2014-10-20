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
package com.google.dart.engine.source;

/**
 * The enumeration {@code UriKind} defines the different kinds of URI's that are known to the
 * analysis engine. These are used to keep track of the kind of URI associated with a given source.
 * 
 * @coverage dart.engine.source
 */
public enum UriKind {
  /**
   * A 'dart:' URI.
   */
  DART_URI('d'),

  /**
   * A 'file:' URI.
   */
  FILE_URI('f'),

  /**
   * A 'java:' URI.
   */
  JAVA_URI('j'),

  /**
   * A 'package:' URI.
   */
  PACKAGE_URI('p');

  /**
   * Return the URI kind represented by the given encoding, or {@code null} if there is no kind with
   * the given encoding.
   * 
   * @param encoding the single character encoding used to identify the URI kind to be returned
   * @return the URI kind represented by the given encoding
   */
  public static UriKind fromEncoding(char encoding) {
    switch (encoding) {
      case 'd':
        return DART_URI;
      case 'f':
        return FILE_URI;
      case 'j':
        return JAVA_URI;
      case 'p':
        return PACKAGE_URI;
    }
    return null;
  }

  /**
   * The single character encoding used to identify this kind of URI.
   */
  private char encoding;

  /**
   * Initialize a newly created URI kind to have the given encoding.
   * 
   * @param encoding the single character encoding used to identify this kind of URI.
   */
  private UriKind(char encoding) {
    this.encoding = encoding;
  }

  /**
   * Return the single character encoding used to identify this kind of URI.
   * 
   * @return the single character encoding used to identify this kind of URI
   */
  public char getEncoding() {
    return encoding;
  }
}
