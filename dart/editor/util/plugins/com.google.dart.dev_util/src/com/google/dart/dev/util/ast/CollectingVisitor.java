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
package com.google.dart.dev.util.ast;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;

import java.util.ArrayList;
import java.util.List;

public class CollectingVisitor extends GeneralizingAstVisitor<Void> {

  private final List<AstNode> nodes = new ArrayList<AstNode>();

  public List<AstNode> getNodes() {
    return nodes;
  }

  @Override
  public Void visitNode(AstNode node) {
    nodes.add(node);
    return null;
  }

}
