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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.element.ElementLocationImpl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

/**
 * Helper to read {@link MemoryIndexStoreImpl} from {@link InputStream}.
 * 
 * @coverage dart.engine.index
 */
class MemoryIndexReader {
  static int FILE_VERSION_NUMBER = 1;

  private final MemoryIndexStoreImpl impl;
  private final AnalysisContext context;
  private final DataInputStream dis;

  MemoryIndexReader(MemoryIndexStoreImpl impl, AnalysisContext context, InputStream input) {
    this.impl = impl;
    this.context = context;
    this.dis = new DataInputStream(input);
  }

  /**
   * Reads information from the given {@link InputStream}.
   * 
   * @throws IOException if cannot read by some reason - incompatible version, file format error,
   *           etc.
   */
  public void read() throws IOException {
    // check version
    {
      int version = dis.readInt();
      if (version != FILE_VERSION_NUMBER) {
        throw new IOException(MessageFormat.format(
            "Incompatible file version, expected: {0} found: {1}",
            FILE_VERSION_NUMBER,
            version));
      }
    }
    // read Element(s)
    int numElements = dis.readInt();
    for (int i = 0; i < numElements; i++) {
      Element element = readElement();
      Relationship relationship = readRelationship();
      // read Location(s)
      int numLocations = dis.readInt();
      for (int j = 0; j < numLocations; j++) {
        Location location = readLocation();
        impl.recordRelationship(element, relationship, location);
      }
    }
  }

  private Element readElement() throws IOException {
    String elementLocationEncoding = dis.readUTF();
    ElementLocation elementLocation = new ElementLocationImpl(elementLocationEncoding);
    return context.getElement(elementLocation);
  }

  private Location readLocation() throws IOException {
    Element locationElement = readElement();
    int offset = dis.readInt();
    int length = dis.readInt();
    return new Location(locationElement, offset, length);
  }

  private Relationship readRelationship() throws IOException {
    String relationshipId = dis.readUTF();
    return Relationship.getRelationship(relationshipId);
  }
}
