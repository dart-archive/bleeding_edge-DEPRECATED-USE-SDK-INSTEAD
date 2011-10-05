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
import com.google.dart.indexer.pagedstorage.catalog.Mapping;
import com.google.dart.indexer.pagedstorage.filesystem.AccessMode;
import com.google.dart.indexer.pagedstorage.infostore.InfoStore;
import com.google.dart.indexer.pagedstorage.treestore.TreeStore;

import junit.framework.TestCase;

public class MappingBasicTests extends TestCase {
  private PagedStorage pagedStorage;
  private TreeStore treeStore;
  private Mapping mapping;

  public void ignoretest1Level1ItemDeletion() throws Exception {
    int id = mapping.findOrCreate(new String[] {"foo"});
    mapping.delete(id);
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 0 items\n"
        + "Catalog\n" + "  CatalogPage 5: 1 items on 1 subpages\n" + "    DataPage 6: 1 items\n"
        + "      ID 1: --deleted--\n" + "", str());
  }

  public void test1Level1Item() throws Exception {
    int id = mapping.findOrCreate(new String[] {"foo"});
    assertEquals("id 1\n" + "TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 1 items\n"
        + "      Item0: child NONE \"foo\" 0 0 0 1\n" + "Catalog\n"
        + "  CatalogPage 5: 1 items on 1 subpages\n" + "    DataPage 6: 1 items\n"
        + "      ID 1: parentRowId=0 treePos=<4,0,0>\n" + "", "id " + id + "\n" + str());
  }

  public void test3Levels1Item() throws Exception {
    mapping.findOrCreate(new String[] {"foo", "abc", "xyz"});
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 3 records\n" + "    Rec0: 1 items\n"
        + "      Item0: child Rec1 \"foo\" 0 0 0 1\n" + "    Rec1: 1 items\n"
        + "      Item0: child Rec2 \"abc\" 0 0 0 2\n" + "    Rec2: 1 items\n"
        + "      Item0: child NONE \"xyz\" 0 0 0 3\n" + "Catalog\n"
        + "  CatalogPage 5: 3 items on 1 subpages\n" + "    DataPage 6: 3 items\n"
        + "      ID 1: parentRowId=0 treePos=<4,0,0>\n"
        + "      ID 2: parentRowId=1 treePos=<4,1,0>\n"
        + "      ID 3: parentRowId=2 treePos=<4,2,0>\n" + "", str());
  }

  public void testEmpty() throws Exception {
    assertEquals("TreeStore root=4\n" + "  PAGE 4: 1 records\n" + "    Rec0: 0 items\n"
        + "Catalog\n" + "  CatalogPage 5: 0 items on 0 subpages\n" + "", str());
  }

  protected void setUp() throws Exception {
    pagedStorage = new PagedStorage("memFS:test" + System.currentTimeMillis(),
        AccessMode.READ_WRITE, 10240, 2);
    pagedStorage.open();
    treeStore = new TreeStore(pagedStorage, 4, pagedStorage.getSpecialPage(0));
    mapping = new Mapping(pagedStorage, pagedStorage.getSpecialPage(1), treeStore, new InfoStore[0]);
  }

  protected String str() {
    return treeStore.toTestableString() + mapping.toString();
  }

  protected void tearDown() throws Exception {
    pagedStorage.close();
  }
}
