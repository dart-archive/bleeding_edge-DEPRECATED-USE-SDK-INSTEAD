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
package com.google.dart.tools.search.internal.ui.text;

import org.eclipse.core.resources.IResourceProxy;

import java.io.File;

/**
 * Identifies file types that should be filtered from dart search scopes.
 */
public class TextSearchScopeFilter {

  private static final String LIB_CONFIG_PATH = "dart-sdk" + File.separator + "lib"
      + File.separator + "config";

  /**
   * Checks if the given file should be filtered out of a search scope.
   * 
   * @param file the file name to check
   * @return <code>true</code> if the file should be excluded, <code>false</code> otherwise
   */
  public static boolean isFiltered(File file) {
    //dart-sdk/lib/config
    if (file.getAbsolutePath().endsWith(LIB_CONFIG_PATH)) {
      return true;
    }
    return isFiltered(file.getName());
  }

  /**
   * Checks if the given file should be filtered out of a search scope.
   * 
   * @param file the file to check
   * @return <code>true</code> if the file should be excluded, <code>false</code> otherwise
   */
  public static boolean isFiltered(IResourceProxy file) {
    return isFiltered(file.getName());
  }

  /**
   * Checks if the given file name should be filtered out of a search scope.
   * 
   * @param fileName the file name to check
   * @return <code>true</code> if the file should be excluded, <code>false</code> otherwise
   */
  private static boolean isFiltered(String fileName) {
    //ignore .files (and avoid traversing into folders prefixed with a '.')
    if (fileName.startsWith(".")) {
      return true;
    }
    if (fileName.endsWith(".dart.js")) {
      return true;
    }
    return false;
  }
}
