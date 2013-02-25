/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.pagestore;

import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.filesystem.AccessMode;
import com.google.dart.indexer.pagedstorage.treestore.TreeStore;

import junit.framework.TestCase;

public class TreeStoreOffsetTests extends TestCase {
  private PagedStorage pagedStorage;
  private TreeStore treeStore;

  public void test1Level1Item() throws Exception {
    treeStore.lookup(new String[] {"foo"}, true);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 1 items\n"
        + "      Item0: child NONE \"foo\" 0 0 0 0\n", treeStore.toString());

  }

  public void test1Level3Items() throws Exception {
    treeStore.lookup(new String[] {"foo"}, true);
    treeStore.lookup(new String[] {"bar"}, true);
    treeStore.lookup(new String[] {"boz"}, true);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 3 items\n"
        + "      Item0: child NONE \"bar\" 0 0 0 0\n" + "      Item1: child NONE \"boz\" 0 0 0 0\n"
        + "      Item2: child NONE \"foo\" 0 0 0 0\n" + "", treeStore.toString());

  }

  public void test3Levels1Item() throws Exception {
    treeStore.lookup(new String[] {"foo", "abc", "xyz"}, true);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 3 records\n" + "    Rec0: 1 items\n"
        + "      Item0: child Rec1 \"foo\" 0 0 0 0\n" + "    Rec1: 1 items\n"
        + "      Item0: child Rec2 \"abc\" 0 0 0 0\n" + "    Rec2: 1 items\n"
        + "      Item0: child NONE \"xyz\" 0 0 0 0\n", treeStore.toString());

  }

  public void testEmpty() throws Exception {
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 0 items\n",
        treeStore.toString());

  }

  protected void setUp() throws Exception {
    pagedStorage = new PagedStorage("memFS:test" + System.currentTimeMillis(),
        AccessMode.READ_WRITE, 10240, 1);
    pagedStorage.open();
    treeStore = new TreeStore(pagedStorage, 4, pagedStorage.getSpecialPage(0));
  }

  protected void tearDown() throws Exception {
    pagedStorage.close();
  }
}
