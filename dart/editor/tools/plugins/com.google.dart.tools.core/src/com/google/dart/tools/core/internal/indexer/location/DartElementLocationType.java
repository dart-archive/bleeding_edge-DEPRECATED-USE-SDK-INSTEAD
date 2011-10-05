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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class DartElementLocationType extends LocationType {
  protected DartElementLocationType(char marker) {
    super(marker);
  }

  /**
   * Parse the given unique identifier to create a location. The unique identifier has the following
   * format:
   * 
   * <pre>
   *    <i>uniqueIdentifier</i> ::= <i>elementMemento</i> ':' <i>additionalData</i>
   *    <i>elementMemento</i> ::= String
   *    <i>additionalData</i> ::= <i>sourceInfo</i>  [ ';' <i>referenceKind</i> ]
   *    <i>sourceInfo</i> ::= <i>sourceOffset</i> ',' <i>sourceLength</i>
   *    <i>sourceOffset</i> ::= int
   *    <i>sourceLength</i> ::= int
   *    <i>referenceKind</i> ::= char
   * </pre>
   */
  @Override
  public Location byUniqueIdentifier(String identifier) {
    //
    // Locate the separator characters
    //
    int colonIndex = identifier.lastIndexOf(':');
    if (colonIndex < 0) {
      throw new IllegalArgumentException("Unique identifier does not contain a colon: "
          + identifier);
    }
    int commaIndex = identifier.indexOf(',', colonIndex);
    int semicolonIndex = identifier.indexOf(';', commaIndex);
    //
    // Parse the additional data
    //
    int offset = Integer.parseInt(identifier.substring(colonIndex + 1, commaIndex));
    int length;
    ReferenceKind kind;
    if (semicolonIndex < 0) {
      length = Integer.parseInt(identifier.substring(commaIndex + 1));
      kind = null;
    } else {
      length = Integer.parseInt(identifier.substring(commaIndex + 1, semicolonIndex));
      kind = ReferenceKind.fromCode(identifier.charAt(semicolonIndex + 1));
    }
    SourceRange sourceRange = new SourceRangeImpl(offset, length);
    identifier = identifier.substring(0, colonIndex);
    //
    // Create the location.
    //
    DartElement element = DartCore.create(identifier);
    if (element == null) {
      // TODO degrade gracefully (i.e. ignore) when DartCore.create returns null
      throw new IllegalArgumentException("DartCore.create returned null for ID \"" + identifier
          + "\", but indexer currently cannot handle this case.");
    }
    DartElementLocationImpl location = createLocation(element, sourceRange);
    if (kind != null) {
      location.setReferenceKind(kind);
    }
    return location;
  }

  @Override
  public final Location load(RandomAccessFile file) throws IOException {
    return byUniqueIdentifier(file.readUTF());
  }

  @Override
  public void save(Location location, RandomAccessFile file) throws IOException {
    file.writeUTF(location.getSemiUniqueIdentifier());
  }

  /**
   * Return a location of this type representing the given element, or <code>null</code> if the
   * given element is an incorrect kind of element (because an element might have been replaced with
   * an element of a different kind).
   * 
   * @param element the element containing the location
   * @param sourceRange the source range of the location
   * @return a location of this type representing the given element
   */
  protected abstract DartElementLocationImpl createLocation(DartElement element,
      SourceRange sourceRange);
}
