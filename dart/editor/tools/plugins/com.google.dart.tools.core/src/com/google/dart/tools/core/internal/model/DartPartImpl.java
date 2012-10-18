/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.common.base.Objects;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartPart;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Information about "part" declaration.
 */
public class DartPartImpl implements DartPart {
  private final SourceRange sourceRange;
  private final SourceRange uriRange;
  private final CompilationUnit unit;

  public DartPartImpl(CompilationUnit unit, SourceRange sourceRange, SourceRange uriRange) {
    this.sourceRange = sourceRange;
    this.uriRange = uriRange;
    this.unit = unit;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DartPartImpl) {
      DartPartImpl other = (DartPartImpl) obj;
      return Objects.equal(other.unit, unit);
    }
    return false;
  }

  @Override
  public SourceRange getSourceRange() {
    return sourceRange;
  }

  @Override
  public CompilationUnit getUnit() {
    return unit;
  }

  @Override
  public SourceRange getUriRange() {
    return uriRange;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(unit);
  }

}
