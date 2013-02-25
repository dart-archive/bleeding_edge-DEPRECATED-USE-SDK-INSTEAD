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

import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageCorruptedException;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.pagestore.Record;
import com.google.dart.indexer.pagedstorage.util.BitField;
import com.google.dart.indexer.storage.paged.store.Data;

/**
 * The list of free pages of a page store. The format of a free list trunk page is:
 * <ul>
 * <li>0-3: parent page id (always 0)</li>
 * <li>4-4: page type</li>
 * <li>5-remainder: data</li>
 * </ul>
 */
public class PageAllocationBitmapPage extends Record {
  private static final int DATA_START = 5;

  private final PagedStorage storage;
  private final BitField used = new BitField();
  private final int pageCount;
  private boolean full;
  private Data data;

  public static final int TYPE_FREE_LIST = 6;

  public static final int TYPE_EMPTY = 0;

  /**
   * Get the number of pages that can fit in a free list.
   * 
   * @param pageSize the page size
   * @return the number of pages
   */
  public static int getPagesAddressed(int pageSize) {
    return (pageSize - DATA_START) * 8;
  }

  public PageAllocationBitmapPage(PagedStorage storage, int pageId) {
    setPos(pageId);
    this.storage = storage;
    pageCount = (storage.getPageSize() - DATA_START) * 8;
    used.set(0);
  }

  /**
   * Get the estimated memory size.
   * 
   * @return number of double words (4 bytes)
   */
  @Override
  public int getMemorySize() {
    return storage.getPageSize() >> 2;
  }

  @Override
  public void write() throws PagedStorageException {
    data = storage.createData();
    data.writeInt(0);
    int type = PageAllocationBitmapPage.TYPE_FREE_LIST;
    data.writeByte((byte) type);
    for (int i = 0; i < pageCount; i += 8) {
      data.writeByte((byte) used.getByte(i));
    }
    storage.writePage(getPos(), data);
  }

  /**
   * Allocate a page from the free list.
   * 
   * @return the page, or -1 if all pages are used
   */
  int allocate() throws PagedStorageException {
    if (full) {
      return -1;
    }
    // TODO cache last result
    int free = used.nextClearBit(0);
    if (free >= pageCount) {
      full = true;
      return -1;
    }
    used.set(free);
    storage.updateRecord(this, true, data);
    return free + getPos();
  }

  /**
   * Mark a page as used.
   * 
   * @param pos the page id
   * @return the page id, or -1
   */
  int allocate(int pos) throws PagedStorageException {
    int idx = pos - getPos();
    if (idx >= 0 && !used.get(idx)) {
      used.set(idx);
      storage.updateRecord(this, true, data);
    }
    return pos;
  }

  /**
   * Add a page to the free list.
   * 
   * @param pageId the page id to add
   */
  void free(int pageId) throws PagedStorageException {
    full = false;
    used.clear(pageId - getPos());
    storage.updateRecord(this, true, data);
  }

  /**
   * Check if a page is already in use.
   * 
   * @param pageId the page to check
   * @return true if it is in use
   */
  boolean isUsed(int pageId) {
    return used.get(pageId - getPos());
  }

  /**
   * Read the page from the disk.
   */
  void read() throws PagedStorageException {
    data = storage.createData();
    storage.readPage(getPos(), data);
    int p = data.readInt();
    int t = data.readByte();
    if (t == PageAllocationBitmapPage.TYPE_EMPTY) {
      return;
    }
    if (t != PageAllocationBitmapPage.TYPE_FREE_LIST || p != 0) {
      throw new PagedStorageCorruptedException("Page allocation bitmap corrupted, pos:" + getPos()
          + " type:" + t + " parent:" + p + " expected type:"
          + PageAllocationBitmapPage.TYPE_FREE_LIST);
    }
    for (int i = 0; i < pageCount; i += 8) {
      used.setByte(i, data.readByte() & 255);
    }
    full = false;
  }
}
