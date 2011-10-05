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
package com.google.dart.indexer.pagedstorage.pagestore;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.pagedstorage.PagedStorage;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.storage.paged.store.Data;

public abstract class StdRecord extends Record {
  public final Data data;
  private final PagedStorage pagedStorage;

  public StdRecord(PagedStorage pagedStorage, Data data, int pageId) {
    if (pagedStorage == null) {
      throw new NullPointerException("pageStore is null");
    }
    if (data == null) {
      throw new NullPointerException("data is null");
    }
    this.pagedStorage = pagedStorage;
    this.data = data;
    setPos(pageId);
  }

  @Override
  public int getMemorySize() {
    return pagedStorage.getPageSize() >> 2;
  }

  @Override
  public final void write() throws PagedStorageException {
    serializeCached();
    pagedStorage.writePage(pageId, data);
  }

  protected void changed() {
    try {
      pagedStorage.updateRecord(this);
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
    }
  }

  protected void serializeCached() {
  }
}
