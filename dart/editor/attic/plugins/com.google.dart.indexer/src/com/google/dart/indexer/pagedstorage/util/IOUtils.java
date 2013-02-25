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
package com.google.dart.indexer.pagedstorage.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class IOUtils {
  /**
   * Close an input stream without throwing an exception.
   * 
   * @param in the input stream or null
   */
  public static void closeSilently(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /**
   * Close an output stream without throwing an exception.
   * 
   * @param out the output stream or null
   */
  public static void closeSilently(OutputStream out) {
    if (out != null) {
      try {
        out.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /**
   * Close a reader without throwing an exception.
   * 
   * @param reader the reader or null
   */
  public static void closeSilently(Reader reader) {
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /**
   * Close a writer without throwing an exception.
   * 
   * @param writer the writer or null
   */
  public static void closeSilently(Writer writer) {
    if (writer != null) {
      try {
        writer.flush();
        writer.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  /**
   * Copy all data from the input stream to the output stream. Both streams are kept open.
   * 
   * @param in the input stream
   * @param out the output stream
   * @return the number of bytes copied
   */
  public static long copy(InputStream in, OutputStream out) throws IOException {
    long written = 0;
    byte[] buffer = new byte[4 * 1024];
    while (true) {
      int len = in.read(buffer);
      if (len < 0) {
        break;
      }
      out.write(buffer, 0, len);
      written += len;
    }
    return written;
  }

  /**
   * Copy all data from the input stream to the output stream and close both streams. Exceptions
   * while closing are ignored.
   * 
   * @param in the input stream
   * @param out the output stream
   * @return the number of bytes copied
   */
  public static long copyAndClose(InputStream in, OutputStream out) throws IOException {
    try {
      long len = copyAndCloseInput(in, out);
      out.close();
      return len;
    } finally {
      closeSilently(out);
    }
  }

  /**
   * Copy all data from the input stream to the output stream and close the input stream. Exceptions
   * while closing are ignored.
   * 
   * @param in the input stream
   * @param out the output stream
   * @return the number of bytes copied
   */
  public static long copyAndCloseInput(InputStream in, OutputStream out) throws IOException {
    try {
      return copy(in, out);
    } finally {
      closeSilently(in);
    }
  }

  /**
   * Copy all data from the reader to the writer and close the reader. Exceptions while closing are
   * ignored.
   * 
   * @param in the reader
   * @param out the writer
   * @return the number of characters copied
   */
  public static long copyAndCloseInput(Reader in, Writer out) throws IOException {
    long written = 0;
    try {
      char[] buffer = new char[4 * 1024];
      while (true) {
        int len = in.read(buffer);
        if (len < 0) {
          break;
        }
        out.write(buffer, 0, len);
        written += len;
      }
    } finally {
      in.close();
    }
    return written;
  }

  private IOUtils() {
    // utility class
  }
}
