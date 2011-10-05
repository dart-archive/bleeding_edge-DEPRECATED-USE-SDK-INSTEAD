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
import com.google.dart.indexer.pagedstorage.treestore.PageRecPos;
import com.google.dart.indexer.pagedstorage.treestore.TreeStore;

import junit.framework.TestCase;

public class TreeStoreBasicTests extends TestCase {
  protected static String makeLongName(String name, int size) {
    StringBuffer result = new StringBuffer();
    result.append(name);
    while (result.length() < size)
      result.append("-").append(name);
    result.append("!");
    return result.toString();
  }

  private PagedStorage pagedStorage;

  private TreeStore treeStore;

  public void test1Level1Item() throws Exception {
    treeStore.lookup(new String[] {"foo"}, true);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 1 items\n"
        + "      Item0: child NONE \"foo\" 0 0 0 0\n", treeStore.toTestableString());

  }

  public void test1Level3Items() throws Exception {
    treeStore.lookup(new String[] {"foo"}, true);
    treeStore.lookup(new String[] {"bar"}, true);
    treeStore.lookup(new String[] {"boz"}, true);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 3 items\n"
        + "      Item0: child NONE \"bar\" 0 0 0 0\n" + "      Item1: child NONE \"boz\" 0 0 0 0\n"
        + "      Item2: child NONE \"foo\" 0 0 0 0\n" + "", treeStore.toTestableString());

  }

  public void test1Level3ItemsDelete() throws Exception {
    treeStore.lookup(new String[] {"foo"}, true);
    PageRecPos pos = treeStore.lookup(new String[] {"bar"}, true);
    treeStore.lookup(new String[] {"boz"}, true);
    pos.delete();
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 2 items\n"
        + "      Item0: child NONE \"boz\" 0 0 0 0\n" + "      Item1: child NONE \"foo\" 0 0 0 0\n"
        + "", treeStore.toTestableString());

  }

  public void test2Levels4Items() throws Exception {
    treeStore.lookup(new String[] {"foo", "abc"}, true);
    treeStore.lookup(new String[] {"bar"}, true);
    treeStore.lookup(new String[] {"boz", "def"}, true);
    treeStore.lookup(new String[] {"boz", "ghi"}, true);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 3 records\n" + "    Rec0: 3 items\n"
        + "      Item0: child NONE \"bar\" 0 0 0 0\n" + "      Item1: child Rec2 \"boz\" 0 0 0 0\n"
        + "      Item2: child Rec1 \"foo\" 0 0 0 0\n" + "    Rec1: 1 items\n"
        + "      Item0: child NONE \"abc\" 0 0 0 0\n" + "    Rec2: 2 items\n"
        + "      Item0: child NONE \"def\" 0 0 0 0\n" + "      Item1: child NONE \"ghi\" 0 0 0 0\n"
        + "", treeStore.toTestableString());

  }

  public void test2Levels4ItemsDeleteSublevel() throws Exception {
    treeStore.lookup(new String[] {"foo", "abc"}, true);
    treeStore.lookup(new String[] {"bar"}, true);
    treeStore.lookup(new String[] {"boz", "def"}, true);
    treeStore.lookup(new String[] {"boz", "ghi"}, true);
    PageRecPos pos = treeStore.lookup(new String[] {"boz"}, false);
    pos.delete();
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 2 records\n" + "    Rec0: 2 items\n"
        + "      Item0: child NONE \"bar\" 0 0 0 0\n" + "      Item1: child Rec1 \"foo\" 0 0 0 0\n"
        + "    Rec1: 1 items\n" + "      Item0: child NONE \"abc\" 0 0 0 0\n" + "",
        treeStore.toTestableString());

  }

  public void test3Levels1Item() throws Exception {
    treeStore.lookup(new String[] {"foo", "abc", "xyz"}, true);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 3 records\n" + "    Rec0: 1 items\n"
        + "      Item0: child Rec1 \"foo\" 0 0 0 0\n" + "    Rec1: 1 items\n"
        + "      Item0: child Rec2 \"abc\" 0 0 0 0\n" + "    Rec2: 1 items\n"
        + "      Item0: child NONE \"xyz\" 0 0 0 0\n", treeStore.toTestableString());

  }

  public void testEmpty() throws Exception {
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 0 items\n",
        treeStore.toTestableString());

  }

  public void testLevel2ContainerOverflow() throws Exception {
    // int items = 10;
    // int size = pagedStorage.getPageSize() * 1 / 5;
    // for (int i = 0; i < items; i++)
    // treeStore.lookup(new String[] { "foo", makeLongName("abc" + i, size) },
    // true);
    // assertEquals(
    // "TreeStore root=4\n" +
    // // not sure what should be here
    // "",
    // treeStore.toTestableString());

  }

  public void testLevel2MovedToSubpage() throws Exception {
    int size = pagedStorage.getPageSize() * 1 / 5;
    treeStore.lookup(new String[] {"foo", "abc", makeLongName("xyz", size)}, true);
    treeStore.lookup(new String[] {"foo", "abc", makeLongName("wxy", size)}, true);
    treeStore.lookup(new String[] {"foo", "abc", makeLongName("vwx", size)}, true);
    treeStore.lookup(new String[] {"foo", "def", makeLongName("uvw", size)}, true);
    treeStore.lookup(new String[] {"foo", "def", makeLongName("tuv", size)}, true);
    PageRecPos pos = treeStore.lookup(new String[] {"foo", "abc", makeLongName("wxy", size)}, false);
    assertEquals(
        "TreeStore root=4\n"
            + "  PAGE 4: 3 records\n"
            + "    Rec0: 1 items\n"
            + "      Item0: child Rec1 \"foo\" 0 0 0 0\n"
            + "    Rec1: 2 items\n"
            + "      Item0: child Page5 \"abc\" 0 0 0 0\n"
            + "      Item1: child Rec2 \"def\" 0 0 0 0\n"
            + "    Rec2: 2 items\n"
            + "      Item0: child NONE \"tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv-tuv!\" 0 0 0 0\n"
            + "      Item1: child NONE \"uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw-uvw!\" 0 0 0 0\n"
            + "  PAGE 5: 1 records\n"
            + "    Rec0: 3 items\n"
            + "      Item0: child NONE \"vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx-vwx!\" 0 0 0 0\n"
            + "      Item1: child NONE \"wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy-wxy!\" 0 0 0 0\n"
            + "      Item2: child NONE \"xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz-xyz!\" 0 0 0 0\n"
            + "" + "<5,0,1>", treeStore.toTestableString() + pos.toString());

  }

  protected void setUp() throws Exception {
    pagedStorage = new PagedStorage("memFS:test" + System.currentTimeMillis(),
        AccessMode.READ_WRITE, 10240, 1);
    pagedStorage.setPageSize(1024);
    pagedStorage.open();
    treeStore = new TreeStore(pagedStorage, 4, pagedStorage.getSpecialPage(0));
  }

  protected void tearDown() throws Exception {
    pagedStorage.close();
  }
}
