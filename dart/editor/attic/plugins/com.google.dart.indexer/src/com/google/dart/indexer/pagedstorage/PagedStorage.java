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
package com.google.dart.indexer.pagedstorage;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageCorruptedException;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageNonexistentPageReferenceException;
import com.google.dart.indexer.pagedstorage.filesystem.AccessMode;
import com.google.dart.indexer.pagedstorage.filesystem.FileSystem;
import com.google.dart.indexer.pagedstorage.pagestore.Record;
import com.google.dart.indexer.pagedstorage.pagestore.RecordFactory;
import com.google.dart.indexer.pagedstorage.util.Cache;
import com.google.dart.indexer.pagedstorage.util.CacheTQ;
import com.google.dart.indexer.pagedstorage.util.FileUtils;
import com.google.dart.indexer.pagedstorage.util.ObjectArray;
import com.google.dart.indexer.storage.paged.store.CacheObject;
import com.google.dart.indexer.storage.paged.store.CacheWriter;
import com.google.dart.indexer.storage.paged.store.Data;
import com.google.dart.indexer.storage.paged.store.DataHandler;
import com.google.dart.indexer.storage.paged.store.FileStore;

import java.io.IOException;
import java.util.Iterator;
import java.util.zip.CRC32;

/**
 * This class represents a file that is organized as a number of pages. Page 0 contains a static
 * file header, and pages 1 and 2 both contain the variable file header (page 2 is a copy of page 1
 * and is only read if the checksum of page 1 is invalid). The format of page 0 is:
 * <ul>
 * <li>0-47: file header (static string, repeated 3 times)</li>
 * <li>48-51: page size in bytes (512 - 32768, must be a power of 2)</li>
 * <li>52: write version (if not 0 the file is opened in read-only mode)</li>
 * <li>53: read version (if not 0 opening the file fails)</li>
 * </ul>
 * The format of page 1 and 2 is:
 * <ul>
 * <li>0-7: write counter (incremented each time the header changes)</li>
 * <li>8-X: special pages (2 bytes per special page)</li>
 * <li>X-(X+8): checksum of bytes 0-X (CRC32)</li>
 * </ul>
 * Page 3 contains the first free list page. Page 4 contains the first special page.
 */
public class PagedStorage implements CacheWriter, DataHandler {
  public static final int PAGE_SIZE_MIN = 128;

  public static final int PAGE_SIZE_MAX = 32768;

  public static final int PAGE_SIZE_DEFAULT = 1024 * 32;

  private static final int FIRST_PAGE_ALLOCATION_BITMAP_PAGE = 3;

  private static final int MIN_PAGE_COUNT = 6;

  private static final int INCREMENT_PAGES = 128;

  private static final int READ_VERSION = 0;
  private static final int WRITE_VERSION = 0;

  private String fileName;
  private FileStore file;
  private AccessMode accessMode;
  private int pageSize;
  private int pageSizeShift;
  private long writeCount;
  private int[] specialPages;
  private int specialPageCount;

  private int cacheSize;
  private Cache cache;

  private int freeListPagesPerList;

  private boolean recoveryRunning;

  /**
   * The file size in bytes.
   */
  private long fileLength;

  /**
   * Number of pages (including free pages).
   */
  private int pageCount;

  /**
   * Create a new page store object.
   * 
   * @param database the database
   * @param fileName the file name
   * @param accessMode the access mode
   * @param cacheSizeDefault the default cache size
   */
  public PagedStorage(String fileName, AccessMode accessMode, int cacheSizeDefault,
      int specialPageCount) {
    this.fileName = fileName;
    this.accessMode = accessMode;
    this.specialPageCount = specialPageCount;
    this.specialPages = new int[specialPageCount];
    this.cacheSize = cacheSizeDefault;
    this.cache = new CacheTQ(this, cacheSize);
    setPageSize(PAGE_SIZE_DEFAULT);
  }

  /**
   * Allocate a page.
   * 
   * @return the page id
   */
  public synchronized int allocatePage() throws PagedStorageException {
    int pos;
    int i = -1;
    do {
      i++;
      PageAllocationBitmapPage bitmap = getAllocationBitmapPage(i);
      pos = bitmap.allocate();
    } while (pos < 0);
    if (pos >= pageCount) {
      increaseFileSize(INCREMENT_PAGES);
    }
    return pos;
  }

  public void cacheRecord(Record record) throws PagedStorageException {
    cache.put(record);
  }

  /**
   * Flush all pending changes to disk, and re-open the log file.
   */
  public synchronized void checkpoint() throws PagedStorageException {
    writeBack();
    byte[] empty = new byte[pageSize];
    // TODO avoid to write empty pages
    for (int i = FIRST_PAGE_ALLOCATION_BITMAP_PAGE; i < pageCount; i++) {
      if (!isUsed(i)) {
        file.seek((long) i << pageSizeShift);
        file.write(empty, 0, pageSize);
        writeCount++;
      }
    }
    // TODO shrink file if required here
    // int pageCount = getFreeList().getLastUsed() + 1;
    // trace.debug("pageCount:" + pageCount);
    // file.setLength((long) pageCount << pageSizeShift);
  }

  /**
   * Close the file without further writing.
   */
  public void close() {
    if (file != null) {
      try {
        file.close();
      } catch (IOException e) {
        // ignore: can't do anything about a failed close() anyway
        IndexerPlugin.getLogger().logError(e, "Could not close file: \"" + file + "\"");
      } finally {
        file = null;
      }
    }
  }

  /**
   * Create a data object.
   * 
   * @return the data page.
   */
  public Data createData() {
    return Data.create(this, new byte[pageSize]);
  }

  /**
   * Add a page to the free list.
   * 
   * @param pageId the page id
   * @param logUndo if an undo entry need to be logged
   * @param old the old data (if known)
   */
  public synchronized void freePage(int pageId) throws PagedStorageException {
    cache.remove(pageId);
    doFreePage(pageId);
    if (recoveryRunning) {
      writePage(pageId, createData());
    }
  }

  @Override
  public int getChecksum(byte[] data, int start, int end) {
    // TODO implement checksum
    return 0;
  }

  /**
   * Get the number of pages (including free pages).
   * 
   * @return the page count
   */
  public int getPageCount() {
    return pageCount;
  }

  /**
   * Get the page size.
   * 
   * @return the page size
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Get the record if it is stored in the file, or null if not.
   * 
   * @param pos the page id
   * @return the record or null
   */
  public synchronized Record getRecord(int pos) {
    CacheObject obj = cache.find(pos);
    return (Record) obj;
  }

  public int getSpecialPage(int index) {
    if (index < 0 || index >= specialPageCount) {
      throw new IllegalArgumentException("special page index incorrect: " + index);
    }
    return specialPages[index];
  }

  public long getWriteCount() {
    return writeCount;
  }

  @Override
  public void handleInvalidChecksum() throws PagedStorageException {
    // TODO implement checksum
  }

  /**
   * Open the file and read the header.
   */
  public void open() throws PagedStorageException {
    try {
      if (FileSystem.getInstance(fileName).exists(fileName)) {
        if (FileUtils.length(fileName) < MIN_PAGE_COUNT * PAGE_SIZE_MIN) {
          // the database was not fully created
          openNew();
        } else {
          openExisting();
        }
      } else {
        openNew();
      }
      // lastUsedPage = getFreeList().getLastUsed() + 1;
    } catch (PagedStorageException e) {
      close();
      throw e;
    }
  }

  /**
   * Read a page.
   * 
   * @param pos the page id
   * @return the page
   */
  public Data readPage(int pos) throws PagedStorageException {
    Data page = createData();
    readPage(pos, page);
    return page;
  }

  public Record readRecord(int pageId, RecordFactory factory) throws PagedStorageException {
    if (pageId <= 0) {
      pageId = allocatePage();
      Data data = createData();
      Record record = factory.read(this, data, pageId, true);
      updateRecord(record);
      return record;
    }
    Record record = getRecord(pageId);
    if (record != null) {
      return record;
    }
    Data data = readPage(pageId);
    record = factory.read(this, data, pageId, false);
    cacheRecord(record);
    return record;
  }

  /**
   * Remove a page from the cache.
   * 
   * @param pageId the page id
   */
  public synchronized void removeRecord(int pageId) {
    cache.remove(pageId);
  }

  /**
   * Set the page size. The size must be a power of two. This method must be called before opening.
   * 
   * @param size the page size
   */
  public void setPageSize(int size) {
    if (size < PAGE_SIZE_MIN || size > PAGE_SIZE_MAX) {
      throw new IllegalArgumentException("Invalid page size");
    }
    boolean good = false;
    int shift = 0;
    for (int i = 1; i <= size;) {
      if (size == i) {
        good = true;
        break;
      }
      shift++;
      i += i;
    }
    if (!good) {
      throw new IllegalArgumentException("Page size is not a power of two");
    }
    pageSize = size;
    pageSizeShift = shift;
  }

  public void setSpecialPage(int index, int page) {
    if (index < 0 || index >= specialPageCount) {
      throw new IllegalArgumentException("special page index incorrect: " + index);
    }
    specialPages[index] = page;
    writeVariableHeader();
  }

  /**
   * Update a record.
   * 
   * @param record the record
   * @param logUndo if an undo entry need to be logged
   * @param old the old data (if known)
   */
  public void updateRecord(Record record) throws PagedStorageException {
    updateRecord(record, false, null);
  }

  public synchronized void updateRecord(Record record, boolean logUndo, Data old)
      throws PagedStorageException {
    checkOpen();
    record.setChanged(true);
    int pos = record.getPos();
    allocatePage(pos);
    cache.update(pos, record);
    if (logUndo && !recoveryRunning) {
      if (old == null) {
        old = readPage(pos);
      }
      // log.addUndo(pos, old);
    }
  }

  @Override
  public synchronized void writeBack(CacheObject obj) throws PagedStorageException {
    Record record = (Record) obj;
    record.write();
    record.setChanged(false);
  }

  /**
   * Write a page.
   * 
   * @param pageId the page id
   * @param data the data
   */
  public synchronized void writePage(int pageId, Data data) {
    file.seek((long) pageId << pageSizeShift);
    file.write(data.getBytes(), 0, pageSize);
    writeCount++;
  }

  /**
   * Set the bit of an already allocated page.
   * 
   * @param pageId the page to allocate
   */
  void allocatePage(int pageId) throws PagedStorageException {
    PageAllocationBitmapPage bitmap = getAllocationBitmapPageForPage(pageId);
    bitmap.allocate(pageId);
  }

  /**
   * Read a page.
   * 
   * @param pos the page id
   * @param page the page
   */
  synchronized void readPage(int pos, Data page) throws PagedStorageException {
    if (pos >= pageCount) {
      throw new PagedStorageNonexistentPageReferenceException(pos + " of " + pageCount);
    }
    file.seek((long) pos << pageSizeShift);
    file.readFully(page.getBytes(), 0, pageSize);
  }

  private void checkOpen() {
    if (file == null) {
      throw new IllegalStateException("File is not open");
    }
  }

  private void doFreePage(int pageId) throws PagedStorageException {
    PageAllocationBitmapPage bitmap = getAllocationBitmapPageForPage(pageId);
    bitmap.free(pageId);
  }

  private PageAllocationBitmapPage getAllocationBitmapPage(int ordinal)
      throws PagedStorageException {
    int p = FIRST_PAGE_ALLOCATION_BITMAP_PAGE + ordinal * freeListPagesPerList;
    while (p >= pageCount) {
      increaseFileSize(INCREMENT_PAGES);
    }
    PageAllocationBitmapPage bitmap = (PageAllocationBitmapPage) getRecord(p);
    if (bitmap == null) {
      bitmap = new PageAllocationBitmapPage(this, p);
      if (p < pageCount) {
        bitmap.read();
      }
      cache.put(bitmap);
    }
    return bitmap;
  }

  private PageAllocationBitmapPage getAllocationBitmapPageForPage(int pageId)
      throws PagedStorageException {
    return getAllocationBitmapPage((pageId - FIRST_PAGE_ALLOCATION_BITMAP_PAGE)
        / freeListPagesPerList);
  }

  private void increaseFileSize(int increment) {
    pageCount += increment;
    long newLength = (long) pageCount << pageSizeShift;
    file.setLength(newLength);
    writeCount++;
    fileLength = newLength;
  }

  private boolean isUsed(int pageId) throws PagedStorageException {
    return getAllocationBitmapPageForPage(pageId).isUsed(pageId);
  }

  private void openExisting() throws PagedStorageException {
    FileStore store = FileStore.open(this, fileName, accessMode);
    try {
      store.init();
    } catch (PagedStorageException e) {
      store.closeSilently();
      throw e;
    }
    file = store;
    readStaticHeader();
    freeListPagesPerList = PageAllocationBitmapPage.getPagesAddressed(pageSize);
    fileLength = file.length();
    pageCount = (int) (fileLength / pageSize);
    if (pageCount < MIN_PAGE_COUNT) {
      close();
      openNew();
      return;
    }
    readVariableHeader();
    checkpoint();
  }

  private void openNew() throws PagedStorageException {
    freeListPagesPerList = PageAllocationBitmapPage.getPagesAddressed(pageSize);
    FileStore store = FileStore.open(this, fileName, accessMode);
    try {
      store.init();
    } catch (PagedStorageException e) {
      store.closeSilently();
      throw e;
    }
    file = store;
    recoveryRunning = true;
    writeStaticHeader();

    writeVariableHeader();
    increaseFileSize(MIN_PAGE_COUNT);

    // openMetaIndex();
    // log.openForWriting(logFirstTrunkPage);
    recoveryRunning = false;
    increaseFileSize(INCREMENT_PAGES);

    for (int i = 0; i < specialPageCount; i++) {
      specialPages[i] = allocatePage();
      allocatePage(specialPages[i]);
    }
    writeVariableHeader();
    checkpoint();
  }

  private void readStaticHeader() throws PagedStorageException {
    file.seek(FileStore.HEADER_LENGTH);
    Data page = Data.create(this, new byte[PAGE_SIZE_MIN - FileStore.HEADER_LENGTH]);
    file.readFully(page.getBytes(), 0, PAGE_SIZE_MIN - FileStore.HEADER_LENGTH);
    setPageSize(page.readInt());
    int writeVersion = page.readByte();
    int readVersion = page.readByte();
    if (readVersion != 0) {
      throw new PagedStorageCorruptedException("readVersion - expected:0 got:" + readVersion);
    }
    if (writeVersion != 0) {
      throw new PagedStorageCorruptedException("writeVersion - expected:0 got:" + writeVersion);
    }
  }

  private void readVariableHeader() throws PagedStorageException {
    Data page = Data.create(this, pageSize);
    for (int i = 1;; i++) {
      if (i == 3) {
        throw new PagedStorageCorruptedException("All copies of the variable header are corrupted");
      }
      page.reset();
      readPage(i, page);
      writeCount = page.readLong();
      for (int pp = 0; pp < specialPageCount; pp++) {
        specialPages[pp] = page.readInt();
      }
      CRC32 crc = new CRC32();
      crc.update(page.getBytes(), 0, page.length());
      long expected = crc.getValue();
      long got = page.readLong();
      if (expected == got) {
        break;
      }
    }
  }

  private void writeBack() throws PagedStorageException {
    ObjectArray<CacheObject> list = cache.getAllChanged();
    CacheObject.sort(list);
    for (Iterator<CacheObject> iterator = list.iterator(); iterator.hasNext();) {
      CacheObject rec = iterator.next();
      writeBack(rec);
    }
  }

  private void writeStaticHeader() {
    Data page = Data.create(this, new byte[pageSize - FileStore.HEADER_LENGTH]);
    page.writeInt(pageSize);
    page.writeByte((byte) WRITE_VERSION);
    page.writeByte((byte) READ_VERSION);
    file.seek(FileStore.HEADER_LENGTH);
    file.write(page.getBytes(), 0, pageSize - FileStore.HEADER_LENGTH);
  }

  private void writeVariableHeader() {
    Data page = Data.create(this, pageSize);
    page.writeLong(writeCount);
    for (int pp = 0; pp < specialPageCount; pp++) {
      page.writeInt(specialPages[pp]);
    }
    CRC32 crc = new CRC32();
    crc.update(page.getBytes(), 0, page.length());
    page.writeLong(crc.getValue());
    file.seek(pageSize);
    file.write(page.getBytes(), 0, pageSize);
    file.seek(pageSize + pageSize);
    file.write(page.getBytes(), 0, pageSize);
    writeCount++;
  }
}
