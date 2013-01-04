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

package com.google.dart.engine.internal.index;

import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.index.IndexStore;

/**
 * Visits resolved AST and adds relationships into {@link IndexStore}.
 */
public class IndexContributor extends RecursiveASTVisitor<Void> {

  private final IndexStore store;

  public IndexContributor(IndexStore store) {
    this.store = store;
  }

}
