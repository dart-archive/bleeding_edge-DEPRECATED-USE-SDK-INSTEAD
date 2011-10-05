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
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>VariableLocation</code> represent a location within a variable.
 */
public class VariableLocation extends DartElementLocationImpl {
  public final static LocationType TYPE = new DartElementLocationType('V') {
    @Override
    protected DartElementLocationImpl createLocation(DartElement element, SourceRange sourceRange) {
      if (element instanceof DartVariableDeclaration) {
        return new VariableLocation((DartVariableDeclaration) element, sourceRange);
      }
      return null;
    }
  };

  /**
   * The variable represented by this location.
   */
  private final DartVariableDeclaration variable;

  /**
   * Initialize a newly created variable location to represent the given variable.
   * 
   * @param variable the variable represented by this location
   * @param sourceRange the source range associated with this location
   */
  public VariableLocation(DartVariableDeclaration variable, SourceRange sourceRange) {
    super(sourceRange);
    if (variable == null) {
      throw new NullPointerException("variable");
    }
    this.variable = variable;
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
    final VariableLocation other = (VariableLocation) obj;
    if (variable == null) {
      if (other.variable != null) {
        return false;
      }
    } else if (!variable.equals(other.variable)) {
      return false;
    }
    return true;
  }

  @Override
  public DartElement getDartElement() {
    return getVariable();
  }

  @Override
  public LocationType getLocationType() {
    return TYPE;
  }

  /**
   * Return the variable represented by this location.
   * 
   * @return the variable represented by this location
   */
  public DartVariableDeclaration getVariable() {
    return variable;
  }

  @Override
  public int hashCode() {
    return (variable == null) ? 3 : variable.hashCode();
  }

  @Override
  public String toString() {
    return "variable " + variable.getElementName();
  }
}
