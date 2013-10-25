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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.internal.cache.DartEntry;
import com.google.dart.engine.internal.cache.DartEntryImpl;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.internal.context.IncrementalAnalysisCache.clear;
import static com.google.dart.engine.internal.context.IncrementalAnalysisCache.update;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;

import java.io.File;

public class IncrementalAnalysisCacheTest extends TestCase {

  private Source source = new TestSource();
  private DartEntryImpl entry = new DartEntryImpl();
  private CompilationUnit unit = mock(CompilationUnit.class);
  private IncrementalAnalysisCache result;

  public void test_clear_differentSource() throws Exception {
    IncrementalAnalysisCache cache = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);

    ContentCache contentCache = new ContentCache();
    Source otherSource = new TestSource(contentCache, new File("blat.dart"), "blat");
    result = clear(cache, otherSource);
    assertSame(cache, result);
  }

  public void test_clear_nullCache() throws Exception {
    IncrementalAnalysisCache cache = null;

    result = clear(cache, source);
    assertNull(result);
  }

  public void test_clear_sameSource() throws Exception {
    IncrementalAnalysisCache cache = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);

    result = clear(cache, source);
    assertNull(result);
  }

  public void test_update_append() throws Exception {
    IncrementalAnalysisCache cache = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);

    result = update(cache, source, "hbazlo", "hbazxlo", 4, 0, 1, null);
    assertNotNull(result);
    assertSame(source, result.getSource());
    assertSame(unit, result.getResolvedUnit());
    assertEquals("hello", result.getOldContents());
    assertEquals("hbazxlo", result.getNewContents());
    assertEquals(1, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(4, result.getNewLength());
  }

  public void test_update_delete() throws Exception {
    IncrementalAnalysisCache cache = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);

    result = update(cache, source, "hbazlo", "hzlo", 1, 2, 0, null);
    assertNotNull(result);
    assertSame(source, result.getSource());
    assertSame(unit, result.getResolvedUnit());
    assertEquals("hello", result.getOldContents());
    assertEquals("hzlo", result.getNewContents());
    assertEquals(1, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(1, result.getNewLength());
  }

  public void test_update_insert_nonContiguous_after() throws Exception {
    IncrementalAnalysisCache cache = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);

    result = update(cache, source, "hbazlo", "hbazlox", 6, 0, 1, null);
    assertNull(result);
  }

  public void test_update_insert_nonContiguous_before() throws Exception {
    IncrementalAnalysisCache cache = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);

    result = update(cache, source, "hbazlo", "xhbazlo", 0, 0, 1, null);
    assertNull(result);
  }

  public void test_update_newSource_entry() throws Exception {
    ContentCache contentCache = new ContentCache();
    Source oldSource = new TestSource(contentCache, new File("blat.dart"), "blat");
    DartEntryImpl oldEntry = new DartEntryImpl();
    CompilationUnit oldUnit = mock(CompilationUnit.class);
    oldEntry.setValue(DartEntry.RESOLVED_UNIT, source, oldUnit);
    IncrementalAnalysisCache cache = update(null, oldSource, "hello", "hbazlo", 1, 2, 3, oldEntry);
    assertSame(oldSource, cache.getSource());
    assertSame(oldUnit, cache.getResolvedUnit());

    result = update(cache, source, "foo", "foobz", 3, 0, 2, entry);
    assertNotNull(result);
    assertSame(source, result.getSource());
    assertSame(unit, result.getResolvedUnit());
    assertEquals("foo", result.getOldContents());
    assertEquals("foobz", result.getNewContents());
    assertEquals(3, result.getOffset());
    assertEquals(0, result.getOldLength());
    assertEquals(2, result.getNewLength());
  }

  public void test_update_newSource_noEntry() throws Exception {
    ContentCache contentCache = new ContentCache();
    Source oldSource = new TestSource(contentCache, new File("blat.dart"), "blat");
    DartEntryImpl oldEntry = new DartEntryImpl();
    CompilationUnit oldUnit = mock(CompilationUnit.class);
    oldEntry.setValue(DartEntry.RESOLVED_UNIT, source, oldUnit);
    IncrementalAnalysisCache cache = update(null, oldSource, "hello", "hbazlo", 1, 2, 3, oldEntry);
    assertSame(oldSource, cache.getSource());
    assertSame(oldUnit, cache.getResolvedUnit());

    result = update(cache, source, "foo", "foobar", 3, 0, 3, null);
    assertNull(result);
  }

  public void test_update_noCache_entry() {
    result = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);
    assertNotNull(result);
    assertSame(source, result.getSource());
    assertSame(unit, result.getResolvedUnit());
    assertEquals("hello", result.getOldContents());
    assertEquals("hbazlo", result.getNewContents());
    assertEquals(1, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(3, result.getNewLength());
  }

  public void test_update_noCache_entry_noOldSource_append() {
    result = update(null, source, null, "hellxo", 4, 0, 1, entry);
    assertNotNull(result);
    assertSame(source, result.getSource());
    assertSame(unit, result.getResolvedUnit());
    assertEquals("hello", result.getOldContents());
    assertEquals("hellxo", result.getNewContents());
    assertEquals(4, result.getOffset());
    assertEquals(0, result.getOldLength());
    assertEquals(1, result.getNewLength());
  }

  public void test_update_noCache_entry_noOldSource_delete() {
    result = update(null, source, null, "helo", 4, 1, 0, entry);
    assertNull(result);
  }

  public void test_update_noCache_entry_noOldSource_replace() {
    result = update(null, source, null, "helxo", 4, 1, 1, entry);
    assertNull(result);
  }

  public void test_update_noCache_noEntry() {
    result = update(null, source, "hello", "hbazlo", 1, 2, 3, null);
    assertNull(result);
  }

  public void test_update_replace() throws Exception {
    IncrementalAnalysisCache cache = update(null, source, "hello", "hbazlo", 1, 2, 3, entry);

    result = update(cache, source, "hbazlo", "hbarrlo", 3, 1, 2, null);
    assertNotNull(result);
    assertSame(source, result.getSource());
    assertSame(unit, result.getResolvedUnit());
    assertEquals("hello", result.getOldContents());
    assertEquals("hbarrlo", result.getNewContents());
    assertEquals(1, result.getOffset());
    assertEquals(2, result.getOldLength());
    assertEquals(4, result.getNewLength());
  }

  @Override
  protected void setUp() throws Exception {
    entry.setValue(DartEntry.RESOLVED_UNIT, source, unit);
  }
}
