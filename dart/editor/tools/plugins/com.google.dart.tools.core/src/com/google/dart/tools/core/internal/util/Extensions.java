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

package com.google.dart.tools.core.internal.util;

import java.io.File;

/**
 * The class <code>Extensions</code> defines utility methods for working with various file
 * extensions.
 */
public final class Extensions {
  /**
   * The file extension used by Dart source files, without the leading dot.
   */
  public static final String DART = "dart";

  /**
   * The file extension used by Dart source files, with the leading dot.
   */
  public static final String DOT_DART = "." + DART;

  /**
   * Return <code>true</code> if the given file is a Dart source file.
   * 
   * @param file the file being tested
   * @return <code>true</code> if the given file is a Dart source file
   */
  public static boolean isDartFile(File file) {
    return file.getName().endsWith(DOT_DART);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private Extensions() {
    super();
  }
}
