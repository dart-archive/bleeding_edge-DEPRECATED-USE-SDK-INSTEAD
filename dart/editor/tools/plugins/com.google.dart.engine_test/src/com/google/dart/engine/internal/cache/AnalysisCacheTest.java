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
import com.google.dart.engine.utilities.collection.MapIterator;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class AnalysisCacheTest extends EngineTestCase {
  public void test_creation() {
    assertNotNull(new AnalysisCache(new CachePartition[0]));
  }

  public void test_get() {
    AnalysisCache cache = new AnalysisCache(new CachePartition[0]);
    TestSource source = new TestSource();
    assertNull(cache.get(source));
  }

  public void test_iterator() {
    CachePartition partition = new UniversalCachePartition(null, 8, new DefaultRetentionPolicy());
    AnalysisCache cache = new AnalysisCache(new CachePartition[] {partition});
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    cache.put(source, entry);
    MapIterator<Source, SourceEntry> iterator = cache.iterator();
    assertTrue(iterator.moveNext());
    assertSame(source, iterator.getKey());
    assertSame(entry, iterator.getValue());
    assertFalse(iterator.moveNext());
  }

  public void test_put_noFlush() {
    CachePartition partition = new UniversalCachePartition(null, 8, new DefaultRetentionPolicy());
    AnalysisCache cache = new AnalysisCache(new CachePartition[] {partition});
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    cache.put(source, entry);
    assertSame(entry, cache.get(source));
  }

  public void test_setMaxCacheSize() {
    CachePartition partition = new UniversalCachePartition(null, 8, new CacheRetentionPolicy() {
      @Override
      public RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry) {
        return RetentionPriority.LOW;
      }
    });
    AnalysisCache cache = new AnalysisCache(new CachePartition[] {partition});
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
    partition.setMaxCacheSize(newSize);
    assertNonFlushedCount(newSize, cache);
  }

  public void test_size() {
    CachePartition partition = new UniversalCachePartition(null, 8, new DefaultRetentionPolicy());
    AnalysisCache cache = new AnalysisCache(new CachePartition[] {partition});
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
    MapIterator<Source, SourceEntry> iterator = cache.iterator();
    while (iterator.moveNext()) {
      if (iterator.getValue().getState(DartEntry.PARSED_UNIT) != CacheState.FLUSHED) {
        nonFlushedCount++;
      }
    }
    assertEquals(expectedCount, nonFlushedCount);
  }
}
