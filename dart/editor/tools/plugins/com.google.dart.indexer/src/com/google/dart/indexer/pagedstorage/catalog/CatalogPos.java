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
package com.google.dart.indexer.pagedstorage.catalog;

import com.google.dart.indexer.pagedstorage.catalog.FixedLengthPagedArray.DataPage;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.storage.paged.store.Data;

public class CatalogPos {
  public static final int SIZE = 4;

  public static CatalogPos read(FixedLengthPagedArray container, Data data) {
    int rowId = data.readInt();
    return new CatalogPos(container, rowId, null, -1);
  }

  private final int rowId;
  private DataPage page;
  private int index;

  private final FixedLengthPagedArray container;

  public CatalogPos(FixedLengthPagedArray container, int rowId, DataPage page, int index) {
    if (container == null) {
      throw new NullPointerException("container is null");
    }
    if (rowId <= 0) {
      throw new IllegalArgumentException("Invalid rowId: " + rowId);
    }
    if (index < 0 || index >= page.getCount()) {
      throw new IllegalArgumentException("Invalid index " + index + " (max: " + page.getCount()
          + ") on page " + page.getPos() + ", rowId " + rowId);
    }
    this.container = container;
    this.rowId = rowId;
    this.page = page;
    this.index = index;
  }

  public void delete() throws PagedStorageException {
    needResolved();
    page.delete(index);
  }

  public int getInfoPage(int layer) throws PagedStorageException {
    needResolved();
    return page.getInfoPage(index, layer);
  }

  public int getPageId() throws PagedStorageException {
    needResolved();
    return page.getPos();
  }

  public int getParentRowId() throws PagedStorageException {
    needResolved();
    return page.getParentRowId(index);
  }

  public int getRowId() {
    return rowId;
  }

  public boolean isDeleted() throws PagedStorageException {
    needResolved();
    return page.isDeleted(index);
  }

  public Data readableData() throws PagedStorageException {
    needResolved();
    return page.readableData(index);
  }

  public void setInfoPage(int layer, int infoPage) throws PagedStorageException {
    needResolved();
    page.setInfoPage(index, layer, infoPage);
  }

  public void setParentRowId(int parentRowId) throws PagedStorageException {
    writableData().seek(0).writeInt(parentRowId);
  }

  public Data writableData() throws PagedStorageException {
    needResolved();
    return page.writableData(index);
  }

  public void write(Data data) {
    data.writeInt(rowId);
  }

  private void needResolved() throws PagedStorageException {
    if (page == null) {
      CatalogPos pos = container.resolve(rowId);
      page = pos.page;
      index = pos.index;
    }
  }
}
