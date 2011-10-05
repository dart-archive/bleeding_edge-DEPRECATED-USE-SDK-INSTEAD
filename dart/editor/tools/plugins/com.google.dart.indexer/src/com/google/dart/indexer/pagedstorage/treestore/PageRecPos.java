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
package com.google.dart.indexer.pagedstorage.treestore;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.debug.IndexerDebugOptions;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.treestore.TreeStore.Page;
import com.google.dart.indexer.storage.paged.store.Data;
import com.google.dart.indexer.utilities.io.PrintStringWriter;

public class PageRecPos {
  public static final int SIZE = 4 + 2 + 2;

  final Page page;
  final int record;
  final int pos;

  public PageRecPos(Page page, int record, int pos) {
    if (page == null) {
      throw new NullPointerException("page is null");
    }
    if (record < 0 || record >= page.getRecordCount()) {
      throw new IllegalArgumentException("Invalid record: " + record);
    }
    if (pos < 0 || pos >= page.itemCount(record)) {
      throw new IllegalArgumentException("Invalid index " + pos + " in record " + record);
    }
    this.page = page;
    this.record = record;
    this.pos = pos;
  }

  public PageRec children(boolean add) throws PagedStorageException {
    int childRecord = page.getChildren(record, pos);
    if (childRecord == 0) {
      if (add) {
        childRecord = page.addEmptyChildRecord(record, pos);
        if (childRecord < 0) {
          Page subpage = page.addEmptyPageWithAnEmptyChildRecord();
          page.setChildPage(record, pos, subpage.getPos());
          return new PageRec(subpage, 0);
        } else {
          // IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
          // "Added child record " + childRecord +
          // " to page " + page.getPos());
        }
      } else {
        return null;
      }
    } else {
      // IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
      // "Existing child record " + childRecord + " on page "
      // + page.getPos());
    }
    if (childRecord > TreeStore.PAGE_MARKER) {
      int childPageId = childRecord - TreeStore.PAGE_MARKER;
      Page subpage = page.getTreeStore().readPage(childPageId);
      return new PageRec(subpage, 0);
    }
    return new PageRec(page, childRecord);
  }

  public void delete() throws PagedStorageException {
    PageRec children = children(false);
    page.deleteItem(record, pos);

    if (children != null) {
      children.delete();
    }

    if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_MODIFICATIONS)) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.println("==========================================");
      writer.println("*** Removed " + toString()
          + (children != null ? " and " + children.toString() : "") + " ***");
      writer.println(page.getTreeStore().toString().trim());
      writer.println("==========================================");
      IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_MODIFICATIONS, writer.toString());
    }
  }

  public int getRowId() {
    return page.getRowId(record, pos);
  }

  public long getTimestamp() {
    return page.getTimestamp(record, pos);
  }

  public String readName() {
    return page.readName(record, pos);
  }

  public void setRowId(int rowId) {
    page.setRowId(record, pos, rowId);
  }

  public void setTimestamp(long stamp) {
    page.setTimestamp(record, pos, stamp);
  }

  @Override
  public String toString() {
    return "<" + page.getPos() + "," + record + "," + pos + ">";
  }

  public void write(Data data) {
    data.writeInt(page.getPos());
    data.writeShortInt(record);
    data.writeShortInt(pos);
  }
}
