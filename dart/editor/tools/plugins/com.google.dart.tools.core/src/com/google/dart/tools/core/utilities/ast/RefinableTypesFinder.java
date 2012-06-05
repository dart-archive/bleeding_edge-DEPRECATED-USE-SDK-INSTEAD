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
package com.google.dart.tools.core.utilities.ast;

import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.type.Type;

import java.util.List;

/**
 * Instances of the class <code>RefinableTypesFinder</code> find AST nodes whose type is refinable
 * (e.g., dynamic or inferred).
 */
public class RefinableTypesFinder extends ASTVisitor<Void> {

  private final List<DartNode> matches = Lists.newArrayList();

  /**
   * Get matched AST nodes.
   * 
   * @return matched AST nodes.
   */
  public Iterable<DartNode> getMatches() {
    return matches;
  }

  /**
   * Perform the search.
   * 
   * @param ast the AST to search within
   */
  public void searchWithin(DartNode ast) {
    ast.accept(this);
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {

    if (isRefinable(node)) {
      matches.add(node);
    }

    return super.visitIdentifier(node);
  }

  private boolean isRefinable(DartNode node) {
    Type type = node.getType();
    return type != null && type.isInferred();
  }
}
