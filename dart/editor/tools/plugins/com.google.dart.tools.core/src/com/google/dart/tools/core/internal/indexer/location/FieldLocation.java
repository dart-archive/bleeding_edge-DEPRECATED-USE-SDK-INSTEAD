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

import com.google.dart.indexer.locations.LocationType;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>FieldLocation</code> represent a location within a field.
 */
public class FieldLocation extends DartElementLocationImpl {
  public final static LocationType TYPE = new DartElementLocationType('F') {
    @Override
    protected DartElementLocationImpl createLocation(DartElement element, SourceRange sourceRange) {
      if (element instanceof Field) {
        return new FieldLocation((Field) element, sourceRange);
      }
      return null;
    }
  };

  /**
   * The field represented by this location.
   */
  private final Field field;

  /**
   * Initialize a newly created field location to represent the given field.
   * 
   * @param field the field represented by this location
   * @param sourceRange the source range associated with this location
   */
  public FieldLocation(Field field, SourceRange sourceRange) {
    super(sourceRange);
    if (field == null) {
      throw new NullPointerException("field");
    }
    this.field = field;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final FieldLocation other = (FieldLocation) obj;
    if (field == null) {
      if (other.field != null) {
        return false;
      }
    } else if (!field.equals(other.field)) {
      return false;
    }
    return true;
  }

  @Override
  public DartElement getDartElement() {
    return getField();
  }

  /**
   * Return the field represented by this location.
   * 
   * @return the field represented by this location
   */
  public Field getField() {
    return field;
  }

  @Override
  public LocationType getLocationType() {
    return TYPE;
  }

  @Override
  public int hashCode() {
    return (field == null) ? 2 : field.hashCode();
  }

  @Override
  public String toString() {
    return "field " + field.getElementName();
  }
}
