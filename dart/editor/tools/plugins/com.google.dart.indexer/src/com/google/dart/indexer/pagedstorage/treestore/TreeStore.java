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
package com.google.dart.indexer.pagedstorage.treestore;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.pagestore.Record;
import com.google.dart.indexer.pagedstorage.pagestore.RecordFactory;
import com.google.dart.indexer.pagedstorage.pagestore.StdRecord;
import com.google.dart.indexer.pagedstorage.util.ArrayUtils;
import com.google.dart.indexer.pagedstorage.util.StringUtils;
import com.google.dart.indexer.storage.paged.TreeStoreStats;
import com.google.dart.indexer.storage.paged.store.Data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TreeStore {
  class LeafIterator implements Iterator<TreeLeaf> {
    private static final int MAX_LEVEL = 20;

    private String[] components;
    private Page[] page;
    private int[] record, index;
    private int level;
    private boolean hasNext;

    public LeafIterator() {
      page = new Page[MAX_LEVEL];
      record = new int[MAX_LEVEL];
      index = new int[MAX_LEVEL];
      components = new String[MAX_LEVEL];
      level = -1;
      hasNext = moveDownOrRight(rootPage, 0, 0);
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public TreeLeaf next() {
      String[] path = new String[level + 1];
      System.arraycopy(components, 0, path, 0, path.length);
      TreeLeaf result = new TreeLeaf(path, new PageRecPos(page[level], record[level], index[level]));
      hasNext = moveRightOrUp();
      return result;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    boolean moveDown(Page p, int r, int i) {
      if (i >= p.itemCount(r)) {
        return false;
      }
      push(p, r, i);
      int children = p.getChildren(r, i);
      if (children == 0) {
        return true; // leaf
      }
      Page pp;
      int rr;
      if (children >= PAGE_MARKER) {
        try {
          pp = readPage(children - PAGE_MARKER);
        } catch (PagedStorageException e) {
          IndexerPlugin.getLogger().logError(e);
          return false;
        }
        rr = 0;
      } else {
        pp = page[level];
        rr = children;
      }
      if (moveDownOrRight(pp, rr, 0)) {
        return true;
      }
      pop();
      return false;
    }

    boolean moveDownOrRight(Page p, int r, int i) {
      if (moveDown(p, r, i)) {
        return true;
      }
      return moveRight(p, r, i);
    }

    boolean moveRight(Page p, int r, int i) {
      int itemCount = p.itemCount(r);
      while (++i < itemCount) {
        if (moveDown(p, r, i)) {
          return true;
        }
      }
      return false;
    }

    boolean moveRightOrUp() {
      while (level >= 0) {
        Page p = page[level];
        int r = record[level];
        int i = index[level];
        pop();
        if (moveRight(p, r, i)) {
          return true;
        }
      }
      return false;
    }

    void pop() {
      --level;
    }

    void push(Page p, int r, int i) {
      ++level;
      // TODO extend when needed
      page[level] = p;
      record[level] = r;
      index[level] = i;
      components[level] = p.readName(r, i);
    }
  }

  /**
   * <p>
   * Page: N off1 off2 ... offN offL rec1 rec2 ... recN
   * </p>
   * <p>
   * Each record: N off1 child1 off2 child2 ... offN childN offL data1 data2 ... dataN
   * </p>
   */
  class Page extends StdRecord {
    private static final int COUNT_LEN = 2;
    private static final int OFFSET_LEN = 2;
    private static final int CHILDREN_LEN = 2;

    private static final int CB_EMPTY_PAGE_METADATA = COUNT_LEN + OFFSET_LEN /*
                                                                              * N, offL
                                                                              */;
    private static final int CB_PAGE_METADATA_PER_RECORD = OFFSET_LEN /* offX */;
    // private static final int CB_RECORD_PREFIX_METADATA = COUNT_LEN /* N */;
    private static final int CB_EMPTY_RECORD_METADATA = COUNT_LEN + OFFSET_LEN;
    private static final int CB_RECORD_METADATA_PER_ITEM = OFFSET_LEN + CHILDREN_LEN;

    private int recordCount;
    private int[] recordOffsets;

    private int totalSize;
    private int[] itemCounts;
    private int[][] itemOffsets;
    private int[][] itemChildren;
    private int[][] itemRowIds;
    private String[][] itemNames;
    private byte[][][] itemNamesEncoded;
    private long[][] itemTimeStamps;

    public Page(Data data, int pageId) {
      super(pagedStorage, data, pageId);
      data.reset();
      recordCount = data.readShortInt();
      recordOffsets = new int[recordCount + 1];
      for (int i = 0; i <= recordCount; i++) {
        recordOffsets[i] = data.readShortInt();
      }

      if (recordCount == 0) {
        recordOffsets = new int[1];
        recordOffsets[0] = CB_EMPTY_PAGE_METADATA /* ??? */;
      }
      totalSize = recordOffsets[recordCount];
      itemCounts = new int[recordCount];
      itemOffsets = new int[recordCount][];
      itemChildren = new int[recordCount][];
      itemRowIds = new int[recordCount][];
      if (additionalData >= 12) {
        itemTimeStamps = new long[recordCount][];
      }
      itemNames = new String[recordCount][];
      itemNamesEncoded = new byte[recordCount][][];
    }

    public int addChildRecord(int recordSize) {
      if (totalSize + recordSize + CB_PAGE_METADATA_PER_RECORD > pagedStorage.getPageSize()) {
        return ADD_NOROOM;
      }

      deserializeAll();

      int newRecord = recordCount;
      recordCount += 1;
      totalSize += CB_PAGE_METADATA_PER_RECORD + recordSize;
      changed();

      return newRecord;
    }

    public int addEmptyChildRecord() {
      int newRecord = addChildRecord(CB_EMPTY_RECORD_METADATA);
      if (newRecord < 0) {
        return newRecord;
      }
      itemCounts = ArrayUtils.add(itemCounts, 0, newRecord);
      itemOffsets = ArrayUtils.add(itemOffsets, null, newRecord);
      itemChildren = ArrayUtils.add(itemChildren, new int[0], newRecord);
      itemRowIds = ArrayUtils.add(itemRowIds, new int[0], newRecord);
      if (itemTimeStamps != null) {
        itemTimeStamps = ArrayUtils.add(itemTimeStamps, new long[0], newRecord);
      }
      itemNames = ArrayUtils.add(itemNames, new String[0], newRecord);
      itemNamesEncoded = ArrayUtils.add(itemNamesEncoded, new byte[0][], newRecord);

      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
      return newRecord;
    }

    public int addEmptyChildRecord(int record, int item) {
      checkItem(record, item);

      int newRecord = addEmptyChildRecord();
      if (newRecord < 0) {
        return newRecord;
      }
      itemChildren[record][item] = newRecord;

      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
      return newRecord;
    }

    public Page addEmptyPageWithAnEmptyChildRecord() throws PagedStorageException {
      Page page = readPage(-1);
      int rec = page.addEmptyChildRecord();
      if (rec != 0) {
        throw new AssertionError("First record of an empty page is expected to be 0");
      }
      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
      return page;
    }

    public int additionalDataSize() {
      return additionalData;
    }

    public Page createSubpage() throws PagedStorageException {
      return (Page) pagedStorage.readRecord(-1, recordFactory);
    }

    public void delete(int record) {
      if (record == 0) {
        throw new IllegalArgumentException("Cannot delete record 0");
      }
      if (record < 0 || record >= recordCount) {
        throw new IllegalArgumentException("Invalid record ID " + record);
      }
      deserializeAll();

      int size = CB_PAGE_METADATA_PER_RECORD + CB_EMPTY_RECORD_METADATA
          + CB_RECORD_METADATA_PER_ITEM * itemCounts[record];
      for (int item = 0; item < itemCounts[record]; item++) {
        size += additionalData + itemNamesEncoded[record][item].length;
      }
      for (int rec = 0; rec < recordCount; rec++) {
        int count = itemCounts[rec];
        for (int item = 0; item < count; item++) {
          if (itemChildren[rec][item] == record) {
            throw new IllegalArgumentException(
                "Cannot delete a record that is still referenced by another record");
          }
        }
      }

      for (int rec = 0; rec < recordCount; rec++) {
        int count = itemCount(rec);
        for (int item = 0; item < count; item++) {
          int child = itemChildren[rec][item];
          if (child > 0 && child < PAGE_MARKER && child > record) {
            itemChildren[rec][item] = child - 1;
          }
        }
      }

      itemCounts = ArrayUtils.remove(itemCounts, record);
      itemChildren = ArrayUtils.remove(itemChildren, record);
      itemRowIds = ArrayUtils.remove(itemRowIds, record);
      if (itemTimeStamps != null) {
        itemTimeStamps = ArrayUtils.remove(itemTimeStamps, record);
      }
      itemNames = ArrayUtils.remove(itemNames, record);
      itemNamesEncoded = ArrayUtils.remove(itemNamesEncoded, record);
      totalSize -= size;
      --recordCount;
      if (recordCount == 0) {
        throw new AssertionError("Record count is zero after record deletion");
      }
      changed();
      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
    }

    public void deleteItem(int record, int item) throws PagedStorageException {
      checkItem(record, item);
      deserializeAll();

      int nameLength = itemNamesEncoded[record][item].length;
      itemChildren[record] = ArrayUtils.remove(itemChildren[record], item);
      itemRowIds[record] = ArrayUtils.remove(itemRowIds[record], item);
      if (itemTimeStamps != null) {
        itemTimeStamps[record] = ArrayUtils.remove(itemTimeStamps[record], item);
      }
      itemNames[record] = ArrayUtils.remove(itemNames[record], item);
      itemNamesEncoded[record] = ArrayUtils.remove(itemNamesEncoded[record], item);
      --itemCounts[record];
      totalSize -= CB_RECORD_METADATA_PER_ITEM + additionalData + nameLength;
      notifyRecordItemsPosChanged(record, item);

      changed();
      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
    }

    public void dump(List<Page> pagesToDump, StringBuilder out, int level, boolean detailed)
        throws PagedStorageException {
      out.append(StringUtils.indent(level));
      out.append("PAGE ").append(pageId).append(": ").append(recordCount).append(" records\n");
      for (int rec = 0; rec < recordCount; rec++) {
        dumpRecord(pagesToDump, out, rec, level + 1, detailed);
      }
    }

    public int find(int record, String name) {
      if (record < 0 || record >= recordCount) {
        throw new IllegalArgumentException("Invalid record: " + record);
      }

      deserializeRecordEntirely(record);

      int count = itemCounts[record];
      int l = 0, r = count;
      while (l < r) {
        int m = (l + r) >>> 1;
        int cmp = itemNames[record][m].compareTo(name);
        if (cmp == 0) {
          return m;
        }
        if (cmp < 0) {
          l = m + 1;
        } else {
          r = m;
        }
      }
      return -l - 1;
    }

    public int findLargestRecord() {
      if (recordCount < 2) {
        return -1;
      }

      updateRecordOffsets();

      int maxRecord = 1;
      int maxRecordSize = recordOffsets[2] - recordOffsets[1];
      for (int rec = 2; rec < recordCount; rec++) {
        int size = recordOffsets[rec + 1] - recordOffsets[rec];
        if (size > maxRecordSize) {
          maxRecord = rec;
          maxRecordSize = size;
        }
      }
      return maxRecord;
    }

    public int getChildren(int record, int index) {
      deserializeMeta(record);
      return itemChildren[record][index];
    }

    public int getRecordCount() {
      return recordCount;
    }

    public int getRowId(int record, int item) {
      deserializeRecordEntirely(record);
      return itemRowIds[record][item];
    }

    public long getTimestamp(int record, int item) {
      deserializeRecordEntirely(record);
      return itemTimeStamps[record][item];
    }

    public TreeStore getTreeStore() {
      return TreeStore.this;
    }

    public int itemCount(int record) {
      deserializeMeta(record);
      return itemCounts[record];
    }

    public int[] move(int record, Page targetPage) throws PagedStorageException {
      if (record < 0 || record >= recordCount) {
        throw new IllegalArgumentException("Invalid record: " + record);
      }
      if (record == 0) {
        throw new IllegalArgumentException("Cannot move record 0");
      }
      if (targetPage.getRecordCount() > 0) {
        throw new IllegalArgumentException("Can only move records to an empty page");
      }

      deserializeAll();

      boolean[] marks = new boolean[recordCount];
      int[] newRecords = new int[recordCount];
      for (int i = 0; i < newRecords.length; ++i) {
        newRecords[i] = -1;
      }
      int newRecord = recursiveCopy(record, targetPage, marks, newRecords);
      if (newRecord != 0) {
        throw new AssertionError("Top-level moved record should have become record 0");
      }

      fixupChildReferences(record, PAGE_MARKER + targetPage.getPos());

      for (int rec = recordCount - 1; rec > 0; --rec) {
        if (marks[rec]) {
          delete(rec);
        }
      }

      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
        targetPage.consistencyCheck();
      }

      for (int rec = 1; rec < recordCount; rec++) {
        notifyRecordItemsPosChanged(rec, 0);
      }

      for (int rec = 0; rec < targetPage.getRecordCount(); rec++) {
        targetPage.notifyRecordItemsPosChanged(rec, 0);
      }

      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
        targetPage.consistencyCheck();
      }

      return newRecords;
    }

    public String readName(int record, int item) {
      deserializeMeta(record);
      checkItem(record, item);
      return itemNames[record][item];
    }

    public void setChildPage(int record, int index, int pageId) {
      checkItem(record, index);
      deserializeAll();
      itemChildren[record][index] = PAGE_MARKER + pageId;
      changed();
      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
    }

    public void setRowId(int record, int item, int rowId) {
      deserializeAll();
      itemRowIds[record][item] = rowId;
      changed();
    }

    public void setTimestamp(int record, int item, long timestamp) {
      deserializeAll();
      itemTimeStamps[record][item] = timestamp;
      changed();
    }

    public void stats(TreeStoreStats stats, List<Page> pagesToDump) throws PagedStorageException {
      for (int rec = 0; rec < recordCount; rec++) {
        statsRecord(stats, pagesToDump, rec);
      }
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      try {
        dump(new ArrayList<Page>(), result, 0, true);
      } catch (PagedStorageException exception) {
        IndexerPlugin.getLogger().logError(exception);
      }
      return result.toString();
    }

    public int tryAdd(int record, String name) throws PagedStorageException {
      if (record >= recordCount) {
        throw new IllegalArgumentException("Invalid record: " + record);
      }
      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
      byte[] nameEncoded = Data.encodeStringAndCopy(name, new byte[10240]);
      int size = CB_RECORD_METADATA_PER_ITEM + additionalData + nameEncoded.length;
      if (totalSize + size > pagedStorage.getPageSize()) {
        return ADD_NOROOM;
      }
      deserializeAll();

      // decide a position to insert at
      int item = find(record, name);
      if (item >= 0) {
        return ADD_EXISTS;
      }
      item = -(item + 1);

      itemChildren[record] = ArrayUtils.add(itemChildren[record], 0, item);
      itemRowIds[record] = ArrayUtils.add(itemRowIds[record], 0, item);
      if (itemTimeStamps != null) {
        itemTimeStamps[record] = ArrayUtils.add(itemTimeStamps[record], 0, item);
      }
      itemNames[record] = ArrayUtils.add(itemNames[record], name, item);
      itemNamesEncoded[record] = ArrayUtils.add(itemNamesEncoded[record], nameEncoded, item);
      totalSize += size;
      ++itemCounts[record];

      if (coordListener != null) {
        for (int k = item + 1; k < itemCounts[record]; k++) {
          int id = itemRowIds[record][k];
          coordListener.treeCoordChanged(id, this.getPos(), record, k);
        }
      }
      changed();
      if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_CONSISTENCY)) {
        consistencyCheck();
      }
      return 1;
    }

    @Override
    protected void serializeCached() {
      if (recordOffsets != null) {
        return;
      }

      updateRecordOffsets();

      data.reset();
      data.writeShortInt(recordCount);
      for (int i = 0; i <= recordCount; i++) {
        data.writeShortInt(recordOffsets[i]);
      }

      for (int record = 0; record < recordCount; record++) {
        if (data.getPos() != recordOffsets[record]) {
          throw new AssertionError("TreeStore page record offset calculation failed (at record "
              + record + ")");
        }

        int count = itemCounts[record];
        int[] offsets = itemOffsets[record];
        int[] children = itemChildren[record];
        int[] rowIds = itemRowIds[record];
        long[] timeStamps = (itemTimeStamps == null ? null : itemTimeStamps[record]);
        byte[][] namesEncoded = itemNamesEncoded[record];

        data.writeShortInt(count);
        for (int item = 0; item < count; item++) {
          data.writeShortInt(offsets[item]);
          data.writeShortInt(children[item]);
        }
        data.writeShortInt(offsets[count]);

        for (int item = 0; item < count; item++) {
          if (data.getPos() != recordOffsets[record] + offsets[item]) {
            throw new AssertionError("TreeStore page item offset calculation failed (at item "
                + item + " of rec " + record + ")");
          }
          data.writeInt(rowIds[item]);
          if (timeStamps != null) {
            data.writeLong(timeStamps[item]);
          }
          data.write(namesEncoded[item]);
        }
        if (data.getPos() != recordOffsets[record] + offsets[count]) {
          throw new AssertionError("TreeStore page item offset calculation failed (at count="
              + count + " of record " + record + ")");
        }
      }
      if (data.getPos() != recordOffsets[recordCount]) {
        throw new AssertionError("TreeStore page record offset calculation failed (at recordCount="
            + recordCount + ")");
      }
    }

    private int addChildRecord(int size, Page page, int record) {
      int newRecord = addChildRecord(size);
      if (newRecord < 0) {
        return newRecord;
      }

      itemCounts = ArrayUtils.add(itemCounts, page.itemCounts[record], newRecord);
      itemOffsets = ArrayUtils.add(itemOffsets, page.itemOffsets[record], newRecord);
      itemChildren = ArrayUtils.add(itemChildren, page.itemChildren[record], newRecord);
      itemRowIds = ArrayUtils.add(itemRowIds, page.itemRowIds[record], newRecord);
      if (itemTimeStamps != null) {
        itemTimeStamps = ArrayUtils.add(itemTimeStamps, page.itemTimeStamps[record], newRecord);
      }
      itemNames = ArrayUtils.add(itemNames, page.itemNames[record], newRecord);
      itemNamesEncoded = ArrayUtils.add(itemNamesEncoded, page.itemNamesEncoded[record], newRecord);

      return newRecord;
    }

    private void checkItem(int record, int item) {
      if (record < 0 || record >= recordCount) {
        throw new IllegalArgumentException("Invalid record: " + record);
      }
      if (item < 0 || item >= itemCounts[record]) {
        throw new IllegalArgumentException("Invalid index " + item + " in record " + record);
      }
    }

    private void consistencyCheck() {
      updateRecordOffsets();

      for (int rec = 0; rec < recordCount; ++rec) {
        int count = itemCount(rec);
        for (int index = 0; index < count; ++index) {
          int child = getChildren(rec, index);
          if (child < 0) {
            throw new AssertionError("Consistency check failed for page " + getPos() + ", rec "
                + rec + ", item " + index + ": invalid child: " + child);
          } else if (child > 0 && child < PAGE_MARKER) {
            if (child > recordCount) {
              throw new AssertionError("Consistency check failed for page " + getPos() + ", rec "
                  + rec + ", item " + index + ": child does not exist: " + child);
            }
          }
        }
      }
    }

    private void deserializeAll() {
      if (recordOffsets == null) {
        return;
      }
      for (int record = 0; record < recordCount; record++) {
        deserializeRecordEntirely(record);
        itemOffsets[record] = null;
      }
      recordOffsets = null;
    }

    private void deserializeMeta(int record) {
      deserializeRecordEntirely(record);
    }

    private void deserializeRecordEntirely(int record) {
      if (record < 0 || record >= recordCount) {
        throw new IllegalArgumentException("Invalid record: " + record);
      }

      if (recordOffsets == null || itemChildren[record] != null) {
        return;
      }

      data.setPos(recordOffsets[record]);
      int count = itemCounts[record] = data.readShortInt();

      int[] offsets = itemOffsets[record] = new int[count + 1];
      int[] children = itemChildren[record] = new int[count];
      int[] rowIds = itemRowIds[record] = new int[count];
      long[] timeStamps = (itemTimeStamps == null ? null
          : (itemTimeStamps[record] = new long[count]));
      String[] names = itemNames[record] = new String[count];
      byte[][] namesEncoded = itemNamesEncoded[record] = new byte[count][];

      for (int item = 0; item < count; item++) {
        offsets[item] = data.readShortInt();
        children[item] = data.readShortInt();
      }
      offsets[count] = data.readShortInt();

      for (int item = 0; item < count; item++) {
        data.setPos(recordOffsets[record] + offsets[item]);
        rowIds[item] = data.readInt();
        if (timeStamps != null) {
          timeStamps[item] = data.readLong();
        }
        int nameLength = offsets[item + 1] - offsets[item] - additionalData;

        byte[] nameEncoded = namesEncoded[item] = new byte[nameLength];
        data.read(nameEncoded, 0, nameEncoded.length);
        names[item] = Data.decodeString(nameEncoded, nameEncoded.length);
      }
    }

    private void dumpItem(List<Page> pagesToDump, StringBuilder out, int rec, int index, int level,
        boolean detailed) throws PagedStorageException {
      int child = itemChildren[rec][index];
      out.append(StringUtils.indent(level)).append("Item").append(index).append(":");
      out.append(" child ").append(
          child == 0 ? "NONE" : (child < PAGE_MARKER ? "Rec" + child : "Page"
              + (child - PAGE_MARKER)));
      if (child > PAGE_MARKER) {
        Page childPage = readPage(child - PAGE_MARKER);
        pagesToDump.add(childPage);
      }
      out.append(" ").append('"').append(itemNames[rec][index]).append('"');
      out.append(" 0 0 0 ").append(itemRowIds[rec][index]);
      out.append("\n");
    }

    private void dumpRecord(List<Page> pagesToDump, StringBuilder out, int rec, int level,
        boolean detailed) throws PagedStorageException {
      deserializeRecordEntirely(rec);

      int count = itemCounts[rec];
      out.append(StringUtils.indent(level)).append("Rec").append(rec).append(": ").append(count).append(
          " items");
      out.append("\n");
      for (int i = 0; i < count; i++) {
        dumpItem(pagesToDump, out, rec, i, level + 1, detailed);
      }
    }

    private void fixupChildReferences(int oldChild, int newChild) {
      for (int rec = 0; rec < recordCount; rec++) {
        if (rec == oldChild) {
          continue;
        }
        int count = itemCount(rec);
        for (int item = 0; item < count; item++) {
          if (getChildren(rec, item) == oldChild) {
            setChildrenX(rec, item, newChild);
          }
        }
      }
    }

    private void notifyRecordItemsPosChanged(int record, int lowerItem)
        throws PagedStorageException {
      int count = itemCount(record);
      if (coordListener != null) {
        for (int item = lowerItem; item < count; item++) {
          int id = itemRowIds[record][item];
          coordListener.treeCoordChanged(id, this.getPos(), record, item);
        }
      }
    }

    private int recordSize(int record) {
      int count = itemCounts[record];
      String[] names = itemNames[record];

      int size = CB_EMPTY_RECORD_METADATA + CB_RECORD_METADATA_PER_ITEM * count;
      for (int item = 0; item < count; item++) {
        size += additionalData + Data.encodeString(names[item], null);
      }
      return size;
    }

    private int recursiveCopy(int record, Page targetPage, boolean[] marks, int[] newRecords) {
      int newRecord = shallowCopy(record, targetPage);
      marks[record] = true;
      newRecords[record] = newRecord;

      int count = itemCount(record);
      for (int item = 0; item < count; item++) {
        int child = getChildren(record, item);
        if (child > 0 && child < PAGE_MARKER) {
          int newChild = recursiveCopy(child, targetPage, marks, newRecords);
          targetPage.setChildrenX(newRecord, item, newChild);

          // remove our child ref so that delete() does not complain
          // (completely unnecessary algorithm-wise)
          setChildrenX(record, item, 0);
        }
      }

      return newRecord;
    }

    private void setChildrenX(int rec, int item, int child) {
      itemChildren[rec][item] = child;
    }

    private int shallowCopy(int record, Page targetPage) throws AssertionError {
      int size = recordSize(record);
      return targetPage.addChildRecord(size, this, record);
    }

    private void statsItem(TreeStoreStats stats, List<Page> pagesToDump, int rec, int index)
        throws PagedStorageException {
      int child = itemChildren[rec][index];
      if (child > PAGE_MARKER) {
        Page childPage = readPage(child - PAGE_MARKER);
        pagesToDump.add(childPage);
      }
      stats.componentLength.add(itemNames[rec][index].length());
    }

    private void statsRecord(TreeStoreStats stats, List<Page> pagesToDump, int rec)
        throws PagedStorageException {
      deserializeRecordEntirely(rec);
      int count = itemCounts[rec];
      for (int i = 0; i < count; i++) {
        statsItem(stats, pagesToDump, rec, i);
      }
    }

    private void updateRecordOffsets() {
      if (recordOffsets != null) {
        return;
      }

      recordOffsets = new int[recordCount + 1];

      byte[] buf = new byte[10240];
      int offset = CB_EMPTY_PAGE_METADATA + recordCount * CB_PAGE_METADATA_PER_RECORD;
      for (int record = 0; record < recordCount; record++) {
        recordOffsets[record] = offset;

        int count = itemCounts[record];
        byte[][] namesEncoded = itemNamesEncoded[record];
        String[] names = itemNames[record];
        int[] offsets = itemOffsets[record] = new int[count + 1];

        for (int item = 0; item < count; item++) {
          if (namesEncoded[item] == null) {
            namesEncoded[item] = Data.encodeStringAndCopy(names[item], buf);
          }
        }

        int itemOffset = CB_EMPTY_RECORD_METADATA + CB_RECORD_METADATA_PER_ITEM * count;
        for (int item = 0; item < count; item++) {
          offsets[item] = itemOffset;
          itemOffset += additionalData + namesEncoded[item].length;
        }
        offsets[count] = itemOffset;

        offset += itemOffset;
      }
      recordOffsets[recordCount] = offset;
      if (recordOffsets[recordCount] != totalSize) {
        throw new AssertionError("totalSize != finalRecordOffset: " + totalSize + " != " + offset);
      }
    }
  }

  public static final int ADD_NOROOM = -1;

  public static final int ADD_EXISTS = -2;
  static final int PAGE_MARKER = 10000;
  private final PagedStorage pagedStorage;

  private Page rootPage;

  private final int additionalData;

  private RecordFactory recordFactory = new RecordFactory() {

    @Override
    public Record read(PagedStorage pagedStorage, Data data, int pageId, boolean isNew)
        throws PagedStorageException {
      return new Page(data, pageId);
    }

  };

  private TreeCoordListener coordListener;

  public TreeStore(PagedStorage pagedStorage, int additionalData, int rootPageId)
      throws PagedStorageException {
    this.pagedStorage = pagedStorage;
    this.additionalData = additionalData;
    open(rootPageId);
  }

  public void dump(StringBuilder out, int level, boolean detailed) {
    out.append(StringUtils.indent(level)).append("TreeStore");
    out.append(" root=").append(rootPage.pageId);
    out.append("\n");
    List<Page> pagesToDump = new LinkedList<Page>();
    pagesToDump.add(rootPage);
    while (!pagesToDump.isEmpty()) {
      Page page = pagesToDump.remove(0);
      try {
        page.dump(pagesToDump, out, level + 1, detailed);
      } catch (PagedStorageException exception) {
        IndexerPlugin.getLogger().logError(exception);
      }
    }
  }

  public PageRecPos lookup(String[] path, boolean add) throws PagedStorageException {
    if (add) {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS,
          "TreeStore<" + rootPage.pageId + "> search-or-add: " + StringUtils.join(path));
    } else {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS,
          "TreeStore<" + rootPage.pageId + "> search: " + StringUtils.join(path));
    }
    PageRec pageRec = root();
    int pathLength = path.length;
    PageRecPos result = null;
    for (int i = 0; i < pathLength; i++) {
      String component = path[i];
      if (component == null) {
        throw new NullPointerException("Path component is null");
      }

      PageRecPos pos = pageRec.lookup(component, add);
      if (pos == null) {
        if (add && IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.ANOMALIES)) {
          IndexerPlugin.getLogger().trace(IndexerDebugOptions.ANOMALIES,
              "Item creation failed for path " + StringUtils.join(path));
        } else {
          IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS,
              " NOT FOUND at #" + i + " " + component);
        }
        return null;
      }
      if (i == pathLength - 1) {
        result = pos;
      } else {
        pageRec = pos.children(add);
        if (pageRec == null) {
          if (add && IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.ANOMALIES)) {
            IndexerPlugin.getLogger().trace(IndexerDebugOptions.ANOMALIES,
                "Child record creation failed for path " + StringUtils.join(path));
          } else {
            IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS,
                " NO CHILDREN at #" + i + " " + component);
          }
          return null;
        }
      }
    }
    if (result == null) {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS, "result is null");
    } else {
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_LOOKUPS,
          " FOUND " + result.page.pageId + " - " + toString().trim());
    }
    return result;
  }

  public Iterator<TreeLeaf> pathIterator() {
    return new LeafIterator();
  }

  public PageRecPos readPos(Data data) throws PagedStorageException {
    int pageId = data.readInt();
    int record = data.readShortInt();
    int index = data.readShortInt();
    return new PageRecPos(readPage(pageId), record, index);
  }

  public PageRec root() {
    return new PageRec(rootPage, 0);
  }

  public void setCoordListener(TreeCoordListener coordListener) {
    this.coordListener = coordListener;
  }

  public void stats(TreeStoreStats stats) {
    List<Page> pagesToDump = new LinkedList<Page>();
    pagesToDump.add(rootPage);
    while (!pagesToDump.isEmpty()) {
      Page page = pagesToDump.remove(0);
      ++stats.pages;
      try {
        page.stats(stats, pagesToDump);
      } catch (PagedStorageException exception) {
        IndexerPlugin.getLogger().logError(exception);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    dump(result, 0, true);
    return result.toString();
  }

  public String toTestableString() {
    StringBuilder result = new StringBuilder();
    dump(result, 0, false);
    return result.toString();
  }

  Page readPage(int pageId) throws PagedStorageException {
    return (Page) pagedStorage.readRecord(pageId, recordFactory);
  }

  private void open(int rootPageId) throws PagedStorageException {
    if (rootPageId < 0) {
      rootPageId = pagedStorage.allocatePage();
      rootPage = new Page(pagedStorage.createData(), rootPageId);
      pagedStorage.updateRecord(rootPage);
    } else {
      rootPage = readPage(rootPageId);
    }
    if (rootPage.recordCount == 0) {
      rootPage.addEmptyChildRecord();
    }
  }
}
