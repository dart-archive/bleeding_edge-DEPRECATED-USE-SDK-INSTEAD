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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * File which uses NIO FileChannel.
 */
public class NioFileObject implements FileObject {
  private final String name;

  private FileChannel channel;

  NioFileObject(String fileName, AccessMode mode) throws FileNotFoundException {
    this.name = fileName;
    RandomAccessFile file = new RandomAccessFile(fileName, mode.getMode());
    channel = file.getChannel();
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  @Override
  public long getFilePointer() throws IOException {
    return channel.position();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long length() throws IOException {
    return channel.size();
  }

  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
    if (len == 0) {
      return;
    }
    // reading the size can reduce the performance
    // if (channel.size() <= off + len) {
    // throw new java.io.EOFException();
    // }
    ByteBuffer buf = ByteBuffer.wrap(b);
    buf.position(off);
    buf.limit(off + len);
    channel.read(buf);
  }

  @Override
  public void seek(long pos) throws IOException {
    channel.position(pos);
  }

  @Override
  public void setFileLength(long newLength) throws IOException {
    if (newLength <= channel.size()) {
      long oldPos = channel.position();
      channel.truncate(newLength);
      if (oldPos > newLength) {
        oldPos = newLength;
      }
      channel.position(oldPos);
    } else {
      // extend by writing to the new location
      ByteBuffer b = ByteBuffer.allocate(1);
      channel.write(b, newLength - 1);
    }
  }

  @Override
  public void sync() throws IOException {
    channel.force(true);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    ByteBuffer buf = ByteBuffer.wrap(b);
    buf.position(off);
    buf.limit(off + len);
    channel.write(buf);
  }
}
