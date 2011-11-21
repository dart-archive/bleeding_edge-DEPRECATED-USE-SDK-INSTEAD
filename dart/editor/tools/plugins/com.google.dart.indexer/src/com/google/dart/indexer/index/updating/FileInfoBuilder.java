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
package com.google.dart.indexer.index.updating;

import com.google.dart.indexer.index.entries.DependentLocation;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.source.IndexableSource;
import com.google.dart.indexer.storage.StorageTransaction;

import org.eclipse.core.resources.IFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FileInfoBuilder {
  private final Set<Location> sourceLocations = new LinkedHashSet<Location>();
  private final Map<IFile, Set<DependentLocation>> filesToDependencySets = new HashMap<IFile, Set<DependentLocation>>();
  private final Map<IndexableSource, Set<DependentLocation>> sourcesToDependencySets = new HashMap<IndexableSource, Set<DependentLocation>>();

  public FileInfoBuilder() {
    super();
  }

  @Deprecated
  public FileInfoBuilder(IFile currentFile) {
    super();
  }

  @Deprecated
  public void addDependency(IFile masterFile, DependentLocation dependency) {
    dependencySetFor(masterFile).add(dependency);
  }

  public void addDependency(IndexableSource masterSource, DependentLocation dependency) {
    dependencySetFor(masterSource).add(dependency);
  }

  public void addSourceLocation(Location location) {
    if (location == null) {
      throw new NullPointerException("location is null");
    }
    sourceLocations.add(location);
  }

  public void storeDependenciesInto(StorageTransaction storageTransaction) {
  }

  public void storeSourceLocationsInto(FileInfo fileInfo) {
    fileInfo.setSourceLocations(sourceLocations);
  }

  @Deprecated
  private Set<DependentLocation> dependencySetFor(IFile file) {
    Set<DependentLocation> result = filesToDependencySets.get(file);
    if (result == null) {
      result = new HashSet<DependentLocation>();
      filesToDependencySets.put(file, result);
    }
    return result;
  }

  private Set<DependentLocation> dependencySetFor(IndexableSource source) {
    Set<DependentLocation> result = sourcesToDependencySets.get(source);
    if (result == null) {
      result = new HashSet<DependentLocation>();
      sourcesToDependencySets.put(source, result);
    }
    return result;
  }
}
