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
package com.google.dart.indexer.pagedstorage.util;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.DebugConstants;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.storage.paged.store.CacheObject;
import com.google.dart.indexer.storage.paged.store.CacheWriter;

/**
 * A cache implementation based on the last recently used (LRU) algorithm.
 */
public class CacheLRU implements Cache {
  static final String TYPE_NAME = "LRU";

  private final CacheWriter writer;
  private final CacheObject head = new CacheHead();
  private final int len;
  private final int mask;
  private int maxSize;
  private CacheObject[] values;
  private int recordCount;
  private int sizeMemory;

  private CacheLRU(CacheWriter writer, int maxKb) {
    this.maxSize = maxKb * 1024 / 4;
    this.writer = writer;
    this.len = MathUtils.nextPowerOf2(maxSize / 64);
    this.mask = len - 1;
    MathUtils.checkPowerOf2(len);
    clear();
  }

  @Override
  public void clear() {
    head.next = head.previous = head;
    // first set to null - avoiding out of memory
    values = null;
    values = new CacheObject[len];
    recordCount = 0;
    sizeMemory = 0;
  }

  @Override
  public CacheObject find(int pos) {
    CacheObject rec = values[pos & mask];
    while (rec != null && rec.getPos() != pos) {
      rec = rec.chained;
    }
    return rec;
  }

  @Override
  public CacheObject get(int pos) {
    CacheObject rec = find(pos);
    if (rec != null) {
      removeFromLinkedList(rec);
      addToFront(rec);
    }
    return rec;
  }

  @Override
  public ObjectArray<CacheObject> getAllChanged() {
    // TODO cache: should probably use the LRU list
    ObjectArray<CacheObject> list = ObjectArray.newInstance();
    for (int i = 0; i < len; i++) {
      CacheObject rec = values[i];
      while (rec != null) {
        if (rec.isChanged()) {
          list.add(rec);
          if (list.size() >= recordCount) {
            if (DebugConstants.CHECK) {
              if (list.size() > recordCount) {
                throw new AssertionError("cache chain error");
              }
            } else {
              break;
            }
          }
        }
        rec = rec.chained;
      }
    }
    return list;
  }

  @Override
  public int getMaxSize() {
    return maxSize;
  }

  @Override
  public int getSize() {
    return sizeMemory;
  }

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  @Override
  public void put(CacheObject rec) throws PagedStorageException {
    if (DebugConstants.CHECK) {
      // int pos = rec.getPos();
      // for (int i = 0; i < rec.getBlockCount(); i++) {
      // CacheObject old = find(pos + i);
      // if (old != null) {
      // Message.throwInternalError("try to add a record twice pos:" + pos +
      // " i:" + i);
      // }
      // }
    }
    int index = rec.getPos() & mask;
    rec.chained = values[index];
    values[index] = rec;
    recordCount++;
    sizeMemory += rec.getMemorySize();
    addToFront(rec);
    removeOldIfRequired();
  }

  @Override
  public void remove(int pos) {
    int index = pos & mask;
    CacheObject rec = values[index];
    if (rec == null) {
      return;
    }
    if (rec.getPos() == pos) {
      values[index] = rec.chained;
    } else {
      CacheObject last;
      do {
        last = rec;
        rec = rec.chained;
        if (rec == null) {
          return;
        }
      } while (rec.getPos() != pos);
      last.chained = rec.chained;
    }
    recordCount--;
    sizeMemory -= rec.getMemorySize();
    removeFromLinkedList(rec);
    if (DebugConstants.CHECK) {
      rec.chained = null;
      if (find(pos) != null) {
        throw new AssertionError("not removed!");
      }
    }
  }

  @Override
  public void setMaxSize(int maxKb) throws PagedStorageException {
    int newSize = maxKb * 1024 / 4;
    maxSize = newSize < 0 ? 0 : newSize;
    // can not resize, otherwise existing records are lost
    // resize(maxSize);
    removeOldIfRequired();
  }

  @Override
  public CacheObject update(int pos, CacheObject rec) throws PagedStorageException {
    CacheObject old = find(pos);
    if (old == null) {
      put(rec);
    } else {
      if (DebugConstants.CHECK) {
        if (old != rec) {
          throw new IllegalArgumentException(
              "Attemp to write conflicting records into cache - pos:" + pos + " old:" + old
                  + " new:" + rec);
        }
      }
      removeFromLinkedList(rec);
      addToFront(rec);
    }
    return old;
  }

  private void addToFront(CacheObject rec) {
    if (DebugConstants.CHECK && rec == head) {
      throw new IllegalArgumentException("try to move head");
    }
    rec.next = head;
    rec.previous = head.previous;
    rec.previous.next = rec;
    head.previous = rec;
  }

  private void removeFromLinkedList(CacheObject rec) {
    if (DebugConstants.CHECK && rec == head) {
      throw new IllegalArgumentException("try to remove head");
    }
    rec.previous.next = rec.next;
    rec.next.previous = rec.previous;
    // TODO cache: mystery: why is this required? needs more memory if we
    // don't do this
    rec.next = null;
    rec.previous = null;
  }

  private void removeOld() throws PagedStorageException {
    int i = 0;
    ObjectArray<CacheObject> changed = ObjectArray.newInstance();
    while (sizeMemory * 4 > maxSize * 3 && recordCount > Constants.CACHE_MIN_RECORDS) {
      i++;
      if (i >= recordCount * 2) {
        // hopefully this does not happen too much, but it could happen
        // theoretically
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.RARE_ANOMALIES,
            "Cannot remove records, cache size too small?");
        break;
      }
      CacheObject last = head.next;
      if (DebugConstants.CHECK && last == head) {
        throw new AssertionError("try to remove head");
      }
      // we are not allowed to remove it if the log is not yet written
      // (because we need to log before writing the data)
      // also, can't write it if the record is pinned
      if (!last.canRemove()) {
        removeFromLinkedList(last);
        addToFront(last);
        continue;
      }
      remove(last.getPos());
      if (last.isChanged()) {
        changed.add(last);
      }
    }
    if (changed.size() > 0) {
      CacheObject.sort(changed);
      for (i = 0; i < changed.size(); i++) {
        CacheObject rec = changed.get(i);
        writer.writeBack(rec);
      }
    }
  }

  private void removeOldIfRequired() throws PagedStorageException {
    // a small method, to allow inlining
    if (sizeMemory >= maxSize) {
      removeOld();
    }
  }
}
