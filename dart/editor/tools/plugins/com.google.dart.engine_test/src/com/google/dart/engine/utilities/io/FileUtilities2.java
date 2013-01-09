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

import java.io.File;

/**
 * The class {@code FileUtilities2} implements utility methods used to create and manipulate files.
 */
public final class FileUtilities2 {
  /**
   * Create a file with the given path after replacing any forward slashes ('/') in the path with
   * the current file separator.
   * 
   * @param path the path of the file to be created
   * @return the file representing the path
   */
  public static File createFile(String path) {
    return new File(convertPath(path)).getAbsoluteFile();
  }

  /**
   * Convert all forward slashes in the given path to the current file separator.
   * 
   * @param path the path to be converted
   * @return the converted path
   */
  private static String convertPath(String path) {
    if (File.separator.equals("/")) {
      // We're on a unix-ish OS.
      return path;
    } else {
      // On windows, the path separator is '\'.
      return path.replaceAll("/", "\\\\");
    }
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private FileUtilities2() {
  }
}
