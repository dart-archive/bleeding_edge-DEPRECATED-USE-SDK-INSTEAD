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
package com.google.dart.indexer.pagedstorage.infostore;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;
import com.google.dart.indexer.pagedstorage.infostore.InfoStore.Page;
import com.google.dart.indexer.storage.paged.store.Data;

public class InfoPos {
  public static final int SIZE = 4;
  private Page page;

  public InfoPos(Page page) {
    if (page == null) {
      throw new NullPointerException("page is null");
    }
    this.page = page;
  }

  public boolean addItem(int id, int item) throws PagedStorageException {
    InfoPos newPos = page.getInfoStore().addItem(page.getPos(), id, item);
    if (newPos.page == page) {
      return false;
    } else {
      page = newPos.page;
      return true;
    }
  }

  public boolean addItems(int id, int[] payload) throws PagedStorageException {
    InfoPos newPos = page.getInfoStore().addItems(page.getPos(), id, payload);
    if (newPos.page == page) {
      return false;
    } else {
      page = newPos.page;
      return true;
    }
  }

  public void delete(int id) {
    page.delete(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    InfoPos other = (InfoPos) obj;
    if (page == null) {
      if (other.page != null) {
        return false;
      }
    } else if (!page.equals(other.page)) {
      return false;
    }
    return true;
  }

  public Page getPage() {
    return page;
  }

  public int getPageId() {
    return page.getPos();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((page == null) ? 0 : page.hashCode());
    return result;
  }

  public boolean hasItem(int id) {
    return page.hasItem(id);
  }

  public int[] readEntireData(int itemId) {
    return page.read(itemId);
  }

  @Override
  public String toString() {
    return "<" + page.getPos() + ">";
  }

  public boolean update(int id, int[] payload) {
    try {
      InfoPos newPos = page.getInfoStore().replace(page.getPos(), id, payload, true);
      if (newPos.page == page) {
        return false;
      } else {
        page = newPos.page;
        return true;
      }
    } catch (PagedStorageException exception) {
      IndexerPlugin.getLogger().logError(exception);
      return false;
    }
  }

  public void write(Data data) {
    data.writeInt(page.getPos());
  }
}
