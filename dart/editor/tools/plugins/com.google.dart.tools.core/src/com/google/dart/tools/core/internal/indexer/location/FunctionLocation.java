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
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>FunctionLocation</code> represent a location within a function
 * declaration.
 */
public class FunctionLocation extends DartElementLocationImpl {
  public final static LocationType TYPE = new DartElementLocationType('N') {
    @Override
    protected DartElementLocationImpl createLocation(DartElement element, SourceRange sourceRange) {
      if (element instanceof DartFunction) {
        return new FunctionLocation((DartFunction) element, sourceRange);
      }
      return null;
    }
  };

  /**
   * The function represented by this location.
   */
  private final DartFunction function;

  /**
   * Initialize a newly created function location to represent the given function.
   * 
   * @param function the type represented by this location
   * @param sourceRange the source range associated with this location
   */
  public FunctionLocation(DartFunction function, SourceRange sourceRange) {
    super(sourceRange);
    if (function == null) {
      throw new NullPointerException("function");
    }
    this.function = function;
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
    final FunctionLocation other = (FunctionLocation) obj;
    if (function == null) {
      if (other.function != null) {
        return false;
      }
    } else if (!function.equals(other.function)) {
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
  public DartFunction getFunction() {
    return function;
  }

  @Override
  public LocationType getLocationType() {
    return TYPE;
  }

  @Override
  public int hashCode() {
    return (function == null) ? 3 : function.hashCode();
  }

  @Override
  public String toString() {
    return "function " + function.getElementName();
  }
}
