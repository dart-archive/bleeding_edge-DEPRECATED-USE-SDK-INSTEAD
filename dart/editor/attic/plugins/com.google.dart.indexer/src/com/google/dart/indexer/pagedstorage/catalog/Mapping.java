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
package com.google.dart.indexer.pagedstorage.catalog;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.infostore.InfoPos;
import com.google.dart.indexer.pagedstorage.infostore.InfoStore;
import com.google.dart.indexer.pagedstorage.infostore.InfoStore.Page;
import com.google.dart.indexer.pagedstorage.infostore.InfoStoreItemsHierarchy;
import com.google.dart.indexer.pagedstorage.stats.MappingLayerStats;
import com.google.dart.indexer.pagedstorage.treestore.PageRec;
import com.google.dart.indexer.pagedstorage.treestore.PageRecPos;
import com.google.dart.indexer.pagedstorage.treestore.TreeCoordListener;
import com.google.dart.indexer.pagedstorage.treestore.TreeStore;
import com.google.dart.indexer.pagedstorage.util.StringUtils;
import com.google.dart.indexer.storage.paged.MappingStats;
import com.google.dart.indexer.storage.paged.store.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Mapping implements TreeCoordListener, InfoStoreItemsHierarchy {
  public static final int ID_NONE = -1;

  public static final int CB_ID = 4;

  private FixedLengthPagedArray catalog;

  private final TreeStore treeStore;

  private final InfoStore[] infoStores;

  static int lookupCount = 0;

  public Mapping(PagedStorage pagedStorage, int rootPageId, TreeStore treeStore,
      InfoStore[] infoStores) throws PagedStorageException {
    if (treeStore == null) {
      throw new NullPointerException("treeStore is null");
    }
    this.treeStore = treeStore;
    this.infoStores = infoStores;
    catalog = new FixedLengthPagedArray(pagedStorage, rootPageId, CB_ID + PageRecPos.SIZE
        + InfoPos.SIZE * infoStores.length);
    treeStore.setCoordListener(this);
  }

  public void addToInfo(int id, int layerId, int item) throws PagedStorageException {
    CatalogPos pos = catalog.resolve(id);
    if (pos == null) {
      throw new IllegalArgumentException("Catalog ID not found: " + id);
    }
    Data data = pos.readableData().seek(infoOffset(layerId));
    InfoStore infoStore = infoStores[layerId];
    InfoPos infoPos = infoStore.readPos(data);
    if (infoPos == null || !infoPos.hasItem(id)) {
      infoPos = infoStore.writeNew(id, new int[] {item});
      pos.setInfoPage(layerId, infoPos.getPageId());
    } else {
      if (infoPos.addItem(id, item)) {
        pos.setInfoPage(layerId, infoPos.getPageId());
      }
    }
    if (!infoPos.hasItem(id)) {
      throw new AssertionError("Adding failed");
    }
  }

  public void addToInfo(int id, int layerId, int[] actual) throws PagedStorageException {
    CatalogPos pos = catalog.resolve(id);
    if (pos == null) {
      throw new IllegalArgumentException("Catalog ID not found: " + id);
    }
    Data data = pos.readableData().seek(infoOffset(layerId));
    InfoStore infoStore = infoStores[layerId];
    InfoPos infoPos = infoStore.readPos(data);
    if (infoPos == null) {
      infoPos = infoStore.writeNew(id, actual);
      pos.setInfoPage(layerId, infoPos.getPageId());
    } else {
      if (infoPos.addItems(id, actual)) {
        pos.setInfoPage(layerId, infoPos.getPageId());
      }
    }
    if (!infoPos.hasItem(id)) {
      throw new AssertionError("Adding failed");
    }
  }

  public synchronized void delete(int id) throws PagedStorageException {
    CatalogPos pos = catalog.resolve(id);
    if (pos == null) {
      return; // does not exist, hmm...
    }
    if (pos.isDeleted()) {
      return;
    }
    Data data = pos.readableData().seek(CB_ID);
    PageRecPos treePos = treeStore.readPos(data);
    InfoPos[] infoPos = new InfoPos[infoStores.length];
    for (int i = 0; i < infoStores.length; i++) {
      infoPos[i] = infoStores[i].readPos(data);
    }
    treePos.delete();
    for (int i = 0; i < infoStores.length; i++) {
      if (infoPos[i] != null) {
        infoPos[i].delete(id);
      }
    }
    pos.delete();
  }

  public synchronized int find(String[] path) throws PagedStorageException {
    return lookup(path, false);
  }

  public synchronized int findOrCreate(String[] path) throws PagedStorageException {
    return lookup(path, true);
  }

  @Override
  public int findPageOf(int storeId, int itemId) {
    try {
      CatalogPos pos = catalog.resolve(itemId);
      int page = pos.getInfoPage(storeId);
      while (page <= 0) {
        itemId = pos.getParentRowId();
        if (itemId <= 0) {
          break;
        }
        pos = catalog.resolve(itemId);
        page = pos.getInfoPage(storeId);
      }
      return (page > 0 ? page : -1);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      throw new RuntimeException(exception);
    }
  }

  public int getMaxId() {
    return catalog.getOverallItemCount() - 1;
  }

  @Override
  public int getParent(int itemId) {
    try {
      CatalogPos pos = catalog.resolve(itemId);
      return pos.getParentRowId();
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      throw new RuntimeException(exception);
    }
  }

  public synchronized int[] load(int id) throws PagedStorageException {
    int[] result = new int[infoStores.length];
    CatalogPos pos = catalog.resolve(id);
    Data data = pos.readableData();
    for (int i = 0; i < result.length; i++) {
      result[i] = data.readInt();
    }
    return result;
  }

  public synchronized InfoPos locateInfo(int id, int layerId) throws PagedStorageException {
    CatalogPos pos = catalog.resolve(id);
    if (pos == null) {
      return null;
    }
    Data data = pos.readableData().seek(infoOffset(layerId));
    return infoStores[layerId].readPos(data);
  }

  public synchronized int parentOf(int id) throws PagedStorageException {
    CatalogPos pos = catalog.resolve(id);
    if (pos == null) {
      return -1;
    }
    Data data = pos.readableData().seek(0);
    return data.readInt();
  }

  public synchronized String[] resolve(int id) throws PagedStorageException {
    List<String> path = new ArrayList<String>();
    do {
      CatalogPos pos = catalog.resolve(id);
      if (pos.isDeleted()) {
        return null;
      }
      PageRecPos treePos = treeStore.readPos(pos.readableData().seek(CB_ID));
      String component = treePos.readName();
      path.add(component);
      id = parentOf(id);
    } while (id > 0);
    Collections.reverse(path);
    return path.toArray(new String[path.size()]);
  }

  @SuppressWarnings("unchecked")
  public void stats(MappingStats stats) throws PagedStorageException {
    stats.itemCount = catalog.getOverallItemCount();
    Set<Integer>[] pages = new Set[infoStores.length];
    for (int i = 0; i < infoStores.length; i++) {
      pages[i] = new HashSet<Integer>();
    }
    MappingLayerStats[] layerStats = new MappingLayerStats[infoStores.length];
    for (int i = 0; i < infoStores.length; i++) {
      layerStats[i] = new MappingLayerStats();
    }
    for (int id = 1; id < stats.itemCount; id++) {
      CatalogPos pos = catalog.resolve(id);
      if (pos.isDeleted()) {
        ++stats.deletedItemCount;
      }
      for (int i = 0; i < infoStores.length; i++) {
        int page = pos.getInfoPage(i);
        if (page > 0) {
          pages[i].add(new Integer(page));
        }
      }
    }
    for (int i = 0; i < infoStores.length; i++) {
      layerStats[i].usedPages = pages[i].size();
      for (Iterator<Integer> iterator = pages[i].iterator(); iterator.hasNext();) {
        int page = iterator.next().intValue();
        Page p = infoStores[i].readPage(page);
        p.stats(layerStats[i]);
      }
    }
    for (int i = 0; i < infoStores.length; i++) {
      stats.pageCount += layerStats[i].usedPages;
      stats.usedPageCounts.add(layerStats[i].usedPages);
      stats.pageFillFactors.addAll(layerStats[i].pageFillFactors);
      stats.itemLength.addAll(layerStats[i].itemLength);
      stats.itemsPerPage.addAll(layerStats[i].itemsPerPage);
    }
    treeStore.stats(stats.treeStoreStats);
    catalog.stats(stats.catalogStats);
  }

  @Override
  public String toString() {
    return catalog.toString();
  }

  @Override
  public void treeCoordChanged(int id, int newPage, int newRecord, int newPos)
      throws PagedStorageException {
    CatalogPos pos = catalog.resolve(id);
    if (pos == null) {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
          "Hm, catalog does not have an entry for id " + id);
      return;
    }
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.DEPENDENT_UPDATES,
        "Item ID " + id + " new coords: <" + newPage + "," + newRecord + "," + newPos + ">");
    Data data = pos.writableData().seek(CB_ID);
    data.writeInt(newPage);
    data.writeShortInt(newRecord);
    data.writeShortInt(newPos);
  }

  @Override
  public void updatePageId(int storeId, int itemId, int newPage) {
    try {
      CatalogPos pos = catalog.resolve(itemId);
      pos.setInfoPage(storeId, newPage);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      throw new RuntimeException(exception);
    }
  }

  public synchronized void writeInfo(int id, int layerId, int[] payload)
      throws PagedStorageException {
    CatalogPos pos = catalog.resolve(id);
    if (pos == null) {
      throw new IllegalArgumentException("Catalog ID not found: " + id);
    }
    Data data = pos.readableData().seek(infoOffset(layerId));
    InfoStore infoStore = infoStores[layerId];
    InfoPos infoPos = infoStore.readPos(data);
    if (infoPos == null) {
      infoPos = infoStore.writeNew(id, payload);
      pos.setInfoPage(layerId, infoPos.getPageId());
    } else {
      if (infoPos.update(id, payload)) {
        pos.setInfoPage(layerId, infoPos.getPageId());
      }
    }
    if (!infoPos.hasItem(id)) {
      throw new AssertionError("Adding failed");
    }
  }

  @Override
  public void writeNewlyAllocatedPageId(int storeId, int itemId, int newPage) {
    try {
      CatalogPos pos = catalog.resolve(itemId);
      int oldPage = pos.getInfoPage(storeId);
      if (oldPage > 0) {
        throw new IllegalArgumentException("Item " + itemId + " on page " + pos.getPageId()
            + " already has an assigned page");
      }
      do {
        pos.setInfoPage(storeId, newPage);

        itemId = pos.getParentRowId();
        if (itemId <= 0) {
          break;
        }
        pos = catalog.resolve(itemId);
        oldPage = pos.getInfoPage(storeId);
      } while (oldPage <= 0);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      throw new RuntimeException(exception);
    }
  }

  protected void fillTreeDataForNewItem(String[] path, PageRecPos pos) {

  }

  private int infoOffset(int layerId) {
    return CB_ID + PageRecPos.SIZE + layerId * InfoPos.SIZE;
  }

  private int lookup(String[] path, boolean add) throws PagedStorageException {
    ++lookupCount;
    PageRec pageRec = treeStore.root();
    int pathLength = path.length;
    int rowId = 0;
    for (int i = 0; i < pathLength; i++) {
      String component = path[i];
      PageRecPos pos = pageRec.lookup(component, add);
      if (pos == null) {
        if (add && IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.ANOMALIES)) {
          IndexerPlugin.getLogger().trace(IndexerDebugOptions.ANOMALIES,
              "Item creation failed for path " + StringUtils.join(path));
        } else {
          IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS,
              " NOT FOUND at #" + i + " " + component);
        }
        return ID_NONE;
      }

      int parentRowId = rowId;
      rowId = pos.getRowId();
      if (rowId > 0 && catalog.resolve(rowId).isDeleted()) {
        if (add) {
          rowId = -1;
        } else {
          return ID_NONE;
        }
      }
      if (rowId <= 0) {
        CatalogPos catPos = catalog.allocateNew();
        Data data = catPos.writableData();
        pos.write(data.seek(CB_ID));
        for (int k = 0; k < infoStores.length; k++) {
          infoStores[k].writeEmptyPos(data);
        }

        catPos.setParentRowId(parentRowId);
        rowId = catPos.getRowId();
        pos.setRowId(rowId);
        fillTreeDataForNewItem(path, pos);
      }

      if (i < pathLength - 1) {
        pageRec = pos.children(add);
        if (pageRec == null) {
          if (add && IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.ANOMALIES)) {
            IndexerPlugin.getLogger().trace(IndexerDebugOptions.ANOMALIES,
                "Child record creation failed for path " + StringUtils.join(path));
          } else {
            IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS,
                " NO CHILDREN at #" + i + " " + component);
          }
          return ID_NONE;
        }
      }
    }
    return rowId;
  }
}
