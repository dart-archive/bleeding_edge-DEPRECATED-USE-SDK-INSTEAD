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
package com.google.dart.indexer.index.readonly;

import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.storage.AbstractIntegratedStorage;

import org.eclipse.core.runtime.IPath;

import java.io.IOException;

/**
 * Represents an empty read-only index. It might be substituted for a real index when there no
 * suitable real index exists.
 */
public class EmptyIndexImpl implements Index {
  private AbstractIntegratedStorage storage;

  public EmptyIndexImpl(AbstractIntegratedStorage storage) {
    this.storage = storage;
  }

  @Override
  public String diskIndexAsString() throws IOException {
    return "<Empty>";
  }

  @Override
  public IPath[] getFilesWithErrors() {
    return new IPath[0];
  }

  @Override
  public LocationInfo getLocationInfo(Location location, Layer layer)
      throws IndexRequiresFullRebuild {
    return null;
  }

  @Override
  public AbstractIntegratedStorage getUnderlyingStorage() {
    return storage;
  }

  @Override
  public boolean hasErrors() {
    return false;
  }

  @Override
  public PathAndModStamp[] loadAllFileHeaders() throws IOException {
    return new PathAndModStamp[0];
  }
}
