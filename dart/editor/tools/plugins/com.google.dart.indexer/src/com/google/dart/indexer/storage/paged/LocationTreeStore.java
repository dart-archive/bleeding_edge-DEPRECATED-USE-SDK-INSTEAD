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
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLocationInfo;
import com.google.dart.indexer.index.layers.reverse_edges.ReverseEdgesLocationInfo;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationPersitence;
import com.google.dart.indexer.pagedstorage.DebugConstants;
import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.catalog.Mapping;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.infostore.InfoPos;
import com.google.dart.indexer.pagedstorage.infostore.InfoStore;
import com.google.dart.indexer.pagedstorage.treestore.TreeLeaf;
import com.google.dart.indexer.pagedstorage.treestore.TreeStore;
import com.google.dart.indexer.pagedstorage.util.SimpleCacheLRU;
import com.google.dart.indexer.pagedstorage.util.SimpleCacheObject;
import com.google.dart.indexer.pagedstorage.util.StringUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LocationTreeStore implements LocationIdEncoder {
  public static class CachedInfo extends SimpleCacheObject {
    int id;

    public CachedInfo(Object data, int id) {
      super(data);
      this.id = id;
    }
  }

  private static final int KIND_SOURCE = 0;

  private static final int KIND_DESTINATION = 1;

  static final int ID_UNKNOWN = Mapping.ID_NONE - 1;

  static final int ID_FAILED = Mapping.ID_NONE - 2;

  private static String joinPath(String[] path) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < path.length; i++) {
      builder.append(path[i]);
    }
    String id = builder.toString();
    return id;
  }

  private static int scanPath(String uniqueIdentifier, String[] outPath) {
    int len = uniqueIdentifier.length();
    int count = 0;
    int start = 0;
    boolean firstTilde = true;
    outerLoop : for (int i = 0; i < len; i++) {
      char ch = uniqueIdentifier.charAt(i);
      switch (ch) {
        case '\\':
          i++; // skip the next character without processing
          continue outerLoop;
        case '_':
          if (DebugConstants.DONT_SPLIT_LOCATION_PATH_ON_UNDERSCORE) {
            continue outerLoop;
          }
          if (uniqueIdentifier.charAt(start) != '^') {
            continue outerLoop;
          }
          if (i - start < 5) {
            continue outerLoop;
          }
          {
            boolean allUppercase = true;
            for (int k = start + 1; k < i; ++k) {
              char chch = uniqueIdentifier.charAt(k);
              if (chch != Character.toUpperCase(chch)) {
                allUppercase = false;
                break;
              }
            }
            if (allUppercase) {
              continue outerLoop;
            }
          }
          // non-uppercase fields with underscores are probably
          // Eclipse-style message bundles, so split on first underscore
          break;
        case '~':
          if (!firstTilde) {
            continue outerLoop;
          } else {
            firstTilde = false;
            break;
          }
        case '/':
        case '{':
        case '<':
        case '(':
        case '^':
        case '[':
          break;
        default:
          continue outerLoop;
      }
      if (outPath != null) {
        outPath[count] = uniqueIdentifier.substring(start, i);
      }
      start = i;
      count++;
    }
    if (outPath != null) {
      outPath[count] = uniqueIdentifier.substring(start, len);
    }
    count++;
    return count;
  }

  private static String[] splitPath(String uniqueIdentifier) throws AssertionError {
    int count = scanPath(uniqueIdentifier, null);
    String[] path = new String[count];
    if (count != scanPath(uniqueIdentifier, path)) {
      throw new AssertionError("scanPath malfunction");
    }
    if (DebugConstants.CHECK_LOCATION_PATH_SPLITTING) {
      if (!uniqueIdentifier.equals(joinPath(path))) {
        throw new AssertionError("Non-idempotent path splitting for " + uniqueIdentifier);
      }
    }
    return path;
  }

  private final TreeStore treeStore;

  private final InfoStore[] infoStores;

  private final Mapping mapping;

  private final SimpleCacheLRU cache;

  public LocationTreeStore(PagedStorage pagedStorage, int rootTreePageId, int rootMappingPageId,
      int layers) throws PagedStorageException {
    treeStore = new TreeStore(pagedStorage, 4, rootTreePageId);
    infoStores = new InfoStore[layers];
    for (int i = 0; i < infoStores.length; i++) {
      infoStores[i] = new InfoStore(pagedStorage);
    }
    mapping = new Mapping(pagedStorage, rootMappingPageId, treeStore, infoStores);
    for (int i = 0; i < infoStores.length; i++) {
      infoStores[i].setHierarchy(mapping, i);
    }
    cache = new SimpleCacheLRU(10000, 1, 16);
  }

  public void addReference(Location sourceLocation, Location destinationLocation, int layerId,
      boolean bidirectional) throws PagedStorageException {
    int sourceId = locationToId(sourceLocation);
    int destinationId = locationToId(destinationLocation);
    if (sourceId == Mapping.ID_NONE || destinationId == Mapping.ID_NONE) {
      return; // failed to create
    }
    mapping.addToInfo(destinationId, layerId, (KIND_SOURCE << 29) | sourceId);
    if (bidirectional) {
      mapping.addToInfo(sourceId, layerId, (KIND_DESTINATION << 29) | destinationId);
    }
  }

  public void delete(Location location) throws PagedStorageException {
    int id = mapping.find(pathFor(location));
    if (id != Mapping.ID_NONE) {
      mapping.delete(id);
    }
  }

  @Override
  public Location locationFromId(int id) throws PagedStorageException {
    String[] path = mapping.resolve(id);
    if (path == null) {
      return null; // can only happen for already deleted IDs
    }
    return fromPath(path);
  }

  /**
   * Might return <code>ID_NONE</code> to indicate that creation has failed.
   */
  @Override
  public int locationToId(Location location) throws PagedStorageException {
    CachedInfo cachedInfo = (CachedInfo) cache.get(location);
    if (cachedInfo == null || (cachedInfo.id < 0 && cachedInfo.id != ID_FAILED)) {
      int id = mapping.findOrCreate(pathFor(location));
      if (id == Mapping.ID_NONE) {
        id = ID_FAILED;
      }
      if (cachedInfo != null) {
        cachedInfo.id = id;
      } else {
        cachedInfo = new CachedInfo(location, id);
        cache.putNew(cachedInfo);
      }
    }
    if (cachedInfo.id < 0) {
      return Mapping.ID_NONE;
    }
    return cachedInfo.id;
  }

  public LocationInfo read(Location location, int layerId, boolean bidirectional)
      throws PagedStorageException {
    int id = mapping.find(pathFor(location));
    if (id == Mapping.ID_NONE) {
      return null;
    }
    InfoPos infoPos = mapping.locateInfo(id, layerId);
    if (infoPos == null) {
      return null;
    }
    int[] payload = infoPos.readEntireData(id);
    return decode(payload, bidirectional);
  }

  public Map<Location, LocationInfo> readAll(int layerId, boolean bidirectional)
      throws PagedStorageException {
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
        "LocationTreeStore.readAll(" + layerId + ", " + bidirectional + ")");
    Map<Location, LocationInfo> result = new HashMap<Location, LocationInfo>();
    for (Iterator<TreeLeaf> iterator = treeStore.pathIterator(); iterator.hasNext();) {
      TreeLeaf leaf = iterator.next();
      Location key = fromPath(leaf.getPath());
      LocationInfo info = read(key, layerId, bidirectional);
      if (info != null) {
        if (!info.isEmpty()) {
          result.put(key, info);
        }
      } else {
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
            "Skipping location because no info exists: " + StringUtils.join(leaf.getPath()));
      }
    }
    return result;
  }

  public void runConsistencyCheck(IProgressMonitor monitor) {
    int maxId = mapping.getMaxId();
    SubMonitor progress = SubMonitor.convert(monitor, maxId * (1 + infoStores.length));
    int errors = 0;
    for (int id = 1; id < maxId; id++) {
      if (progress.isCanceled()) {
        return;
      }
      if (id % 10 == 1) {
        progress.subTask((maxId - id) + " items left, " + errors + " error(s) so far");
      }
      // String[] path;
      try {
        /* path = */mapping.resolve(id);
        progress.worked(1);
      } catch (PagedStorageException e) {
        System.err.println("Resolving failed for ID " + id);
        e.printStackTrace(System.err);
        progress.worked(infoStores.length);
        ++errors;
        continue;
      }
      for (int layerId = 0; layerId < infoStores.length; layerId++) {
        try {
          InfoPos pos = mapping.locateInfo(id, layerId);
          if (pos != null) {
            pos.readEntireData(id);
          }
        } catch (PagedStorageException e) {
          System.err.println("Reading info failed for ID " + id + ", layer " + layerId);
          e.printStackTrace(System.err);
          ++errors;
        }
        progress.worked(1);
      }
    }

    progress.subTask("Iterating tree paths...");
    int count = 0;
    for (Iterator<TreeLeaf> iterator = treeStore.pathIterator(); iterator.hasNext();) {
      TreeLeaf leaf = iterator.next();
      String[] path = leaf.getPath();
      if (++count % 10 == 0) {
        progress.subTask("Iterating tree paths... " + count + " so far");
      }
      int id;
      try {
        id = mapping.find(path);
        if (id == Mapping.ID_NONE) {
          continue;
        }
      } catch (PagedStorageException e) {
        System.err.println("Resolving failed for path " + StringUtils.join(path));
        e.printStackTrace(System.err);
        ++errors;
        continue;
      }
      for (int layerId = 0; layerId < infoStores.length; layerId++) {
        try {
          InfoPos pos = mapping.locateInfo(id, layerId);
          if (pos != null) {
            /* int[] data = */pos.readEntireData(id);
          }
        } catch (PagedStorageException e) {
          System.err.println("Reading info failed for ID " + id + ", layer " + layerId + ", path "
              + StringUtils.join(path));
          e.printStackTrace(System.err);
          ++errors;
        }
        // progress.worked(1);
      }
    }
    progress.subTask("Done.");
  }

  public void stats(MappingStats stats) {
    try {
      mapping.stats(stats);
      stats.idCacheHits = cache.getHits();
      stats.idCacheMisses = cache.getMisses();
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  @Override
  public String toString() {
    return treeStore.toString() + mapping.toString();
  }

  public void write(Location location, int layerId, LocationInfo info) throws PagedStorageException {
    int id = locationToId(location);
    if (id == Mapping.ID_NONE) {
      return; // failed to store the location, so cannot write info
    }
    int[] payload = encode(info);
    mapping.writeInfo(id, layerId, payload);
  }

  int decodeKind(int encoded) {
    return (encoded >> 29);
  }

  private LocationInfo decode(int[] payload, boolean bidirectional) throws PagedStorageException {
    if (payload.length == 0) {
      return null;
    }
    if (!bidirectional) {
      ReverseEdgesLocationInfo info = new ReverseEdgesLocationInfo();
      for (int i = 0; i < payload.length; i++) {
        int encoded = payload[i];
        Location location = decodeLocation(encoded);
        if (location != null) {
          switch (decodeKind(encoded)) {
            case KIND_SOURCE:
              info.addSourceLocation(location);
              break;
            case KIND_DESTINATION:
              throw new AssertionError("ReverseEdgesLocationInfo cannot have destination edges");
            default:
              throw new AssertionError("Unknown kind: " + decodeKind(encoded));
          }
        }
      }
      return info;
    } else {
      BidirectionalEdgesLocationInfo info = new BidirectionalEdgesLocationInfo();
      for (int i = 0; i < payload.length; i++) {
        int encoded = payload[i];
        Location location = decodeLocation(encoded);
        if (location != null) {
          switch (decodeKind(encoded)) {
            case KIND_SOURCE:
              info.addSourceLocation(location);
              break;
            case KIND_DESTINATION:
              info.addDestinationLocation(location);
              break;
            default:
              throw new AssertionError("Unknown kind: " + decodeKind(encoded));
          }
        }
      }
      return info;
    }
  }

  private Location decodeLocation(int encoded) throws PagedStorageException {
    int id = (encoded & 0x1FFFFFFF);
    return locationFromId(id);
  }

  private int[] encode(LocationInfo info) throws PagedStorageException {
    Location[] source, destination;
    if (info instanceof ReverseEdgesLocationInfo) {
      ReverseEdgesLocationInfo reli = (ReverseEdgesLocationInfo) info;
      source = reli.getSourceLocations();
      destination = new Location[0];
    } else if (info instanceof BidirectionalEdgesLocationInfo) {
      BidirectionalEdgesLocationInfo beli = (BidirectionalEdgesLocationInfo) info;
      source = beli.getSourceLocations();
      destination = beli.getDestinationLocations();
    } else {
      throw new AssertionError("Unsupported kind of location info");
    }
    int[] data = new int[source.length + destination.length];
    int index = 0;
    for (int i = 0; i < source.length; i++) {
      int id = locationToId(source[i]);
      if (id != Mapping.ID_NONE) {
        data[index++] = (KIND_SOURCE << 29) | id;
      }
    }
    for (int i = 0; i < destination.length; i++) {
      int id = locationToId(destination[i]);
      if (id != Mapping.ID_NONE) {
        data[index++] = (KIND_DESTINATION << 29) | id;
      }
    }
    return data;
  }

  private Location fromPath(String[] path) {
    String id = joinPath(path);
    return LocationPersitence.getInstance().byUniqueIdentifier(id);
  }

  private String[] pathFor(Location location) {
    String uniqueIdentifier = LocationPersitence.getInstance().getUniqueIdentifier(location);
    return splitPath(uniqueIdentifier);
  }
}
