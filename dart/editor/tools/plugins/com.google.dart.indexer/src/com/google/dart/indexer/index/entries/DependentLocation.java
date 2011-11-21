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
package com.google.dart.indexer.index.entries;

import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;

import java.util.Set;

public class DependentLocation implements DependentEntity {
  private final Location dependentLocation;
  private final Layer dependentLayer;

  public DependentLocation(Location dependentLocation, Layer dependentLayer) {
    this.dependentLocation = dependentLocation;
    this.dependentLayer = dependentLayer;
    if (dependentLocation == null) {
      throw new NullPointerException("dependentLocation is null");
    }
    if (dependentLayer == null) {
      throw new NullPointerException("dependentLayer is null");
    }
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
    final DependentLocation other = (DependentLocation) obj;
    if (dependentLayer == null) {
      if (other.dependentLayer != null) {
        return false;
      }
    } else if (!dependentLayer.equals(other.dependentLayer)) {
      return false;
    }
    if (dependentLocation == null) {
      if (other.dependentLocation != null) {
        return false;
      }
    } else if (!dependentLocation.equals(other.dependentLocation)) {
      return false;
    }
    return true;
  }

  public Layer getDependentLayer() {
    return dependentLayer;
  }

  public Location getDependentLocation() {
    return dependentLocation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dependentLayer == null) ? 0 : dependentLayer.hashCode());
    result = prime * result + ((dependentLocation == null) ? 0 : dependentLocation.hashCode());
    return result;
  }

  @Override
  @Deprecated
  public boolean isStale(IFile staleFile, Set<Location> staleLocations) {
    return staleLocations.contains(dependentLocation);
  }

  @Override
  public boolean isStale(IndexableSource staleSource, Set<Location> staleLocations) {
    return staleLocations.contains(dependentLocation);
  }

  @Override
  public String toString() {
    return "(" + dependentLocation + "; " + dependentLayer + ")";
  }

  @Override
  public String toString(boolean showLayers) {
    if (showLayers) {
      return dependentLocation + " @ " + dependentLayer;
    } else {
      return dependentLocation + "";
    }
  }
}
