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
package com.google.dart.indexer.storage.paged.store;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.DebugConstants;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.filesystem.AccessMode;
import com.google.dart.indexer.pagedstorage.filesystem.FileObject;
import com.google.dart.indexer.pagedstorage.filesystem.FileSystem;
import com.google.dart.indexer.pagedstorage.util.ByteUtils;
import com.google.dart.indexer.pagedstorage.util.Constants;

import java.io.IOException;

/**
 * This class is an abstraction of a random access file. Each file contains a magic header, and
 * reading / writing is done in blocks. See also {@link SecureFileStore}
 */
public class FileStore {
  /**
   * The size of the file header in bytes.
   */
  public static final int HEADER_LENGTH = 3 * Constants.FILE_BLOCK_SIZE;

  /**
   * An empty buffer to speed up extending the file (it seems that writing 0 bytes is faster then
   * calling setLength).
   */
  protected static final byte[] EMPTY = new byte[16 * 1024];

  /**
   * Open a non encrypted file store with the given settings.
   * 
   * @param handler the data handler
   * @param name the file name
   * @param mode the access mode
   * @return the created object
   */
  public static FileStore open(DataHandler handler, String name, AccessMode mode)
      throws PagedStorageException {
    return new FileStore(handler, name, mode);
  }

  private static void trace(String method, String fileName, Object o) {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.ALL_IO,
        "FileStore." + method + " " + fileName + " " + o);
  }

  /**
   * The file name.
   */
  private String name;

  /**
   * The callback object is responsible to check access rights, and free up disk space if required.
   */
  @SuppressWarnings("unused")
  private DataHandler handler;

  private FileObject file;
  private long filePos;
  private long fileLength;

  // private Reference autoDeleteReference;
  // private boolean checkedWriting = true;
  private boolean synchronousMode;

  private AccessMode mode;

  /**
   * Create a new file using the given settings.
   * 
   * @param handler the callback object
   * @param name the file name
   * @param mode the access mode
   */
  protected FileStore(DataHandler handler, String name, AccessMode mode)
      throws PagedStorageException {
    FileSystem fs = FileSystem.getInstance(name);
    this.handler = handler;
    this.name = name;
    this.mode = mode;
    try {
      fs.createDirs(name);
      if (fs.exists(name) && !fs.canWrite(name)) {
        mode = AccessMode.READ_ONLY;
        this.mode = mode;
      }
      file = fs.openFileObject(name, mode);
      if (mode.isSynchronous()) {
        synchronousMode = true;
      }
      fileLength = file.length();
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, "name: " + name + " mode: " +
      // mode);
    }
  }

  /**
   * Close the file.
   */
  public void close() throws IOException {
    if (file != null) {
      try {
        trace("close", name, file);
        file.close();
      } finally {
        file = null;
      }
    }
  }

  /**
   * Close the file. The file may later be re-opened using openFile.
   */
  public void closeFile() throws IOException {
    file.close();
    file = null;
  }

  /**
   * Close the file without throwing any exceptions. Exceptions are simply ignored.
   */
  public void closeSilently() {
    try {
      close();
    } catch (IOException e) {
      // ignore
    }
  }

  /**
   * Get the current location of the file pointer.
   * 
   * @return the location
   */
  public long getFilePointer() {
    if (DebugConstants.CHECK2) {
      try {
        if (file.getFilePointer() != filePos) {
          throw new AssertionError();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
        // throw Message.convertIOException(e, name);
      }
    }
    return filePos;
  }

  /**
   * Initialize the file. This method will write or check the file header if required.
   */
  public void init() throws PagedStorageException {
    int len = Constants.FILE_BLOCK_SIZE;
    byte[] salt;
    byte[] magic = Constants.MAGIC_FILE_HEADER.getBytes();
    if (length() < HEADER_LENGTH) {
      // write unencrypted
      // checkedWriting = false;
      writeDirect(magic, 0, len);
      salt = generateSalt();
      writeDirect(salt, 0, len);
      initKey(salt);
      // write (maybe) encrypted
      write(magic, 0, len);
      // checkedWriting = true;
    } else {
      // read unencrypted
      seek(0);
      byte[] buff = new byte[len];
      readFullyDirect(buff, 0, len);
      if (ByteUtils.compareNotNull(buff, magic) != 0) {
        throw new RuntimeException();
        // throw Message.getPagedMemoryException(ErrorCode.FILE_VERSION_ERROR_1,
        // name);
      }
      salt = new byte[len];
      readFullyDirect(salt, 0, len);
      initKey(salt);
      // read (maybe) encrypted
      readFully(buff, 0, Constants.FILE_BLOCK_SIZE);
      if (ByteUtils.compareNotNull(buff, magic) != 0) {
        throw new RuntimeException();
        // throw
        // Message.getPagedMemoryException(ErrorCode.FILE_ENCRYPTION_ERROR_1,
        // name);
      }
    }
  }

  /**
   * Check if the file is encrypted.
   * 
   * @return true if it is
   */
  public boolean isEncrypted() {
    return false;
  }

  /**
   * Get the file size in bytes.
   * 
   * @return the file size
   */
  @SuppressWarnings("unused")
  public long length() throws PagedStorageException {
    try {
      long len = fileLength;
      if (DebugConstants.CHECK2) {
        len = file.length();
        if (len != fileLength) {
          throw new AssertionError("file " + name + " length " + len + " expected " + fileLength);
        }
      }
      if (DebugConstants.CHECK2 && len % Constants.FILE_BLOCK_SIZE != 0) {
        long newLength = len + Constants.FILE_BLOCK_SIZE - (len % Constants.FILE_BLOCK_SIZE);
        file.setFileLength(newLength);
        fileLength = newLength;
        throw new AssertionError("unaligned file length " + name + " len " + len);
      }
      return len;
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, name);
    }
  }

  /**
   * Re-open the file. The file pointer will be reset to the previous location.
   */
  public void openFile() throws IOException {
    if (file == null) {
      file = FileSystem.getInstance(name).openFileObject(name, mode);
      file.seek(filePos);
    }
  }

  /**
   * Read a number of bytes.
   * 
   * @param b the target buffer
   * @param off the offset
   * @param len the number of bytes to read
   */
  public void readFully(byte[] b, int off, int len) {
    if (DebugConstants.CHECK && len < 0) {
      throw new AssertionError("read len " + len);
    }
    if (DebugConstants.CHECK && len % Constants.FILE_BLOCK_SIZE != 0) {
      throw new AssertionError("unaligned read " + name + " len " + len);
    }
    try {
      file.readFully(b, off, len);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    filePos += len;
  }

  /**
   * Read a number of bytes without decrypting.
   * 
   * @param b the target buffer
   * @param off the offset
   * @param len the number of bytes to read
   */
  public void readFullyDirect(byte[] b, int off, int len) {
    readFully(b, off, len);
  }

  /**
   * Go to the specified file location.
   * 
   * @param pos the location
   */
  public void seek(long pos) {
    if (DebugConstants.CHECK && pos % Constants.FILE_BLOCK_SIZE != 0) {
      throw new AssertionError("unaligned seek " + name + " pos " + pos);
    }
    try {
      if (pos != filePos) {
        file.seek(pos);
        filePos = pos;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, name);
    }
  }

  public void setCheckedWriting(boolean value) {
    // this.checkedWriting = value;
  }

  /**
   * Set the length of the file. This will expand or shrink the file.
   * 
   * @param newLength the new file size
   */
  public void setLength(long newLength) {
    if (DebugConstants.CHECK && newLength % Constants.FILE_BLOCK_SIZE != 0) {
      throw new AssertionError("unaligned setLength " + name + " pos " + newLength);
    }
    try {
      if (synchronousMode && newLength > fileLength) {
        extendByWriting(newLength);
      } else {
        file.setFileLength(newLength);
      }
      fileLength = newLength;
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, name);
    }
  }

  /**
   * Call fsync. Depending on the operating system and hardware, this may or may not in fact write
   * the changes.
   */
  public void sync() {
    try {
      file.sync();
    } catch (IOException e) {
    }
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Write a number of bytes.
   * 
   * @param b the source buffer
   * @param off the offset
   * @param len the number of bytes to write
   */
  public void write(byte[] b, int off, int len) {
    if (DebugConstants.CHECK && len < 0) {
      throw new AssertionError("read len " + len);
    }
    if (DebugConstants.CHECK && len % Constants.FILE_BLOCK_SIZE != 0) {
      throw new AssertionError("unaligned write " + name + " len " + len);
    }
    try {
      file.write(b, off, len);
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, name);
    }
    filePos += len;
    fileLength = Math.max(filePos, fileLength);
  }

  /**
   * Generate the random salt bytes if required.
   * 
   * @return the random salt or the magic
   */
  protected byte[] generateSalt() {
    return Constants.MAGIC_FILE_HEADER.getBytes();
  }

  /**
   * Initialize the key using the given salt.
   * 
   * @param salt the salt
   */
  protected void initKey(byte[] salt) {
    // do nothing
  }

  /**
   * Write a number of bytes without encrypting.
   * 
   * @param b the source buffer
   * @param off the offset
   * @param len the number of bytes to write
   */
  protected void writeDirect(byte[] b, int off, int len) {
    write(b, off, len);
  }

  private void extendByWriting(long newLength) throws IOException {
    long pos = filePos;
    file.seek(fileLength);
    byte[] empty = EMPTY;
    while (true) {
      int p = (int) Math.min(newLength - fileLength, EMPTY.length);
      if (p <= 0) {
        break;
      }
      file.write(empty, 0, p);
      fileLength += p;
    }
    file.seek(pos);
  }
}
