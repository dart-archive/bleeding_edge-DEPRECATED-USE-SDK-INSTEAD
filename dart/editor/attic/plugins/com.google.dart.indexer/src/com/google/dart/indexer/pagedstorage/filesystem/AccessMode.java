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

/**
 * Values of the enum <code>AccessMode</code> represent the access modes that are valid for a
 * {@link FileObject}.
 */
public enum AccessMode {
  /**
   * Open for reading only. Invoking any of the <tt>write</tt> methods of the resulting object will
   * cause an {@link java.io.IOException} to be thrown.
   */
  READ_ONLY("r"),

  /**
   * Open for reading and writing. If the file does not already exist then an attempt will be made
   * to create it.
   */
  READ_WRITE("rw"),

  /**
   * Open for reading and writing, as with <tt>READ_WRITE</tt>, and also require that every update
   * to the file's content or metadata be written synchronously to the underlying storage device.
   */
  READ_WRITE_SYNC("rws"),

  /**
   * Open for reading and writing, as with <tt>READ_WRITE</tt>, and also require that every update
   * to the file's content be written synchronously to the underlying storage device.
   */
  READ_WRITE_DATA("rwd");

  /**
   * The String representation of this mode.
   */
  private String mode;

  /**
   * Prevent the creation of new instances of this type.
   */
  private AccessMode(String mode) {
    this.mode = mode;
  }

  /**
   * Return the String representation of this mode.
   * 
   * @return the String representation of this mode
   */
  public String getMode() {
    return mode;
  }

  /**
   * Return <code>true</code> if this mode requires that some or all of the writes be synchronous.
   * 
   * @return <code>true</code> if this mode requires that some or all of the writes be synchronous
   */
  public boolean isSynchronous() {
    return mode.length() > 2;
  }
}
