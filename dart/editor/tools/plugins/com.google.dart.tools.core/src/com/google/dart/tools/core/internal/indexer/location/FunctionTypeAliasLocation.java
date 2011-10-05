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
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>FunctionTypeAliasLocation</code> represent a location within a
 * function type alias declaration.
 */
public class FunctionTypeAliasLocation extends DartElementLocationImpl {
  public final static LocationType TYPE = new DartElementLocationType('A') {
    @Override
    protected DartElementLocationImpl createLocation(DartElement element, SourceRange sourceRange) {
      if (element instanceof DartFunctionTypeAlias) {
        return new FunctionTypeAliasLocation((DartFunctionTypeAlias) element, sourceRange);
      }
      return null;
    }
  };

  /**
   * The function type alias represented by this location.
   */
  private final DartFunctionTypeAlias alias;

  /**
   * Initialize a newly created function location to represent the given function.
   * 
   * @param alias the type represented by this location
   * @param sourceRange the source range associated with this location
   */
  public FunctionTypeAliasLocation(DartFunctionTypeAlias alias, SourceRange sourceRange) {
    super(sourceRange);
    if (alias == null) {
      throw new NullPointerException("function type alias");
    }
    this.alias = alias;
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
    final FunctionTypeAliasLocation other = (FunctionTypeAliasLocation) obj;
    if (alias == null) {
      if (other.alias != null) {
        return false;
      }
    } else if (!alias.equals(other.alias)) {
      return false;
    }
    return true;
  }

  @Override
  public DartElement getDartElement() {
    return getFunction();
  }

  /**
   * Return the function represented by this location.
   * 
   * @return the function represented by this location
   */
  public DartFunctionTypeAlias getFunction() {
    return alias;
  }

  @Override
  public LocationType getLocationType() {
    return TYPE;
  }

  @Override
  public int hashCode() {
    return (alias == null) ? 5 : alias.hashCode();
  }

  @Override
  public String toString() {
    return "function type alias " + alias.getElementName();
  }
}
