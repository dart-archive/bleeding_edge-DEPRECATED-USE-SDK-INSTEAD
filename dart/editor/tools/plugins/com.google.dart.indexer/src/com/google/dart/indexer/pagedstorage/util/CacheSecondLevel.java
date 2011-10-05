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

import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.storage.paged.store.CacheObject;

import java.util.Map;

/**
 * Cache which wraps another cache (proxy pattern) and adds caching using map. This is useful for
 * WeakReference, SoftReference or hard reference cache.
 */
class CacheSecondLevel implements Cache {
  private final Cache baseCache;
  private final String prefix;
  private final Map<Integer, CacheObject> map;

  CacheSecondLevel(Cache cache, String prefix, Map<Integer, CacheObject> map) {
    this.baseCache = cache;
    this.prefix = prefix;
    this.map = map;
  }

  @Override
  public void clear() {
    map.clear();
    baseCache.clear();
  }

  @Override
  public CacheObject find(int pos) {
    CacheObject ret = baseCache.find(pos);
    if (ret == null) {
      ret = map.get(new Integer(pos));
    }
    return ret;
  }

  @Override
  public CacheObject get(int pos) {
    CacheObject ret = baseCache.get(pos);
    if (ret == null) {
      ret = map.get(new Integer(pos));
    }
    return ret;
  }

  @Override
  public ObjectArray<CacheObject> getAllChanged() {
    return baseCache.getAllChanged();
  }

  @Override
  public int getMaxSize() {
    return baseCache.getMaxSize();
  }

  @Override
  public int getSize() {
    return baseCache.getSize();
  }

  @Override
  public String getTypeName() {
    return prefix + baseCache.getTypeName();
  }

  @Override
  public void put(CacheObject r) throws PagedStorageException {
    baseCache.put(r);
    map.put(new Integer(r.getPos()), r);
  }

  @Override
  public void remove(int pos) {
    baseCache.remove(pos);
    map.remove(new Integer(pos));
  }

  @Override
  public void setMaxSize(int size) throws PagedStorageException {
    baseCache.setMaxSize(size);
  }

  @Override
  public CacheObject update(int pos, CacheObject record) throws PagedStorageException {
    CacheObject oldRec = baseCache.update(pos, record);
    map.put(new Integer(pos), record);
    return oldRec;
  }
}
