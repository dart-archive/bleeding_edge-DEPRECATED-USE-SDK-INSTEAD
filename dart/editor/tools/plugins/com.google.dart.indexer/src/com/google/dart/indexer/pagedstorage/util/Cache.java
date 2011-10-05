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

/**
 * The cache keeps frequently used objects in the main memory.
 */
public interface Cache {
  /**
   * Clear the cache.
   */
  void clear();

  /**
   * Get an element from the cache if it is available. This will not move the item to the front of
   * the list.
   * 
   * @param pos the unique key of the element
   * @return the element or null
   */
  CacheObject find(int pos);

  /**
   * Get an element in the cache if it is available. This will move the item to the front of the
   * list.
   * 
   * @param pos the unique key of the element
   * @return the element or null
   */
  CacheObject get(int pos);

  /**
   * Get all objects in the cache that have been changed.
   * 
   * @return the list of objects
   */
  ObjectArray<CacheObject> getAllChanged();

  /**
   * Get the maximum size in words (4 bytes).
   * 
   * @return the maximum size in number of double words (4 bytes)
   */
  int getMaxSize();

  /**
   * Get the used size in words (4 bytes).
   * 
   * @return the current size in number of double words (4 bytes)
   */
  int getSize();

  /**
   * Get the name of the cache type in a human readable form.
   * 
   * @return the cache type name
   */
  String getTypeName();

  /**
   * Add an element to the cache. Other items may fall out of the cache because of this. It is not
   * allowed to add the same record twice.
   * 
   * @param r the object
   */
  void put(CacheObject r) throws PagedStorageException;

  /**
   * Remove an object from the cache.
   * 
   * @param pos the unique key of the element
   */
  void remove(int pos);

  /**
   * Set the maximum memory to be used by this cache.
   * 
   * @param size in number of double words (4 bytes)
   */
  void setMaxSize(int size) throws PagedStorageException;

  /**
   * Update an element in the cache. This will move the item to the front of the list.
   * 
   * @param pos the unique key of the element
   * @param record the element
   * @return the element
   */
  CacheObject update(int pos, CacheObject record) throws PagedStorageException;
}
