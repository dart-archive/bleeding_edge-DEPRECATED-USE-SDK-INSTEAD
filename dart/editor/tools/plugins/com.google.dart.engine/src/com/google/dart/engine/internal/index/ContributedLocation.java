/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.index.Location;
import com.google.dart.engine.source.Source;

import java.util.Set;

/**
 * Instances of the class {@link ContributedLocation} record the {@link Source} that was being
 * analyzed when a relationship was recorded.
 * 
 * @coverage dart.engine.index
 */
public class ContributedLocation {
  private final Set<ContributedLocation> declarationOwner;
  private final Set<ContributedLocation> locationOwner;

  /**
   * The location that is part of the relationship contributed by the contributor.
   */
  private final Location location;

  /**
   * Initialize a newly created contributed location with the given information.
   * 
   * @param declarationOwner {@link Set} to remove from when declaration {@link Source} is removed
   * @param locationOwner {@link Set} to remove from when location {@link Source} is removed
   * @param location the location that is part of the relationship contributed by the contributor
   */
  public ContributedLocation(Set<ContributedLocation> declarationOwner,
      Set<ContributedLocation> locationOwner, Location location) {
    this.declarationOwner = declarationOwner;
    this.locationOwner = locationOwner;
    this.location = location;
    locationOwner.add(this);
    declarationOwner.add(this);
  }

  /**
   * @return the owner {@link Set} to remove from when declaration {@link Source} is removed.
   */
  public Set<ContributedLocation> getDeclarationOwner() {
    return declarationOwner;
  }

  /**
   * Return the location that is part of the relationship contributed by the contributor.
   * 
   * @return the location that is part of the relationship contributed by the contributor
   */
  public Location getLocation() {
    return location;
  }

  /**
   * @return the owner {@link Set} to remove from when location {@link Source} is removed.
   */
  public Set<ContributedLocation> getLocationOwner() {
    return locationOwner;
  }
}
