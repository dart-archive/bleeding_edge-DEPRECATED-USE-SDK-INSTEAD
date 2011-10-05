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
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;

/**
 * Instances of the class <code>TypeLocation</code> represent a location within a type.
 */
public class TypeLocation extends DartElementLocationImpl {
  public final static LocationType TYPE = new DartElementLocationType('T') {
    @Override
    protected DartElementLocationImpl createLocation(DartElement element, SourceRange sourceRange) {
      if (element instanceof Type) {
        return new TypeLocation((Type) element, sourceRange);
      }
      return null;
    }
  };

  /**
   * The type represented by this location.
   */
  private final Type type;

  /**
   * Initialize a newly created type location to represent the given type.
   * 
   * @param type the type represented by this location
   * @param sourceRange the source range associated with this location
   */
  public TypeLocation(Type type, SourceRange sourceRange) {
    super(sourceRange);
    if (type == null) {
      throw new NullPointerException("type");
    }
    this.type = type;
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
    final TypeLocation other = (TypeLocation) obj;
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

  @Override
  public DartElement getDartElement() {
    return getType();
  }

  @Override
  public LocationType getLocationType() {
    return TYPE;
  }

  /**
   * Return the type represented by this location.
   * 
   * @return the type represented by this location
   */
  public Type getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return (type == null) ? 13 : type.hashCode();
  }

  @Override
  public String toString() {
    return "type " + type.getElementName();
  }
}
