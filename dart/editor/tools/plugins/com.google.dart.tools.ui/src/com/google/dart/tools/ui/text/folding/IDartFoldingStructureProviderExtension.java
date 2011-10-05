/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text.folding;

import com.google.dart.tools.core.model.DartElement;

/**
 * Extends {@link IDartFoldingStructureProvider} with the following functions:
 * <ul>
 * <li>collapsing of comments and members</li>
 * <li>expanding and collapsing of certain JavaScript elements</li>
 * </ul>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface IDartFoldingStructureProviderExtension {
  /**
   * Collapses all comments.
   */
  void collapseComments();

  /**
   * Collapses the given elements.
   * 
   * @param elements the JavaScript elements to collapse (the array and its elements must not be
   *          modified)
   */
  void collapseElements(DartElement[] elements);

  /**
   * Collapses all members except for top level types.
   */
  void collapseMembers();

  /**
   * Expands the given elements.
   * 
   * @param elements the JavaScript elements to expand (the array and its elements must not be
   *          modified)
   */
  void expandElements(DartElement[] elements);
}
