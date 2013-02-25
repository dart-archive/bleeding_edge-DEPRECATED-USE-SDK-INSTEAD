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
 * A cache implementation based on the 2Q algorithm. For about the algorithm, see
 * http://www.vldb.org/conf/1994/P439.PDF . In this implementation, items are moved from 'in' queue
 * and move to the 'main' queue if the are referenced again.
 */
public class CacheTQ implements Cache {
  static final String TYPE_NAME = "TQ";

  private static final int MAIN = 1, IN = 2, OUT = 3;
  private static final int PERCENT_IN = 20, PERCENT_OUT = 50;

  private final CacheWriter writer;
  private final CacheObject headMain = new CacheHead();
  private final CacheObject headIn = new CacheHead();
  private final CacheObject headOut = new CacheHead();
  private final int len;
  private final int mask;
  private int maxSize;
  private int maxMain, maxIn, maxOut;
  private int sizeMain, sizeIn, sizeOut;
  private int recordCount;
  private CacheObject[] values;

  public CacheTQ(CacheWriter writer, int maxKb) {
    int maxSize = maxKb * 1024 / 4;
    this.writer = writer;
    this.maxSize = maxSize;
    this.len = MathUtils.nextPowerOf2(maxSize / 64);
    this.mask = len - 1;
    MathUtils.checkPowerOf2(len);
    recalculateMax();
    clear();
  }

  @Override
  public void clear() {
    headMain.next = headMain.previous = headMain;
    headIn.next = headIn.previous = headIn;
    headOut.next = headOut.previous = headOut;
    // first set to null - avoiding out of memory
    values = null;
    values = new CacheObject[len];
    sizeIn = sizeOut = sizeMain = 0;
    recordCount = 0;
  }

  @Override
  public CacheObject find(int pos) {
    CacheObject o = findCacheObject(pos);
    if (o != null && o.cacheQueue != OUT) {
      return o;
    }
    return null;
  }

  @Override
  public CacheObject get(int pos) {
    CacheObject r = findCacheObject(pos);
    if (r == null) {
      return null;
    }
    if (r.cacheQueue == MAIN) {
      removeFromList(r);
      addToFront(headMain, r);
    } else if (r.cacheQueue == OUT) {
      return null;
    } else if (r.cacheQueue == IN) {
      removeFromList(r);
      sizeIn -= r.getMemorySize();
      sizeMain += r.getMemorySize();
      r.cacheQueue = MAIN;
      addToFront(headMain, r);
    }
    return r;
  }

  @Override
  public ObjectArray<CacheObject> getAllChanged() {
    ObjectArray<CacheObject> list = ObjectArray.newInstance();
    for (CacheObject o = headMain.next; o != headMain; o = o.next) {
      if (o.isChanged()) {
        list.add(o);
      }
    }
    for (CacheObject o = headIn.next; o != headIn; o = o.next) {
      if (o.isChanged()) {
        list.add(o);
      }
    }
    CacheObject.sort(list);
    return list;
  }

  @Override
  public int getMaxSize() {
    return maxSize;
  }

  @Override
  public int getSize() {
    return sizeIn + sizeOut + sizeMain;
  }

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  @Override
  public void put(CacheObject rec) throws PagedStorageException {
    int pos = rec.getPos();
    CacheObject r = findCacheObject(pos);
    if (r != null) {
      if (r.cacheQueue == OUT) {
        removeCacheObject(pos);
        removeFromList(r);
        removeOldIfRequired();
        rec.cacheQueue = MAIN;
        putCacheObject(rec);
        addToFront(headMain, rec);
        sizeMain += rec.getMemorySize();
      }
    } else if (sizeMain < maxMain) {
      removeOldIfRequired();
      rec.cacheQueue = MAIN;
      putCacheObject(rec);
      addToFront(headMain, rec);
      sizeMain += rec.getMemorySize();
    } else {
      removeOldIfRequired();
      rec.cacheQueue = IN;
      putCacheObject(rec);
      addToFront(headIn, rec);
      sizeIn += rec.getMemorySize();
    }
  }

  @Override
  public void remove(int pos) {
    CacheObject r = removeCacheObject(pos);
    if (r != null) {
      removeFromList(r);
      if (r.cacheQueue == MAIN) {
        sizeMain -= r.getMemorySize();
      } else if (r.cacheQueue == IN) {
        sizeIn -= r.getMemorySize();
      }
    }
  }

  @Override
  public void setMaxSize(int maxKb) throws PagedStorageException {
    int newSize = maxKb * 1024 / 4;
    maxSize = newSize < 0 ? 0 : newSize;
    recalculateMax();
    // can not resize, otherwise existing records are lost
    // resize(maxSize);
    removeOldIfRequired();
  }

  @Override
  public CacheObject update(int pos, CacheObject rec) throws PagedStorageException {
    CacheObject old = find(pos);
    if (old == null || old.cacheQueue == OUT) {
      put(rec);
    } else {
      if (old == rec) {
        if (rec.cacheQueue == MAIN) {
          removeFromList(rec);
          addToFront(headMain, rec);
        }
      }
    }
    return old;
  }

  private void addToFront(CacheObject head, CacheObject rec) {
    if (DebugConstants.CHECK) {
      if (rec == head) {
        throw new IllegalArgumentException("try to move head");
      }
      if (rec.next != null || rec.previous != null) {
        throw new IllegalArgumentException("already linked");
      }
    }
    rec.next = head;
    rec.previous = head.previous;
    rec.previous.next = rec;
    head.previous = rec;
  }

  private CacheObject findCacheObject(int pos) {
    CacheObject rec = values[pos & mask];
    while (rec != null && rec.getPos() != pos) {
      rec = rec.chained;
    }
    return rec;
  }

  private void putCacheObject(CacheObject rec) {
    if (DebugConstants.CHECK) {
      // for (int i = 0; i < rec.getBlockCount(); i++) {
      // CacheObject old = find(rec.getPos() + i);
      // if (old != null) {
      // Message.throwInternalError("try to add a record twice i=" + i);
      // }
      // }
    }
    int index = rec.getPos() & mask;
    rec.chained = values[index];
    values[index] = rec;
    recordCount++;
  }

  private void recalculateMax() {
    maxMain = maxSize;
    maxIn = maxSize * PERCENT_IN / 100;
    maxOut = maxSize * PERCENT_OUT / 100;
  }

  private CacheObject removeCacheObject(int pos) {
    int index = pos & mask;
    CacheObject rec = values[index];
    if (rec == null) {
      return null;
    }
    if (rec.getPos() == pos) {
      values[index] = rec.chained;
    } else {
      CacheObject last;
      do {
        last = rec;
        rec = rec.chained;
        if (rec == null) {
          return null;
        }
      } while (rec.getPos() != pos);
      last.chained = rec.chained;
    }
    recordCount--;
    if (DebugConstants.CHECK) {
      rec.chained = null;
    }
    return rec;
  }

  private void removeFromList(CacheObject rec) {
    if (DebugConstants.CHECK && (rec instanceof CacheHead && rec.cacheQueue != OUT)) {
      throw new AssertionError("CacheTQ internal inconsistency");
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
    while (((sizeIn * 4 > maxIn * 3) || (sizeOut * 4 > maxOut * 3) || (sizeMain * 4 > maxMain * 3))
        && recordCount > Constants.CACHE_MIN_RECORDS) {
      i++;
      if (i >= recordCount * 2) {
        // hopefully this does not happen too much, but it could happen
        // theoretically
        IndexerPlugin.getLogger().trace(IndexerDebugOptions.RARE_ANOMALIES,
            "Cannot remove records, cache size too small?");
        break;
      }
      if (sizeIn > maxIn) {
        CacheObject r = headIn.next;
        if (!r.canRemove()) {
          removeFromList(r);
          addToFront(headIn, r);
          continue;
        }
        sizeIn -= r.getMemorySize();
        int pos = r.getPos();
        removeCacheObject(pos);
        removeFromList(r);
        if (r.isChanged()) {
          changed.add(r);
        }
        r = new CacheHead();
        r.setPos(pos);
        r.cacheQueue = OUT;
        putCacheObject(r);
        addToFront(headOut, r);
        sizeOut++;
        if (sizeOut >= maxOut) {
          r = headOut.next;
          sizeOut--;
          removeCacheObject(r.getPos());
          removeFromList(r);
        }
      } else if (sizeMain > 0) {
        CacheObject r = headMain.next;
        if (!r.canRemove() && !(r instanceof CacheHead)) {
          removeFromList(r);
          addToFront(headMain, r);
          continue;
        }
        sizeMain -= r.getMemorySize();
        removeCacheObject(r.getPos());
        removeFromList(r);
        if (r.isChanged()) {
          changed.add(r);
        }
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
    if ((sizeIn >= maxIn) || (sizeOut >= maxOut) || (sizeMain >= maxMain)) {
      removeOld();
    }
  }
}
