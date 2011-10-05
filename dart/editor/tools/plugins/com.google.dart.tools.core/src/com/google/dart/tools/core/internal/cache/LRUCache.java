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

import com.google.dart.tools.core.internal.util.ToStringSorter;
import com.google.dart.tools.core.model.DartElement;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Instances of the class <code>LRUCache</code> implement a map that stores a finite number of
 * elements. When an attempt is made to add values to a full cache, the least recently used values
 * in the cache are discarded to make room for the new values as necessary.
 * <p>
 * The data structure is based on the LRU virtual memory paging scheme.
 * <p>
 * Elements can take up a variable amount of cache space by implementing the {@link LRUCacheable}
 * interface.
 * <p>
 * This implementation is NOT thread-safe. Synchronization wrappers would have to be added to ensure
 * atomic insertions and deletions from the cache.
 */
public class LRUCache<K, V> {
  /**
   * Instances of the class <code>LRUCacheEntry</code> are used internally by the LRUCache to
   * represent entries stored in the cache.
   */
  protected static class LRUCacheEntry<K, V> {
    /**
     * The key for the entry.
     */
    public K key;

    /**
     * The value for the entry.
     */
    public V value;

    /**
     * Time value for queue sorting.
     */
    public int timestamp;

    /**
     * Cache footprint of this entry.
     */
    public int space;

    /**
     * The previous entry in queue.
     */
    public LRUCacheEntry<K, V> previous;

    /**
     * The next entry in queue.
     */
    public LRUCacheEntry<K, V> next;

    /**
     * Initialize a newly created instance of the receiver with the provided values for key, value,
     * and space.
     */
    public LRUCacheEntry(K key, V value, int space) {
      this.key = key;
      this.value = value;
      this.space = space;
    }

    /**
     * Return a String that represents the value of this object.
     */
    @Override
    public String toString() {
      return "LRUCacheEntry [" + key + "-->" + value + "]"; //$NON-NLS-3$ //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /**
   * Amount of cache space used so far
   */
  protected int currentSpace;

  /**
   * Maximum space allowed in cache
   */
  protected int spaceLimit;

  /**
   * Counter for handing out sequential timestamps
   */
  protected int timestampCounter;

  /**
   * Hash table for fast random access to cache entries
   */
  protected HashMap<K, LRUCacheEntry<K, V>> entryTable;

  /**
   * Start of queue (most recently used entry)
   */
  protected LRUCacheEntry<K, V> entryQueue;

  /**
   * End of queue (least recently used entry)
   */
  protected LRUCacheEntry<K, V> entryQueueTail;

  /**
   * Default amount of space in the cache
   */
  protected static final int DEFAULT_SPACELIMIT = 100;

  /**
   * Creates a new cache. Size of cache is defined by <code>DEFAULT_SPACELIMIT</code>.
   */
  public LRUCache() {
    this(DEFAULT_SPACELIMIT);
  }

  /**
   * Creates a new cache.
   * 
   * @param size Size of Cache
   */
  public LRUCache(int size) {
    timestampCounter = currentSpace = 0;
    entryQueue = entryQueueTail = null;
    entryTable = new HashMap<K, LRUCacheEntry<K, V>>(size);
    spaceLimit = size;
  }

  /**
   * Return a new cache containing the same contents.
   * 
   * @return a new cache containing the same contents
   */
  // public Object clone() {
  // LRUCache<K, V> newCache = newInstance(fSpaceLimit);
  // LRUCacheEntry<K, V> qEntry;
  //
  // /* Preserve order of entries by copying from oldest to newest */
  // qEntry = fEntryQueueTail;
  // while (qEntry != null) {
  // newCache.privateAdd (qEntry._fKey, qEntry._fValue, qEntry._fSpace);
  // qEntry = qEntry._fPrevious;
  // }
  // return newCache;
  // }

  public double fillingRatio() {
    return (currentSpace) * 100.0 / spaceLimit;
  }

  /**
   * Flushes all entries from the cache.
   */
  public void flush() {
    currentSpace = 0;
    LRUCacheEntry<K, V> entry = entryQueueTail; // Remember last entry
    entryTable = new HashMap<K, LRUCacheEntry<K, V>>(); // Clear it out
    entryQueue = entryQueueTail = null;
    while (entry != null) { // send deletion notifications in LRU order
      entry = entry.previous;
    }
  }

  /**
   * Flushes the given entry from the cache. Does nothing if entry does not exist in cache.
   * 
   * @param key the key of the object to flush
   */
  public void flush(K key) {
    LRUCacheEntry<K, V> entry = entryTable.get(key);
    /* If entry does not exist, return */
    if (entry == null) {
      return;
    }
    privateRemoveEntry(entry, false);
  }

  /**
   * Return the value in the cache at the given key, or <code>null</code> if the value is not in the
   * cache.
   * 
   * @param key the key of the object to retrieve
   * @return the value in the cache at the given key
   */
  public V get(K key) {
    LRUCacheEntry<K, V> entry = entryTable.get(key);
    if (entry == null) {
      return null;
    }
    updateTimestamp(entry);
    return entry.value;
  }

  /**
   * Return the amount of space that is currently used in the cache.
   * 
   * @return the amount of space that is currently used in the cache
   */
  public int getCurrentSpace() {
    return currentSpace;
  }

  /**
   * Return the existing key that is equal to the given key. If the key is not in the cache, return
   * the given key.
   * 
   * @param key the key to be retrieved
   * @return the existing key that is equal to the given key
   */
  public K getKey(K key) {
    LRUCacheEntry<K, V> entry = this.entryTable.get(key);
    if (entry == null) {
      return key;
    }
    return entry.key;
  }

  /**
   * Return the maximum amount of space available in the cache.
   * 
   * @return the maximum amount of space available in the cache
   */
  public int getSpaceLimit() {
    return spaceLimit;
  }

  /**
   * Return a set containing the keys currently in the cache.
   * 
   * @return a set containing the keys currently in the cache
   */
  public Set<K> keySet() {

    return entryTable.keySet();
  }

  /**
   * Return the value in the cache at the given key. If the value is not in the cache, returns
   * <code>null</code>. This function does not modify timestamps.
   */
  public V peek(K key) {
    LRUCacheEntry<K, V> entry = entryTable.get(key);
    if (entry == null) {
      return null;
    }
    return entry.value;
  }

  /**
   * Set the value in the cache at the given key. Return the value.
   * 
   * @param key the key of the object to add
   * @param value the object to add
   * @return the added value
   */
  public V put(K key, V value) {
    int newSpace, oldSpace, newTotal;
    LRUCacheEntry<K, V> entry;

    /* Check whether there's an entry in the cache */
    newSpace = spaceFor(value);
    entry = entryTable.get(key);

    if (entry != null) {
      /*
       * Replace the entry in the cache if it would not overflow the cache. Otherwise flush the
       * entry and re-add it so as to keep cache within budget
       */
      oldSpace = entry.space;
      newTotal = getCurrentSpace() - oldSpace + newSpace;
      if (newTotal <= getSpaceLimit()) {
        updateTimestamp(entry);
        entry.value = value;
        entry.space = newSpace;
        currentSpace = newTotal;
        return value;
      } else {
        privateRemoveEntry(entry, false);
      }
    }
    if (makeSpace(newSpace)) {
      privateAdd(key, value, newSpace);
    }
    return value;
  }

  /**
   * Remove and return the value in the cache for the given key. If the key is not in the cache,
   * returns <code>null</code>.
   * 
   * @param key the key of the object to remove from cache
   * @return the value that was removed from the cache
   */
  public V removeKey(K key) {
    LRUCacheEntry<K, V> entry = entryTable.get(key);
    if (entry == null) {
      return null;
    }
    V value = entry.value;
    privateRemoveEntry(entry, false);
    return value;
  }

  /**
   * Sets the maximum amount of space that the cache can store
   * 
   * @param limit the number of units of cache space
   */
  public void setSpaceLimit(int limit) {
    if (limit < spaceLimit) {
      makeSpace(spaceLimit - limit);
    }
    spaceLimit = limit;
  }

  /**
   * Return a String that represents the value of this object. This method is for debugging purposes
   * only.
   * 
   * @return a String that represents the value of this object
   */
  @Override
  public String toString() {
    return toStringFillingRation("LRUCache") + //$NON-NLS-1$
        toStringContents();
  }

  public String toStringFillingRation(String cacheName) {
    StringBuffer buffer = new StringBuffer(cacheName);
    buffer.append('[');
    buffer.append(getSpaceLimit());
    buffer.append("]: "); //$NON-NLS-1$
    buffer.append(NumberFormat.getInstance().format(fillingRatio()));
    buffer.append("% full"); //$NON-NLS-1$
    return buffer.toString();
  }

  /**
   * Return an enumeration that iterates over all the keys and values currently in the cache.
   */
  // public ICacheEnumeration keysAndValues() {
  // return new ICacheEnumeration() {
  // Enumeration fValues = fEntryTable.elements();
  // LRUCacheEntry<K, V> fEntry;
  //
  // public boolean hasMoreElements() {
  // return fValues.hasMoreElements();
  // }
  //
  // public K nextElement() {
  // fEntry = fValues.nextElement();
  // return fEntry._fKey;
  // }
  //
  // public V getValue() {
  // if (fEntry == null) {
  // throw new java.util.NoSuchElementException();
  // }
  // return fEntry._fValue;
  // }
  // };
  // }

  /**
   * Ensure there is the specified amount of free space in the receiver, by removing old entries if
   * necessary. Return <code>true</code> if the requested space was made available.
   * 
   * @param space the amount of space to free up
   */
  protected boolean makeSpace(int space) {
    int limit;

    limit = getSpaceLimit();

    /* if space is already available */
    if (currentSpace + space <= limit) {
      return true;
    }

    /* if entry is too big for cache */
    if (space > limit) {
      return false;
    }

    /* Free up space by removing oldest entries */
    while (currentSpace + space > limit && entryQueueTail != null) {
      privateRemoveEntry(entryQueueTail, false);
    }
    return true;
  }

  /**
   * Return a new LRUCache instance.
   */
  protected LRUCache<K, V> newInstance(int size) {
    return new LRUCache<K, V>(size);
  }

  /**
   * Add an entry for the given key/value/space.
   */
  protected void privateAdd(K key, V value, int space) {
    LRUCacheEntry<K, V> entry = new LRUCacheEntry<K, V>(key, value, space);
    privateAddEntry(entry, false);
  }

  /**
   * Add the given entry to the receiver.
   * 
   * @param shuffle indicates whether we are just shuffling the queue (in which case, the entry
   *          table is not modified)
   */
  protected void privateAddEntry(LRUCacheEntry<K, V> entry, boolean shuffle) {
    if (!shuffle) {
      entryTable.put(entry.key, entry);
      currentSpace += entry.space;
    }

    entry.timestamp = timestampCounter++;
    entry.next = entryQueue;
    entry.previous = null;

    if (entryQueue == null) {
      /* this is the first and last entry */
      entryQueueTail = entry;
    } else {
      entryQueue.previous = entry;
    }

    entryQueue = entry;
  }

  /**
   * The given entry has fallen off the bottom of the LRU queue. Subclasses could over-ride this to
   * implement a persistent cache below the LRU cache.
   * 
   * @param entry the entry that has been removed from the cache
   */
  protected void privateNotifyDeletionFromCache(LRUCacheEntry<K, V> entry) {
    // Default is NOP.
  }

  /**
   * Remove the given entry from the entry queue.
   * 
   * @param shuffle indicates whether we are just shuffling the queue (in which case, the entry
   *          table is not modified)
   */
  protected void privateRemoveEntry(LRUCacheEntry<K, V> entry, boolean shuffle) {
    LRUCacheEntry<K, V> previous, next;

    previous = entry.previous;
    next = entry.next;

    if (!shuffle) {
      entryTable.remove(entry.key);
      currentSpace -= entry.space;
    }

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
   * Return the space taken by the given value.
   * 
   * @return the space taken by the given value
   */
  protected int spaceFor(V value) {
    if (value instanceof LRUCacheable) {
      return ((LRUCacheable) value).getCacheFootprint();
    } else {
      return 1;
    }
  }

  /**
   * Return a String that represents the contents of this object. This method is for debugging
   * purposes only.
   * 
   * @return a String that represents the contents of this object
   */
  @SuppressWarnings("unchecked")
  protected String toStringContents() {
    StringBuffer result = new StringBuffer();
    int length = entryTable.size();
    Object[] unsortedKeys = new Object[length];
    String[] unsortedToStrings = new String[length];
    Iterator<K> keys = keySet().iterator();
    for (int i = 0; i < length; i++) {
      K key = keys.next();
      unsortedKeys[i] = key;
      unsortedToStrings[i] = (key instanceof DartElement) ? ((DartElement) key).getElementName()
          : key.toString();
    }
    ToStringSorter<Object> sorter = new ToStringSorter<Object>();
    sorter.sort(unsortedKeys, unsortedToStrings);
    Object[] sortedObjects = sorter.getSortedObjects();
    String[] sortedStrings = sorter.getSortedStrings();
    for (int i = 0; i < length; i++) {
      String toString = sortedStrings[i];
      Object value = get((K) sortedObjects[i]);
      result.append(toString);
      result.append(" -> "); //$NON-NLS-1$
      result.append(value);
      result.append("\n"); //$NON-NLS-1$
    }
    return result.toString();
  }

  /**
   * Updates the timestamp for the given entry, ensuring that the queue is kept in correct order.
   * The entry must exist.
   */
  protected void updateTimestamp(LRUCacheEntry<K, V> entry) {
    entry.timestamp = timestampCounter++;
    if (entryQueue != entry) {
      privateRemoveEntry(entry, true);
      privateAddEntry(entry, true);
    }
    return;
  }
}
