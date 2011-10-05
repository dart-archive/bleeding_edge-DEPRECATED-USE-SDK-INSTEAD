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
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.entries.DependentEntity;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLocationInfo;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.filesystem.AccessMode;
import com.google.dart.indexer.storage.AbstractIntegratedStorage;
import com.google.dart.indexer.storage.StorageTransaction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiskMappedStorage extends AbstractIntegratedStorage {
  private PagedStorage pagedStorage;
  private FileTreeStore fileTreeStore;
  private LocationTreeStore locationTreeStore;
  private File file;

  private final IndexConfigurationInstance configuration;

  public DiskMappedStorage(IndexConfigurationInstance configuration, File rootFolder)
      throws PagedStorageException {
    super(configuration);
    this.configuration = configuration;
    file = new File(rootFolder, "indexerdb");
    // file = new File("/tmp/indexerdb");
    createPageStore();
  }

  public void addDependenciesToFileInfo(IFile file, Set<DependentEntity> dependencies,
      boolean internal) {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.addDependenciesToFileInfo(" + file + ", " + internal + ")");
    if (dependencies.isEmpty()) {
      return;
    }
    try {
      fileTreeStore.addDependenciesToFileInfo(file, dependencies, internal);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }

  }

  public void addReference(Layer layer, Location sourceLocation, Location destinationLocation) {
    IndexerPlugin.getLogger().trace(
        IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.addReference(" + sourceLocation + ", " + destinationLocation + ", "
            + layer.getId().stringValue() + ")");
    try {
      locationTreeStore.addReference(sourceLocation, destinationLocation, layer.ordinal(),
          layer.isBidirectional());
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void checkpoint() {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.checkpoint()");
    try {
      pagedStorage.checkpoint();
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void close() {
    try {
      pagedStorage.checkpoint();
      pagedStorage.close();
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public StorageTransaction createTransaction() {
    return new PagedStorageTransaction(this);
  }

  @Override
  public void deleteFileInfo(IFile file) {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.deleteFileInfo(" + file + ")");
    try {
      fileTreeStore.delete(file);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void deleteLocationInfo(Location location) {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.deleteLocationInfo(" + location + ")");
    try {
      locationTreeStore.delete(location);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void destroy() {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.destroy()");
    try {
      pagedStorage.close();
      file.delete();
      createPageStore();
    } catch (PagedStorageException exception) {
    }
  }

  @Override
  public void flushCaches() {
    try {
      close();
      createPageStore();
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public Object gatherStatistics() {
    PagedIndexStatistics stats = new PagedIndexStatistics();
    locationTreeStore.stats(stats.locationStats);
    fileTreeStore.stats(stats.fileStats);
    stats.resolve(pagedStorage.getPageSize());
    return stats;
  }

  @Override
  public Map<IFile, FileInfo> readAllFileInfos(IndexConfigurationInstance configuration) {
    try {
      return fileTreeStore.readAll();
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return new HashMap<IFile, FileInfo>();
    }
  }

  @Override
  public void readAllLayerLocationsInto(Map<Location, LocationInfo> locationInfos, Layer layer) {
    int layerId = layer.ordinal();
    try {
      locationInfos.putAll(locationTreeStore.readAll(layerId, layer.isBidirectional()));
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public FileInfo readFileInfo(IFile file) {
    try {
      return fileTreeStore.read(file);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return new FileInfo();
    }
  }

  @Override
  public PathAndModStamp[] readFileNamesAndStamps(HashSet<IFile> unprocessedExistingFiles) {
    return fileTreeStore.readFileNamesAndStamps();
  }

  @Override
  public LocationInfo readLocationInfo(Location location, Layer layer) {
    try {
      return locationTreeStore.read(location, layer.ordinal(), layer.isBidirectional());
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return new BidirectionalEdgesLocationInfo();
    }
  }

  @Override
  public void runConsistencyCheck(IProgressMonitor monitor) {
    SubMonitor progress = SubMonitor.convert(monitor, 10);
    try {
      locationTreeStore.runConsistencyCheck(progress.newChild(10));
    } finally {
      monitor.done();
    }
  }

  @Override
  public String toString() {
    return locationTreeStore.toString() + fileTreeStore.toString();
  }

  @Override
  public void writeFileInfo(IFile file, FileInfo info) {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.writeFileInfo(" + file + ")");
    try {
      fileTreeStore.write(file, info);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public void writeLocationInfo(Location location, LocationInfo info, Layer layer) {
    IndexerPlugin.getLogger().trace(
        IndexerDebugOptions.STORAGE_CALLS,
        "DiskMappedStorage.writeLocationInfo(" + location + ", " + layer.getId().stringValue()
            + ")");
    try {
      locationTreeStore.write(location, layer.ordinal(), info);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  private void createPageStore() throws PagedStorageException {
    int cacheSizeKb = 1024 * 128;
    // nioMapped:
    pagedStorage = new PagedStorage("nio:" + file.getPath(), AccessMode.READ_WRITE, cacheSizeKb, 4);
    pagedStorage.open();

    locationTreeStore = new LocationTreeStore(pagedStorage, pagedStorage.getSpecialPage(2),
        pagedStorage.getSpecialPage(3), configuration.getLayers().length);
    fileTreeStore = new FileTreeStore(pagedStorage, pagedStorage.getSpecialPage(0),
        pagedStorage.getSpecialPage(1), locationTreeStore, configuration);
  }
}
