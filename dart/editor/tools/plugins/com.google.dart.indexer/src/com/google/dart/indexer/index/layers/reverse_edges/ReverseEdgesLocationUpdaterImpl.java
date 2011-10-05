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
package com.google.dart.indexer.index.layers.reverse_edges;

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.updating.DependenciesBuilder;
import com.google.dart.indexer.index.updating.LocationUpdater;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.storage.FileTransaction;

public class ReverseEdgesLocationUpdaterImpl implements LocationUpdater {
  private final Location sourceLocation;
  private final Layer layer;
  private final DependenciesBuilder dependenciesBuilder;
  private final FileTransaction fileTransaction;

  public ReverseEdgesLocationUpdaterImpl(FileTransaction fileTransaction, Location sourceLocation,
      Layer layer, DependenciesBuilder dependenciesBuilder) {
    this.fileTransaction = fileTransaction;
    this.sourceLocation = sourceLocation;
    this.layer = layer;
    this.dependenciesBuilder = dependenciesBuilder;
  }

  @Override
  public Location getSourceLocation() {
    return sourceLocation;
  }

  @Override
  public void hasReferenceTo(Location destinationLocation) throws IndexRequestFailed {
    fileTransaction.addReference(layer, sourceLocation, destinationLocation);
    dependenciesBuilder.currentFileAffectsLocationOfCurrentLayer(destinationLocation);
  }
}
