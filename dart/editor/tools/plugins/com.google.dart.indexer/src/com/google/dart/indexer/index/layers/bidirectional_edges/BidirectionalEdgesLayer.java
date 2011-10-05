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
package com.google.dart.indexer.index.layers.bidirectional_edges;

import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.updating.LayerUpdater;
import com.google.dart.indexer.storage.FileTransaction;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BidirectionalEdgesLayer extends Layer {
  @Override
  public LocationInfo createEmptyLocationInfo() {
    return new BidirectionalEdgesLocationInfo();
  }

  @Override
  public LayerUpdater createLayerUpdater(FileTransaction fileTransaction) {
    return new BidirectionalEdgesLayerUpdater(fileTransaction, this);
  }

  @Override
  public boolean isBidirectional() {
    return true;
  }

  @Override
  public LocationInfo loadLocationInfo(RandomAccessFile ra) throws IOException {
    return BidirectionalEdgesLocationInfo.load(ra);
  }

  @Override
  public void save(LocationInfo info, RandomAccessFile ra) throws IOException {
    ((BidirectionalEdgesLocationInfo) info).save(ra);
  }
}
