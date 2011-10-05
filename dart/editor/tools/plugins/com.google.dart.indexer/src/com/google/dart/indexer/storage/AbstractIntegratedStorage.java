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

import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class AbstractIntegratedStorage {
  protected final IndexConfigurationInstance configuration;

  public AbstractIntegratedStorage(IndexConfigurationInstance configuration) {
    if (configuration == null) {
      throw new NullPointerException("configuration is null");
    }
    this.configuration = configuration;
  }

  public void checkpoint() {
  }

  public void close() {
  }

  public StorageTransaction createTransaction() {
    return new GenericStorageTransaction(this);
  }

  public abstract void deleteFileInfo(IFile file);

  public abstract void deleteLocationInfo(Location location);

  public void destroy() {
  }

  public void flushCaches() {
  }

  public Object gatherStatistics() {
    return null;
  }

  public final IndexConfigurationInstance getConfiguration() {
    return configuration;
  }

  public abstract Map<IFile, FileInfo> readAllFileInfos(IndexConfigurationInstance configuration);

  public final Map<Location, LocationInfo> readAllLayerLocations(Layer layer) {
    Map<Location, LocationInfo> result = new HashMap<Location, LocationInfo>();
    readAllLayerLocationsInto(result, layer);
    return result;
  }

  public abstract FileInfo readFileInfo(IFile file);

  public abstract PathAndModStamp[] readFileNamesAndStamps(HashSet<IFile> unprocessedExistingFiles);

  public abstract LocationInfo readLocationInfo(Location location, Layer layer);

  public void runConsistencyCheck(IProgressMonitor monitor) {
  }

  public abstract void writeFileInfo(IFile file, FileInfo info);

  public abstract void writeLocationInfo(Location location, LocationInfo info, Layer layer);

  protected abstract void readAllLayerLocationsInto(Map<Location, LocationInfo> locationInfos,
      Layer layer);
}
