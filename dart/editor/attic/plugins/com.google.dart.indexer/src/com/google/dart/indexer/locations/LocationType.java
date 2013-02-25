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

public abstract class LocationType {
  private final char marker;

  protected LocationType(char marker) {
    this.marker = marker;
  }

  public abstract Location byUniqueIdentifier(String identifier);

  public char getMarker() {
    return marker;
  }

  public abstract Location load(RandomAccessFile file) throws IOException;

  public abstract void save(Location location, RandomAccessFile file) throws IOException;

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + marker + ")";
  }
}
