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
package com.google.dart.indexer.storage.paged;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.filesystem.AccessMode;
import com.google.dart.indexer.pagedstorage.filesystem.DiskFileSystem;
import com.google.dart.indexer.pagedstorage.filesystem.FileObject;
import com.google.dart.indexer.pagedstorage.filesystem.FileSystem;
import com.google.dart.indexer.pagedstorage.filesystem.InMemoryFileSystem;
import com.google.dart.indexer.pagedstorage.filesystem.MemoryMappedFileSystem;
import com.google.dart.indexer.pagedstorage.filesystem.NioFileSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

public class SpeedTest {
  public static void main(String[] args) throws Exception {
    File file = new File("/tmp/test.foo");
    int fileSize = 1024 * 1024 * 100;
    int readAttemps = 1000000;
    int readSize = 1024;

    Random random = new Random();

    byte[] buf = new byte[readSize];

    FileSystem[] fileSystems = new FileSystem[] {
        InMemoryFileSystem.getInstance(), MemoryMappedFileSystem.getInstance(),
        DiskFileSystem.getInstance(), NioFileSystem.getInstance(),};
    for (int i = 0; i < fileSystems.length; i++) {
      FileSystem fileSystem = fileSystems[i];
      if (!fileSystem.exists(file.getPath())) {
        createFile(fileSystem, fileSize, file, random);
      }

      FileObject fileObject = fileSystem.openFileObject(file.getPath(), AccessMode.READ_WRITE);
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
          "Running read test with " + fileSystem.getClass().getName() + "... ");
      long start = System.currentTimeMillis();
      for (int attemp = 0; attemp < readAttemps; attemp++) {
        long offset = random.nextInt(fileSize - readSize);
        fileObject.seek(offset);
        fileObject.readFully(buf, 0, buf.length);
      }
      fileObject.close();
      long end = System.currentTimeMillis();
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS, (end - start) + " ms.");
    }
  }

  private static void createFile(FileSystem fileSystem, int fileSize, File file, Random random)
      throws FileNotFoundException, IOException, PagedStorageException {
    OutputStream os = fileSystem.openFileOutputStream(file.getPath(), false);
    byte[] buf = new byte[1024];
    random.nextBytes(buf);
    int rows = fileSize / buf.length;
    for (int i = 0; i < rows; i++) {
      os.write(buf);
    }
    os.close();
  }
}
