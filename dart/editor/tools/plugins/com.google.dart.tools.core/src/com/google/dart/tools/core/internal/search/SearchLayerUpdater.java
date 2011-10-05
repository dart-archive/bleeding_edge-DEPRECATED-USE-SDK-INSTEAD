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
package com.google.dart.tools.core.internal.search;

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.updating.LayerUpdater;
import com.google.dart.indexer.index.updating.LocationUpdater;
import com.google.dart.indexer.locations.Location;

import java.util.List;

/**
 * Instances of the class <code>SearchLayerUpdater</code> implement a layer updater used to parse
 * matches from working copies that have not yet been indexed.
 */
public class SearchLayerUpdater implements LayerUpdater {
  /**
   * The list to which matching locations will be added.
   */
  private List<Location> matchingLocations;

  /**
   * An array containing the target locations that form relationships that constitute a match.
   */
  private Location[] validTargets;

  /**
   * Initialize a newly created layer updater to add matching locations to the given list.
   * 
   * @param matchingLocations the list to which matching locations will be added
   * @param validTargets the target locations that form relationships that constitute a match
   */
  public SearchLayerUpdater(List<Location> matchingLocations, Location[] validTargets) {
    this.matchingLocations = matchingLocations;
    this.validTargets = validTargets;
  }

  @Override
  public LocationUpdater startLocation(Location location) throws IndexRequestFailed {
    return new SearchLocationUpdater(matchingLocations, location, validTargets);
  }
}
