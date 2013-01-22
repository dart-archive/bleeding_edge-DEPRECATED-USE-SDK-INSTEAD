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
package com.google.dart.tools.ui.search;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.search.SearchScope;

/**
 * Describes a search query by giving the {@link DartNode} to search for.
 */
public class NodeQuerySpecification extends QuerySpecification {

  private DartNode node;

  public NodeQuerySpecification(DartNode node, int limitTo, SearchScope scope,
      String scopeDescription) {
    super(limitTo, scope, scopeDescription);
    this.node = node;
  }

  public DartNode getNode() {
    return node;
  }

  @Override
  public boolean hasNode() {
    return true;
  }
}
