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

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.filesystem.FileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FileUtils {
  /**
   * Check if it is a file or a directory.
   * 
   * @param fileName the file or directory name
   * @return true if it is a directory
   */
  public static boolean isDirectory(String fileName) {
    return FileSystem.getInstance(fileName).isDirectory(fileName);
  }

  /**
   * Get the length of a file.
   * 
   * @param fileName the file name
   * @return the length in bytes
   */
  public static long length(String fileName) {
    return FileSystem.getInstance(fileName).length(fileName);
  }

  /**
   * Create an input stream to read from the file.
   * 
   * @param fileName the file name
   * @return the input stream
   */
  public static InputStream openFileInputStream(String fileName) throws IOException {
    return FileSystem.getInstance(fileName).openFileInputStream(fileName);
  }

  /**
   * Create an output stream to write into the file.
   * 
   * @param fileName the file name
   * @param append if true, the file will grow, if false, the file will be truncated first
   * @return the output stream
   */
  public static OutputStream openFileOutputStream(String fileName, boolean append)
      throws PagedStorageException {
    return FileSystem.getInstance(fileName).openFileOutputStream(fileName, append);
  }

  /**
   * Change the length of the file.
   * 
   * @param file the random access file
   * @param newLength the new length
   */
  public static void setLength(RandomAccessFile file, long newLength) throws IOException {
    try {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.ALL_IO, "FileUtils.setLength " + file);
      file.setLength(newLength);
    } catch (IOException e) {
      long length = file.length();
      if (newLength < length) {
        throw e;
      }
      long pos = file.getFilePointer();
      file.seek(length);
      long remaining = newLength - length;
      int maxSize = 1024 * 1024;
      int block = (int) Math.min(remaining, maxSize);
      byte[] buffer = new byte[block];
      while (remaining > 0) {
        int write = (int) Math.min(remaining, maxSize);
        file.write(buffer, 0, write);
        remaining -= write;
      }
      file.seek(pos);
    }
  }

  private FileUtils() {
    // utility class
  }
}
