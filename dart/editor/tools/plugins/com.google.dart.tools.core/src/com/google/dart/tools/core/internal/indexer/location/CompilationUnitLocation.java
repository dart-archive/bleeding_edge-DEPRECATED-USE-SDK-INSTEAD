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
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>CompilationUnitLocation</code> represent a location within a
 * compilation unit.
 */
public class CompilationUnitLocation extends DartElementLocationImpl {
  public final static LocationType TYPE = new DartElementLocationType('C') {
    @Override
    protected DartElementLocationImpl createLocation(DartElement element, SourceRange sourceRange) {
      if (element instanceof CompilationUnit) {
        return new CompilationUnitLocation((CompilationUnit) element, sourceRange);
      }
      return null;
    }
  };

  /**
   * The compilation unit represented by this location.
   */
  private final CompilationUnit compilationUnit;

  /**
   * Initialize a newly created compilation unit location to represent the given compilation unit.
   * 
   * @param compilationUnit the compilation unit represented by this location
   * @param sourceRange the source range associated with this location
   */
  public CompilationUnitLocation(CompilationUnit compilationUnit, SourceRange sourceRange) {
    super(sourceRange);
    if (compilationUnit == null) {
      throw new NullPointerException("compilationUnit");
    }
    this.compilationUnit = compilationUnit;
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
    final CompilationUnitLocation other = (CompilationUnitLocation) obj;
    if (compilationUnit == null) {
      if (other.compilationUnit != null) {
        return false;
      }
    } else if (!compilationUnit.equals(other.compilationUnit)) {
      return false;
    }
    return true;
  }

  /**
   * Return the compilation unit represented by this location.
   * 
   * @return the compilation unit represented by this location
   */
  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  @Override
  public DartElement getDartElement() {
    return getCompilationUnit();
  }

  @Override
  public LocationType getLocationType() {
    return TYPE;
  }

  @Override
  public int hashCode() {
    return (compilationUnit == null) ? 1 : compilationUnit.hashCode();
  }

  @Override
  public String toString() {
    return "compilation unit " + compilationUnit.getElementName();
  }
}
