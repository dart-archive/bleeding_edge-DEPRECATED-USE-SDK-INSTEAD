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
package com.google.dart.engine.utilities.io;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.utilities.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * The class {@code FileUtilities} implements utility methods used to create and manipulate files.
 * 
 * @coverage dart.engine.utilities
 */
public final class FileUtilities {

  /**
   * Ensure that the given file exists and is executable. If it exists but is not executable, then
   * make it executable and log that it was necessary for us to do so.
   * 
   * @return {@code true} if the file exists and is executable, else {@code false}.
   */
  public static boolean ensureExecutable(File file) {
    if (file == null || !file.exists()) {
      return false;
    }
    if (!file.canExecute()) {
      Logger logger = AnalysisEngine.getInstance().getLogger();
      if (!makeExecutable(file)) {
        logger.logError(file + " cannot be made executable");
        return false;
      }
      logger.logError(file + " was not executable");
    }
    return true;
  }

  /**
   * Return the contents of the given file, interpreted as a string.
   * 
   * @param file the file whose contents are to be returned
   * @return the contents of the given file, interpreted as a string
   * @throws IOException if the file contents could not be read
   */
  public static String getContents(File file) throws IOException {
    FileReader fileReader = new FileReader(file);
    try {
      BufferedReader reader = new BufferedReader(fileReader);
      return getContents(reader);
    } finally {
      fileReader.close();
    }
  }

  /**
   * Return the contents of the given reader, interpreted as a string. The client is responsible for
   * closing the reader.
   * 
   * @param reader the reader whose contents are to be returned
   * @return the contents of the given reader, interpreted as a string
   * @throws IOException if the reader could not be read
   */
  public static String getContents(Reader reader) throws IOException {
    StringBuilder builder = new StringBuilder();
    int nextChar = reader.read();
    while (nextChar >= 0) {
      builder.append((char) nextChar);
      nextChar = reader.read();
    }
    return builder.toString();
  }

  /**
   * Return the extension from the given file name, or an empty string if the file name has no
   * extension. The extension is the portion of the name that occurs after the final period when a
   * file name is assumed to be of the form <code>baseName '.' extension</code>.
   * 
   * @return the extension from the given file name
   */
  public static String getExtension(String fileName) {
    if (fileName == null) {
      return "";
    }
    int index = fileName.lastIndexOf('.');
    if (index >= 0) {
      return fileName.substring(index + 1);
    }
    return "";
  }

  /**
   * Attempt to make the given file executable.
   * 
   * @param file the file to be made executable
   * @return {@code true} if the file is executable
   */
  public static boolean makeExecutable(File file) {
    // Try to make the file executable for all users.
    if (file.setExecutable(true, false)) {
      return true;
    }
    // If that fails, then try to make it executable for the current user.
    return file.setExecutable(true, true);
  }

  /**
   * Disallow the creation of instances of this class.
   */
  private FileUtilities() {
  }
}
