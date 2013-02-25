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
import java.io.OutputStream;

/**
 * Allows to write to a file object like an output stream.
 */
public class FileObjectOutputStream extends OutputStream {
  private FileObject file;
  private byte[] buffer = new byte[1];

  /**
   * Create a new file object output stream from the file object.
   * 
   * @param file the file object
   * @param append true for append mode, false for truncate and overwrite
   */
  public FileObjectOutputStream(FileObject file, boolean append) throws IOException {
    this.file = file;
    if (append) {
      file.seek(file.length());
    } else {
      file.seek(0);
      file.setFileLength(0);
    }
  }

  @Override
  public void close() throws IOException {
    file.close();
  }

  @Override
  public void write(byte[] b) throws IOException {
    file.write(b, 0, b.length);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    file.write(b, off, len);
  }

  @Override
  public void write(int b) throws IOException {
    buffer[0] = (byte) b;
    file.write(buffer, 0, 1);
  }
}
