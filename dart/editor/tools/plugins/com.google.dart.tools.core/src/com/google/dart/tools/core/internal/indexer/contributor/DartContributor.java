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
package com.google.dart.tools.core.internal.indexer.contributor;

import com.google.dart.compiler.ast.DartPlainVisitor;
import com.google.dart.indexer.index.configuration.Contributor;
import com.google.dart.indexer.index.updating.LayerUpdater;
import com.google.dart.tools.core.model.CompilationUnit;

public interface DartContributor extends Contributor, DartPlainVisitor<Void> {
  /**
   * Initialize this contributor to visit the nodes in the given compilation unit, using the given
   * layer updater to update the layer being contributed to.
   * 
   * @param compilationUnit the compilation unit that is about to be visited
   * @param layerUpdater the updater used to contribute relationships to the appropriate layer
   */
  public void initialize(CompilationUnit compilationUnit, LayerUpdater layerUpdater);
}
