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
package com.google.dart.tools.core.internal.indexer.location;

import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationType;

import java.io.IOException;
import java.io.RandomAccessFile;

public class SyntheticLocationType extends LocationType {
  /**
   * The unique instance of this class.
   */
  private static final SyntheticLocationType UniqueInstance = new SyntheticLocationType();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static SyntheticLocationType getInstance() {
    return UniqueInstance;
  }

  /**
   * Prevent the creation of any instance of this class other than the one unique instance.
   */
  private SyntheticLocationType() {
    super('Z');
  }

  @Override
  public Location byUniqueIdentifier(String identifier) {
    return SyntheticLocation.getInstance(identifier);
  }

  @Override
  public final Location load(RandomAccessFile file) throws IOException {
    return byUniqueIdentifier(file.readUTF());
  }

  @Override
  public void save(Location location, RandomAccessFile file) throws IOException {
    file.writeUTF(location.getSemiUniqueIdentifier());
  }
}
