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

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.util.Constants;
import com.google.dart.indexer.pagedstorage.util.FileUtils;
import com.google.dart.indexer.pagedstorage.util.IOUtils;
import com.google.dart.indexer.pagedstorage.util.SysProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * This file system stores files on disk. This is the most common file system.
 */
public class DiskFileSystem extends FileSystem {
  private static final DiskFileSystem INSTANCE = new DiskFileSystem();
  // TODO detection of 'case in sensitive filesystem'
  // could maybe implemented using some other means
  @SuppressWarnings("unused")
  private static final boolean IS_FILE_SYSTEM_CASE_INSENSITIVE = File.separatorChar == '\\';

  public static DiskFileSystem getInstance() {
    return INSTANCE;
  }

  private static void wait(int i) {
    if (i > 8) {
      System.gc();
    }
    try {
      // sleep at most 256 ms
      long sleep = Math.min(256, i * i);
      Thread.sleep(sleep);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  protected DiskFileSystem() {
    // nothing to do
  }

  @Override
  public boolean canWrite(String fileName) {
    fileName = translateFileName(fileName);
    return new File(fileName).canWrite();
  }

  @Override
  public void copy(String original, String copy) throws PagedStorageException {
    original = translateFileName(original);
    copy = translateFileName(copy);
    OutputStream out = null;
    InputStream in = null;
    try {
      out = FileUtils.openFileOutputStream(copy, false);
      in = FileUtils.openFileInputStream(original);
      byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
      while (true) {
        int len = in.read(buffer);
        if (len < 0) {
          break;
        }
        out.write(buffer, 0, len);
      }
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeSilently(in);
      IOUtils.closeSilently(out);
    }
  }

  @Override
  public void createDirs(String fileName) throws PagedStorageException {
    fileName = translateFileName(fileName);
    File f = new File(fileName);
    if (!f.exists()) {
      String parent = f.getParent();
      if (parent == null) {
        return;
      }
      File dir = new File(parent);
      for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++) {
        if (dir.exists() || dir.mkdirs()) {
          return;
        }
        wait(i);
      }
      throw new RuntimeException();
    }
  }

  @Override
  public boolean createNewFile(String fileName) {
    fileName = translateFileName(fileName);
    File file = new File(fileName);
    for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++) {
      try {
        return file.createNewFile();
      } catch (IOException e) {
        // 'access denied' is really a concurrent access problem
        wait(i);
      }
    }
    return false;
  }

  @Override
  public String createTempFile(String name, String suffix, boolean deleteOnExit, boolean inTempDir)
      throws IOException {
    name = translateFileName(name);
    name += ".";
    String prefix = new File(name).getName();
    File dir;
    if (inTempDir) {
      dir = null;
    } else {
      dir = new File(name).getAbsoluteFile().getParentFile();
      dir.mkdirs();
    }
    if (prefix.length() < 3) {
      prefix += "0";
    }
    File f = File.createTempFile(prefix, suffix, dir);
    if (deleteOnExit) {
      try {
        f.deleteOnExit();
      } catch (Throwable e) {
        // sometimes this throws a NullPointerException
        // at java.io.DeleteOnExitHook.add(DeleteOnExitHook.java:33)
        // we can ignore it
      }
    }
    return f.getCanonicalPath();
  }

  @Override
  public void delete(String fileName) throws PagedStorageException {
    fileName = translateFileName(fileName);
    File file = new File(fileName);
    if (file.exists()) {
      for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++) {
        trace("delete", fileName, null);
        boolean ok = file.delete();
        if (ok) {
          return;
        }
        wait(i);
      }
      throw new RuntimeException("delete failed: " + fileName);
    }
  }

  @Override
  public void deleteRecursive(String fileName) throws PagedStorageException {
    fileName = translateFileName(fileName);
    if (FileUtils.isDirectory(fileName)) {
      String[] list = listFiles(fileName);
      for (int i = 0; list != null && i < list.length; i++) {
        deleteRecursive(list[i]);
      }
    }
    delete(fileName);
  }

  @Override
  public boolean exists(String fileName) {
    fileName = translateFileName(fileName);
    return new File(fileName).exists();
  }

  @Override
  public String getAbsolutePath(String fileName) {
    fileName = translateFileName(fileName);
    File parent = new File(fileName).getAbsoluteFile();
    return parent.getAbsolutePath();
  }

  @Override
  public String getFileName(String name) throws PagedStorageException {
    name = translateFileName(name);
    String separator = File.separator;
    String path = getParent(name);
    if (!path.endsWith(separator)) {
      path += separator;
    }
    String fullFileName = normalize(name);
    if (!fullFileName.startsWith(path)) {
      throw new RuntimeException();
      // Message.throwInternalError("file utils error: " + fullFileName +
      // " does not start with " + path);
    }
    String fileName = fullFileName.substring(path.length());
    return fileName;
  }

  @Override
  public long getLastModified(String fileName) {
    fileName = translateFileName(fileName);
    return new File(fileName).lastModified();
  }

  @Override
  public String getParent(String fileName) {
    fileName = translateFileName(fileName);
    return new File(fileName).getParent();
  }

  @Override
  public boolean isAbsolute(String fileName) {
    fileName = translateFileName(fileName);
    File file = new File(fileName);
    return file.isAbsolute();
  }

  @Override
  public boolean isDirectory(String fileName) {
    fileName = translateFileName(fileName);
    return new File(fileName).isDirectory();
  }

  @Override
  public boolean isReadOnly(String fileName) {
    fileName = translateFileName(fileName);
    File f = new File(fileName);
    return f.exists() && !f.canWrite();
  }

  @Override
  public long length(String fileName) {
    fileName = translateFileName(fileName);
    return new File(fileName).length();
  }

  @Override
  public String[] listFiles(String path) throws PagedStorageException {
    path = translateFileName(path);
    File f = new File(path);
    try {
      String[] list = f.list();
      if (list == null) {
        return new String[0];
      }
      String base = f.getCanonicalPath();
      if (!base.endsWith(File.separator)) {
        base += File.separator;
      }
      for (int i = 0; i < list.length; i++) {
        list[i] = base + list[i];
      }
      return list;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String normalize(String fileName) throws PagedStorageException {
    fileName = translateFileName(fileName);
    File f = new File(fileName);
    try {
      return f.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, fileName);
    }
  }

  @Override
  public InputStream openFileInputStream(String fileName) throws IOException {
    if (fileName.indexOf(':') > 1) {
      // if the : is in position 1, a windows file access is assumed: C:.. or D:
      // otherwise a URL is assumed
      URL url = new URL(fileName);
      InputStream in = url.openStream();
      return in;
    }
    fileName = translateFileName(fileName);
    FileInputStream in = new FileInputStream(fileName);
    trace("openFileInputStream", fileName, in);
    return in;
  }

  @Override
  public FileObject openFileObject(String fileName, AccessMode mode) throws IOException {
    fileName = translateFileName(fileName);
    DiskFileObject f;
    try {
      f = new DiskFileObject(fileName, mode.getMode());
      trace("openRandomAccessFile", fileName, f);
    } catch (IOException e) {
      freeMemoryAndFinalize();
      try {
        f = new DiskFileObject(fileName, mode.getMode());
      } catch (IOException e2) {
        throw e;
      }
    }
    return f;
  }

  @Override
  public OutputStream openFileOutputStream(String fileName, boolean append)
      throws PagedStorageException {
    fileName = translateFileName(fileName);
    try {
      File file = new File(fileName);
      createDirs(file.getAbsolutePath());
      FileOutputStream out = new FileOutputStream(fileName, append);
      trace("openFileOutputStream", fileName, out);
      return out;
    } catch (IOException e) {
      freeMemoryAndFinalize();
      try {
        return new FileOutputStream(fileName);
      } catch (IOException e2) {
        throw new RuntimeException(e2);
      }
    }
  }

  @Override
  public void rename(String oldName, String newName) throws PagedStorageException {
    oldName = translateFileName(oldName);
    newName = translateFileName(newName);
    File oldFile = new File(oldName);
    File newFile = new File(newName);
    if (oldFile.getAbsolutePath().equals(newFile.getAbsolutePath())) {
      throw new RuntimeException("Rename failed: old=new: " + oldName + " -> " + newName);
    }
    if (!oldFile.exists()) {
      throw new RuntimeException("Rename failed: " + oldName + " -> " + newName);
    }
    if (newFile.exists()) {
      throw new RuntimeException("Rename failed: " + oldName + " -> " + newName);
    }
    for (int i = 0; i < SysProperties.MAX_FILE_RETRY; i++) {
      trace("rename", oldName + " >" + newName, null);
      boolean ok = oldFile.renameTo(newFile);
      if (ok) {
        return;
      }
      wait(i);
    }
    throw new RuntimeException("Rename failed: " + oldName + " -> " + newName);
  }

  @Override
  public boolean tryDelete(String fileName) {
    fileName = translateFileName(fileName);
    trace("tryDelete", fileName, null);
    return new File(fileName).delete();
  }

  /**
   * Call the garbage collection and run finalization. This close all files that were not closed,
   * and are no longer referenced.
   */
  protected void freeMemoryAndFinalize() {
    trace("freeMemoryAndFinalize", null, null);
    Runtime rt = Runtime.getRuntime();
    long mem = rt.freeMemory();
    for (int i = 0; i < 16; i++) {
      rt.gc();
      long now = rt.freeMemory();
      rt.runFinalization();
      if (now == mem) {
        break;
      }
      mem = now;
    }
  }

  /**
   * Print a trace message if tracing is enabled.
   * 
   * @param method the method
   * @param fileName the file name
   * @param o the object
   */
  protected void trace(String method, String fileName, Object o) {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.ALL_IO,
        "FileSystem." + method + " " + fileName + " " + o);
  }

  /**
   * Translate the file name to the native format. This will expand the home directory (~).
   * 
   * @param fileName the file name
   * @return the native file name
   */
  protected String translateFileName(String fileName) {
    return fileName;
  }
}
