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

import com.google.dart.indexer.pagedstorage.util.FileUtils;
import com.google.dart.indexer.pagedstorage.util.SysProperties;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

/**
 * FileObject which is using NIO MappedByteBuffer mapped to memory from file.
 */
public class MemoryMappedFileObject implements FileObject {
  // TODO support files over 2 GB by using multiple buffers
  private static final long GC_TIMEOUT_MS = 10000;
  private final String name;
  private final MapMode mode;
  private RandomAccessFile file;
  private MappedByteBuffer mapped;

  MemoryMappedFileObject(String fileName, String mode) throws IOException {
    if ("r".equals(mode)) {
      this.mode = MapMode.READ_ONLY;
    } else {
      this.mode = MapMode.READ_WRITE;
    }
    this.name = fileName;
    file = new RandomAccessFile(fileName, mode);
    reMap();
  }

  @Override
  public void close() throws IOException {
    unMap();
    file.close();
    file = null;
  }

  @Override
  public long getFilePointer() {
    return mapped.position();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long length() throws IOException {
    return file.length();
  }

  @Override
  public void readFully(byte[] b, int off, int len) {
    mapped.get(b, off, len);
  }

  @Override
  public void seek(long pos) {
    mapped.position((int) pos);
  }

  @Override
  public void setFileLength(long newLength) throws IOException {
    FileUtils.setLength(file, newLength);
    reMap();
  }

  @Override
  public void sync() throws IOException {
    file.getFD().sync();
    mapped.force();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    // check if need to expand file
    if (mapped.capacity() < mapped.position() + len) {
      setFileLength(mapped.position() + len);
    }
    mapped.put(b, off, len);
  }

  /**
   * Re-map byte buffer into memory, called when file size has changed or file was created.
   */
  private void reMap() throws IOException {
    if (file.length() > Integer.MAX_VALUE) {
      throw new RuntimeException("File over 2GB is not supported yet");
    }
    int oldPos = 0;
    if (mapped != null) {
      oldPos = mapped.position();
      mapped.force();
      unMap();
    }

    // maps new MappedByteBuffer, old one is disposed during GC
    mapped = file.getChannel().map(mode, 0, file.length());
    if (SysProperties.NIO_LOAD_MAPPED) {
      mapped.load();
    }
    mapped.position(oldPos);
  }

  private void unMap() {
    if (mapped != null) {
      // first write all data
      mapped.force();

      // need to dispose old direct buffer, see bug
      // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038

      boolean useSystemGc;
      if (SysProperties.NIO_CLEANER_HACK) {
        try {
          useSystemGc = false;
          Method cleanerMethod = mapped.getClass().getMethod("cleaner", new Class[0]);
          cleanerMethod.setAccessible(true);
          Object cleaner = cleanerMethod.invoke(mapped, new Object[0]);
          Method clearMethod = cleaner.getClass().getMethod("clear", new Class[0]);
          clearMethod.invoke(cleaner, new Object[0]);
        } catch (Throwable e) {
          useSystemGc = true;
        }
      } else {
        useSystemGc = true;
      }
      if (useSystemGc) {
        WeakReference<MappedByteBuffer> bufferWeakRef = new WeakReference<MappedByteBuffer>(mapped);
        mapped = null;
        long start = System.currentTimeMillis();
        while (bufferWeakRef.get() != null) {
          if (System.currentTimeMillis() - start > GC_TIMEOUT_MS) {
            throw new RuntimeException("Timeout (" + GC_TIMEOUT_MS
                + " ms) reached while trying to GC mapped buffer");
          }
          System.gc();
          Thread.yield();
        }
      }
    }
  }
}
