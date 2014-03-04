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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Set;

/**
 * Instances of the class {@code ResolutionVerifier} verify that all of the nodes in an AST
 * structure that should have been resolved were resolved.
 */
public class ResolutionVerifier extends RecursiveAstVisitor<Void> {
  /**
   * A set containing nodes that are known to not be resolvable and should therefore not cause the
   * test to fail.
   */
  private Set<AstNode> knownExceptions;

  /**
   * A list containing all of the AST nodes that were not resolved.
   */
  private ArrayList<AstNode> unresolvedNodes = new ArrayList<AstNode>();

  /**
   * A list containing all of the AST nodes that were resolved to an element of the wrong type.
   */
  private ArrayList<AstNode> wrongTypedNodes = new ArrayList<AstNode>();

  /**
   * Initialize a newly created verifier to verify that all of the nodes in the visited AST
   * structures that are expected to have been resolved have an element associated with them.
   */
  public ResolutionVerifier() {
    this(null);
  }

  /**
   * Initialize a newly created verifier to verify that all of the identifiers in the visited AST
   * structures that are expected to have been resolved have an element associated with them. Nodes
   * in the set of known exceptions are not expected to have been resolved, even if they normally
   * would have been expected to have been resolved.
   * 
   * @param knownExceptions a set containing nodes that are known to not be resolvable and should
   *          therefore not cause the test to fail
   **/
  public ResolutionVerifier(Set<AstNode> knownExceptions) {
    this.knownExceptions = knownExceptions;
  }

  /**
   * Assert that all of the visited identifiers were resolved.
   */
  public void assertResolved() {
    if (!unresolvedNodes.isEmpty() || !wrongTypedNodes.isEmpty()) {
      PrintStringWriter writer = new PrintStringWriter();
      if (!unresolvedNodes.isEmpty()) {
        writer.print("Failed to resolve ");
        writer.print(unresolvedNodes.size());
        writer.println(" nodes:");
        printNodes(writer, unresolvedNodes);
      }
      if (!wrongTypedNodes.isEmpty()) {
        writer.print("Resolved ");
        writer.print(wrongTypedNodes.size());
        writer.println(" to the wrong type of element:");
        printNodes(writer, wrongTypedNodes);
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
    Type operandType = node.getLeftOperand().getStaticType();
    if (operandType == null || operandType.isDynamic()) {
      return null;
    }
    return checkResolved(node, node.getStaticElement(), MethodElement.class);
  }

  @Override
  public Void visitCommentReference(CommentReference node) {
    // We don't care whether we were able to resolve references in comments.
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    node.visitChildren(this);
    return checkResolved(node, node.getElement(), CompilationUnitElement.class);
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    // Not sure how to test the combinators given that it isn't an error if the names are not defined.
    return checkResolved(node, node.getElement(), ExportElement.class);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    node.visitChildren(this);
    if (node.getElement() instanceof LibraryElement) {
      wrongTypedNodes.add(node);
    }
    return null;
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    node.visitChildren(this);
    // TODO(brianwilkerson) If we start resolving function expressions, then conditionally check to
    // see whether the node was resolved correctly.
    return null; //checkResolved(node, node.getElement(), FunctionElement.class);
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    // Not sure how to test the combinators given that it isn't an error if the names are not defined.
    checkResolved(node, node.getElement(), ImportElement.class);
    SimpleIdentifier prefix = node.getPrefix();
    if (prefix == null) {
      return null;
    }
    return checkResolved(prefix, prefix.getStaticElement(), PrefixElement.class);
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    node.visitChildren(this);
    Type targetType = node.getRealTarget().getStaticType();
    if (targetType == null || targetType.isDynamic()) {
      return null;
    }
    return checkResolved(node, node.getStaticElement(), MethodElement.class);
  }

  @Override
  public Void visitLibraryDirective(LibraryDirective node) {
    return checkResolved(node, node.getElement(), LibraryElement.class);
  }

  @Override
  public Void visitNamedExpression(NamedExpression node) {
    // We don't resolve the names of named expressions
    return node.getExpression().accept(this);
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    return checkResolved(node, node.getElement(), CompilationUnitElement.class);
  }

  @Override
  public Void visitPartOfDirective(PartOfDirective node) {
    return checkResolved(node, node.getElement(), LibraryElement.class);
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    node.visitChildren(this);
    if (!node.getOperator().isUserDefinableOperator()) {
      return null;
    }
    Type operandType = node.getOperand().getStaticType();
    if (operandType == null || operandType.isDynamic()) {
      return null;
    }
    return checkResolved(node, node.getStaticElement(), MethodElement.class);
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefix = node.getPrefix();
    prefix.accept(this);
    Type prefixType = prefix.getStaticType();
    if (prefixType == null || prefixType.isDynamic()) {
      return null;
    }
    return checkResolved(node, node.getStaticElement(), null);
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    node.visitChildren(this);
    if (!node.getOperator().isUserDefinableOperator()) {
      return null;
    }
    Type operandType = node.getOperand().getStaticType();
    if (operandType == null || operandType.isDynamic()) {
      return null;
    }
    return checkResolved(node, node.getStaticElement(), MethodElement.class);
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    Expression target = node.getRealTarget();
    target.accept(this);
    Type targetType = target.getStaticType();
    if (targetType == null || targetType.isDynamic()) {
      return null;
    }
    return node.getPropertyName().accept(this);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    if (node.getName().equals("void")) {
      return null;
    }
    AstNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = ((MethodInvocation) parent);
      if (invocation.getMethodName() == node) {
        Expression target = invocation.getRealTarget();
        Type targetType = target == null ? null : target.getStaticType();
        if (targetType == null || targetType.isDynamic()) {
          return null;
        }
      }
    }
    return checkResolved(node, node.getStaticElement(), null);
  }

  private Void checkResolved(AstNode node, Element element, Class<? extends Element> expectedClass) {
    if (element == null) {
      if (knownExceptions == null || !knownExceptions.contains(node)) {
        unresolvedNodes.add(node);
      }
    } else if (expectedClass != null) {
      if (!expectedClass.isInstance(element)) {
        wrongTypedNodes.add(node);
      }
    }
    return null;
  }

  private String getFileName(AstNode node) {
    // TODO (jwren) there are two copies of this method, one here and one in StaticTypeVerifier,
    // they should be resolved into a single method
    if (node != null) {
      AstNode root = node.getRoot();
      if (root instanceof CompilationUnit) {
        CompilationUnit rootCU = ((CompilationUnit) root);
        if (rootCU.getElement() != null) {
          return rootCU.getElement().getSource().getFullName();
        } else {
          return "<unknown file- CompilationUnit.getElement() returned null>";
        }
      } else {
        return "<unknown file- CompilationUnit.getRoot() is not a CompilationUnit>";
      }
    }
    return "<unknown file- ASTNode is null>";
  }

  private void printNodes(PrintStringWriter writer, ArrayList<AstNode> nodes) {
    for (AstNode identifier : nodes) {
      writer.print("  ");
      writer.print(identifier.toString());
      writer.print(" (");
      writer.print(getFileName(identifier));
      writer.print(" : ");
      writer.print(identifier.getOffset());
      writer.println(")");
    }
  }
}
