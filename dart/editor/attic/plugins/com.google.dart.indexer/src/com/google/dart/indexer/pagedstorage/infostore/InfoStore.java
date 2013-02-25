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
package com.google.dart.indexer.pagedstorage.infostore;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.pagestore.Record;
import com.google.dart.indexer.pagedstorage.pagestore.RecordFactory;
import com.google.dart.indexer.pagedstorage.pagestore.StdRecord;
import com.google.dart.indexer.pagedstorage.stats.MappingLayerStats;
import com.google.dart.indexer.pagedstorage.util.ArrayUtils;
import com.google.dart.indexer.storage.paged.store.Data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InfoStore {
  public class Page extends StdRecord {
    private static final int COUNT_SIZE = 2;
    private static final int OFFSET_SIZE = 2;
    private static final int ID_SIZE = 4;
    private static final int ITEM_SIZE = 4;
    private static final int PAGE_METADATA = COUNT_SIZE + OFFSET_SIZE;
    private static final int PER_ITEM_METADATA = OFFSET_SIZE + ID_SIZE;

    private int count;
    private int[] offsets;
    private int[] ids;
    private int[][] items;
    private int totalSize;

    public Page(Data data, int pageId) {
      super(pagedStorage, data, pageId);

      data.reset();
      count = data.readShortInt();
      ids = new int[count];
      items = new int[count][];
      if (count > 0) {
        offsets = new int[count];
        for (int i = 0; i < count; i++) {
          offsets[i] = data.readShortInt();
          ids[i] = data.readInt();
        }
      }
      totalSize = data.readShortInt();
      if (totalSize == 0) {
        if (count > 0) {
          throw new AssertionError("totalSize == 0 when count > 0");
        }
        totalSize = PAGE_METADATA;
      }
    }

    public boolean addItem(int itemId, int payloadItem) {
      return addItems(itemId, new int[] {payloadItem});
    }

    public boolean addItems(int itemId, int[] payload) {
      int item = find(itemId);
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.INFOSTORE_CALLS,
          "InfoStore.Page.addItems(): page" + getPos() + " item id " + itemId + " at " + item);
      if (item < 0) {
        replace(itemId, payload, false);
        return true;
      }

      deserialize(item);

      int[] oldPayload = items[item];
      int[] newPayload = ArrayUtils.addSorted(oldPayload, payload);
      int sizeDelta = ITEM_SIZE * (newPayload.length - oldPayload.length);
      if (sizeDelta == 0) {
        return true; // no new items
      }

      deserializeAll();

      if (count == 1 && (totalSize + sizeDelta) > pagedStorage.getPageSize()) {
        return false;
      }

      items[item] = newPayload;
      totalSize += sizeDelta;
      changed();
      return true;
    }

    public int[] chooseItemsToMove() {
      deserializeAll();

      int[] additionalIds = new int[count * 100];
      int additionalIdCount = 0;

      // find parent indexes of each item
      int[] parents = new int[count * 100];
      for (int i = 0; i < count; i++) {
        int id = hierarchy.getParent(ids[i]);
        while (id > 0) {
          if (find(id) < 0) {
            int pos = binarySearch(additionalIds, additionalIdCount, id);
            if (pos < 0) {
              pos = -pos - 1;
              System.arraycopy(additionalIds, pos, additionalIds, pos + 1, additionalIdCount - pos);
              additionalIds[pos] = id;
              ++additionalIdCount;
            }
          }
          id = hierarchy.getParent(id);
        }
      }
      int virtualCount = count + additionalIdCount;

      for (int i = 0; i < count; i++) {
        int id = hierarchy.getParent(ids[i]);
        int k = i;
        while (id > 0) {
          int pos = find(id);
          if (pos >= 0) {
            parents[k] = pos;
          } else {
            pos = binarySearch(additionalIds, additionalIdCount, id);
            if (pos < 0) {
              throw new AssertionError("WTF? should have added " + id);
            }
            parents[k] = count + pos;
          }
          k = parents[k];
          id = hierarchy.getParent(id);
        }
      }

      // topological sort using iterative DFS
      int[] sorted = new int[virtualCount];
      int sortedCount = 0;
      boolean[] visited = new boolean[virtualCount];
      int[] stack = new int[virtualCount * 2];
      int stackSize;
      for (int i = 0; i < virtualCount; i++) {
        if (!visited[i]) {
          stack[0] = i;
          stackSize = 1;
          visited[i] = true;
          while (stackSize > 0) {
            int item = stack[--stackSize];
            if (item < 0) {
              // endvisit
              item = -item - 1;
              sorted[sortedCount++] = item;
            } else {
              stack[stackSize++] = -item - 1; // enqueue endvisit

              // visit children
              for (int k = 0; k < virtualCount; k++) {
                if (parents[k] == item && !visited[k]) {
                  stack[stackSize++] = k;
                  visited[k] = true;
                }
              }
            }
          }
        }
      }

      // compute deep sizes
      int[] deepSize = new int[virtualCount];
      int[] childrenCounts = new int[virtualCount];
      for (int i = 0; i < count; i++) {
        deepSize[i] = PER_ITEM_METADATA + ITEM_SIZE * items[i].length;
      }
      for (int i = 0; i < virtualCount; i++) {
        childrenCounts[i] = 1;
      }
      for (int i = 0; i < virtualCount; i++) {
        int item = sorted[i];
        if (parents[item] >= 0) {
          deepSize[parents[item]] += deepSize[item];
          childrenCounts[parents[item]] += childrenCounts[item];
        }
      }

      // choose the item to move
      int idealSize = pagedStorage.getPageSize() / 2;
      int bestDeviation = -1;
      int bestItem = -1;
      for (int i = 0; i < virtualCount; i++) {
        int deviation = Math.abs(idealSize - (totalSize - deepSize[i]));
        if (bestItem == -1 || deviation < bestDeviation) {
          bestDeviation = deviation;
          bestItem = i;
        }
      }

      if (childrenCounts[bestItem] == virtualCount) {
        if (count > 1) {
          IndexerPlugin.getLogger().trace(
              IndexerDebugOptions.MISCELLANEOUS,
              "InfoStore.Page.chooseItemsToMove() hmmmm, can only happen if a single item is left, but count="
                  + count);
        }
        return null;
      }

      int[] itemsToMove = new int[childrenCounts[bestItem]];
      int itemsToMoveCount = 0;
      Set<Integer> itemsToMoveSet = new HashSet<Integer>();
      itemsToMoveSet.add(new Integer(bestItem));
      itemsToMove[itemsToMoveCount++] = bestItem;
      for (int i = virtualCount - 1; i >= 0; --i) {
        int item = sorted[i];
        int parent = parents[item];
        if (parent >= 0 && itemsToMoveSet.contains(new Integer(parent))) {
          itemsToMoveSet.add(new Integer(item));
          itemsToMove[itemsToMoveCount++] = item;
        }
      }
      if (itemsToMoveCount != childrenCounts[bestItem]) {
        throw new AssertionError("Internal alg error: itemsToMoveCount != childrenCounts[bestItem]");
      }

      for (int i = 0; i < itemsToMove.length; i++) {
        if (itemsToMove[i] < count) {
          itemsToMove[i] = ids[itemsToMove[i]];
        } else {
          itemsToMove[i] = additionalIds[itemsToMove[i] - count];
        }
      }

      Arrays.sort(itemsToMove);

      return itemsToMove;
    }

    public void delete(int itemId) {
      int item = find(itemId);
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.INFOSTORE_CALLS,
          "InfoStore.Page.delete(): page " + getPos() + " item id " + itemId + " at " + item);
      if (item < 0) {
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.ANOMALIES,
            "InfoStore.delete: item id " + itemId + " does not exist on page " + getPos());
        return;
      }

      deserializeAll();
      totalSize -= PER_ITEM_METADATA + ITEM_SIZE * items[item].length;
      items = ArrayUtils.remove(items, item);
      ids = ArrayUtils.remove(ids, item);
      --count;

      changed();
      // if (count > 0)
      // changed();
      // else
      // try {
      // pagedStorage.freePage(getPos());
      // } catch (PagedStorageException exception) {
      // IndexerPlugin.getLogger().logError(exception);
      // }
    }

    public boolean fits() {
      return totalSize <= pagedStorage.getPageSize();
    }

    public int getCount() {
      return count;
    }

    public double getFillFactor() {
      return totalSize * 1.0 / pagedStorage.getPageSize();
    }

    public InfoStore getInfoStore() {
      return InfoStore.this;
    }

    public boolean hasItem(int id) {
      return find(id) >= 0;
    }

    public boolean itemsToMoveContainsId(int[] itemsToMove, int itemId) {
      return Arrays.binarySearch(itemsToMove, itemId) >= 0;
    }

    public int maximumPayloadLength() {
      return (pagedStorage.getPageSize() - PAGE_METADATA - PER_ITEM_METADATA) / ITEM_SIZE - 10;
    }

    public void move(int[] itemsToMove, Page newPage) {
      deserializeAll();
      for (int i = 0; i < itemsToMove.length; i++) {
        int item = find(itemsToMove[i]);
        if (item >= 0) {
          newPage.replace(itemsToMove[i], items[item], false);
        }
      }
      for (int i = 0; i < itemsToMove.length; i++) {
        if (find(itemsToMove[i]) >= 0) {
          delete(itemsToMove[i]);
        }
      }
      for (int i = 0; i < itemsToMove.length; i++) {
        hierarchy.updatePageId(storeId, itemsToMove[i], newPage.getPos());
      }
    }

    public int[] read(int itemId) {
      int item = find(itemId);
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.INFOSTORE_CALLS,
          "InfoStore.Page.read(): page " + getPos() + " item id " + itemId + " at " + item);
      if (item < 0) {
        // throw new IllegalArgumentException("Item id " + itemId +
        // " not found on page " + getPos());
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.ANOMALIES,
            "InfoStore$Page.read: item id " + itemId + " not found on page " + getPos());
        return new int[0];
      }
      deserialize(item);
      return items[item];
    }

    public void replace(int itemId, int[] payload, boolean shouldExist) {
      int item = find(itemId);
      IndexerPlugin.getLogger().trace(
          IndexerDebugOptions.INFOSTORE_CALLS,
          "InfoStore.Page.replace" + (shouldExist ? "Existing" : "New") + "(): page " + getPos()
              + " item id " + itemId + " at " + item);
      // if (shouldExist && item < 0)
      // throw new IllegalArgumentException("Item id " + itemId +
      // " does not exist on page " + getPos());
      if (!shouldExist && item >= 0) {
        throw new IllegalArgumentException("Item id " + itemId + " already exists on page "
            + getPos());
      }

      Arrays.sort(payload);

      deserializeAll();

      int oldSize = 0;
      if (item >= 0) {
        oldSize = PER_ITEM_METADATA + ITEM_SIZE * items[item].length;
      }
      int newSize = PER_ITEM_METADATA + ITEM_SIZE * payload.length;
      int sizeDelta = newSize - oldSize;

      if (item < 0) {
        item = -item - 1;
        items = ArrayUtils.add(items, payload, item);
        ids = ArrayUtils.add(ids, itemId, item);
        ++count;
      } else {
        items[item] = payload;
      }
      totalSize += sizeDelta;
      changed();
    }

    public void stats(MappingLayerStats stats) {
      deserializeAll();
      for (int i = 0; i < count; i++) {
        stats.itemLength.add(items[i].length);
      }
      stats.itemsPerPage.add(count);
      stats.pageFillFactors.add(getFillFactor() * 100);
    }

    @Override
    protected void changed() {
      updateOffsets();
    }

    @Override
    protected void serializeCached() {
      deserializeAll();

      updateOffsets();

      data.reset();
      data.writeShortInt(count);
      for (int i = 0; i < count; i++) {
        data.writeShortInt(offsets[i]);
        data.writeInt(ids[i]);
      }
      data.writeShortInt(totalSize);
      for (int i = 0; i < count; i++) {
        int[] payload = items[i];
        int len = payload.length;
        for (int k = 0; k < len; k++) {
          data.writeInt(payload[k]);
        }
      }
    }

    private void deserialize(int item) {
      if (item < 0 || item > count) {
        throw new IllegalArgumentException("Info page " + getPos() + ": illegal item id " + item
            + ", max is " + count);
      }
      if (items[item] != null) {
        return;
      }
      data.setPos(offsets[item]);
      int cb = itemSize(item);
      if (cb % 4 != 0) {
        throw new AssertionError("Invalid size " + cb + " of item " + item + " on info page "
            + getPos());
      }
      int length = cb / 4;;
      int[] result = new int[length];
      for (int i = 0; i < length; i++) {
        result[i] = data.readInt();
      }
      items[item] = result;
    }

    private void deserializeAll() {
      if (offsets == null) {
        return;
      }
      for (int i = 0; i < count; i++) {
        if (items[i] == null) {
          deserialize(i);
        }
      }
      offsets = null;
    }

    private int find(int itemId) {
      return Arrays.binarySearch(ids, itemId);
    }

    private int itemSize(int item) {
      if (item == count - 1) {
        return totalSize - offsets[item];
      } else {
        return offsets[item + 1] - offsets[item];
      }
    }

    private void updateOffsets() {
      int offset = COUNT_SIZE + count * PER_ITEM_METADATA + OFFSET_SIZE;
      offsets = new int[count];
      for (int i = 0; i < count; i++) {
        offsets[i] = offset;
        offset += ITEM_SIZE * items[i].length;
      }
      if (offset != totalSize) {
        throw new AssertionError("totalSize != final offset: " + totalSize + " != " + offset);
      }
    }
  }

  static int binarySearch(int[] a, int count, int key) {
    int low = 0;
    int high = count - 1;

    while (low <= high) {
      int mid = (low + high) >> 1;
      int midVal = a[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        return mid; // key found
      }
    }
    return -(low + 1); // key not found.
  }

  private final PagedStorage pagedStorage;

  private InfoStoreItemsHierarchy hierarchy;

  private int storeId;

  private RecordFactory recordFactory = new RecordFactory() {

    @Override
    public Record read(PagedStorage pagedStorage, Data data, int pageId, boolean isNew)
        throws PagedStorageException {
      return new Page(data, pageId);
    }

  };

  public InfoStore(PagedStorage pagedStorage) {
    if (pagedStorage == null) {
      throw new NullPointerException("pageStore is null");
    }
    this.pagedStorage = pagedStorage;
  }

  public InfoPos addItem(int page, int itemId, int payloadItem) throws PagedStorageException {
    Page p = readPage(page);
    if (p.addItem(itemId, payloadItem)) {
      p = makePageFit(itemId, p);
    }
    return new InfoPos(p);
  }

  public InfoPos addItems(int page, int itemId, int[] payload) throws PagedStorageException {
    Page p = readPage(page);
    if (p.addItems(itemId, payload)) {
      p = makePageFit(itemId, p);
    }
    return new InfoPos(p);
  }

  public Page readPage(int pageId) throws PagedStorageException {
    return (Page) pagedStorage.readRecord(pageId, recordFactory);
  }

  public InfoPos readPos(Data data) throws PagedStorageException {
    int pageId = data.readInt();
    if (pageId <= 0) {
      return null;
    }
    return new InfoPos(readPage(pageId));
  }

  public InfoPos replace(int page, int itemId, int[] payload, boolean shouldExist)
      throws PagedStorageException {
    Page p = readPage(page);

    if (payload.length > p.maximumPayloadLength()) {
      payload = ArrayUtils.truncate(payload, p.maximumPayloadLength());
    }

    // if (DebugConstants.INFOSTORE_EACH_ITEM_ON_SEPARATE_PAGE && !shouldExist)
    // {
    // if (page > 0)
    // p = readPage(-1);
    // p.replace(itemId, payload, shouldExist);
    // if (!p.fits())
    // throw new AssertionError("Cannot add payload of size " + payload.length +
    // " to an empty page with maxPayloadLenth=" + p.maximumPayloadLength());
    // return new InfoPos(p);
    // }

    p.replace(itemId, payload, shouldExist);
    p = makePageFit(itemId, p);
    return new InfoPos(p);
  }

  public void setHierarchy(InfoStoreItemsHierarchy hierarchy, int storeId) {
    if (hierarchy == null) {
      throw new NullPointerException("hierarchy is null");
    }
    this.hierarchy = hierarchy;
    this.storeId = storeId;
  }

  public void writeEmptyPos(Data data) {
    data.writeInt(0);
  }

  public InfoPos writeNew(int itemId, int[] payload) throws PagedStorageException {
    int page = hierarchy.findPageOf(storeId, itemId);
    InfoPos newPos = replace(page, itemId, payload, false);
    if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.INFOSTORE_MICROSTATS)) {
      if (page > 0 && newPos.getPageId() == page) {
        IndexerPlugin.getLogger().trace(
            IndexerDebugOptions.INFOSTORE_MICROSTATS,
            "New item " + itemId + " (size " + payload.length + ") fits on existing page " + page
                + " which now has " + newPos.getPage().getCount() + " items.");
      } else if (page > 0 && newPos.getPageId() != page) {
        IndexerPlugin.getLogger().trace(
            IndexerDebugOptions.INFOSTORE_MICROSTATS,
            "New item " + itemId + " (size " + payload.length + ") did not fit on existing page "
                + page + ".");
      } else {
        IndexerPlugin.getLogger().trace(
            IndexerDebugOptions.INFOSTORE_MICROSTATS,
            "New item " + itemId + " (size " + payload.length
                + ") saved to a new page, because no candidate page existed.");
      }
    }
    if (newPos.getPageId() != page) {
      if (page <= 0) {
        hierarchy.writeNewlyAllocatedPageId(storeId, itemId, newPos.getPageId());
      } else {
        hierarchy.updatePageId(storeId, itemId, newPos.getPageId());
      }
    }
    return newPos;
  }

  private Page makePageFit(int itemId, Page p) throws PagedStorageException {
    while (!p.fits()) {
      try {
        int[] itemsToMove = p.chooseItemsToMove();

        boolean willMoveCurrent = p.itemsToMoveContainsId(itemsToMove, itemId);
        Page newPage = readPage(-1);
        p.move(itemsToMove, newPage);

        IndexerPlugin.getLogger().trace(
            IndexerDebugOptions.INFOSTORE_SPLITS,
            "Moving " + itemsToMove.length + " items from page " + p.getPos() + " to page "
                + newPage.getPos());

        if (willMoveCurrent) {
          Page t = p;
          p = newPage;
          newPage = t;
        }
        makePageFit(itemId, newPage); // dunno, but maybe newPage may need
                                      // fitting too

      } catch (RuntimeException exception) {
        IndexerPlugin.getLogger().logError(exception);
        throw exception;
      } catch (Error exception) {
        IndexerPlugin.getLogger().logError(exception);
        throw exception;
      }
      // if (DebugConstants.TRACE_INFOSTORE_SPLITS)
      // if (!shouldExist) {
      // IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
      // "New item " + itemId + " (payload size " +
      // payload.length + ") does not fit on page "
      // + p.getPos() + " with " + p.getCount() +
      // " items, allocating new page");
      // } else {
      // IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
      // "Existing item " + itemId + " (payload size " +
      // payload.length + ") no longer fits on page "
      // + p.getPos() + " with " + p.getCount() +
      // " items, moving to a new page");
      // }
    }
    return p;
  }
}
