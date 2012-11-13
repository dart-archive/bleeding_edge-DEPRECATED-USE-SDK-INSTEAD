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
package com.google.dart.engine.resolver;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Instances of the class {@code ResolutionVerifier} verify that all of the nodes in an AST
 * structure that should have been resolved were resolved.
 */
public class ResolutionVerifier extends RecursiveASTVisitor<Void> {
  /**
   * A table mapping the AST nodes that have been resolved to the element to which they were
   * resolved.
   */
  private Map<ASTNode, Element> resolvedElementMap;

  /**
   * A set containing nodes that are known to not be resolvable and should therefore not cause the
   * test to fail.
   */
  private Set<ASTNode> knownExceptions;

  /**
   * A list containing all of the AST nodes that were not resolved.
   */
  private ArrayList<ASTNode> unresolvedNodes = new ArrayList<ASTNode>();

  /**
   * Initialize a newly created verifier to verify that all of the nodes in the visited AST
   * structures that are expected to have been resolved have an entry in the given map.
   * 
   * @param resolvedElementMap a table mapping the AST nodes that have been resolved to the element
   *          to which they were resolved
   */
  public ResolutionVerifier(Map<ASTNode, Element> resolvedElementMap) {
    this(resolvedElementMap, null);
  }

  /**
   * Initialize a newly created verifier to verify that all of the identifiers in the visited AST
   * structures that are expected to have been resolved have an entry in the given map. Nodes in the
   * set of known exceptions are not expected to have been resolved, even if they normally would
   * have been expected to have been resolved.
   * 
   * @param resolvedElementMap a table mapping the AST nodes that have been resolved to the element
   *          to which they were resolved
   * @param knownExceptions a set containing nodes that are known to not be resolvable and should
   *          therefore not cause the test to fail
   **/
  public ResolutionVerifier(Map<ASTNode, Element> resolvedElementMap, Set<ASTNode> knownExceptions) {
    this.resolvedElementMap = resolvedElementMap;
    this.knownExceptions = knownExceptions;
  }

  /**
   * Assert that all of the visited identifiers were resolved.
   */
  public void assertResolved() {
    if (!unresolvedNodes.isEmpty()) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Failed to resolve ");
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
  public Void visitBinaryExpression(BinaryExpression node) {
    node.visitChildren(this);
    if (!node.getOperator().isUserDefinableOperator()) {
      return null;
    }
    return checkResolved(node);
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    node.visitChildren(this);
    return checkResolved(node);
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    // Not sure how to test the combinators given that it isn't an error if the names are not defined.
    checkResolved(node.getLibraryUri());
    return checkResolved(node.getPrefix());
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    node.visitChildren(this);
    return checkResolved(node);
  }

  @Override
  public Void visitLibraryDirective(LibraryDirective node) {
    return checkResolved(node);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    node.visitChildren(this);
    return checkResolved(node);
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    return checkResolved(node.getPartUri());
  }

  @Override
  public Void visitPartOfDirective(PartOfDirective node) {
    return checkResolved(node);
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    node.visitChildren(this);
    if (!node.getOperator().isUserDefinableOperator()) {
      return null;
    }
    return checkResolved(node);
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    node.visitChildren(this);
    if (!node.getOperator().isUserDefinableOperator()) {
      return null;
    }
    return checkResolved(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    if (node.getName().equals("void")) {
      return null;
    }
    return checkResolved(node);
  }

  private Void checkResolved(ASTNode node) {
    if (node != null && !resolvedElementMap.containsKey(node)) {
      if (knownExceptions == null || !knownExceptions.contains(node)) {
        unresolvedNodes.add(node);
      }
    }
    return null;
  }
}
