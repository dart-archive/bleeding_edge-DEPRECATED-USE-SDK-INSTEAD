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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * The class <code>FileUtilities</code> implements utility methods used to create and manipulate
 * files.
 */
public final class FileUtilities {
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
   * Attempt to make the given file executable.
   * 
   * @param file the file to be made executable
   * @return <code>true</code> if the file is executable
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
