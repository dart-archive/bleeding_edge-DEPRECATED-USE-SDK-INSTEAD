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
public class ReverseEdgesLocationInfo implements LocationInfo {
  public static ReverseEdgesLocationInfo load(RandomAccessFile file) throws IOException {
    ReverseEdgesLocationInfo result = new ReverseEdgesLocationInfo();
    result.setSourceLocations(LocationPersitence.loadLocations(file));
    return result;
  }

  private Collection<Location> sourceLocations;

  public ReverseEdgesLocationInfo() {
    sourceLocations = new ArrayList<Location>();
  }

  public ReverseEdgesLocationInfo(Collection<Location> source) {
    sourceLocations = source;
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
  }

  @Override
  public Location[] getLocationsAffectedByRemovalOfSelf() {
    return getSourceLocations();
  }

  public Location[] getSourceLocations() {
    return sourceLocations.toArray(new Location[sourceLocations.size()]);
  }

  @Override
  public boolean isEmpty() {
    return sourceLocations.isEmpty();
  }

  public void removeSourceLocations(Set<Location> locationsToRemove) {
    Set<Location> sourceLocationsSet = new HashSet<Location>(sourceLocations);

    sourceLocationsSet.removeAll(locationsToRemove);
    sourceLocations.clear();
    sourceLocations.addAll(sourceLocationsSet);
  }

  public void save(RandomAccessFile file) throws IOException {
    LocationPersitence.saveLocations(file, sourceLocations);
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
  }

  private void setSourceLocations(Collection<Location> locations) {
    sourceLocations.clear();
    sourceLocations.addAll(locations);
  }
}
