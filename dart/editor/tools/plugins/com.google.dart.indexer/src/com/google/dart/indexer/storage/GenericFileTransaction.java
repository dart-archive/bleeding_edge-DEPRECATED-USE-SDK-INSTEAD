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
import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.index.entries.DependentEntity;
import com.google.dart.indexer.index.entries.DependentFileInfo;
import com.google.dart.indexer.index.entries.DependentLocation;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GenericFileTransaction extends FileTransaction {
  private final AbstractIntegratedStorage storage;
  /**
   * @deprecated use {@link #currentSource}
   */
  @Deprecated
  private final IFile currentFile;
  private final IndexableSource currentSource;
  private FileInfo originalFileInfo;
  private final Set<Location> sourceLocations = new LinkedHashSet<Location>();
  /**
   * @deprecated use {@link #sourcesToDependencySets}
   */
  @Deprecated
  private final Map<IFile, Set<DependentEntity>> filesToDependencySets = new HashMap<IFile, Set<DependentEntity>>();
  private final Map<IndexableSource, Set<DependentEntity>> sourcesToDependencySets = new HashMap<IndexableSource, Set<DependentEntity>>();
  private final GenericStorageTransaction storageTransaction;

  /**
   * @deprecated use
   *             {@link #GenericFileTransaction(GenericStorageTransaction, AbstractIntegratedStorage, IndexableSource)}
   */
  @Deprecated
  public GenericFileTransaction(GenericStorageTransaction storageTransaction,
      AbstractIntegratedStorage storage, IFile file) {
    if (storageTransaction == null) {
      throw new NullPointerException("storageTransaction is null");
    }
    if (storage == null) {
      throw new NullPointerException("storage is null");
    }
    if (file == null) {
      throw new NullPointerException("file is null");
    }
    this.storageTransaction = storageTransaction;
    this.storage = storage;
    this.currentFile = file;
    this.currentSource = null;
    this.originalFileInfo = storage.readFileInfo(file);
    if (this.originalFileInfo == null) {
      this.originalFileInfo = new FileInfo();
    }
  }

  public GenericFileTransaction(GenericStorageTransaction storageTransaction,
      AbstractIntegratedStorage storage, IndexableSource source) {
    if (storageTransaction == null) {
      throw new NullPointerException("storageTransaction is null");
    }
    if (storage == null) {
      throw new NullPointerException("storage is null");
    }
    if (source == null) {
      throw new NullPointerException("file is null");
    }
    this.storageTransaction = storageTransaction;
    this.storage = storage;
    this.currentFile = null;
    this.currentSource = source;
    this.originalFileInfo = storage.readFileInfo(source);
    if (this.originalFileInfo == null) {
      this.originalFileInfo = new FileInfo();
    }
  }

  @Override
  @Deprecated
  public void addDependency(IFile masterFile, DependentLocation dependency)
      throws IndexRequestFailed {
    try {
      dependencySetFor(masterFile).add(dependency);
    } catch (Throwable e) {
      throw new IndexRequiresFullRebuild(e);
    }
  }

  @Override
  public void addDependency(IndexableSource masterFile, DependentLocation dependency)
      throws IndexRequestFailed {
    try {
      dependencySetFor(masterFile).add(dependency);
    } catch (Throwable e) {
      throw new IndexRequiresFullRebuild(e);
    }
  }

  @Override
  public void addReference(Layer layer, Location sourceLocation, Location destinationLocation)
      throws IndexRequestFailed {
    try {
      storageTransaction.addReference(layer, sourceLocation, destinationLocation);
    } catch (Throwable e) {
      throw new IndexRequiresFullRebuild(e);
    }
  }

  @Override
  public void addSourceLocation(Location location) throws IndexRequestFailed {
    try {
      if (location == null) {
        throw new NullPointerException("location is null");
      }
      sourceLocations.add(location);
    } catch (Throwable e) {
      throw new IndexRequiresFullRebuild(e);
    }
  }

  @Override
  public void commit() throws IndexRequestFailed {
    FileInfo fileInfo = originalFileInfo;

    Set<Location> removedSourceLocations = new HashSet<Location>(fileInfo.getSourceLocations());
    removedSourceLocations.removeAll(sourceLocations);
    for (Iterator<Location> iterator = removedSourceLocations.iterator(); iterator.hasNext();) {
      Location location = iterator.next();
      storage.deleteLocationInfo(location);
    }

    fileInfo.setSourceLocations(sourceLocations);
    storageTransaction.writeFileInfo(currentFile, fileInfo);

    Set<DependentEntity> currentFileDependencies = dependencySetFor(currentFile);
    for (Iterator<Map.Entry<IFile, Set<DependentEntity>>> iterator = filesToDependencySets.entrySet().iterator(); iterator.hasNext();) {
      IFile file = iterator.next().getKey();
      if (file.equals(currentFile)) {
        continue;
      }
      currentFileDependencies.add(new DependentFileInfo(file));
    }

//    for (Iterator<Map.Entry<IndexableSource, Set<DependentEntity>>> iterator = sourcesToDependencySets.entrySet().iterator(); iterator.hasNext();) {
//      Map.Entry<IndexableSource, Set<DependentEntity>> entry = iterator.next();
//      IndexableSource file = entry.getKey();
    for (Iterator<Map.Entry<IFile, Set<DependentEntity>>> iterator = filesToDependencySets.entrySet().iterator(); iterator.hasNext();) {
      Map.Entry<IFile, Set<DependentEntity>> entry = iterator.next();
      IFile file = entry.getKey();
      Set<DependentEntity> dependencies = entry.getValue();
      storageTransaction.addDependenciesToFileInfo(file, dependencies, currentFile.equals(file));
    }
  }

  @Override
  public FileInfo getOriginalFileInfo() {
    return originalFileInfo;
  }

  /**
   * @deprecated use {@link #dependencySetFor(IndexableSource)}
   */
  @Deprecated
  private Set<DependentEntity> dependencySetFor(IFile file) throws IndexRequestFailed {
    try {
      Set<DependentEntity> result = filesToDependencySets.get(file);
      if (result == null) {
        result = new HashSet<DependentEntity>();
        filesToDependencySets.put(file, result);
      }
      return result;
    } catch (Throwable e) {
      throw new IndexRequiresFullRebuild(e);
    }
  }

  private Set<DependentEntity> dependencySetFor(IndexableSource file) throws IndexRequestFailed {
    try {
      Set<DependentEntity> result = sourcesToDependencySets.get(file);
      if (result == null) {
        result = new HashSet<DependentEntity>();
        sourcesToDependencySets.put(file, result);
      }
      return result;
    } catch (Throwable e) {
      throw new IndexRequiresFullRebuild(e);
    }
  }
}
