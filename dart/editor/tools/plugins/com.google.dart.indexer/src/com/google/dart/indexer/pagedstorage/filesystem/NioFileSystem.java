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

/**
 * This file system stores files on disk and uses java.nio to access the files. This class uses
 * FileChannel.
 */
public class NioFileSystem extends DiskFileSystem {
  private static final NioFileSystem INSTANCE = new NioFileSystem();

  public static DiskFileSystem getInstance() {
    return INSTANCE;
  }

  @Override
  public String createTempFile(String name, String suffix, boolean deleteOnExit, boolean inTempDir)
      throws IOException {
    String file = super.createTempFile(name, suffix, deleteOnExit, inTempDir);
    return getPrefix() + file;
  }

  @Override
  public String getAbsolutePath(String fileName) {
    return getPrefix() + super.getAbsolutePath(fileName);
  }

  @Override
  public String getParent(String fileName) {
    return getPrefix() + super.getParent(fileName);
  }

  @Override
  public String[] listFiles(String path) throws PagedStorageException {
    String[] list = super.listFiles(path);
    for (int i = 0; list != null && i < list.length; i++) {
      list[i] = getPrefix() + list[i];
    }
    return list;
  }

  @Override
  public String normalize(String fileName) throws PagedStorageException {
    return getPrefix() + super.normalize(fileName);
  }

  @Override
  public InputStream openFileInputStream(String fileName) throws IOException {
    return super.openFileInputStream(translateFileName(fileName));
  }

  @Override
  public FileObject openFileObject(String fileName, AccessMode mode) throws IOException {
    fileName = translateFileName(fileName);
    FileObject f;
    try {
      f = open(fileName, mode);
      trace("openRandomAccessFile", fileName, f);
    } catch (IOException e) {
      freeMemoryAndFinalize();
      try {
        f = open(fileName, mode);
      } catch (IOException e2) {
        throw e;
      }
    }
    return f;
  }

  /**
   * Get the prefix for this file system.
   * 
   * @return the prefix
   */
  protected String getPrefix() {
    return FileSystem.PREFIX_NIO;
  }

  /**
   * Try to open a file with this name and mode.
   * 
   * @param fileName the file name
   * @param mode the open mode
   * @return the file object
   * @throws IOException if opening fails
   */
  protected FileObject open(String fileName, AccessMode mode) throws IOException {
    return new NioFileObject(fileName, mode);
  }

  @Override
  protected String translateFileName(String fileName) {
    if (fileName.startsWith(getPrefix())) {
      fileName = fileName.substring(getPrefix().length());
    }
    return super.translateFileName(fileName);
  }
}
