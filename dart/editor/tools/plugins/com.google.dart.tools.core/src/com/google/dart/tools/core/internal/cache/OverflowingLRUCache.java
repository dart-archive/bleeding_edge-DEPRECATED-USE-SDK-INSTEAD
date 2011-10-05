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
package com.google.dart.tools.core.internal.cache;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Instances of the class <code>OverflowingLRUCache</code> implement an LRUCache that attempts to
 * maintain a size equal or less than its <code>fSpaceLimit</code> by removing the least recently
 * used elements.
 * <p>
 * The cache will remove elements which successfully close and all elements which are explicitly
 * removed.
 * <p>
 * If the cache cannot remove enough old elements to add new elements it will grow beyond
 * <code>fSpaceLimit</code>. Later, it will attempt to shink back to the maximum space limit. The
 * method <code>close</code> should attempt to close the element. If the element is successfully
 * closed it will return true and the element will be removed from the cache. Otherwise the element
 * will remain in the cache.
 * <p>
 * The cache implicitly attempts shrinks on calls to <code>put</code>and <code>setSpaceLimit</code>.
 * Explicitly calling the <code>shrink</code> method will also cause the cache to attempt to shrink.
 * <p>
 * The cache calculates the used space of all elements which implement <code>ILRUCacheable</code>.
 * All other elements are assumed to be of size one.
 * <p>
 * Use the <code>#peek(K)</code> and <code>#disableTimestamps()</code> method to circumvent the
 * timestamp feature of the cache. This feature is intended to be used only when the
 * <code>#close(LRUCacheEntry)</code> method causes changes to the cache. For example, if a parent
 * closes its children when </code>#close(LRUCacheEntry)</code> is called, it should be careful not
 * to change the LRU linked list. It can be sure it is not causing problems by calling
 * <code>#peek(K)</code> instead of <code>#get(K)</code> method.
 */
public abstract class OverflowingLRUCache<K, V> extends LRUCache<K, V> {
  /**
   * Indicates if the cache has been over filled and by how much.
   */
  protected int overflow = 0;

  /**
   * Indicates whether or not timestamps should be updated
   */
  protected boolean timestampsOn = true;

  /**
   * Indicates how much space should be reclaimed when the cache overflows. Inital load factor of
   * one third.
   */
  protected double loadFactor = 0.333;

  /**
   * Initialize a newly created cache.
   * 
   * @param size the size limit of the cache
   */
  public OverflowingLRUCache(int size) {
    this(size, 0);
  }

  /**
   * Initialize a newly created cache.
   * 
   * @param size the size limit of the cache
   * @param overflow the size of the overflow
   */
  public OverflowingLRUCache(int size, int overflow) {
    super(size);
    this.overflow = overflow;
  }

  @Override
  public double fillingRatio() {
    return (currentSpace + overflow) * 100.0 / spaceLimit;
  }

  /**
   * For internal testing only. This method exposed only for testing purposes!
   */
  public HashMap<K, LRUCacheEntry<K, V>> getEntryTable() {
    return entryTable;
  }

  /**
   * Return the load factor for the cache. The load factor determines how much space is reclaimed
   * when the cache exceeds its space limit.
   * 
   * @return the load factor for the cache
   */
  public double getLoadFactor() {
    return loadFactor;
  }

  /**
   * Return the space by which the cache has overflown.
   * 
   * @return the space by which the cache has overflown
   */
  public int getOverflow() {
    return overflow;
  }

  /**
   * Return an iterator of the values in the cache with the most recently used first.
   */
  public Iterator<V> iterator() {
    if (entryQueue == null) {
      return new LRUCacheIterator<V>(null);
    }
    LRUCacheIterator.Element<V> head = new LRUCacheIterator.Element<V>(entryQueue.value);
    LRUCacheEntry<K, V> currentEntry = entryQueue.next;
    LRUCacheIterator.Element<V> currentElement = head;
    while (currentEntry != null) {
      currentElement.next = new LRUCacheIterator.Element<V>(currentEntry.value);
      currentElement = currentElement.next;
      currentEntry = currentEntry.next;
    }
    return new LRUCacheIterator<V>(head);
  }

  /**
   * For testing purposes only
   */
  public void printStats() {
    int forwardListLength = 0;
    LRUCacheEntry<K, V> entry = entryQueue;
    while (entry != null) {
      forwardListLength++;
      entry = entry.next;
    }
    System.out.println("Forward length: " + forwardListLength); //$NON-NLS-1$

    int backwardListLength = 0;
    entry = entryQueueTail;
    while (entry != null) {
      backwardListLength++;
      entry = entry.previous;
    }
    System.out.println("Backward length: " + backwardListLength); //$NON-NLS-1$

    Iterator<K> keys = entryTable.keySet().iterator();
    class Temp {
      public Class<?> fClass;
      public int fCount;

      public Temp(Class<?> aClass) {
        fClass = aClass;
        fCount = 1;
      }

      @Override
      public String toString() {
        return "Class: " + fClass + " has " + fCount + " entries."; //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-1$
      }
    }
    HashMap<Class<?>, Temp> h = new HashMap<Class<?>, Temp>();
    while (keys.hasNext()) {
      entry = entryTable.get(keys.next());
      Class<?> key = entry.value.getClass();
      Temp t = h.get(key);
      if (t == null) {
        h.put(key, new Temp(key));
      } else {
        t.fCount++;
      }
    }

    for (Iterator<Temp> iter = h.values().iterator(); iter.hasNext();) {
      System.out.println(iter.next());
    }
  }

  @Override
  public V put(K key, V value) {
    /* attempt to rid ourselves of the overflow, if there is any */
    if (overflow > 0) {
      shrink();
    }

    /* Check whether there's an entry in the cache */
    int newSpace = spaceFor(value);
    LRUCacheEntry<K, V> entry = entryTable.get(key);

    if (entry != null) {

      /**
       * Replace the entry in the cache if it would not overflow the cache. Otherwise flush the
       * entry and re-add it so as to keep cache within budget
       */
      int oldSpace = entry.space;
      int newTotal = currentSpace - oldSpace + newSpace;
      if (newTotal <= spaceLimit) {
        updateTimestamp(entry);
        entry.value = value;
        entry.space = newSpace;
        currentSpace = newTotal;
        overflow = 0;
        return value;
      } else {
        privateRemoveEntry(entry, false, false);
      }
    }

    // attempt to make new space
    makeSpace(newSpace);

    // add without worring about space, it will
    // be handled later in a makeSpace call
    privateAdd(key, value, newSpace);

    return value;
  }

  /**
   * Removes and returns the value in the cache for the given key. If the key is not in the cache,
   * returns null.
   * 
   * @param key Key of object to remove from cache.
   * @return Value removed from cache.
   */
  public V remove(K key) {
    return removeKey(key);
  }

  /**
   * Sets the load factor for the cache. The load factor determines how much space is reclaimed when
   * the cache exceeds its space limit.
   * 
   * @param newLoadFactor double
   * @throws IllegalArgumentException when the new load factor is not in (0.0, 1.0]
   */
  public void setLoadFactor(double newLoadFactor) throws IllegalArgumentException {
    if (newLoadFactor <= 1.0 && newLoadFactor > 0.0) {
      loadFactor = newLoadFactor;
    } else {
      // throw new IllegalArgumentException(Messages.cache_invalidLoadFactor);
      throw new IllegalArgumentException("Invalid load factor, must be between 0.0 and 1.0: "
          + newLoadFactor);
    }
  }

  /**
   * Sets the maximum amount of space that the cache can store
   * 
   * @param limit Number of units of cache space
   */
  @Override
  public void setSpaceLimit(int limit) {
    if (limit < spaceLimit) {
      makeSpace(spaceLimit - limit);
    }
    spaceLimit = limit;
  }

  /**
   * Attempt to shrink the cache if it has overflown. Return <code>true</code> if the cache shrinks
   * to less than or equal to <code>fSpaceLimit</code>.
   * 
   * @return <code>true</code> if we were able to shrink the cache to within its space limit
   */
  public boolean shrink() {
    if (overflow > 0) {
      return makeSpace(0);
    }
    return true;
  }

  @Override
  public String toString() {
    return toStringFillingRation("OverflowingLRUCache ") + //$NON-NLS-1$
        toStringContents();
  }

  /**
   * Returns a new cache containing the same contents.
   * 
   * @return New copy of this object.
   */
  // public Object clone() {
  // OverflowingLRUCache<K, V> newCache = (OverflowingLRUCache<K, V>)
  // newInstance(fSpaceLimit, fOverflow);
  // LRUCacheEntry<K, V> qEntry;
  //
  // /* Preserve order of entries by copying from oldest to newest */
  // qEntry = this.fEntryQueueTail;
  // while (qEntry != null) {
  // newCache.privateAdd (qEntry._fKey, qEntry._fValue, qEntry._fSpace);
  // qEntry = qEntry._fPrevious;
  // }
  // return newCache;
  // }

  /**
   * Return <code>true</code> if the element is successfully closed and removed from the cache.
   * <p>
   * NOTE: this triggers an external remove from the cache by closing the object.
   */
  protected abstract boolean close(LRUCacheEntry<K, V> entry);

  /**
   * Ensures there is the specified amount of free space in the receiver, by removing old entries if
   * necessary. Return <code>true</code> if the requested space was made available. Might not be
   * able to free enough space because some elements cannot be removed until they are saved.
   * 
   * @param space the amount of space to free up
   */
  @Override
  protected boolean makeSpace(int space) {
    int limit = spaceLimit;
    if (overflow == 0 && currentSpace + space <= limit) {
      /* if space is already available */
      return true;
    }

    /* Free up space by removing oldest entries */
    int spaceNeeded = (int) ((1 - loadFactor) * limit);
    spaceNeeded = (spaceNeeded > space) ? spaceNeeded : space;
    LRUCacheEntry<K, V> entry = entryQueueTail;

    try {
      // disable timestamps update while making space so that the previous and
      // next links are not changed
      // (by a call to get(K) for example)
      timestampsOn = false;

      while (currentSpace + spaceNeeded > limit && entry != null) {
        this.privateRemoveEntry(entry, false, false);
        entry = entry.previous;
      }
    } finally {
      timestampsOn = true;
    }

    /* check again, since we may have aquired enough space */
    if (currentSpace + space <= limit) {
      overflow = 0;
      return true;
    }

    /* update fOverflow */
    overflow = currentSpace + space - limit;
    return false;
  }

  /**
   * Return a new instance of the receiver.
   */
  protected abstract LRUCache<K, V> newInstance(int size, int overflow);

  /**
   * Remove the entry from the entry queue. Calls <code>privateRemoveEntry</code> with the external
   * functionality enabled.
   * 
   * @param shuffle indicates whether we are just shuffling the queue (in which case, the entry
   *          table is not modified)
   */
  @Override
  protected void privateRemoveEntry(LRUCacheEntry<K, V> entry, boolean shuffle) {
    privateRemoveEntry(entry, shuffle, true);
  }

  /**
   * Remove the entry from the entry queue. If <i>external</i> is true, the entry is removed without
   * checking if it can be removed. It is assumed that the client has already closed the element it
   * is trying to remove (or will close it promptly). If <i>external</i> is false, and the entry
   * could not be closed, it is not removed and the pointers are not changed.
   * 
   * @param shuffle indicates whether we are just shuffling the queue (in which case, the entry
   *          table is not modified)
   */
  protected void privateRemoveEntry(LRUCacheEntry<K, V> entry, boolean shuffle, boolean external) {

    if (!shuffle) {
      if (external) {
        entryTable.remove(entry.key);
        currentSpace -= entry.space;
      } else {
        if (!close(entry)) {
          return;
        }
        // buffer close will recursively call #privateRemoveEntry with
        // external==true
        // thus entry will already be removed if reaching this point.
        if (entryTable.get(entry.key) == null) {
          return;
        } else {
          // basic removal
          entryTable.remove(entry.key);
          currentSpace -= entry.space;
        }
      }
    }
    LRUCacheEntry<K, V> previous = entry.previous;
    LRUCacheEntry<K, V> next = entry.next;

    /* if this was the first entry */
    if (previous == null) {
      entryQueue = next;
    } else {
      previous.next = next;
    }
    /* if this was the last entry */
    if (next == null) {
      entryQueueTail = previous;
    } else {
      next.previous = previous;
    }
  }

  /**
   * Updates the timestamp for the given entry, ensuring that the queue is kept in correct order.
   * The entry must exist.
   * <p>
   * This method will do nothing if timestamps have been disabled.
   */
  @Override
  protected void updateTimestamp(LRUCacheEntry<K, V> entry) {
    if (timestampsOn) {
      entry.timestamp = timestampCounter++;
      if (entryQueue != entry) {
        this.privateRemoveEntry(entry, true);
        this.privateAddEntry(entry, true);
      }
    }
  }
}
