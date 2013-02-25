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
package com.google.dart.indexer.index.entries;

import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationPersitence;

import java.io.IOException;
import java.io.RandomAccessFile;

public class LocationInfoHeaders {
  private static final String SIGNATURE = "LocationInfo1";

  public static String readHeader(RandomAccessFile file) throws IOException {
    String signature = file.readUTF();
    if (!signature.equals(SIGNATURE)) {
      throw new IOException("Incorrect LocationInfo signature");
    }
    String locationId = file.readUTF();
    return locationId;
  }

  public static Location readHeaderAsLocation(RandomAccessFile file) throws IOException {
    return LocationPersitence.getInstance().byUniqueIdentifier(readHeader(file));
  }

  public static void writeHeader(RandomAccessFile file, Location location) throws IOException {
    file.writeUTF(SIGNATURE);
    file.writeUTF(LocationPersitence.getInstance().getUniqueIdentifier(location));
  }
}
