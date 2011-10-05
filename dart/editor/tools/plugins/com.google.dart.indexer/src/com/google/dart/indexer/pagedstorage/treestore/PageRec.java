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
import com.google.dart.indexer.utilities.io.PrintStringWriter;

public class PageRec {
  Page page;
  int record;

  public PageRec(Page page, int record) {
    this.page = page;
    this.record = record;
  }

  public void delete() {
    // TODO recursive deletion of all children
    if (record > 0) {
      page.delete(record);
    }
  }

  public PageRecPos lookup(String component, boolean add) throws PagedStorageException {
    int pos = page.find(record, component);
    if (pos < 0) {
      if (!add) {
        return null;
      } else {
        int offset;
        while ((offset = page.tryAdd(record, component)) < 0) {
          if (offset == TreeStore.ADD_EXISTS) {
            throw new AssertionError("Component exists, but not found by find(): " + component);
          }
          if (offset == TreeStore.ADD_NOROOM) {
            int largestRecord = page.findLargestRecord();
            if (largestRecord < 0) {
              return null; // should have added an overflow page, but can't yet
            }
            try {
              Page subpage = page.createSubpage();
              int[] newRecs = page.move(largestRecord, subpage);
              int oldRec = record;
              if (newRecs[record] >= 0) {
                page = subpage;
                record = newRecs[record];
              } else {
                // adjust record # according to deleted recs
                for (int r = 0; r < oldRec; ++r) {
                  if (newRecs[r] >= 0) {
                    --record;
                  }
                }
              }
              if (record >= page.getRecordCount()) {
                throw new AssertionError("Incorrect fixup of record # after move");
                // IndexerPlugin.getLogger().trace(IndexerDebugOptions.MISCELLANEOUS,
                // page.getTreeStore().toString().trim());
              }
            } catch (PagedStorageException exception) {
              IndexerPlugin.getLogger().logError(exception);
            }
          } else {
            throw new AssertionError("Unknown error code returned from treeStore's page.add(): "
                + offset);
          }
        }
        page.data.zeroFill(offset, page.additionalDataSize());
        pos = -(pos + 1);
        if (IndexerPlugin.getLogger().isTracing(IndexerDebugOptions.TREE_MODIFICATIONS)) {
          PrintStringWriter writer = new PrintStringWriter();
          writer.println("==========================================");
          writer.println("*** Added " + component + " ***");
          writer.println(page.getTreeStore().toString().trim());
          writer.println("==========================================");
          IndexerPlugin.getLogger().trace(IndexerDebugOptions.TREE_MODIFICATIONS, writer.toString());
        }
      }
    }

    return new PageRecPos(page, record, pos);
  }

  @Override
  public String toString() {
    return "<" + page.getPos() + "," + record + ">";
  }
}
