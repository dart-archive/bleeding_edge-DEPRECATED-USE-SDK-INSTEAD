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
package com.google.dart.indexer.index.entries;

import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationPersitence;
import com.google.dart.indexer.utils.Debugging;
import com.google.dart.indexer.utils.PathUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents the information about a specific indexed file. Currently this is a list of source
 * locations (the locations defined in this file), and a list of destination locations (the
 * locations that the locations in this file are connected to).
 */
public class FileInfo {
  private static final String BASE_SIGNATURE = "FileInfo1";

  // different representations of pathes are used on different versions of
  // Eclipse,
  // so the signatures must be different too

  private static final String SIGNATURE = BASE_SIGNATURE;

  public static FileInfo load(RandomAccessFile file, IndexConfigurationInstance configuration)
      throws IOException {
    List<Location> sourceLocations = LocationPersitence.loadLocations(file);
    List<DependentEntity> internalDependencies = DependencyPersistance.loadDependencies(file,
        configuration);
    List<DependentEntity> externalDependencies = DependencyPersistance.loadDependencies(file,
        configuration);
    return new FileInfo(sourceLocations, internalDependencies, externalDependencies);
  }

  public static PathAndModStamp readHeader(RandomAccessFile file) throws IOException {
    String signature = file.readUTF();
    if (!signature.equals(SIGNATURE)) {
      throw new IOException("Incorrect FileInfo signature");
    }
    String path = file.readUTF();
    long modificationStamp = file.readLong();
    return new PathAndModStamp(path, modificationStamp);
  }

  public static IPath readHeaderAsPath(RandomAccessFile ra) throws IOException {
    return PathUtils.fromPortableString(readHeader(ra).getPath());
  }

  // private long version;

  public static String readHeaderPath(RandomAccessFile file) throws IOException {
    String signature = file.readUTF();
    if (!signature.equals(SIGNATURE)) {
      throw new IOException("Incorrect FileInfo signature");
    }
    String path = file.readUTF();
    return path;
  }

  public static void writeHeader(RandomAccessFile ra, IFile file) throws IOException {
    ra.writeUTF(SIGNATURE);
    ra.writeUTF(PathUtils.toPortableString(file.getFullPath()));
    ra.writeLong(file.getModificationStamp());
  }

  private final Collection<DependentEntity> internalDependencies = new ArrayList<DependentEntity>();

  private final Collection<DependentEntity> externalDependencies = new ArrayList<DependentEntity>();

  private final Collection<Location> sourceLocations = new ArrayList<Location>();

  public FileInfo() {
  }

  public FileInfo(Collection<Location> sourceLocations,
      Collection<DependentEntity> internalDependencies,
      Collection<DependentEntity> externalDependencies) {
    this.internalDependencies.addAll(internalDependencies);
    this.externalDependencies.addAll(externalDependencies);
    this.sourceLocations.addAll(sourceLocations);
  }

  public void addDependency(DependentEntity dependency, boolean internal) {
    if (dependency == null) {
      throw new NullPointerException("dependency is null");
    }
    (internal ? this.internalDependencies : this.externalDependencies).add(dependency);
  }

  public void addSourceLocation(Location location) {
    sourceLocations.add(location);
  }

  public void clearInternalDependencies() {
    internalDependencies.clear();
  }

  public Collection<DependentEntity> getExternalDependencies() {
    return externalDependencies;
  }

  public Collection<DependentEntity> getInternalDependencies() {
    return internalDependencies;
  }

  public Collection<Location> getSourceLocations() {
    return sourceLocations;
  }

  public void save(RandomAccessFile file) throws IOException {
    LocationPersitence.saveLocations(file, sourceLocations);
    DependencyPersistance.saveDependencies(file, internalDependencies);
    DependencyPersistance.saveDependencies(file, externalDependencies);
  }

  public void setExternalDependencies(Collection<DependentEntity> externalDependencies) {
    this.externalDependencies.clear();
    this.externalDependencies.addAll(externalDependencies);
  }

  public void setSourceLocations(Set<Location> sourceLocations) {
    this.sourceLocations.clear();
    this.sourceLocations.addAll(sourceLocations);
  }

  public void setVersion(long version) {
    // this.version=version;
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    toString(out, "", true);
    return out.toString();
  }

  public void toString(StringBuilder out, String indent, boolean showLayers) {
    toStringDependencies(out, indent, showLayers, internalDependencies, "dependent(s)");
    if (externalDependencies.size() > 0) {
      toStringDependencies(out, indent, showLayers, externalDependencies,
          "externally contributed dependent(s)");
    }
    out.append(indent).append(sourceLocations.size()).append(" source location(s)\n");
    for (Iterator<Location> iterator = Debugging.sortByStrings1(sourceLocations).iterator(); iterator.hasNext();) {
      Location location = iterator.next();
      out.append(indent).append(Debugging.INDENT).append(location).append("\n");
    }
  }

  private void toStringDependencies(StringBuilder out, String indent, boolean showLayers,
      Collection<DependentEntity> dependencies, String caption) {
    out.append(indent).append(dependencies.size()).append(" " + caption + "\n");
    for (Iterator<DependentEntity> iterator = Debugging.sortByStrings(dependencies).iterator(); iterator.hasNext();) {
      DependentEntity entity = iterator.next();
      out.append(indent).append(Debugging.INDENT).append(entity.toString(showLayers)).append("\n");
    }
  }
}
