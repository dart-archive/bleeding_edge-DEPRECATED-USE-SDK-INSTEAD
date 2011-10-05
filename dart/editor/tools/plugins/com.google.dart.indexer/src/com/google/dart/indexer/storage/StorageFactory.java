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
package com.google.dart.indexer.storage;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.storage.db.DbStorage;
import com.google.dart.indexer.storage.inmemory.OptimizedIndexStorage;
import com.google.dart.indexer.storage.paged.DiskMappedStorage;

import java.io.File;

public class StorageFactory {
  private static final int STORAGE_TYPE = 2;

  @SuppressWarnings("fallthrough")
  public static AbstractIntegratedStorage createStorage(IndexConfigurationInstance configuration,
      File rootFolder) {
    switch (STORAGE_TYPE) {
      case 0:
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
            "Creating in-memory ('old') storage.");
        return new OptimizedIndexStorage(configuration);
      case 1:
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS, "Creating DB storage.");
        return new DbStorage(configuration);
      case 2:
        try {
          IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
              "Creating paged memory storage.");
          return new DiskMappedStorage(configuration, rootFolder);
        } catch (PagedStorageException exception) {
          IndexerPlugin.getLogger().logError(exception);
        }
      default:
        throw new AssertionError();
    }
  }
}
