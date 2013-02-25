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

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.updating.LayerUpdater;
import com.google.dart.indexer.index.updating.LocationUpdater;
import com.google.dart.indexer.index.updating.PerLocationDependenciesBuilder;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.storage.FileTransaction;

public class BidirectionalEdgesLayerUpdater implements LayerUpdater {
  private final FileTransaction fileTransaction;
  private final Layer layer;

  public BidirectionalEdgesLayerUpdater(FileTransaction fileTransaction, Layer layer) {
    if (fileTransaction == null) {
      throw new NullPointerException("fileTransaction is null");
    }
    if (layer == null) {
      throw new NullPointerException("layer is null");
    }
    this.fileTransaction = fileTransaction;
    this.layer = layer;
  }

  @Override
  public LocationUpdater startLocation(Location location) throws IndexRequestFailed {
    fileTransaction.addSourceLocation(location);

    return new BidirectionalEdgesLocationUpdaterImpl(fileTransaction, location, layer,
        new PerLocationDependenciesBuilder(location, layer, fileTransaction));
  }
}
