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
 * This file system stores files on disk and uses java.nio to access the files. This class used
 * memory mapped files.
 */
public class MemoryMappedFileSystem extends NioFileSystem {
  private static final MemoryMappedFileSystem INSTANCE = new MemoryMappedFileSystem();

  public static DiskFileSystem getInstance() {
    return INSTANCE;
  }

  @Override
  protected String getPrefix() {
    return FileSystem.PREFIX_NIO_MAPPED;
  }

  protected FileObject open(String fileName, String mode) throws IOException {
    return new MemoryMappedFileObject(fileName, mode);
  }
}
