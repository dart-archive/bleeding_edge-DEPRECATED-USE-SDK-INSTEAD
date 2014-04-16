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
package com.google.dart.engine.context;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.source.TestSource;

import java.util.Map;

public class ChangeSetTest extends EngineTestCase {
  public void test_changedContent() {
    TestSource source = new TestSource();
    String content = "";
    ChangeSet changeSet = new ChangeSet();
    changeSet.changedContent(source, content);

    assertSizeOfList(0, changeSet.getAddedSources());
    assertSizeOfList(0, changeSet.getChangedSources());
    Map<Source, String> map = changeSet.getChangedContents();
    assertSizeOfMap(1, map);
    assertSame(content, map.get(source));
    assertSizeOfMap(0, changeSet.getChangedRanges());
    assertSizeOfList(0, changeSet.getDeletedSources());
    assertSizeOfList(0, changeSet.getRemovedSources());
    assertSizeOfList(0, changeSet.getRemovedContainers());
  }

  public void test_changedRange() {
    TestSource source = new TestSource();
    String content = "";
    ChangeSet changeSet = new ChangeSet();
    changeSet.changedRange(source, content, 1, 2, 3);

    assertSizeOfList(0, changeSet.getAddedSources());
    assertSizeOfList(0, changeSet.getChangedSources());
    assertSizeOfMap(0, changeSet.getChangedContents());
    Map<Source, ChangeSet.ContentChange> map = changeSet.getChangedRanges();
    assertSizeOfMap(1, map);
    ChangeSet.ContentChange change = map.get(source);
    assertNotNull(change);
    assertEquals(content, change.getContents());
    assertEquals(1, change.getOffset());
    assertEquals(2, change.getOldLength());
    assertEquals(3, change.getNewLength());
    assertSizeOfList(0, changeSet.getDeletedSources());
    assertSizeOfList(0, changeSet.getRemovedSources());
    assertSizeOfList(0, changeSet.getRemovedContainers());
  }

  public void test_toString() {
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(new TestSource());
    changeSet.changedSource(new TestSource());
    changeSet.changedContent(new TestSource(), "");
    changeSet.changedRange(new TestSource(), "", 0, 0, 0);
    changeSet.deletedSource(new TestSource());
    changeSet.removedSource(new TestSource());
    changeSet.removedContainer(new SourceContainer() {
      @Override
      public boolean contains(Source source) {
        return false;
      }
    });
    assertNotNull(changeSet.toString());
  }
}
