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

import com.google.dart.engine.context.AnalysisContentStatistics;
import com.google.dart.engine.internal.cache.CacheState;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DataDescriptor;
import com.google.dart.engine.source.Source;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the {@link AnalysisContentStatistics}.
 */
public class AnalysisContentStatisticsImpl implements AnalysisContentStatistics {
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

  @Override
  public CacheRow[] getCacheRows() {
    Collection<CacheRow> items = dataMap.values();
    return items.toArray(new CacheRow[items.size()]);
  }

  public void putCacheItem(DartEntry dartEntry, DataDescriptor<?> descriptor) {
    putCacheItem(descriptor, dartEntry.getState(descriptor));
  }

  public void putCacheItem(DartEntry dartEntry, Source librarySource, DataDescriptor<?> descriptor) {
    putCacheItem(descriptor, dartEntry.getState(descriptor, librarySource));
  }

  public void putCacheItem(DataDescriptor<?> rowDesc, CacheState state) {
    // prepare state -> count map
    String rowName = rowDesc.toString();
    CacheRowImpl row = (CacheRowImpl) dataMap.get(rowName);
    if (row == null) {
      row = new CacheRowImpl(rowName);
      dataMap.put(rowName, row);
    }
    // increment count
    row.incState(state);
  }
}
