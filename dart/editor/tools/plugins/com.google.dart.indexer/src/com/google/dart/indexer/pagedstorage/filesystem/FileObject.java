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
package com.google.dart.indexer.pagedstorage.filesystem;

import java.io.IOException;

/**
 * This interface represents a random access file.
 */
public interface FileObject {
  /**
   * Close the file.
   */
  void close() throws IOException;

  /**
   * Get the file pointer.
   * 
   * @return the current file pointer
   */
  long getFilePointer() throws IOException;

  /**
   * Get the full qualified name of this file.
   * 
   * @return the name
   */
  String getName();

  /**
   * Get the length of the file.
   * 
   * @return the length
   */
  long length() throws IOException;

  /**
   * Read from the file.
   * 
   * @param b the byte array
   * @param off the offset
   * @param len the number of bytes
   */
  void readFully(byte[] b, int off, int len) throws IOException;

  /**
   * Go to the specified position in the file.
   * 
   * @param pos the new position
   */
  void seek(long pos) throws IOException;

  /**
   * Change the length of the file.
   * 
   * @param newLength the new length
   */
  void setFileLength(long newLength) throws IOException;

  /**
   * Force changes to the physical location.
   */
  void sync() throws IOException;

  /**
   * Write to the file.
   * 
   * @param b the byte array
   * @param off the offset
   * @param len the number of bytes
   */
  void write(byte[] b, int off, int len) throws IOException;
}
