/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.type;

import java.util.Set;

/**
 * A flat immutable union of {@code Type}s. Here "flat" means a union type never contains another
 * union type.
 */
// Other operations we may want:
//
// - lub: fold lub over the elements of the union. This will be less useful with
//   the spec-defined lub, but more useful with a custom lub that e.g. defines
//   [lub(List<int>, List<double>) = List<dynamic>].

// Discussion
//
// The union type is not really like other types in the implementation:
//
// - union types are never associated with [Element]s.
// - no user program ever declares one.
// - no value ever has union type.
// - they are never components of other types (e.g., never have [G<X|Y>]).
//
// With this in mind, I'm inclined to not make [UnionType] a subtype of [Type]. However,
// that causes problems if we want to make the propagated type a union type, since
// much existing code depends on the propagated type being a [Type].  But it
// might be more sane to make the propagated type always be a union type, which
// we can reduce to a [Type] by taking lubs (singleton is a special but common case
// where a union type is equivalent to a [Type], independent of the definition of lub).
//
// On the other hand, by making [UnionType] a [Type], we can more easily do
// more type related things with it in the future.  For example, if we were doing type
// inference for functions, then we might infer that a function retured a union type.
public interface UnionType extends Type {
  /**
   * @return an immutable view of the types in this union type.
   */
  public Set<Type> getElements();
}
