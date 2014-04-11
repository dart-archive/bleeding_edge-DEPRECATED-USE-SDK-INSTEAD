/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.context.WorkManager.WorkIterator;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.util.NoSuchElementException;

public class WorkManagerTest extends EngineTestCase {
  public void test_addFirst() {
    TestSource source1 = new TestSource(createFile("/f1.dart"), "");
    TestSource source2 = new TestSource(createFile("/f2.dart"), "");
    WorkManager manager = new WorkManager();
    manager.add(source1, SourcePriority.UNKNOWN);
    manager.addFirst(source2, SourcePriority.UNKNOWN);
    WorkIterator iterator = manager.iterator();
    assertSame(source2, iterator.next());
    assertSame(source1, iterator.next());
  }

  public void test_creation() {
    assertNotNull(new WorkManager());
  }

  public void test_iterator_empty() {
    WorkManager manager = new WorkManager();
    WorkIterator iterator = manager.iterator();
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException exception) {
    }
  }

  public void test_iterator_nonEmpty() {
    TestSource source = new TestSource();
    WorkManager manager = new WorkManager();
    manager.add(source, SourcePriority.UNKNOWN);
    WorkIterator iterator = manager.iterator();
    assertTrue(iterator.hasNext());
    assertSame(source, iterator.next());
  }

  public void test_remove() {
    TestSource source1 = new TestSource(createFile("/f1.dart"), "");
    TestSource source2 = new TestSource(createFile("/f2.dart"), "");
    TestSource source3 = new TestSource(createFile("/f3.dart"), "");
    WorkManager manager = new WorkManager();
    manager.add(source1, SourcePriority.UNKNOWN);
    manager.add(source2, SourcePriority.UNKNOWN);
    manager.add(source3, SourcePriority.UNKNOWN);
    manager.remove(source2);
    WorkIterator iterator = manager.iterator();
    assertSame(source1, iterator.next());
    assertSame(source3, iterator.next());
  }

  public void test_toString_empty() {
    WorkManager manager = new WorkManager();
    assertNotNull(manager.toString());
  }

  public void test_toString_nonEmpty() {
    WorkManager manager = new WorkManager();
    manager.add(new TestSource(), SourcePriority.HTML);
    manager.add(new TestSource(), SourcePriority.LIBRARY);
    manager.add(new TestSource(), SourcePriority.NORMAL_PART);
    manager.add(new TestSource(), SourcePriority.PRIORITY_PART);
    manager.add(new TestSource(), SourcePriority.UNKNOWN);
    assertNotNull(manager.toString());
  }
}
