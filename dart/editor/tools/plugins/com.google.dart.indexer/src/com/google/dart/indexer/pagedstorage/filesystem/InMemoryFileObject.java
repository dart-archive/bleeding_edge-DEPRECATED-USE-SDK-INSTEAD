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

import com.google.dart.indexer.pagedstorage.util.MathUtils;

import java.io.EOFException;
import java.io.IOException;

/**
 * This class is an abstraction of an in-memory random access file. Data compression using the LZF
 * algorithm is supported as well.
 */
public class InMemoryFileObject implements FileObject {
  private static final int BLOCK_SIZE_SHIFT = 16;
  private static final int BLOCK_SIZE = 1 << BLOCK_SIZE_SHIFT;
  private static final int BLOCK_SIZE_MASK = BLOCK_SIZE - 1;

  private String name;
  private long length;
  private long pos;
  private byte[][] data;
  private long lastModified;

  InMemoryFileObject(String name) {
    this.name = name;
    data = new byte[0][];
    touch();
  }

  @Override
  public void close() {
    pos = 0;
  }

  @Override
  public long getFilePointer() {
    return pos;
  }

  public long getLastModified() {
    return lastModified;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
    readWrite(b, off, len, false);
  }

  @Override
  public void seek(long pos) {
    this.pos = (int) pos;
  }

  @Override
  public void setFileLength(long newLength) {
    touch();
    if (newLength < length) {
      pos = Math.min(pos, newLength);
      changeLength(newLength);
      long end = MathUtils.roundUpLong(newLength, BLOCK_SIZE);
      if (end != newLength) {
        int lastPage = (int) (newLength >>> BLOCK_SIZE_SHIFT);
        byte[] d = data[lastPage];
        for (int i = (int) (newLength & BLOCK_SIZE_MASK); i < BLOCK_SIZE; i++) {
          d[i] = 0;
        }
      }
    } else {
      changeLength(newLength);
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void sync() {
    // nothing to do
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    touch();
    readWrite(b, off, len, true);
  }

  private void changeLength(long len) {
    length = len;
    len = MathUtils.roundUpLong(len, BLOCK_SIZE);
    int blocks = (int) (len >>> BLOCK_SIZE_SHIFT);
    if (blocks != data.length) {
      byte[][] n = new byte[blocks][];
      System.arraycopy(data, 0, n, 0, Math.min(data.length, n.length));
      for (int i = data.length; i < blocks; i++) {
        n[i] = new byte[BLOCK_SIZE];
      }
      data = n;
    }

  }

  private void readWrite(byte[] b, int off, int len, boolean write) throws IOException {
    long end = pos + len;
    if (end > length) {
      if (write) {
        changeLength(end);
      } else {
        if (len == 0) {
          return;
        }
        throw new EOFException("File: " + name);
      }
    }
    while (len > 0) {
      int l = (int) Math.min(len, BLOCK_SIZE - (pos & BLOCK_SIZE_MASK));
      int page = (int) (pos >>> BLOCK_SIZE_SHIFT);
      byte[] block = data[page];
      int blockOffset = (int) (pos & BLOCK_SIZE_MASK);
      if (write) {
        System.arraycopy(b, off, block, blockOffset, l);
      } else {
        System.arraycopy(block, blockOffset, b, off, l);
      }
      off += l;
      pos += l;
      len -= l;
    }
  }

  private void touch() {
    lastModified = System.currentTimeMillis();
  }
}
