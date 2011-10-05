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

import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationPersitence;
import com.google.dart.indexer.utils.Debugging;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents the information of a specific location that is stored in the index. Currently this is
 * a list of source locations that are connected to this location.
 */
public class BidirectionalEdgesLocationInfo implements LocationInfo {
  public static BidirectionalEdgesLocationInfo load(RandomAccessFile file) throws IOException {
    BidirectionalEdgesLocationInfo result = new BidirectionalEdgesLocationInfo();
    result.setSourceLocations(LocationPersitence.loadLocations(file));
    result.setDestinationLocations(LocationPersitence.loadLocations(file));
    return result;
  }

  private Collection<Location> sourceLocations = new ArrayList<Location>();

  private Collection<Location> destinationLocations = new ArrayList<Location>();

  public BidirectionalEdgesLocationInfo() {
    sourceLocations = new ArrayList<Location>();
    destinationLocations = new ArrayList<Location>();
  }

  public BidirectionalEdgesLocationInfo(Collection<Location> source,
      Collection<Location> destination) {
    this.sourceLocations = source;
    this.destinationLocations = destination;
  }

  public void addDestinationLocation(Location location) {
    if (destinationLocations == Collections.EMPTY_LIST) {
      destinationLocations = new ArrayList<Location>();
    }
    destinationLocations.add(location);
  }

  public void addSourceLocation(Location location) {
    if (sourceLocations == Collections.EMPTY_LIST) {
      sourceLocations = new ArrayList<Location>();
    }
    sourceLocations.add(location);
  }

  @Override
  public void adjustDueToRemovalOf(Set<Location> locations) {
    removeSourceLocations(locations);
    Set<Location> sourceLocationsSet = new HashSet<Location>(destinationLocations);
    sourceLocationsSet.removeAll(locations);
    destinationLocations.clear();
    destinationLocations.addAll(sourceLocationsSet);
  }

  public Location[] getDestinationLocations() {
    return destinationLocations.toArray(new Location[destinationLocations.size()]);
  }

  @Override
  public Location[] getLocationsAffectedByRemovalOfSelf() {
    ArrayList<Location> result = new ArrayList<Location>();
    result.addAll(sourceLocations);
    result.addAll(destinationLocations);
    return result.toArray(new Location[result.size()]);
  }

  public Location[] getSourceLocations() {
    return sourceLocations.toArray(new Location[sourceLocations.size()]);
  }

  @Override
  public boolean isEmpty() {
    return sourceLocations.isEmpty() && destinationLocations.isEmpty();
  }

  public void removeSourceLocations(Set<Location> locationsToRemove) {
    Set<Location> sourceLocationsSet = new HashSet<Location>(sourceLocations);

    sourceLocationsSet.removeAll(locationsToRemove);
    sourceLocations.clear();
    sourceLocations.addAll(sourceLocationsSet);
  }

  public void save(RandomAccessFile file) throws IOException {
    LocationPersitence.saveLocations(file, sourceLocations);
    LocationPersitence.saveLocations(file, destinationLocations);
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    toString(out, "");
    return out.toString();
  }

  @Override
  public void toString(StringBuilder out, String indent) {
    out.append(indent).append(sourceLocations.size()).append(" source location(s)\n");
    for (Iterator<Location> iterator = Debugging.sortByStrings(sourceLocations).iterator(); iterator.hasNext();) {
      Location location = iterator.next();
      out.append(indent).append(Debugging.INDENT).append(location).append("\n");
    }
    out.append(indent).append(destinationLocations.size()).append(" destination location(s)\n");
    for (Iterator<Location> iterator = Debugging.sortByStrings(destinationLocations).iterator(); iterator.hasNext();) {
      Location location = iterator.next();
      out.append(indent).append(Debugging.INDENT).append(location).append("\n");
    }
  }

  private void setDestinationLocations(Collection<Location> locations) {
    destinationLocations.clear();
    destinationLocations.addAll(locations);
  }

  private void setSourceLocations(Collection<Location> locations) {
    sourceLocations.clear();
    sourceLocations.addAll(locations);
  }
}
