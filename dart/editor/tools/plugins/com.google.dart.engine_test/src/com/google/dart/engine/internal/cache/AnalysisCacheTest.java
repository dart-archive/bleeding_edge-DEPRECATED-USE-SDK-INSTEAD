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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class AnalysisCacheTest extends EngineTestCase {
  public void test_creation() {
    assertNotNull(new AnalysisCache(8, null));
  }

  public void test_entrySet() {
    AnalysisCache cache = new AnalysisCache(8, null);
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    cache.put(source, entry);
    Collection<Entry<Source, SourceEntry>> result = cache.entrySet();
    assertCollectionSize(1, result);
    Map.Entry<Source, SourceEntry> mapEntry = result.iterator().next();
    assertSame(source, mapEntry.getKey());
    assertSame(entry, mapEntry.getValue());
  }

  public void test_get() {
    AnalysisCache cache = new AnalysisCache(8, null);
    TestSource source = new TestSource();
    assertNull(cache.get(source));
  }

  public void test_put_noFlush() {
    AnalysisCache cache = new AnalysisCache(8, null);
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    cache.put(source, entry);
    assertSame(entry, cache.get(source));
  }

  public void test_setMaxCacheSize() {
    AnalysisCache cache = new AnalysisCache(8, new CacheRetentionPolicy() {
      @Override
      public RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry) {
        return RetentionPriority.LOW;
      }
    });
    int size = 6;
    for (int i = 0; i < size; i++) {
      Source source = new TestSource(createFile("/test" + i + ".dart"), "");
      DartEntryImpl entry = new DartEntryImpl();
      entry.setValue(DartEntry.PARSED_UNIT, null);
      cache.put(source, entry);
      cache.accessedAst(source);
    }
    assertNonFlushedCount(size, cache);
    int newSize = size - 2;
    cache.setMaxCacheSize(newSize);
    assertNonFlushedCount(newSize, cache);
  }

  public void test_size() {
    AnalysisCache cache = new AnalysisCache(8, null);
    int size = 4;
    for (int i = 0; i < size; i++) {
      Source source = new TestSource(createFile("/test" + i + ".dart"), "");
      cache.put(source, new DartEntryImpl());
      cache.accessedAst(source);
    }
    assertEquals(size, cache.size());
  }

  private void assertNonFlushedCount(int expectedCount, AnalysisCache cache) {
    int nonFlushedCount = 0;
    for (Map.Entry<Source, SourceEntry> entry : cache.entrySet()) {
      if (entry.getValue().getState(DartEntry.PARSED_UNIT) != CacheState.FLUSHED) {
        nonFlushedCount++;
      }
    }
    assertEquals(expectedCount, nonFlushedCount);
  }
}
