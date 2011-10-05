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
package com.google.dart.tools.core.utilities.resource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * The class <code>IFileUtilities</code> defines utility methods for manipulating instances of the
 * class {@link IFile}.
 */
public final class IFileUtilities {
  /**
   * Return the contents of the given file.
   * 
   * @param file the file whose contents are to be returned
   * @return the contents of the file
   * @throws CoreException if the file cannot be opened
   */
  public static String getContents(IFile file) throws CoreException, IOException {
    InputStream input = null;
    try {
      input = file.getContents(true);
      StringBuilder contents = new StringBuilder();
      InputStreamReader reader = new InputStreamReader(input);
      int nextChar = reader.read();
      while (nextChar >= 0) {
        contents.append((char) nextChar);
        nextChar = reader.read();
      }
      return contents.toString();
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException exception) {
          // If we cannot close the stream, then ignore it.
        }
      }
    }
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private IFileUtilities() {
  }
}
