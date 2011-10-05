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
package com.google.dart.indexer.storage.paged.store;

import com.google.dart.indexer.pagedstorage.DebugConstants;
import com.google.dart.indexer.pagedstorage.util.ObjectArray;

import java.util.Comparator;

/**
 * The base object for all cached objects.
 */
public abstract class CacheObject {
  /**
   * Compare cache objects by position.
   */
  static class CacheComparator implements Comparator<CacheObject> {
    @Override
    public int compare(CacheObject a, CacheObject b) {
      int pa = a.getPos();
      int pb = b.getPos();
      return pa == pb ? 0 : (pa < pb ? -1 : 1);
    }
  }

  /**
   * Ensure the class is loaded when initialized, so that sorting is possible even when loading new
   * classes is not allowed any more. This can occur when stopping a web application.
   */
  static {
    new CacheComparator();
  }

  /**
   * Order the given list of cache objects by position.
   * 
   * @param recordList the list of cache objects
   */
  public static void sort(ObjectArray<CacheObject> recordList) {
    recordList.sort(new CacheComparator());
  }

  /**
   * The previous element in the LRU linked list. If the previous element is the head, then this
   * element is the most recently used object.
   */
  public CacheObject previous;

  /**
   * The next element in the LRU linked list. If the next element is the head, then this element is
   * the least recently used object.
   */
  public CacheObject next;

  /**
   * The next element in the hash chain.
   */
  public CacheObject chained;

  /**
   * The cache queue identifier. This field is only used for the 2Q cache algorithm.
   */
  public int cacheQueue;
  public int pageId;

  private boolean changed;

  /**
   * Check if the object can be removed from the cache. For example pinned objects can not be
   * removed.
   * 
   * @return true if it can be removed
   */
  public abstract boolean canRemove();

  /**
   * Get the estimated memory size.
   * 
   * @return number of double words (4 bytes)
   */
  public int getMemorySize() {
    return 0;
  }

  public int getPos() {
    return pageId;
  }

  /**
   * Check if this cache object has been changed and thus needs to be written back to the storage.
   * 
   * @return if it has been changed
   */
  public boolean isChanged() {
    return changed;
  }

  /**
   * Check if this cache object can be removed from the cache.
   * 
   * @return if it can be removed
   */
  public boolean isPinned() {
    return false;
  }

  public void setChanged(boolean b) {
    changed = b;
  }

  public void setPos(int pos) {
    if (DebugConstants.CHECK && (previous != null || next != null || chained != null)) {
      throw new IllegalStateException("setPos too late");
    }
    this.pageId = pos;
  }
}
