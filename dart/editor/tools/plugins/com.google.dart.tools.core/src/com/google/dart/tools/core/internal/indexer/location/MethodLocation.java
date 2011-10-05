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
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>MethodLocation</code> represent a location within a method.
 */
public class MethodLocation extends DartElementLocationImpl {
  /**
   * The method represented by this location.
   */
  private final Method method;

  public final static LocationType TYPE = new DartElementLocationType('M') {
    @Override
    protected DartElementLocationImpl createLocation(DartElement element, SourceRange sourceRange) {
      if (element instanceof Method) {
        return new MethodLocation((Method) element, sourceRange);
      }
      return null;
    }
  };

  /**
   * Initialize a newly created method location to represent the given method.
   * 
   * @param method the method represented by this location
   * @param sourceRange the source range associated with this location
   */
  public MethodLocation(Method method, SourceRange sourceRange) {
    super(sourceRange);
    if (method == null) {
      throw new NullPointerException("method");
    }
    this.method = method;
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
    final MethodLocation other = (MethodLocation) obj;
    if (method == null) {
      if (other.method != null) {
        return false;
      }
    } else if (!method.equals(other.method)) {
      return false;
    }
    return true;
  }

  @Override
  public DartElement getDartElement() {
    return getMethod();
  }

  @Override
  public LocationType getLocationType() {
    return TYPE;
  }

  /**
   * Return the method represented by this location.
   * 
   * @return the method represented by this location
   */
  public Method getMethod() {
    return method;
  }

  @Override
  public int hashCode() {
    return (method == null) ? 7 : method.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("method ");
    builder.append(method.getElementName());
    builder.append('(');
    try {
      String[] typeNames = method.getParameterTypeNames();
      for (int i = 0; i < typeNames.length; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        builder.append(typeNames[i]);
      }
    } catch (DartModelException exception) {
    }
    builder.append(')');
    return builder.toString();
  }
}
