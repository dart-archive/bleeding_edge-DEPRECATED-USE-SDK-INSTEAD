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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class UniversalCachePartitionTest extends EngineTestCase {
  public void test_contains() {
    UniversalCachePartition partition = new UniversalCachePartition(null, 8, null);
    TestSource source = new TestSource();
    assertTrue(partition.contains(source));
  }

  public void test_creation() {
    assertNotNull(new UniversalCachePartition(null, 8, null));
  }

  public void test_entrySet() {
    UniversalCachePartition partition = new UniversalCachePartition(null, 8, null);
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    partition.put(source, entry);
    Iterator<Entry<Source, SourceEntry>> entries = partition.getMap().entrySet().iterator();
    assertTrue(entries.hasNext());
    Map.Entry<Source, SourceEntry> mapEntry = entries.next();
    assertSame(source, mapEntry.getKey());
    assertSame(entry, mapEntry.getValue());
    assertFalse(entries.hasNext());
  }

  public void test_get() {
    UniversalCachePartition partition = new UniversalCachePartition(null, 8, null);
    TestSource source = new TestSource();
    assertNull(partition.get(source));
  }

  public void test_put_noFlush() {
    UniversalCachePartition partition = new UniversalCachePartition(null, 8, null);
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    partition.put(source, entry);
    assertSame(entry, partition.get(source));
  }

  public void test_remove() {
    UniversalCachePartition partition = new UniversalCachePartition(null, 8, null);
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    partition.put(source, entry);
    assertSame(entry, partition.get(source));
    partition.remove(source);
    assertNull(partition.get(source));
  }

  public void test_setMaxCacheSize() {
    UniversalCachePartition partition = new UniversalCachePartition(
        null,
        8,
        new CacheRetentionPolicy() {
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
      partition.put(source, entry);
      partition.accessedAst(source);
    }
    assertNonFlushedCount(size, partition);
    int newSize = size - 2;
    partition.setMaxCacheSize(newSize);
    assertNonFlushedCount(newSize, partition);
  }

  public void test_size() {
    UniversalCachePartition partition = new UniversalCachePartition(null, 8, null);
    int size = 4;
    for (int i = 0; i < size; i++) {
      Source source = new TestSource(createFile("/test" + i + ".dart"), "");
      partition.put(source, new DartEntryImpl());
      partition.accessedAst(source);
    }
    assertEquals(size, partition.size());
  }

  private void assertNonFlushedCount(int expectedCount, UniversalCachePartition partition) {
    int nonFlushedCount = 0;
    Iterator<Entry<Source, SourceEntry>> entries = partition.getMap().entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry<Source, SourceEntry> entry = entries.next();
      if (entry.getValue().getState(DartEntry.PARSED_UNIT) != CacheState.FLUSHED) {
        nonFlushedCount++;
      }
    }
    assertEquals(expectedCount, nonFlushedCount);
  }
}
