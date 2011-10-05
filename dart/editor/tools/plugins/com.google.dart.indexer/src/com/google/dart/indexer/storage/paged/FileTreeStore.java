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
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.entries.DependentEntity;
import com.google.dart.indexer.index.entries.DependentFileInfo;
import com.google.dart.indexer.index.entries.DependentLocation;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.catalog.Mapping;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.infostore.InfoPos;
import com.google.dart.indexer.pagedstorage.infostore.InfoStore;
import com.google.dart.indexer.pagedstorage.treestore.PageRecPos;
import com.google.dart.indexer.pagedstorage.treestore.TreeLeaf;
import com.google.dart.indexer.pagedstorage.treestore.TreeStore;
import com.google.dart.indexer.pagedstorage.util.StringUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileTreeStore {
  private static final int KIND_SOURCE_LOCATION = 0;

  private static final int KIND_INTERNAL_DEP_LOCATION = 1;

  private static final int KIND_INTERNAL_DEP_FILE = 2;

  private static final int KIND_EXTERNAL_DEP_LOCATION = 3;

  private static final int KIND_EXTERNAL_DEP_FILE = 4;

  private static final int LAYER_0 = 0;

  private final TreeStore treeStore;

  private final InfoStore infoStore;

  private final Mapping mapping;

  private final LocationIdEncoder locationIdEncoder;

  private Layer[] layers;

  public FileTreeStore(PagedStorage pagedStorage, int rootTreePageId, int rootMappingPageId,
      LocationIdEncoder locationIdEncoder, IndexConfigurationInstance configuration)
      throws PagedStorageException {
    if (locationIdEncoder == null) {
      throw new NullPointerException("locationIdEncoder is null");
    }
    this.locationIdEncoder = locationIdEncoder;

    treeStore = new TreeStore(pagedStorage, 12, rootTreePageId);
    infoStore = new InfoStore(pagedStorage);
    mapping = new Mapping(pagedStorage, rootMappingPageId, treeStore, new InfoStore[] {infoStore}) {

      @Override
      protected void fillTreeDataForNewItem(String[] path, PageRecPos pos) {
        IFile file = fromPath(path);
        long stamp = file.getModificationStamp();
        pos.setTimestamp(stamp);
      }

    };
    infoStore.setHierarchy(mapping, 0);
    layers = configuration.getLayers();
  }

  public void addDependenciesToFileInfo(IFile file, Set<DependentEntity> dependencies,
      boolean internal) throws PagedStorageException {
    int id = fileToId(file);
    if (id == Mapping.ID_NONE) {
      return; // failed to create
    }

    int[] actual = encodeAdditionalPayload(dependencies, internal);
    mapping.addToInfo(id, 0, actual);
  }

  public void delete(IFile file) throws PagedStorageException {
    int id = mapping.find(pathFor(file));
    if (id != Mapping.ID_NONE) {
      mapping.delete(id);
    }
  }

  public IFile fileFromId(int id) throws PagedStorageException {
    String[] path = mapping.resolve(id);
    if (path == null) {
      return null; // can only happen for already deleted IDs
    }
    return fromPath(path);
  }

  public int fileToId(IFile file) throws PagedStorageException {
    return mapping.findOrCreate(pathFor(file));
  }

  public FileInfo read(IFile file) throws PagedStorageException {
    int id = mapping.find(pathFor(file));
    if (id == Mapping.ID_NONE) {
      return null;
    }
    InfoPos infoPos = mapping.locateInfo(id, LAYER_0);
    if (infoPos == null) {
      return null; // data is corrupted, but we can degrade gracefully
    }
    // here
    int[] payload = infoPos.readEntireData(id);
    return decode(payload);
  }

  public Map<IFile, FileInfo> /* <IFile,Integer> */readAll() throws PagedStorageException {
    Map<IFile, FileInfo> result = new HashMap<IFile, FileInfo>();
    for (Iterator<TreeLeaf> iterator = treeStore.pathIterator(); iterator.hasNext();) {
      TreeLeaf leaf = iterator.next();
      IFile key = fromPath(leaf.getPath());
      FileInfo info = read(key);
      if (info != null) {
        result.put(key, info);
      }
    }
    return result;
  }

  public PathAndModStamp[] readFileNamesAndStamps() {
    List<PathAndModStamp> result = new ArrayList<PathAndModStamp>();
    for (Iterator<TreeLeaf> iterator = treeStore.pathIterator(); iterator.hasNext();) {
      TreeLeaf leaf = iterator.next();
      long stamp = leaf.getPos().getTimestamp();
      result.add(new PathAndModStamp(StringUtils.join(leaf.getPath(), "/"), stamp));
    }
    return result.toArray(new PathAndModStamp[result.size()]);
  }

  public void stats(MappingStats stats) {
    try {
      mapping.stats(stats);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public String toString() {
    return treeStore.toString() + mapping.toString();
  }

  public void write(IFile file, FileInfo info) throws PagedStorageException {
    int id = fileToId(file);
    if (id == Mapping.ID_NONE) {
      return; // failed to store the name of the file, so cannot write info
    }
    int[] payload = encode(info);
    mapping.writeInfo(id, LAYER_0, payload);
  }

  @SuppressWarnings("fallthrough")
  private FileInfo decode(int[] payload) throws PagedStorageException {
    int len = payload.length;
    FileInfo result = new FileInfo();
    for (int i = 0; i < len; i++) {
      int encoded = payload[i];
      int kind = (encoded & 0xE0000000) >> 29;
      int layerId = (encoded & 0x1E000000) >> 25;
      int id = (encoded & 0x1FFFFFF);
      IFile file;
      Location location;
      boolean internal = false;
      switch (kind) {
        case KIND_SOURCE_LOCATION:
          location = locationIdEncoder.locationFromId(id);
          if (location != null) {
            result.addSourceLocation(location);
          }
          break;
        case KIND_INTERNAL_DEP_LOCATION:
          internal = true;
        case KIND_EXTERNAL_DEP_LOCATION:
          location = locationIdEncoder.locationFromId(id);
          if (location != null) {
            result.addDependency(new DependentLocation(location, layers[layerId]), internal);
          }
          break;
        case KIND_INTERNAL_DEP_FILE:
          internal = true;
        case KIND_EXTERNAL_DEP_FILE:
          file = fileFromId(id);
          if (file != null) {
            result.addDependency(new DependentFileInfo(file), internal);
          }
          break;
        default:
          throw new AssertionError("Unknown file info element kind: " + kind);
      }
    }
    return result;
  }

  private int[] encode(FileInfo info) throws PagedStorageException {
    Collection<Location> sourceLocations = info.getSourceLocations();
    Collection<DependentEntity> internalDependencies = info.getInternalDependencies();
    Collection<DependentEntity> externalDependencies = info.getExternalDependencies();

    int[] payload = new int[sourceLocations.size() + internalDependencies.size()
        + externalDependencies.size()];
    int index = 0;
    for (Iterator<Location> iterator = sourceLocations.iterator(); iterator.hasNext();) {
      Location location = iterator.next();
      int id = locationIdEncoder.locationToId(location);
      if (id != Mapping.ID_NONE) {
        payload[index++] = (KIND_SOURCE_LOCATION << 29) | id;
      }
    }
    index = encodeExternalDependencies(externalDependencies, payload, index);
    index = encodeInternalDependencies(internalDependencies, payload, index);

    int[] actual = new int[index];
    System.arraycopy(payload, 0, actual, 0, index);
    return actual;
  }

  private int[] encodeAdditionalPayload(Set<DependentEntity> dependencies, boolean internal)
      throws PagedStorageException, AssertionError {
    int[] payload = new int[dependencies.size()];
    int index = 0;
    if (internal) {
      index = encodeInternalDependencies(dependencies, payload, index);
    } else {
      index = encodeExternalDependencies(dependencies, payload, index);
    }
    int[] actual = new int[index];
    System.arraycopy(payload, 0, actual, 0, index);
    return actual;
  }

  private int encodeDependencies(Collection<DependentEntity> dependencies, int[] payload,
      int index, int kindLocation, int kindFile) throws PagedStorageException, AssertionError {
    for (Iterator<DependentEntity> iterator = dependencies.iterator(); iterator.hasNext();) {
      DependentEntity entity = iterator.next();
      if (entity instanceof DependentLocation) {
        DependentLocation dl = (DependentLocation) entity;
        Location location = dl.getDependentLocation();
        int layerId = dl.getDependentLayer().ordinal();
        int id = locationIdEncoder.locationToId(location);
        if (id != Mapping.ID_NONE) {
          payload[index++] = (kindLocation << 29) | (layerId << 25) | id;
        }
      } else if (entity instanceof DependentFileInfo) {
        DependentFileInfo df = (DependentFileInfo) entity;
        IFile file = df.getFile();
        int id = fileToId(file);
        if (id != Mapping.ID_NONE) {
          payload[index++] = (kindFile << 29) | id;
        }
      } else {
        throw new AssertionError("Unsupported kind of dependent entity");
      }
    }
    return index;
  }

  private int encodeExternalDependencies(Collection<DependentEntity> externalDependencies,
      int[] payload, int index) throws PagedStorageException, AssertionError {
    return encodeDependencies(externalDependencies, payload, index, KIND_EXTERNAL_DEP_LOCATION,
        KIND_EXTERNAL_DEP_FILE);
  }

  private int encodeInternalDependencies(Collection<DependentEntity> internalDependencies,
      int[] payload, int index) throws PagedStorageException, AssertionError {
    return encodeDependencies(internalDependencies, payload, index, KIND_INTERNAL_DEP_LOCATION,
        KIND_INTERNAL_DEP_FILE);
  }

  private IFile fromPath(String[] path) {
    IPath p = new Path(StringUtils.join(path, "/"));
    return ResourcesPlugin.getWorkspace().getRoot().getFile(p);
  }

  private String[] pathFor(IFile file) {
    return file.getFullPath().segments();
  }
}
