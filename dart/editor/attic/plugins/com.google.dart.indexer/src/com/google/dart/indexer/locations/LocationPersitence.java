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
package com.google.dart.indexer.locations;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class LocationPersitence {
  /**
   * The unique instance of this class.
   */
  private static final LocationPersitence INSTANCE = new LocationPersitence();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class.
   */
  public static LocationPersitence getInstance() {
    return INSTANCE;
  }

  public static List<Location> loadLocations(RandomAccessFile file) throws IOException {
    int count = file.readInt();
    LocationPersitence locationPersitence = getInstance();
    ArrayList<Location> locations = new ArrayList<Location>();
    for (int i = 0; i < count; i++) {
      Location location = locationPersitence.load(file);
      locations.add(location);
    }
    return locations;
  }

  public static void saveLocations(RandomAccessFile file, final Collection<Location> locations)
      throws IOException {
    file.writeInt(locations.size());
    LocationPersitence locationPersitence = getInstance();
    for (Iterator<Location> iterator = locations.iterator(); iterator.hasNext();) {
      Location location = iterator.next();
      locationPersitence.save(location, file);
    }
  }

  private final Map<Character, LocationType> locationTypes = new HashMap<Character, LocationType>();

  /**
   * Prevent the creation of instances of this class.
   */
  private LocationPersitence() {
  }

  public Location byUniqueIdentifier(String identifier) {
    char typeCode = identifier.charAt(identifier.length() - 1);
    String name = identifier.substring(0, identifier.length() - 1);
    LocationType locationType = locationTypes.get(new Character(typeCode));
    if (locationType == null) {
      throw new IllegalArgumentException("Invalid identifier: unknown type code");
    }
    return locationType.byUniqueIdentifier(name);
  }

  public String getUniqueIdentifier(Location location) {
    return location.getSemiUniqueIdentifier() + location.getLocationType().getMarker();
  }

  public Location load(RandomAccessFile file) throws IOException {
    char marker = file.readChar();
    LocationType locationType = locationTypes.get(new Character(marker));
    if (locationType == null) {
      throw new IllegalArgumentException("Invalid stored location: unknown type code '" + marker
          + "'");
    }
    return locationType.load(file);
  }

  public void registerLocationType(LocationType locationType) {
    char marker = locationType.getMarker();
    Character markerChar = new Character(marker);
    LocationType previousLocationType = locationTypes.get(markerChar);
    if (previousLocationType != null) {
      if (previousLocationType == locationType) {
        return;
      } else {
        throw new IllegalArgumentException("Location types "
            + previousLocationType.getClass().getName() + " and "
            + locationType.getClass().getName() + " use the same marker character '" + marker + "'");
      }
    }
    locationTypes.put(markerChar, locationType);
  }

  public void save(Location location, RandomAccessFile file) throws IOException {
    LocationType locationType = location.getLocationType();
    file.writeChar(locationType.getMarker());
    locationType.save(location, file);
  }
}
