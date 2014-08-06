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
package com.google.dart.tools.core.internal.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Comparator;

/**
 * Instances of the class <code>Util</code>.
 * 
 * @coverage dart.tools.core
 */
public class Util {
  public static final String EMPTY_STRING = "";
  public static final char[] NO_CHAR = new char[0];

  /**
   * A cached copy of the Dart-like file extensions computed by DartCore.
   */
  private static char[][] DART_LIKE_EXTENSIONS;

  private static final int DEFAULT_READING_SIZE = 0x2000;

  private static final String UTF_8 = "UTF-8";

  /**
   * Combine two hash codes to make a new one.
   * 
   * @param hashCode1 the first hash code to be combined
   * @param hashCode2 the second hash code to be combined
   * @return the result of combining the hash codes
   */
  public static int combineHashCodes(int hashCode1, int hashCode2) {
    return hashCode1 * 17 + hashCode2;
  }

  /**
   * Find and return the first line separator used by the given text.
   * 
   * @return </code>"\n"</code> or </code>"\r"</code> or </code>"\r\n"</code>, or <code>null</code>
   *         if the text does not contain a line separator
   */
  public static String findLineSeparator(char[] text) {
    // find the first line separator
    int length = text.length;
    if (length > 0) {
      char nextChar = text[0];
      for (int i = 0; i < length; i++) {
        char currentChar = nextChar;
        nextChar = i < length - 1 ? text[i + 1] : ' ';
        switch (currentChar) {
          case '\n':
            return "\n"; //$NON-NLS-1$
          case '\r':
            return nextChar == '\n' ? "\r\n" : "\r"; //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
    // not found
    return null;
  }

  /**
   * Return the registered Dart-like extensions.
   * 
   * @return the registered Dart-like extensions
   * @see DartCore#getDartLikeExtensions()
   */
  public static char[][] getDartLikeExtensions() {
    if (DART_LIKE_EXTENSIONS == null) {
      String[] coreExtensions = DartCore.getDartLikeExtensions();
      int length = coreExtensions.length;
      char[][] extensions = new char[length][];
      for (int i = 0; i < length; i++) {
        extensions[i] = coreExtensions[i].toCharArray();
      }
      DART_LIKE_EXTENSIONS = extensions;
    }
    return DART_LIKE_EXTENSIONS;
  }

  public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding)
      throws IOException {
    BufferedReader reader = null;
    try {
      reader = encoding == null ? new BufferedReader(new InputStreamReader(stream))
          : new BufferedReader(new InputStreamReader(stream, encoding));
    } catch (UnsupportedEncodingException e) {
      // encoding is not supported
      reader = new BufferedReader(new InputStreamReader(stream));
    }
    char[] contents;
    int totalRead = 0;
    if (length == -1) {
      contents = NO_CHAR;
    } else {
      // length is a good guess when the encoding produces less or the same
      // amount of characters than the file length
      contents = new char[length]; // best guess
    }

    while (true) {
      int amountRequested;
      if (totalRead < length) {
        // until known length is met, reuse same array sized eagerly
        amountRequested = length - totalRead;
      } else {
        // reading beyond known length
        int current = reader.read();
        if (current < 0) {
          break;
        }

        // read at least 8K
        amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);

        // resize contents if needed
        if (totalRead + 1 + amountRequested > contents.length) {
          System.arraycopy(
              contents,
              0,
              contents = new char[totalRead + 1 + amountRequested],
              0,
              totalRead);
        }

        // add current character
        contents[totalRead++] = (char) current; // coming from totalRead==length
      }
      // read as many chars as possible
      int amountRead = reader.read(contents, totalRead, amountRequested);
      if (amountRead < 0) {
        break;
      }
      totalRead += amountRead;
    }

    // Do not keep first character for UTF-8 BOM encoding
    int start = 0;
    if (totalRead > 0 && UTF_8.equals(encoding)) {
      if (contents[0] == 0xFEFF) { // if BOM char then skip
        totalRead--;
        start = 1;
      }
    }

    // resize contents if necessary
    if (totalRead < contents.length) {
      System.arraycopy(contents, start, contents = new char[totalRead], 0, totalRead);
    }

    return contents;
  }

  /**
   * Return the line number corresponding to a source position, given an array of line end
   * positions.
   * 
   * @param position the source position for which a line number is needed
   * @param lineEnds an array of line end positions
   * @param g min index in lineEnds (>= 0)
   * @param d max index in lineEnds (< lineEnds.length)
   * @return line number of source position
   */
  public static int getLineNumber(int position, int[] lineEnds, int g, int d) {
    if (lineEnds == null) {
      return 1;
    }
    if (d == -1) {
      return 1;
    }
    int m = g, start;
    while (g <= d) {
      m = g + (d - g) / 2;
      if (position < (start = lineEnds[m])) {
        d = m - 1;
      } else if (position > start) {
        g = m + 1;
      } else {
        return m + 1;
      }
    }
    if (position < lineEnds[m]) {
      return m + 1;
    }
    return m + 2;
  }

  /**
   * Return the line separator found in the given text. If the text is null, or no line separator is
   * found in the text, return the line delimiter for the given project. If the project is null,
   * return the line separator for the workspace. If still null, return the system line separator.
   */
  public static String getLineSeparator(String text, DartProject project) {
    String lineSeparator = null;

    // line delimiter in given text
    if (text != null && text.length() != 0) {
      lineSeparator = findLineSeparator(text.toCharArray());
      if (lineSeparator != null) {
        return lineSeparator;
      }
    }

    if (Platform.isRunning()) {
      // line delimiter in project preference
      IScopeContext[] scopeContext;
      if (project != null) {
        scopeContext = new IScopeContext[] {new ProjectScope(project.getProject())};
        lineSeparator = Platform.getPreferencesService().getString(
            Platform.PI_RUNTIME,
            Platform.PREF_LINE_SEPARATOR,
            null,
            scopeContext);
        if (lineSeparator != null) {
          return lineSeparator;
        }
      }

      // line delimiter in workspace preference
      scopeContext = new IScopeContext[] {InstanceScope.INSTANCE};
      lineSeparator = Platform.getPreferencesService().getString(
          Platform.PI_RUNTIME,
          Platform.PREF_LINE_SEPARATOR,
          null,
          scopeContext);
      if (lineSeparator != null) {
        return lineSeparator;
      }
    }

    // system line delimiter
    return System.getProperty("line.separator");
  }

  /**
   * Return the substring of the given file name, ending at the start of a Dart-like extension. The
   * entire file name is returned if it doesn't end with a Dart-like extension.
   * 
   * @return the given file name with any Dart-like extension removed
   */
  public static String getNameWithoutDartLikeExtension(String fileName) {
    int index = indexOfDartLikeExtension(fileName);
    if (index == -1) {
      return fileName;
    }
    return fileName.substring(0, index);
  }

  /**
   * Returns the given file's contents as a character array.
   */
  public static char[] getResourceContentsAsCharArray(IFile file) throws DartModelException {
    // Get encoding from file
    String encoding;
    try {
      encoding = file.getCharset();
    } catch (CoreException ce) {
      // do not use any encoding
      encoding = null;
    }
    return getResourceContentsAsCharArray(file, encoding);
  }

  public static char[] getResourceContentsAsCharArray(IFile file, String encoding)
      throws DartModelException {
    // Get file length
    // workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=130736 by using
    // java.io.File if possible
    IPath location = file.getLocation();
    long length;
    if (location == null) {
      // non local file
      try {
        URI locationURI = file.getLocationURI();
        if (locationURI == null) {
          // throw new CoreException(new Status(IStatus.ERROR,
          // DartCore.PLUGIN_ID, Messages.bind(Messages.file_notFound,
          // file.getFullPath().toString())));
          throw new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "File not found: "
              + file.getFullPath().toString()));
        }
        length = EFS.getStore(locationURI).fetchInfo().getLength();
      } catch (CoreException e) {
        throw new DartModelException(e, DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST);
      }
    } else {
      // local file
      length = location.toFile().length();
    }

    // Get resource contents
    InputStream stream = null;
    try {
      stream = file.getContents(true);
    } catch (CoreException e) {
      throw new DartModelException(e, DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST);
    }
    try {
      return getInputStreamAsCharArray(stream, (int) length, encoding);
    } catch (IOException e) {
      throw new DartModelException(e, DartModelStatusConstants.IO_EXCEPTION);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /**
   * Return the index of the Dart-like extension of the given file name or -1 if it doesn't end with
   * a known Dart-like extension. Note this is the index of the '.' even if it is not considered
   * part of the extension.
   * 
   * @return the index of the Dart-like extension of the given file name
   */
  public static int indexOfDartLikeExtension(String fileName) {
    int fileNameLength = fileName.length();
    char[][] javaLikeExtensions = getDartLikeExtensions();
    extensions : for (int i = 0, length = javaLikeExtensions.length; i < length; i++) {
      char[] extension = javaLikeExtensions[i];
      int extensionLength = extension.length;
      int extensionStart = fileNameLength - extensionLength;
      int dotIndex = extensionStart - 1;
      if (dotIndex < 0) {
        continue;
      }
      if (fileName.charAt(dotIndex) != '.') {
        continue;
      }
      for (int j = 0; j < extensionLength; j++) {
        if (fileName.charAt(extensionStart + j) != extension[j]) {
          continue extensions;
        }
      }
      return dotIndex;
    }
    return -1;
  }

  /**
   * Sorts an array of objects in place. The given comparer compares pairs of items.
   */
  public static <T> void sort(T[] objects, Comparator<T> comparer) {
    if (objects.length > 1) {
      quickSort(objects, 0, objects.length - 1, comparer);
    }
  }

  /**
   * Sort the objects in the given collection using the given comparer.
   */
  private static <T> void quickSort(T[] sortedCollection, int left, int right,
      Comparator<T> comparer) {
    int original_left = left;
    int original_right = right;
    T mid = sortedCollection[left + (right - left) / 2];
    do {
      while (comparer.compare(sortedCollection[left], mid) < 0) {
        left++;
      }
      while (comparer.compare(mid, sortedCollection[right]) < 0) {
        right--;
      }
      if (left <= right) {
        T tmp = sortedCollection[left];
        sortedCollection[left] = sortedCollection[right];
        sortedCollection[right] = tmp;
        left++;
        right--;
      }
    } while (left <= right);
    if (original_left < right) {
      quickSort(sortedCollection, original_left, right, comparer);
    }
    if (left < original_right) {
      quickSort(sortedCollection, left, original_right, comparer);
    }
  }
}
