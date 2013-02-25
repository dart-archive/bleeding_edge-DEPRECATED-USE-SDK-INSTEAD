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
 * Represents an object that manages a read-only index.
 */
public interface Index {
  /**
   * @return list of files which may be indexed with errors
   */
  public IPath[] getFilesWithErrors();

  /**
   * @return true if index may be inconsistent
   */
  public boolean hasErrors();

  /**
   * Returns a string representation of the entire index contents stored on the hard drive. This
   * method is intended to be used from tests.
   */
  String diskIndexAsString() throws IOException;

  /**
   * Loads and returns the information about the specified location.
   * 
   * @param layer
   * @return The information about the location if the one is found in the index, <code>null</code>
   *         otherwise.
   */
  LocationInfo getLocationInfo(Location location, Layer layer) throws IndexRequiresFullRebuild;

  AbstractIntegratedStorage getUnderlyingStorage();

  /**
   * Loads and returns the information about every indexed file.
   */
  PathAndModStamp[] loadAllFileHeaders() throws IOException;
}
