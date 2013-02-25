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

import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The file system is a storage abstraction.
 */
public abstract class FileSystem {
  /**
   * The prefix used for an in-memory file system.
   */
  public static final String PREFIX_MEMORY = "memFS:";

  /**
   * The prefix used for the NIO FileChannel file system.
   */
  public static final String PREFIX_NIO = "nio:";

  /**
   * The prefix used for the NIO (memory mapped) file system.
   */
  public static final String PREFIX_NIO_MAPPED = "nioMapped:";

  /**
   * Get the file system object.
   * 
   * @param fileName the file name or prefix
   * @return the file system
   */
  public static FileSystem getInstance(String fileName) {
    if (fileName == null) {
      return DiskFileSystem.getInstance();
    } else if (fileName.startsWith(PREFIX_MEMORY)) {
      return InMemoryFileSystem.getInstance();
    } else if (fileName.startsWith(PREFIX_NIO)) {
      return NioFileSystem.getInstance();
    } else if (fileName.startsWith(PREFIX_NIO_MAPPED)) {
      return MemoryMappedFileSystem.getInstance();
    }
    return DiskFileSystem.getInstance();
  }

  /**
   * Check if the file is writable.
   * 
   * @param fileName the file name
   * @return if the file is writable
   */
  public abstract boolean canWrite(String fileName);

  /**
   * Copy a file from one directory to another, or to another file.
   * 
   * @param original the original file name
   * @param copy the file name of the copy
   */
  public abstract void copy(String original, String copy) throws PagedStorageException;

  /**
   * Create all required directories that are required for this file.
   * 
   * @param fileName the file name (not directory name)
   */
  public abstract void createDirs(String fileName) throws PagedStorageException;

  /**
   * Create a new file.
   * 
   * @param fileName the file name
   * @return true if creating was successful
   */
  public abstract boolean createNewFile(String fileName) throws PagedStorageException;

  /**
   * Create a new temporary file.
   * 
   * @param prefix the prefix of the file name (including directory name if required)
   * @param suffix the suffix
   * @param deleteOnExit if the file should be deleted when the virtual machine exists
   * @param inTempDir if the file should be stored in the temporary directory
   * @return the name of the created file
   */
  public abstract String createTempFile(String prefix, String suffix, boolean deleteOnExit,
      boolean inTempDir) throws IOException;

  /**
   * Delete a file.
   * 
   * @param fileName the file name
   */
  public abstract void delete(String fileName) throws PagedStorageException;

  /**
   * Delete a directory or file and all subdirectories and files.
   * 
   * @param directory the directory
   */
  public abstract void deleteRecursive(String directory) throws PagedStorageException;

  /**
   * Checks if a file exists.
   * 
   * @param fileName the file name
   * @return true if it exists
   */
  public abstract boolean exists(String fileName);

  /**
   * Get the absolute file name.
   * 
   * @param fileName the file name
   * @return the absolute file name
   */
  public abstract String getAbsolutePath(String fileName);

  /**
   * Get the file name (without directory part).
   * 
   * @param name the directory and file name
   * @return just the file name
   */
  public abstract String getFileName(String name) throws PagedStorageException;

  /**
   * Get the last modified date of a file
   * 
   * @param fileName the file name
   * @return the last modified date
   */
  public abstract long getLastModified(String fileName);

  /**
   * Get the parent directory of a file or directory.
   * 
   * @param fileName the file or directory name
   * @return the parent directory name
   */
  public abstract String getParent(String fileName);

  /**
   * Check if the file name includes a path.
   * 
   * @param fileName the file name
   * @return if the file name is absolute
   */
  public abstract boolean isAbsolute(String fileName);

  /**
   * Check if it is a file or a directory.
   * 
   * @param fileName the file or directory name
   * @return true if it is a directory
   */
  public abstract boolean isDirectory(String fileName);

  /**
   * Check if a file is read-only.
   * 
   * @param fileName the file name
   * @return if it is read only
   */
  public abstract boolean isReadOnly(String fileName);

  /**
   * Get the length of a file.
   * 
   * @param fileName the file name
   * @return the length in bytes
   */
  public abstract long length(String fileName);

  /**
   * List the files in the given directory.
   * 
   * @param directory the directory
   * @return the list of fully qualified file names
   */
  public abstract String[] listFiles(String directory) throws PagedStorageException;

  /**
   * Create all required directories.
   * 
   * @param directoryName the directory name
   */
  public void mkdirs(String directoryName) throws PagedStorageException {
    createDirs(directoryName + "/x");
  }

  /**
   * Normalize a file name.
   * 
   * @param fileName the file name
   * @return the normalized file name
   */
  public abstract String normalize(String fileName) throws PagedStorageException;

  /**
   * Create an input stream to read from the file.
   * 
   * @param fileName the file name
   * @return the input stream
   */
  public abstract InputStream openFileInputStream(String fileName) throws IOException;

  /**
   * Open a random access file object.
   * 
   * @param fileName the file name
   * @param mode the access mode
   * @return the file object
   */
  public abstract FileObject openFileObject(String fileName, AccessMode mode) throws IOException;

  /**
   * Create an output stream to write into the file.
   * 
   * @param fileName the file name
   * @param append if true, the file will grow, if false, the file will be truncated first
   * @return the output stream
   */
  public abstract OutputStream openFileOutputStream(String fileName, boolean append)
      throws PagedStorageException;

  /**
   * Rename a file if this is allowed.
   * 
   * @param oldName the old fully qualified file name
   * @param newName the new fully qualified file name
   * @throws PagedStorageException
   */
  public abstract void rename(String oldName, String newName) throws PagedStorageException;

  /**
   * Try to delete a file.
   * 
   * @param fileName the file name
   * @return true if it could be deleted
   */
  public abstract boolean tryDelete(String fileName);

  /**
   * Check if the file system is responsible for this file name.
   * 
   * @param fileName the file name
   * @return true if it is
   */
  protected boolean accepts(String fileName) {
    return false;
  }
}
