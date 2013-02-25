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
package com.google.dart.indexer.storage.paged;

import com.google.dart.indexer.pagedstorage.stats.NumericDistribution;

public class MappingStats {
  public int itemCount;

  public int deletedItemCount;

  public final TreeStoreStats treeStoreStats = new TreeStoreStats();

  public final CatalogStats catalogStats = new CatalogStats();

  public NumericDistribution usedPageCounts = new NumericDistribution();

  public NumericDistribution pageFillFactors = new NumericDistribution();

  public NumericDistribution itemLength = new NumericDistribution();

  public NumericDistribution itemsPerPage = new NumericDistribution();

  public int idCacheHits, idCacheMisses;

  public int pageCount;

  // private int pageSize;

  private int bytesTree;

  private int bytesCatalog;

  private int bytesInfo;

  private int bytes;

  public void resolve(int pageSize) {
    // this.pageSize = pageSize;

    bytesTree = pageSize * treeStoreStats.pages;
    bytesCatalog = pageSize * (catalogStats.catalogPages + catalogStats.dataPages);
    bytesInfo = pageSize * pageCount;
    bytes = bytesTree + bytesCatalog + bytesInfo;
  }

  @Override
  public String toString() {
    int KB = 1024;
    String s = "N=" + itemCount + " (" + ((itemCount - deletedItemCount) * 100 / itemCount) + "%)"
        + "  len=" + treeStoreStats.componentLength + "  fill=" + pageFillFactors + "  i/p="
        + itemsPerPage + "  payload=" + itemLength + "\n" + " SIZE " + (bytes / KB) + " KB: tree "
        + (bytesTree / KB) + " KB, catalog " + (bytesCatalog / KB) + " KB, infos "
        + (bytesInfo / KB) + " KB  (" + (bytesTree * (long) 100 / bytes) + "% : "
        + (bytesCatalog * (long) 100 / bytes) + "% : " + (bytesInfo * (long) 100 / bytes) + "%)";
    if (idCacheHits + idCacheMisses > 0) {
      s += "\n" + " CACHE idLookups " + (idCacheMisses + idCacheHits) + " ("
          + (idCacheHits * 100 / (idCacheMisses + idCacheHits)) + "% hits)";
    }
    return s;
  }
}
