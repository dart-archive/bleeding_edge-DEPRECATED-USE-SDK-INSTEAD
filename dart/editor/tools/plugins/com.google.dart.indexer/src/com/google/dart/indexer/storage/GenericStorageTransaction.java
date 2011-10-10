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

import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.index.entries.DependentEntity;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLocationInfo;
import com.google.dart.indexer.index.layers.reverse_edges.ReverseEdgesLocationInfo;
import com.google.dart.indexer.locations.Location;

import org.eclipse.core.resources.IFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GenericStorageTransaction extends StorageTransaction {
  private final AbstractIntegratedStorage storage;

  public GenericStorageTransaction(AbstractIntegratedStorage storage) {
    if (storage == null) {
      throw new NullPointerException("storage is null");
    }
    this.storage = storage;
  }

  @Override
  public void addDependenciesToFileInfo(IFile file, Set<DependentEntity> dependencies,
      boolean internal) {
    FileInfo info = storage.readFileInfo(file);
    if (info == null) {
      info = new FileInfo();
    }
    for (Iterator<DependentEntity> iterator = dependencies.iterator(); iterator.hasNext();) {
      info.addDependency(iterator.next(), internal);
    }
    storage.writeFileInfo(file, info);
  }

  public void addReference(Layer layer, Location sourceLocation, Location destinationLocation) {
    if (layer.isBidirectional()) {
      BidirectionalEdgesLocationInfo destinationInfo = (BidirectionalEdgesLocationInfo) storage.readLocationInfo(
          destinationLocation, layer);
      if (destinationInfo == null) {
        destinationInfo = new BidirectionalEdgesLocationInfo();
      }
      destinationInfo.addSourceLocation(sourceLocation);
      storage.writeLocationInfo(destinationLocation, destinationInfo, layer);

      BidirectionalEdgesLocationInfo sourceInfo = (BidirectionalEdgesLocationInfo) storage.readLocationInfo(
          sourceLocation, layer);
      if (sourceInfo == null) {
        sourceInfo = new BidirectionalEdgesLocationInfo();
      }
      sourceInfo.addDestinationLocation(destinationLocation);
      storage.writeLocationInfo(sourceLocation, sourceInfo, layer);
    } else {
      ReverseEdgesLocationInfo destinationInfo = (ReverseEdgesLocationInfo) storage.readLocationInfo(
          destinationLocation, layer);
      if (destinationInfo == null) {
        destinationInfo = new ReverseEdgesLocationInfo();
      }
      destinationInfo.addSourceLocation(sourceLocation);
      storage.writeLocationInfo(destinationLocation, destinationInfo, layer);
    }
  }

  @Override
  public void commit() throws IndexRequiresFullRebuild {
    try {
      storage.checkpoint();
    } catch (Throwable e) {
      throw new IndexRequiresFullRebuild(e);
    }
  }

  @Override
  public FileTransaction createFileTransaction(IFile file) {
    return new GenericFileTransaction(this, storage, file);
  }

  @Override
  public LocationInfo readLocationInfo(Layer layer, Location location) {
    return storage.readLocationInfo(location, layer);
  }

  @Override
  public FileInfo removeFileInfo(IFile file) {
    FileInfo result = storage.readFileInfo(file);
    storage.deleteFileInfo(file);
    return result;
  }

  @Override
  public void removeLocationInfo(Location location) {
    storage.deleteLocationInfo(location);
  }

  @Override
  public void removeStaleDependencies(IFile file, IFile staleFile, Set<Location> staleLocations) {
    FileInfo info = storage.readFileInfo(file);
    if (info == null) {
      return;
    }
    info.setExternalDependencies(doRemoveStaleDependencies(staleFile, staleLocations,
        info.getExternalDependencies()));
    storage.writeFileInfo(file, info);
  }

  @Override
  public void removeStaleLocationsFromDestination(Layer layer, Location destination,
      HashSet<Location> staleSourceLocations) {
    LocationInfo destinationInfo = storage.readLocationInfo(destination, layer);
    if (destinationInfo == null) {
      return;
    }
    destinationInfo.adjustDueToRemovalOf(staleSourceLocations);
    storage.writeLocationInfo(destination, destinationInfo, layer);
  }

  public void writeFileInfo(IFile file, FileInfo info) {
    storage.writeFileInfo(file, info);
  }

  private Collection<DependentEntity> doRemoveStaleDependencies(IFile staleFile,
      Set<Location> staleLocations, Collection<DependentEntity> oldDependencies) {
    Collection<DependentEntity> result = new ArrayList<DependentEntity>();
    for (Iterator<DependentEntity> iterator = oldDependencies.iterator(); iterator.hasNext();) {
      DependentEntity dependency = iterator.next();
      if (!dependency.isStale(staleFile, staleLocations)) {
        result.add(dependency);
      }
    }
    return result;
  }
}
