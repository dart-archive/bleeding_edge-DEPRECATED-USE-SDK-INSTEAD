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

import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.queries.Query;
import com.google.dart.indexer.index.readonly.Index;
import com.google.dart.indexer.locations.Location;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public class ReverseEdgesQuery implements Query {
  private final Location location;
  private Location[] sources;
  private final Layer layer;

  public ReverseEdgesQuery(Location location, Layer layer) {
    if (location == null) {
      throw new NullPointerException("location is null");
    }
    this.location = location;
    this.layer = layer;
  }

  @Override
  public void executeUsing(Index index) throws IndexTemporarilyNonOperational,
      IndexRequiresFullRebuild {
    ReverseEdgesLocationInfo info = (ReverseEdgesLocationInfo) index.getLocationInfo(location,
        layer);
    if (info == null) {
      sources = Location.EMPTY_ARRAY;
    } else {
      sources = info.getSourceLocations();
    }
  }

  @Override
  public IProject getContainingProject() {
    IFile file = location.getContainingFile();
    if (file == null) {
      return null;
    } else {
      return file.getProject();
    }
  }

  public Location[] getSources() {
    return sources;
  }
}
