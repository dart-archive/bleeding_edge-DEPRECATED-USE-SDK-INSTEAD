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

package com.google.dart.engine.services.internal.refactoring;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import java.util.List;

/**
 * The {@link SourceRange} in some {@link Source}.
 */
class SourceReference {
  final MatchKind kind;
  final Source source;
  final SourceRange range;
  final List<Element> elements = Lists.newArrayList();

  public SourceReference(MatchKind kind, Source source, SourceRange range) {
    this.kind = kind;
    this.source = source;
    this.range = range;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof SourceReference)) {
      return false;
    }
    SourceReference other = (SourceReference) obj;
    return Objects.equal(other.source, source) && Objects.equal(other.range, range);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(source, range);
  }

  @Override
  public String toString() {
    return source + "@" + range;
  }
}
