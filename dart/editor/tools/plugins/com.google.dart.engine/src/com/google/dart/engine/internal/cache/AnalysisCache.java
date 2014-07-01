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
import com.google.dart.engine.context.AnalysisContextStatistics.PartitionData;
import com.google.dart.engine.internal.context.AnalysisContextStatisticsImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.collection.MapIterator;
import com.google.dart.engine.utilities.collection.MultipleMapIterator;

import java.util.Map;

/**
 * Instances of the class {@code AnalysisCache} implement an LRU cache of information related to
 * analysis.
 */
public class AnalysisCache {
  /**
   * An array containing the partitions of which this cache is comprised.
   */
  private CachePartition[] partitions;

  /**
   * A flag used to control whether trace information should be produced when the content of the
   * cache is modified.
   */
  private static final boolean TRACE_CHANGES = false;

  /**
   * Initialize a newly created cache to have the given partitions. The partitions will be searched
   * in the order in which they appear in the array, so the most specific partition (usually an
   * {@link SdkCachePartition}) should be first and the most general (usually a
   * {@link UniversalCachePartition}) last.
   * 
   * @param partitions the partitions for the newly created cache
   */
  public AnalysisCache(CachePartition[] partitions) {
    this.partitions = partitions;
  }

  /**
   * Record that the AST associated with the given source was just read from the cache.
   * 
   * @param source the source whose AST was accessed
   */
  public void accessedAst(Source source) {
    int count = partitions.length;
    for (int i = 0; i < count; i++) {
      if (partitions[i].contains(source)) {
        partitions[i].accessedAst(source);
        return;
      }
    }
  }

  /**
   * Return the entry associated with the given source.
   * 
   * @param source the source whose entry is to be returned
   * @return the entry associated with the given source
   */
  public SourceEntry get(Source source) {
    int count = partitions.length;
    for (int i = 0; i < count; i++) {
      if (partitions[i].contains(source)) {
        return partitions[i].get(source);
      }
    }
    return null;
  }

  /**
   * Return the number of entries in this cache that have an AST associated with them.
   * 
   * @return the number of entries in this cache that have an AST associated with them
   */
  public int getAstSize() {
    return partitions[partitions.length - 1].getAstSize();
  }

  /**
   * Return information about each of the partitions in this cache.
   * 
   * @return information about each of the partitions in this cache
   */
  public PartitionData[] getPartitionData() {
    int count = partitions.length;
    PartitionData[] data = new PartitionData[count];
    for (int i = 0; i < count; i++) {
      CachePartition partition = partitions[i];
      data[i] = new AnalysisContextStatisticsImpl.PartitionDataImpl(
          partition.getAstSize(),
          partition.getMap().size());
    }
    return data;
  }

  /**
   * Return an iterator returning all of the map entries mapping sources to cache entries.
   * 
   * @return an iterator returning all of the map entries mapping sources to cache entries
   */
  @SuppressWarnings("unchecked")
  public MapIterator<Source, SourceEntry> iterator() {
    int count = partitions.length;
    Map<Source, SourceEntry>[] maps = new Map[count];
    for (int i = 0; i < count; i++) {
      maps[i] = partitions[i].getMap();
    }
    return new MultipleMapIterator<Source, SourceEntry>(maps);
  }

  /**
   * Associate the given entry with the given source.
   * 
   * @param source the source with which the entry is to be associated
   * @param entry the entry to be associated with the source
   */
  public void put(Source source, SourceEntry entry) {
    ((SourceEntryImpl) entry).fixExceptionState();
    int count = partitions.length;
    for (int i = 0; i < count; i++) {
      if (partitions[i].contains(source)) {
        if (TRACE_CHANGES) {
          try {
            SourceEntry oldEntry = partitions[i].get(source);
            if (oldEntry == null) {
              AnalysisEngine.getInstance().getLogger().logInformation(
                  "Added a cache entry for '" + source.getFullName() + "'.");
            } else {
              AnalysisEngine.getInstance().getLogger().logInformation(
                  "Modified the cache entry for " + source.getFullName() + "'. Diff = "
                      + ((SourceEntryImpl) entry).getDiff(oldEntry));
            }
          } catch (Throwable exception) {
            // Ignored
            System.currentTimeMillis();
          }
        }
        partitions[i].put(source, entry);
        return;
      }
    }
  }

  /**
   * Remove all information related to the given source from this cache.
   * 
   * @param source the source to be removed
   */
  public void remove(Source source) {
    int count = partitions.length;
    for (int i = 0; i < count; i++) {
      if (partitions[i].contains(source)) {
        if (TRACE_CHANGES) {
          try {
            AnalysisEngine.getInstance().getLogger().logInformation(
                "Removed the cache entry for " + source.getFullName() + "'.");
          } catch (Throwable exception) {
            // Ignored
            System.currentTimeMillis();
          }
        }
        partitions[i].remove(source);
        return;
      }
    }
  }

  /**
   * Record that the AST associated with the given source was just removed from the cache.
   * 
   * @param source the source whose AST was removed
   */
  public void removedAst(Source source) {
    int count = partitions.length;
    for (int i = 0; i < count; i++) {
      if (partitions[i].contains(source)) {
        partitions[i].removedAst(source);
        return;
      }
    }
  }

  /**
   * Return the number of sources that are mapped to cache entries.
   * 
   * @return the number of sources that are mapped to cache entries
   */
  public int size() {
    int size = 0;
    int count = partitions.length;
    for (int i = 0; i < count; i++) {
      size += partitions[i].size();
    }
    return size;
  }

  /**
   * Record that the AST associated with the given source was just stored to the cache.
   * 
   * @param source the source whose AST was stored
   */
  public void storedAst(Source source) {
    int count = partitions.length;
    for (int i = 0; i < count; i++) {
      if (partitions[i].contains(source)) {
        partitions[i].storedAst(source);
        return;
      }
    }
  }
}
