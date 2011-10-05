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
import com.google.dart.indexer.index.updating.LocationUpdater;
import com.google.dart.indexer.locations.Location;

import java.util.List;

/**
 * Instances of the class <code>SearchLocationUpdater</code> implement a location updater used to
 * parse matches from working copies that have not yet been indexed.
 */
public class SearchLocationUpdater implements LocationUpdater {
  /**
   * The list to which matching locations will be added.
   */
  private List<Location> matchingLocations;

  /**
   * The location that is the source of the relationship.
   */
  private Location sourceLocation;

  /**
   * An array containing the target locations that form relationships that constitute a match.
   */
  private Location[] validTargets;

  /**
   * Initialize a newly created location updater to add matching locations to the given list.
   * 
   * @param matchingLocations the list to which matching locations will be added
   * @param sourceLocation the location that is the source of the relationship
   * @param validTargets the target locations that form relationships that constitute a match
   */
  public SearchLocationUpdater(List<Location> matchingLocations, Location sourceLocation,
      Location[] validTargets) {
    this.matchingLocations = matchingLocations;
    this.sourceLocation = sourceLocation;
    this.validTargets = validTargets;
  }

  @Override
  public Location getSourceLocation() {
    return sourceLocation;
  }

  @Override
  public void hasReferenceTo(Location location) throws IndexRequestFailed {
    if (isValidTarget(location)) {
      matchingLocations.add(sourceLocation);
    }
  }

  private boolean isValidTarget(Location target) {
    if (validTargets == null) {
      return true;
    }
    for (Location validTarget : validTargets) {
      if (validTarget.equals(target)) {
        return true;
      }
    }
    return false;
  }
}
