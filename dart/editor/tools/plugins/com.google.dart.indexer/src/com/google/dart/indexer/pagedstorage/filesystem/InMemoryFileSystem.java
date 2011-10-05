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
import com.google.dart.indexer.pagedstorage.util.IOUtils;
import com.google.dart.indexer.pagedstorage.util.ObjectArray;
import com.google.dart.indexer.pagedstorage.util.RandomUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This file system keeps files fully in memory. There is an option to compress file blocks to safe
 * memory.
 */
public class InMemoryFileSystem extends FileSystem {
  private static final InMemoryFileSystem INSTANCE = new InMemoryFileSystem();
  private static final HashMap<String, InMemoryFileObject> MEMORY_FILES = new HashMap<String, InMemoryFileObject>();

  public static InMemoryFileSystem getInstance() {
    return INSTANCE;
  }

  private InMemoryFileSystem() {
    // don't allow construction
  }

  @Override
  public boolean canWrite(String fileName) {
    return true;
  }

  @Override
  public void copy(String original, String copy) throws PagedStorageException {
    try {
      OutputStream out = openFileOutputStream(copy, false);
      InputStream in = openFileInputStream(original);
      IOUtils.copyAndClose(in, out);
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, "Can not copy " + original + " to "
      // + copy);
    }
  }

  @Override
  public void createDirs(String fileName) {
    // TODO directories are not really supported
  }

  @Override
  public boolean createNewFile(String fileName) {
    synchronized (MEMORY_FILES) {
      if (exists(fileName)) {
        return false;
      }
      getObjectFile(fileName);
    }
    return true;
  }

  @Override
  public String createTempFile(String name, String suffix, boolean deleteOnExit, boolean inTempDir) {
    name += ".";
    synchronized (MEMORY_FILES) {
      for (int i = 0;; i++) {
        String n = name + (RandomUtils.getSecureLong() >>> 1) + suffix;
        if (!exists(n)) {
          getObjectFile(n);
          return n;
        }
      }
    }
  }

  @Override
  public void delete(String fileName) {
    fileName = normalize(fileName);
    synchronized (MEMORY_FILES) {
      MEMORY_FILES.remove(fileName);
    }
  }

  @Override
  public void deleteRecursive(String fileName) {
    fileName = normalize(fileName);
    synchronized (MEMORY_FILES) {
      Iterator<String> it = MEMORY_FILES.keySet().iterator();
      while (it.hasNext()) {
        String name = it.next();
        if (name.startsWith(fileName)) {
          it.remove();
        }
      }
    }
  }

  @Override
  public boolean exists(String fileName) {
    fileName = normalize(fileName);
    synchronized (MEMORY_FILES) {
      return MEMORY_FILES.get(fileName) != null;
    }
  }

  public boolean fileStartsWith(String fileName, String prefix) {
    fileName = normalize(fileName);
    prefix = normalize(prefix);
    return fileName.startsWith(prefix);
  }

  @Override
  public String getAbsolutePath(String fileName) {
    // TODO relative files are not supported
    return normalize(fileName);
  }

  @Override
  public String getFileName(String name) {
    // TODO directories are not supported
    return name;
  }

  @Override
  public long getLastModified(String fileName) {
    return getObjectFile(fileName).getLastModified();
  }

  @Override
  public String getParent(String fileName) {
    fileName = normalize(fileName);
    int idx = fileName.lastIndexOf('/');
    if (idx < 0) {
      idx = fileName.indexOf(':') + 1;
    }
    return fileName.substring(0, idx);
  }

  @Override
  public boolean isAbsolute(String fileName) {
    // TODO relative files are not supported
    return true;
  }

  @Override
  public boolean isDirectory(String fileName) {
    // TODO in memory file system currently doesn't support directories
    return false;
  }

  @Override
  public boolean isReadOnly(String fileName) {
    return false;
  }

  @Override
  public long length(String fileName) {
    return getObjectFile(fileName).length();
  }

  @Override
  public String[] listFiles(String path) {
    ObjectArray<String> list = ObjectArray.newInstance();
    synchronized (MEMORY_FILES) {
      for (Iterator<String> iterator = MEMORY_FILES.keySet().iterator(); iterator.hasNext();) {
        String name = iterator.next();
        if (name.startsWith(path)) {
          list.add(name);
        }
      }
      String[] array = new String[list.size()];
      list.toArray(array);
      return array;
    }
  }

  @Override
  public String normalize(String fileName) {
    fileName = fileName.replace('\\', '/');
    int idx = fileName.indexOf(":/");
    if (idx > 0) {
      fileName = fileName.substring(0, idx + 1) + fileName.substring(idx + 2);
    }
    return fileName;
  }

  @Override
  public InputStream openFileInputStream(String fileName) {
    InMemoryFileObject obj = getObjectFile(fileName);
    obj.seek(0);
    return new FileObjectInputStream(obj);
  }

  @Override
  public FileObject openFileObject(String fileName, AccessMode mode) {
    InMemoryFileObject obj = getObjectFile(fileName);
    obj.seek(0);
    return obj;
  }

  @Override
  public OutputStream openFileOutputStream(String fileName, boolean append)
      throws PagedStorageException {
    try {
      InMemoryFileObject obj = getObjectFile(fileName);
      obj.seek(0);
      return new FileObjectOutputStream(obj, append);
    } catch (IOException e) {
      throw new RuntimeException(e);
      // throw Message.convertIOException(e, fileName);
    }
  }

  @Override
  public void rename(String oldName, String newName) {
    oldName = normalize(oldName);
    newName = normalize(newName);
    synchronized (MEMORY_FILES) {
      InMemoryFileObject f = getObjectFile(oldName);
      f.setName(newName);
      MEMORY_FILES.remove(oldName);
      MEMORY_FILES.put(newName, f);
    }
  }

  @Override
  public boolean tryDelete(String fileName) {
    fileName = normalize(fileName);
    synchronized (MEMORY_FILES) {
      MEMORY_FILES.remove(fileName);
    }
    return true;
  }

  private InMemoryFileObject getObjectFile(String fileName) {
    fileName = normalize(fileName);
    synchronized (MEMORY_FILES) {
      InMemoryFileObject m = MEMORY_FILES.get(fileName);
      if (m == null) {
        m = new InMemoryFileObject(fileName);
        MEMORY_FILES.put(fileName, m);
      }
      return m;
    }
  }
}
