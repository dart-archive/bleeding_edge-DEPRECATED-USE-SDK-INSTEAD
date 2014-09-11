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

import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.collection.MapIterator;
import com.google.dart.engine.utilities.collection.SingleMapIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code CachePartition} implement a single partition in an LRU cache of
 * information related to analysis.
 */
public abstract class CachePartition {
  /**
   * The context that owns this partition. Multiple contexts can reference a partition, but only one
   * context can own it.
   */
  private InternalAnalysisContext context;

  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  private int maxCacheSize;

  /**
   * The policy used to determine which pieces of data to remove from the cache.
   */
  private CacheRetentionPolicy retentionPolicy;

  /**
   * A table mapping the sources belonging to this partition to the information known about those
   * sources.
   */
  private final HashMap<Source, SourceEntry> sourceMap = new HashMap<Source, SourceEntry>();

  /**
   * A list containing the most recently accessed sources with the most recently used at the end of
   * the list. When more sources are added than the maximum allowed then the least recently used
   * source will be removed and will have it's cached AST structure flushed.
   */
  private ArrayList<Source> recentlyUsed;

  /**
   * Initialize a newly created cache to maintain at most the given number of AST structures in the
   * cache.
   * 
   * @param context the context that owns this partition
   * @param maxCacheSize the maximum number of sources for which AST structures should be kept in
   *          the cache
   * @param retentionPolicy the policy used to determine which pieces of data to remove from the
   *          cache
   */
  public CachePartition(InternalAnalysisContext context, int maxCacheSize,
      CacheRetentionPolicy retentionPolicy) {
    this.context = context;
    this.maxCacheSize = maxCacheSize;
    this.retentionPolicy = retentionPolicy;
    recentlyUsed = new ArrayList<Source>(maxCacheSize);
  }

  /**
   * Record that the AST associated with the given source was just read from the cache.
   * 
   * @param source the source whose AST was accessed
   */
  public void accessedAst(Source source) {
    if (recentlyUsed.remove(source)) {
      recentlyUsed.add(source);
      return;
    }
    while (recentlyUsed.size() >= maxCacheSize) {
      if (!flushAstFromCache()) {
        break;
      }
    }
    recentlyUsed.add(source);
  }

  /**
   * Return {@code true} if the given source is contained in this partition.
   * 
   * @param source the source being tested
   * @return {@code true} if the source is contained in this partition
   */
  public abstract boolean contains(Source source);

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
   * Return the number of entries in this partition that have an AST associated with them.
   * 
   * @return the number of entries in this partition that have an AST associated with them
   */
  public int getAstSize() {
    int astSize = 0;
    int count = recentlyUsed.size();
    for (int i = 0; i < count; i++) {
      Source source = recentlyUsed.get(i);
      SourceEntry sourceEntry = sourceMap.get(source);
      if (sourceEntry instanceof DartEntry) {
        if (((DartEntry) sourceEntry).getAnyParsedCompilationUnit() != null) {
          astSize++;
        }
      } else if (sourceEntry instanceof HtmlEntry) {
        if (((HtmlEntry) sourceEntry).getAnyParsedUnit() != null) {
          astSize++;
        }
      }
    }
    return astSize;
  }

  /**
   * Return the context that owns this partition.
   * 
   * @return the context that owns this partition
   */
  public InternalAnalysisContext getContext() {
    return context;
  }

  /**
   * Return a table mapping the sources known to the context to the information known about the
   * source.
   * <p>
   * <b>Note:</b> This method is only visible for use by {@link AnalysisCache} and should not be
   * used for any other purpose.
   * 
   * @return a table mapping the sources known to the context to the information known about the
   *         source
   */
  public Map<Source, SourceEntry> getMap() {
    return sourceMap;
  }

  /**
   * Return an iterator returning all of the map entries mapping sources to cache entries.
   * 
   * @return an iterator returning all of the map entries mapping sources to cache entries
   */
  public MapIterator<Source, SourceEntry> iterator() {
    return new SingleMapIterator<Source, SourceEntry>(sourceMap);
  }

  /**
   * Associate the given entry with the given source.
   * 
   * @param source the source with which the entry is to be associated
   * @param entry the entry to be associated with the source
   */
  public void put(Source source, SourceEntry entry) {
    ((SourceEntryImpl) entry).fixExceptionState();
    sourceMap.put(source, entry);
  }

  /**
   * Remove all information related to the given source from this cache.
   * 
   * @param source the source to be removed
   */
  public void remove(Source source) {
    recentlyUsed.remove(source);
    sourceMap.remove(source);
  }

  /**
   * Record that the AST associated with the given source was just removed from the cache.
   * 
   * @param source the source whose AST was removed
   */
  public void removedAst(Source source) {
    recentlyUsed.remove(source);
  }

  /**
   * Set the maximum size of the cache to the given size.
   * 
   * @param size the maximum number of sources for which AST structures should be kept in the cache
   */
  public void setMaxCacheSize(int size) {
    maxCacheSize = size;
    while (recentlyUsed.size() > maxCacheSize) {
      if (!flushAstFromCache()) {
        break;
      }
    }
  }

  /**
   * Return the number of sources that are mapped to cache entries.
   * 
   * @return the number of sources that are mapped to cache entries
   */
  public int size() {
    return sourceMap.size();
  }

  /**
   * Record that the AST associated with the given source was just stored to the cache.
   * 
   * @param source the source whose AST was stored
   */
  public void storedAst(Source source) {
    if (recentlyUsed.contains(source)) {
      return;
    }
    while (recentlyUsed.size() >= maxCacheSize) {
      if (!flushAstFromCache()) {
        break;
      }
    }
    recentlyUsed.add(source);
  }

  /**
   * Attempt to flush one AST structure from the cache.
   * 
   * @return {@code true} if a structure was flushed
   */
  private boolean flushAstFromCache() {
    Source removedSource = removeAstToFlush();
    if (removedSource == null) {
      return false;
    }
    SourceEntry sourceEntry = sourceMap.get(removedSource);
    if (sourceEntry instanceof HtmlEntry) {
      HtmlEntryImpl htmlCopy = ((HtmlEntry) sourceEntry).getWritableCopy();
      htmlCopy.flushAstStructures();
      sourceMap.put(removedSource, htmlCopy);
    } else if (sourceEntry instanceof DartEntry) {
      DartEntryImpl dartCopy = ((DartEntry) sourceEntry).getWritableCopy();
      dartCopy.flushAstStructures();
      sourceMap.put(removedSource, dartCopy);
    }
    return true;
  }

  /**
   * Remove and return one source from the list of recently used sources whose AST structure can be
   * flushed from the cache. The source that will be returned will be the source that has been
   * unreferenced for the longest period of time but that is not a priority for analysis.
   * 
   * @return the source that was removed
   */
  private Source removeAstToFlush() {
    int sourceToRemove = -1;
    for (int i = 0; i < recentlyUsed.size(); i++) {
      Source source = recentlyUsed.get(i);
      RetentionPriority priority = retentionPolicy.getAstPriority(source, sourceMap.get(source));
      if (priority == RetentionPriority.LOW) {
        return recentlyUsed.remove(i);
      } else if (priority == RetentionPriority.MEDIUM && sourceToRemove < 0) {
        sourceToRemove = i;
      }
    }
    if (sourceToRemove < 0) {
      // This happens if the retention policy returns a priority of HIGH for all of the sources that
      // have been recently used. This is the case, for example, when the list of priority sources
      // is bigger than the current cache size.
      return null;
    }
    return recentlyUsed.remove(sourceToRemove);
  }
}
