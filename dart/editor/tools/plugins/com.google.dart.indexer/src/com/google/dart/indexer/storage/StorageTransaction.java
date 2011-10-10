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

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.entries.DependentEntity;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;

import org.eclipse.core.resources.IFile;

import java.util.HashSet;
import java.util.Set;

public abstract class StorageTransaction {
  public abstract void addDependenciesToFileInfo(IFile file, Set<DependentEntity> dependencies,
      boolean internal);

  public abstract void commit() throws IndexRequestFailed;

  public abstract FileTransaction createFileTransaction(IFile file);

  public abstract LocationInfo readLocationInfo(Layer layer, Location location);

  public abstract FileInfo removeFileInfo(IFile file);

  public abstract void removeLocationInfo(Location location);

  public abstract void removeStaleDependencies(IFile file, IFile staleFile,
      Set<Location> staleLocations);

  public abstract void removeStaleLocationsFromDestination(Layer layer, Location destination,
      HashSet<Location> staleSourceLocations);
}
