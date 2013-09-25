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
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.ast.ASTFactory.compilationUnit;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class AnalysisCacheTest extends EngineTestCase {
  public void test_creation() {
    assertNotNull(new AnalysisCache(8));
  }

  public void test_disableCacheRemoval() {
    AnalysisCache cache = new AnalysisCache(2);
    TestSource[] sources = new TestSource[3];
    CompilationUnit[] units = new CompilationUnit[3];

    for (int i = 0; i < 3; i++) {
      sources[i] = new TestSource(null, createFile("/test" + i + ".dart"), "");
      DartEntryImpl entry = new DartEntryImpl();
      units[i] = compilationUnit();
      entry.setValue(DartEntry.PARSED_UNIT, units[i]);
      cache.put(sources[i], entry);
      cache.accessed(sources[i]);
    }

    // most recent [2], least recent [1], flushed [0]
    assertNull(cache.get(sources[0]).getValue(DartEntry.PARSED_UNIT));
    assertNotNull(cache.get(sources[1]).getValue(DartEntry.PARSED_UNIT));
    assertNotNull(cache.get(sources[2]).getValue(DartEntry.PARSED_UNIT));

    cache.disableCacheRemoval();
    DartEntryImpl dartEntry = (DartEntryImpl) cache.get(sources[0]);
    dartEntry.setValue(DartEntry.PARSED_UNIT, units[0]);
    cache.put(sources[0], dartEntry);
    cache.accessed(sources[0]);

    // most recent [0], least recent [2], flushable (but not yet flushed) [1]
    assertNotNull(cache.get(sources[0]).getValue(DartEntry.PARSED_UNIT));
    assertNotNull(cache.get(sources[1]).getValue(DartEntry.PARSED_UNIT));
    assertNotNull(cache.get(sources[2]).getValue(DartEntry.PARSED_UNIT));

    cache.enableCacheRemoval();

    // most recent [0], least recent [2], flushed [1]
    assertNotNull(cache.get(sources[0]).getValue(DartEntry.PARSED_UNIT));
    assertNull(cache.get(sources[1]).getValue(DartEntry.PARSED_UNIT));
    assertNotNull(cache.get(sources[2]).getValue(DartEntry.PARSED_UNIT));
  }

  public void test_entrySet() {
    AnalysisCache cache = new AnalysisCache(8);
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
    AnalysisCache cache = new AnalysisCache(8);
    TestSource source = new TestSource();
    assertNull(cache.get(source));
  }

  public void test_getPriorityOrder() {
    AnalysisCache cache = new AnalysisCache(8);
    assertLength(0, cache.getPriorityOrder());
  }

  public void test_put() {
    AnalysisCache cache = new AnalysisCache(8);
    TestSource source = new TestSource();
    DartEntryImpl entry = new DartEntryImpl();
    cache.put(source, entry);
    assertSame(entry, cache.get(source));
  }

  public void test_setPriorityOrder() {
    AnalysisCache cache = new AnalysisCache(8);
    TestSource source = new TestSource();
    cache.setPriorityOrder(new Source[] {source});
    Source[] result = cache.getPriorityOrder();
    assertLength(1, result);
    assertSame(source, result[0]);
  }
}
