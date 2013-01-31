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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Instances of the class {@code StaticTypeVerifier} verify that all of the nodes in an AST
 * structure that should have a static type associated with them do have a static type.
 */
public class StaticTypeVerifier extends GeneralizingASTVisitor<Void> {
  /**
   * A list containing all of the AST nodes that were not resolved.
   */
  private ArrayList<ASTNode> unresolvedNodes = new ArrayList<ASTNode>();

  /**
   * Initialize a newly created verifier to verify that all of the nodes in an AST structure that
   * should have a static type associated with them do have a static type.
   */
  public StaticTypeVerifier() {
    super();
  }

  /**
   * Assert that all of the visited nodes have a static type associated with them.
   */
  public void assertResolved() {
    if (!unresolvedNodes.isEmpty()) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Failed to associate types with ");
      writer.print(unresolvedNodes.size());
      writer.println(" nodes:");
      for (ASTNode identifier : unresolvedNodes) {
        writer.print("  ");
        writer.print(identifier.toString());
        writer.print(" (");
        writer.print(identifier.getOffset());
        writer.println(")");
      }
      Assert.fail(writer.toString());
    }
  }

  @Override
  public Void visitExpression(Expression node) {
    node.visitChildren(this);
    if (node.getStaticType() == null) {
      unresolvedNodes.add(node);
    }
    return null;
  }

  @Override
  public Void visitTypeName(TypeName node) {
    node.visitChildren(this);
    if (node.getType() == null) {
      unresolvedNodes.add(node);
    }
    return null;
  }
}
