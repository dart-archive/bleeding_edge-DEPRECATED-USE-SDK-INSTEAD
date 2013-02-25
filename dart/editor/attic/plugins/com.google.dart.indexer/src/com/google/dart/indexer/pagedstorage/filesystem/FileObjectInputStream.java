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
import java.io.InputStream;

/**
 * Allows to read from a file object like an input stream.
 */
public class FileObjectInputStream extends InputStream {
  private FileObject file;
  private byte[] buffer = new byte[1];

  /**
   * Create a new file object input stream from the file object.
   * 
   * @param file the file object
   */
  public FileObjectInputStream(FileObject file) {
    this.file = file;
  }

  @Override
  public void close() throws IOException {
    file.close();
  }

  @Override
  public int read() throws IOException {
    if (file.getFilePointer() >= file.length()) {
      return -1;
    }
    file.readFully(buffer, 0, 1);
    return buffer[0] & 0xff;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (file.getFilePointer() + len < file.length()) {
      file.readFully(b, off, len);
      return len;
    }
    return super.read(b, off, len);
  }
}
