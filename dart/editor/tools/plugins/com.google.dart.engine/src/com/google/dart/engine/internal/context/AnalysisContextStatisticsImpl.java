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

package com.google.dart.engine.internal.context;

import com.google.dart.engine.context.AnalysisContextStatistics;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.cache.CacheState;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DataDescriptor;
import com.google.dart.engine.internal.cache.SourceEntry;
import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Implementation of the {@link AnalysisContextStatistics}.
 */
public class AnalysisContextStatisticsImpl implements AnalysisContextStatistics {
  public static class PartitionDataImpl implements PartitionData {
    private int astCount;

    private int totalCount;

    public PartitionDataImpl(int astCount, int totalCount) {
      this.astCount = astCount;
      this.totalCount = totalCount;
    }

    @Override
    public int getAstCount() {
      return astCount;
    }

    @Override
    public int getTotalCount() {
      return totalCount;
    }
  }
  private static class CacheRowImpl implements CacheRow {
    private final String name;
    private int errorCount;
    private int flushedCount;
    private int inProcessCount;
    private int invalidCount;
    private int validCount;

    public CacheRowImpl(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof CacheRowImpl && ((CacheRowImpl) obj).name.equals(name);
    }

    @Override
    public int getErrorCount() {
      return errorCount;
    }

    @Override
    public int getFlushedCount() {
      return flushedCount;
    }

    @Override
    public int getInProcessCount() {
      return inProcessCount;
    }

    @Override
    public int getInvalidCount() {
      return invalidCount;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int getValidCount() {
      return validCount;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    void incState(CacheState state) {
      if (state == CacheState.ERROR) {
        errorCount++;
      }
      if (state == CacheState.FLUSHED) {
        flushedCount++;
      }
      if (state == CacheState.IN_PROCESS) {
        inProcessCount++;
      }
      if (state == CacheState.INVALID) {
        invalidCount++;
      }
      if (state == CacheState.VALID) {
        validCount++;
      }
    }
  }

  private final Map<String, CacheRow> dataMap = new HashMap<String, CacheRow>();

  private ArrayList<Source> sources = new ArrayList<Source>();

  private HashSet<AnalysisException> exceptions = new HashSet<AnalysisException>();

  private PartitionData[] partitionData;

  public void addSource(Source source) {
    sources.add(source);
  }

  @Override
  public CacheRow[] getCacheRows() {
    Collection<CacheRow> items = dataMap.values();
    return items.toArray(new CacheRow[items.size()]);
  }

  @Override
  public AnalysisException[] getExceptions() {
    return exceptions.toArray(new AnalysisException[exceptions.size()]);
  }

  @Override
  public PartitionData[] getPartitionData() {
    return partitionData;
  }

  @Override
  public Source[] getSources() {
    return sources.toArray(new Source[sources.size()]);
  }

  public void putCacheItem(SourceEntry dartEntry, DataDescriptor<?> descriptor) {
    internalPutCacheItem(dartEntry, descriptor, dartEntry.getState(descriptor));
  }

  public void putCacheItemInLibrary(DartEntry dartEntry, Source librarySource,
      DataDescriptor<?> descriptor) {
    internalPutCacheItem(
        dartEntry,
        descriptor,
        dartEntry.getStateInLibrary(descriptor, librarySource));
  }

  /**
   * Set the partition data returned by this object to the given data.
   */
  public void setPartitionData(PartitionData[] data) {
    partitionData = data;
  }

  private void internalPutCacheItem(SourceEntry dartEntry, DataDescriptor<?> rowDesc,
      CacheState state) {
    String rowName = rowDesc.toString();
    CacheRowImpl row = (CacheRowImpl) dataMap.get(rowName);
    if (row == null) {
      row = new CacheRowImpl(rowName);
      dataMap.put(rowName, row);
    }
    row.incState(state);

    if (state == CacheState.ERROR) {
      AnalysisException exception = dartEntry.getException();
      if (exception != null) {
        exceptions.add(exception);
      }
    }
  }
}
