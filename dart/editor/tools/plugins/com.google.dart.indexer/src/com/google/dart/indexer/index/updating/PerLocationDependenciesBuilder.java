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
package com.google.dart.indexer.index.updating;

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.entries.DependentLocation;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.storage.FileTransaction;

import org.eclipse.core.resources.IFile;

public final class PerLocationDependenciesBuilder implements DependenciesBuilder {
  private final Location currentLocation;
  private final Layer currentLayer;
  private final FileTransaction fileTransaction;

  public PerLocationDependenciesBuilder(Location currentLocation, Layer currentLayer,
      FileTransaction fileTransaction) {
    if (currentLocation == null) {
      throw new NullPointerException("currentLocation is null");
    }
    if (currentLayer == null) {
      throw new NullPointerException("currentLayer is null");
    }
    if (fileTransaction == null) {
      throw new NullPointerException("fileTransaction is null");
    }
    this.currentLocation = currentLocation;
    this.currentLayer = currentLayer;
    this.fileTransaction = fileTransaction;
  }

  @Override
  public void currentFileAffectsLocationOfCurrentLayer(Location location) throws IndexRequestFailed {
    IFile containingFile = currentLocation.getContainingFile();
    if (containingFile != null) {
      fileTransaction.addDependency(containingFile, new DependentLocation(location, currentLayer));
    }
  }

  @Override
  public void currentLocationDependsOnFile(IFile file) throws IndexRequestFailed {
    fileTransaction.addDependency(file, new DependentLocation(currentLocation, currentLayer));
  }
}
