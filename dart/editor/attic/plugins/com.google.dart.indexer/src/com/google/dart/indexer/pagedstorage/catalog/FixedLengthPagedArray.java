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
import com.google.dart.indexer.pagedstorage.pagestore.FullyDeserializedRecord;
import com.google.dart.indexer.pagedstorage.pagestore.Record;
import com.google.dart.indexer.pagedstorage.pagestore.RecordFactory;
import com.google.dart.indexer.pagedstorage.pagestore.StdRecord;
import com.google.dart.indexer.pagedstorage.treestore.PageRecPos;
import com.google.dart.indexer.pagedstorage.util.StringUtils;
import com.google.dart.indexer.storage.paged.CatalogStats;
import com.google.dart.indexer.storage.paged.store.Data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FixedLengthPagedArray {
  class CatalogPage extends FullyDeserializedRecord {
    private int count;
    private int itemCount;
    private int nextPageId;
    private int[] pages;

    public CatalogPage(Data data, int pageId) {
      super(pagedStorage, pageId);
      data.reset();
      nextPageId = data.readInt();
      itemCount = data.readInt();
      count = data.readShortInt();
      pages = new int[catalogEntriesPerPage]; // leave room for additions
      for (int i = 0; i < count; i++) {
        pages[i] = data.readInt();
      }
    }

    public void dump(int itemsDone, StringBuilder out, int level, boolean detailed)
        throws PagedStorageException {
      out.append(StringUtils.indent(level)).append("CatalogPage ").append(getPos()).append(": ").append(
          itemCount).append(" items on ").append(count).append(" subpages\n");
      for (int i = 0; i < count; i++) {
        DataPage dataPage = readDataPage(pages[i]);
        dataPage.dump(itemsDone, out, level + 1, detailed);
        itemsDone += dataPage.count;
      }
    }

    public int get(int index) {
      if (index >= count) {
        throw new IllegalArgumentException("Invalid index " + index + " for catalog page "
            + getPos());
      }
      return pages[index];
    }

    public void increaseItemCount() {
      itemCount++;
      changed();
    }

    public int lastPageId() {
      if (count == 0) {
        return -1;
      }
      return pages[count - 1];
    }

    public void setNextPageId(int pos) {
      this.nextPageId = pos;
      changed();
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      try {
        dump(0, result, 0, true);
      } catch (PagedStorageException exception) {
        IndexerPlugin.getLogger().logError(exception);
      }
      return result.toString();
    }

    public boolean tryAddDataPage(int pos) {
      if (count == catalogEntriesPerPage) {
        return false;
      }
      pages[count++] = pos;
      changed();
      return true;
    }

    @Override
    protected void serializeCached(Data data) {
      data.reset();
      data.writeInt(nextPageId);
      data.writeInt(itemCount);
      data.writeShortInt(count);
      for (int i = 0; i < count; i++) {
        data.writeInt(pages[i]);
      }
    }
  }

  class DataPage extends StdRecord {
    private int count;

    public DataPage(Data data, int pageId) {
      super(pagedStorage, data, pageId);
      data.reset();
      count = data.readShortInt();
    }

    public void delete(int index) {
      // TODO mark this rowId for reuse
      writableData(index).seek(0).writeInt(-2);
    }

    public void dump(int itemsDone, StringBuilder out, int level, boolean detailed) {
      out.append(StringUtils.indent(level)).append("DataPage ").append(getPos()).append(": ").append(
          count).append(" items\n");
      for (int i = 0; i < count; i++) {
        dumpItem(1 + itemsDone + i, i, out, level + 1, detailed);
      }
    }

    public int getCount() {
      return count;
    }

    public int getInfoPage(int index, int layer) {
      return readableData(index).seek(4 + PageRecPos.SIZE + layer * CatalogPos.SIZE).readInt();
    }

    public int getParentRowId(int index) {
      return readableData(index).seek(0).readInt();
    }

    public boolean isDeleted(int index) {
      return readableData(index).seek(0).readInt() == -2;
    }

    public Data readableData(int index) {
      data.setPos(DATA_OVERHEAD + index * dataSize);
      return data;
    }

    public void setInfoPage(int index, int layer, int infoPage) {
      writableData(index).seek(4 + PageRecPos.SIZE + layer * CatalogPos.SIZE).writeInt(infoPage);
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      dump(0, result, 0, true);
      return result.toString();
    }

    public int tryAllocateNewPos() {
      if (count == dataEntriesPerPage) {
        return -1;
      }
      changed();
      return count++;
    }

    public Data writableData(int index) {
      changed();
      return readableData(index);
    }

    public void zeroData(int index) {
      changed();
      data.zeroFill(DATA_OVERHEAD + index * dataSize, dataSize);
    }

    @Override
    protected void serializeCached() {
      data.reset();
      data.writeShortInt(count);
    }

    private void dumpItem(int rowId, int index, StringBuilder out, int level, boolean detailed) {
      out.append(StringUtils.indent(level)).append("ID ").append(rowId).append(": ");
      if (isDeleted(index)) {
        out.append("--deleted--");
      } else {
        Data data = readableData(index);
        int parentRowId = data.readInt();
        int treePage = data.readInt();
        int treeRec = data.readShortInt();
        int treePos = data.readShortInt();
        out.append("parentRowId=").append(parentRowId).append(" ");
        out.append("treePos=<").append(treePage).append(",").append(treeRec).append(",").append(
            treePos).append(">");
        for (int i = 0; i < (dataSize - 4 - PageRecPos.SIZE) / InfoPos.SIZE; i++) {
          int page = data.readInt();
          out.append(" info=<").append(page).append(">");
        }
      }
      out.append("\n");
    }
  }

  private static final int CATALOG_COUNT_SIZE = 2;

  private static final int PAGE_ID_SIZE = 4;

  private static final int CB_ITEM_COUNT_PER_CATALOG_PAGE = 4;

  private static final int CATALOG_OVERHEAD = CATALOG_COUNT_SIZE + PAGE_ID_SIZE
      + CB_ITEM_COUNT_PER_CATALOG_PAGE;

  private static final int DATA_COUNT_SIZE = 2;
  private static final int DATA_OVERHEAD = DATA_COUNT_SIZE;
  private final PagedStorage pagedStorage;
  private final int dataSize;

  private final int catalogEntriesPerPage;
  private final List<CatalogPage> catalogPages;

  private int dataEntriesPerPage;

  private int overallItemCount;

  private final RecordFactory catalogPageFactory = new RecordFactory() {

    @Override
    public Record read(PagedStorage pagedStorage, Data data, int pageId, boolean isNew)
        throws PagedStorageException {
      return new CatalogPage(data, pageId);
    }

  };

  private final RecordFactory dataPageFactory = new RecordFactory() {

    @Override
    public Record read(PagedStorage pagedStorage, Data data, int pageId, boolean isNew)
        throws PagedStorageException {
      return new DataPage(data, pageId);
    }

  };

  public FixedLengthPagedArray(PagedStorage pagedStorage, int rootPageId, int dataSize)
      throws PagedStorageException {
    this.pagedStorage = pagedStorage;
    this.dataSize = dataSize;
    this.catalogEntriesPerPage = (pagedStorage.getPageSize() - CATALOG_OVERHEAD) / PAGE_ID_SIZE;
    this.dataEntriesPerPage = (pagedStorage.getPageSize() - DATA_OVERHEAD) / dataSize;
    this.catalogPages = readAllCatalogPages(rootPageId);
    this.overallItemCount = computeOverallItemCount();
  }

  public CatalogPos allocateNew() throws PagedStorageException {
    CatalogPage lastCatalogPage = catalogPages.get(catalogPages.size() - 1);
    int lastPageId = lastCatalogPage.lastPageId();
    DataPage dataPage;
    if (lastPageId < 0) {
      dataPage = addNewDataPage();
    } else {
      dataPage = readDataPage(lastPageId);
    }
    int index = dataPage.tryAllocateNewPos();
    if (index < 0) {
      dataPage = addNewDataPage();
      index = dataPage.tryAllocateNewPos();
      if (index < 0) {
        throw new AssertionError("Cannot add an item to an empty data page");
      }
    }

    lastCatalogPage = catalogPages.get(catalogPages.size() - 1); // might have
                                                                 // added a new
                                                                 // one
    if (overallItemCount != (catalogPages.size() - 1) * catalogEntriesPerPage * dataEntriesPerPage
        + lastCatalogPage.itemCount) {
      throw new AssertionError("Internal state error: item counts are corrupted");
    }
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.CATALOG_INTERNALS,
        "Catalog: added item to page " + dataPage.getPos());
    lastCatalogPage.increaseItemCount();
    int rowId = ++overallItemCount;
    dataPage.zeroData(index);
    return new CatalogPos(this, rowId, dataPage, index);
  }

  public void dump(StringBuilder out, int level, boolean detailed) throws PagedStorageException {
    out.append(StringUtils.indent(level)).append("Catalog\n");
    int itemsDone = 0;
    for (Iterator<CatalogPage> iterator = catalogPages.iterator(); iterator.hasNext();) {
      CatalogPage catalogPage = iterator.next();
      catalogPage.dump(itemsDone, out, level + 1, detailed);
      itemsDone += catalogPage.itemCount;
    }
  }

  public int getOverallItemCount() {
    return overallItemCount;
  }

  public Data readableData(int pageId, int index) throws PagedStorageException {
    DataPage page = readDataPage(pageId);
    return page.readableData(index);
  }

  public CatalogPos resolve(int rowId) throws PagedStorageException {
    if (rowId <= 0) {
      throw new IllegalArgumentException("Attemp to resolve invalid row ID " + rowId);
    }
    if (rowId > overallItemCount) {
      throw new IllegalArgumentException("Row ID is too large: " + rowId);
    }
    int zeroBasedRowId = rowId - 1;
    int dataPageOrdinal = zeroBasedRowId / dataEntriesPerPage;
    int catalogPageOrdinal = dataPageOrdinal / catalogEntriesPerPage;

    CatalogPage catalogPage = catalogPages.get(catalogPageOrdinal);
    int dataPageId = catalogPage.get(dataPageOrdinal % catalogEntriesPerPage);
    DataPage dataPage = readDataPage(dataPageId);
    return new CatalogPos(this, rowId, dataPage, zeroBasedRowId % dataEntriesPerPage);
  }

  public void stats(CatalogStats stats) {
    stats.catalogPages = catalogPages.size();
    stats.dataPages = (overallItemCount + dataEntriesPerPage - 1) / dataEntriesPerPage;
    stats.dataEntriesPerPage = dataEntriesPerPage;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    try {
      dump(result, 0, true);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
    return result.toString();
  }

  public Data writableData(int pageId, int index) throws PagedStorageException {
    DataPage page = readDataPage(pageId);
    return page.writableData(index);
  }

  int getDataEntriesPerPage() {
    return dataEntriesPerPage;
  }

  private CatalogPage addNewCatalogPage() throws PagedStorageException {
    CatalogPage lastCatalogPage = catalogPages.get(catalogPages.size() - 1);

    CatalogPage newCatalogPage = (CatalogPage) pagedStorage.readRecord(-1, catalogPageFactory);
    catalogPages.add(newCatalogPage);

    lastCatalogPage.setNextPageId(newCatalogPage.getPos());
    return newCatalogPage;
  }

  private DataPage addNewDataPage() throws PagedStorageException {
    DataPage page = (DataPage) pagedStorage.readRecord(-1, dataPageFactory);
    IndexerPlugin.getLogger().trace(IndexerDebugOptions.CATALOG_INTERNALS,
        "Catalog: added page " + page.getPos());

    CatalogPage lastCatalogPage = catalogPages.get(catalogPages.size() - 1);
    if (!lastCatalogPage.tryAddDataPage(page.getPos())) {
      lastCatalogPage = addNewCatalogPage();
      if (!lastCatalogPage.tryAddDataPage(page.getPos())) {
        throw new AssertionError("Cannot add an item to an empty catalog page");
      }
    }

    return page;
  }

  private int computeOverallItemCount() {
    int overallItemCount = 0;
    for (Iterator<CatalogPage> iterator = catalogPages.iterator(); iterator.hasNext();) {
      CatalogPage page = iterator.next();
      overallItemCount += page.itemCount;
    }
    return overallItemCount;
  }

  private List<CatalogPage> readAllCatalogPages(int rootPageId) throws PagedStorageException {
    List<CatalogPage> catalogPagesList = new ArrayList<CatalogPage>(100);
    int nextPageId = rootPageId;
    while (nextPageId > 0) {
      CatalogPage page = readCatalogPage(nextPageId);
      catalogPagesList.add(page);
      nextPageId = page.nextPageId;
    }
    return catalogPagesList;
  }

  private CatalogPage readCatalogPage(int pageId) throws PagedStorageException {
    return (CatalogPage) pagedStorage.readRecord(pageId, catalogPageFactory);
  }

  private DataPage readDataPage(int pageId) throws PagedStorageException {
    return (DataPage) pagedStorage.readRecord(pageId, dataPageFactory);
  }
}
