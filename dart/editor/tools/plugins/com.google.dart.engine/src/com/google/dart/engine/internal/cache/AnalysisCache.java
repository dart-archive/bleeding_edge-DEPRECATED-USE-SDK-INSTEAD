/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.internal.context.CacheState;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Instances of the class {@code AnalysisCache} implement an LRU cache of information related to
 * analysis.
 */
public class AnalysisCache {
  /**
   * A table mapping the sources known to the context to the information known about the source.
   */
  private final HashMap<Source, SourceEntry> sourceMap = new HashMap<Source, SourceEntry>();

  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  private int maxCacheSize;

  /**
   * A list containing the most recently accessed sources with the most recently used at the end of
   * the list. When more sources are added than the maximum allowed then the least recently used
   * source will be removed and will have it's cached AST structure flushed.
   */
  private ArrayList<Source> recentlyUsed;

  /**
   * An array containing sources for which data should not be flushed.
   */
  private Source[] priorityOrder = Source.EMPTY_ARRAY;

  /**
   * The number of times that the flushing of information from the cache has been disabled without
   * being re-enabled.
   */
  private int cacheRemovalCount = 0;

  /**
   * Initialize a newly created cache to maintain at most the given number of AST structures in the
   * cache.
   * 
   * @param maxCacheSize the maximum number of sources for which AST structures should be kept in
   *          the cache
   */
  public AnalysisCache(int maxCacheSize) {
    this.maxCacheSize = maxCacheSize;
    recentlyUsed = new ArrayList<Source>(maxCacheSize);
  }

  /**
   * Record that the given source was just accessed.
   * 
   * @param source the source that was accessed
   */
  public void accessed(Source source) {
    if (recentlyUsed.remove(source)) {
      recentlyUsed.add(source);
      return;
    }
    if (cacheRemovalCount == 0 && recentlyUsed.size() >= maxCacheSize) {
      flushAstFromCache();
    }
    recentlyUsed.add(source);
  }

  /**
   * Disable flushing information from the cache until {@link #enableCacheRemoval()} has been
   * called.
   */
  public void disableCacheRemoval() {
    cacheRemovalCount++;
  }

  /**
   * Re-enable flushing information from the cache.
   */
  public void enableCacheRemoval() {
    if (cacheRemovalCount > 0) {
      cacheRemovalCount--;
    }
    if (cacheRemovalCount == 0) {
      while (recentlyUsed.size() > maxCacheSize) {
        flushAstFromCache();
      }
    }
  }

  /**
   * Return a set containing all of the map entries mapping sources to cache entries. Clients should
   * not modify the returned set.
   * 
   * @return a set containing all of the map entries mapping sources to cache entries
   */
  public Set<Entry<Source, SourceEntry>> entrySet() {
    return sourceMap.entrySet();
  }

  /**
   * Return the entry associated with the given source.
   * 
   * @param source the source whose entry is to be returned
   * @return the entry associated with the given source
   */
  public SourceEntry get(Source source) {
    return sourceMap.get(source);
  }

  /**
   * Return an array containing sources for which data should not be flushed.
   * 
   * @return an array containing sources for which data should not be flushed
   */
  public Source[] getPriorityOrder() {
    return priorityOrder;
  }

  /**
   * Associate the given entry with the given source.
   * 
   * @param source the source with which the entry is to be associated
   * @param entry the entry to be associated with the source
   */
  public void put(Source source, SourceEntry entry) {
    sourceMap.put(source, entry);
  }

  /**
   * Remove all information related to the given source from this cache.
   * 
   * @param source the source to be removed
   */
  public void remove(Source source) {
    sourceMap.remove(source);
  }

  /**
   * Set the sources for which data should not be flushed to the given array.
   * 
   * @param sources the sources for which data should not be flushed
   */
  public void setPriorityOrder(Source[] sources) {
    priorityOrder = sources;
  }

  /**
   * Flush one AST structure from the cache.
   */
  private void flushAstFromCache() {
    Source removedSource = removeAstToFlush();
    SourceEntry sourceEntry = sourceMap.get(removedSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      htmlCopy.setState(HtmlEntry.PARSED_UNIT, CacheState.FLUSHED);
      sourceMap.put(removedSource, htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
      DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
      dartCopy.flushAstStructures();
      sourceMap.put(removedSource, dartCopy);
    }
  }

  /**
   * Return {@code true} if the given source is in the array of priority sources.
   * 
   * @return {@code true} if the given source is in the array of priority sources
   */
  private boolean isPrioritySource(Source source) {
    for (Source prioritySource : priorityOrder) {
      if (source.equals(prioritySource)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Remove and return one source from the list of recently used sources whose AST structure can be
   * flushed from the cache. The source that will be returned will be the source that has been
   * unreferenced for the longest period of time but that is not a priority for analysis.
   * 
   * @return the source that was removed
   */
  private Source removeAstToFlush() {
    for (int i = 0; i < recentlyUsed.size(); i++) {
      Source source = recentlyUsed.get(i);
      if (!isPrioritySource(source)) {
        return recentlyUsed.remove(i);
      }
    }
    AnalysisEngine.getInstance().getLogger().logError(
        "Internal error: The number of priority sources (" + priorityOrder.length
            + ") is greater than the maximum cache size (" + maxCacheSize + ")",
        new Exception());
    return recentlyUsed.remove(0);
  }
}
