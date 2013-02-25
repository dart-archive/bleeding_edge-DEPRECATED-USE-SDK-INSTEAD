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
package com.google.dart.indexer.index;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.readonly.DiskBackedIndexImpl;
import com.google.dart.indexer.index.readonly.EmptyIndexImpl;
import com.google.dart.indexer.index.readonly.Index;
import com.google.dart.indexer.storage.AbstractIntegratedStorage;
import com.google.dart.indexer.storage.StorageFactory;
import com.google.dart.indexer.utilities.io.FileUtilities;

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;

/**
 * Creates readable/writable index and update session objects in such manner that all the indexes
 * share the same configuration, and all the versions of the index are stored in the given root
 * folder.
 */
public class IndexSession {
  public static void destroyIndexStatically() {
    FileUtilities.delete(IndexerPlugin.getDefault().getStateLocation().toFile());
  }

  private final IndexConfigurationInstance configuration;
  private final AbstractIntegratedStorage storage;
  private boolean disposed = false;
  private final int id;
  private static int concurrentSessions = 0;

  private static int instanceCount = 0;

  public IndexSession(File rootFolder, IndexConfigurationInstance configuration) {
    id = ++instanceCount;
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.SESSION_LIFETIME,
        "IndexSession#" + id + " created");
    if (concurrentSessions > 0) {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
          "WARNING: " + concurrentSessions + " sessions exist now");
      throw new IllegalStateException("Cannot create concurrent indexer sessions");
    }
    ++concurrentSessions;
    this.configuration = configuration;
    storage = StorageFactory.createStorage(configuration, rootFolder);
  }

  public IndexSession(IndexConfigurationInstance configuration) {
    this(IndexerPlugin.getDefault().getStateLocation().toFile(), configuration);
  }

  public Index createEmptyRegularIndex() {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    return new EmptyIndexImpl(storage);
  }

  public Index createNewRegularIndex(File folder) throws IndexTemporarilyNonOperational {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    // Write a new version file.
    VersionFile.write(IndexerPlugin.getDefault().getStateLocation().toFile(), configuration);
    // Delete any existing index file.
    storage.destroy();
    DiskBackedIndexImpl diskBackedIndexImpl = new DiskBackedIndexImpl(configuration, storage);
    return diskBackedIndexImpl;
  }

  public Index createRegularIndex(File folder) throws IndexRequiresFullRebuild {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    VersionFile.check(folder, configuration);
    DiskBackedIndexImpl diskBackedIndexImpl = new DiskBackedIndexImpl(configuration, storage);
    return diskBackedIndexImpl;
  }

  public IndexTransaction createTransaction(Index index) {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    return new IndexTransaction(storage.createTransaction(), configuration);
  }

  public void destroyIndex() {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    storage.destroy();
  }

  public void dispose() {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    storage.close();
    disposed = true;
    --concurrentSessions;
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.SESSION_LIFETIME,
        "IndexSession#" + id + " disposed");
  }

  public void flushCaches() {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    storage.flushCaches();
  }

  public IndexSessionStats gatherStatistics() {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    return new IndexSessionStats(storage.gatherStatistics(), configuration.gatherTimeSpentParsing());
  }

  public IndexConfigurationInstance getConfiguration() {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    return configuration;
  }

  public AbstractIntegratedStorage getStorage() {
    if (disposed) {
      throw new IllegalStateException("Index session has already been disposed");
    }
    return storage;
  }

  public void runConsistencyCheck(IProgressMonitor monitor) {
    storage.runConsistencyCheck(monitor);
  }
}
