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
package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.internal.correction.SelectionAnalyzer;
import com.google.dart.engine.services.internal.correction.StatementAnalyzer;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * {@link SelectionAnalyzer} for "Extract Method" refactoring.
 */
public class ExtractMethodAnalyzer extends StatementAnalyzer {

  public ExtractMethodAnalyzer(CorrectionUtils utils, SourceRange selection) {
    super(utils, selection);
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    super.visitAssignmentExpression(node);
    Expression lhs = node.getLeftHandSide();
    if (isFirstSelectedNode(lhs)) {
      invalidSelection(
          "Cannot extract the left-hand side of an assignment.",
          RefactoringStatusContext.create(lhs));
    }
    return null;
  }

  @Override
  public Void visitConstructorInitializer(ConstructorInitializer node) {
    super.visitConstructorInitializer(node);
    if (isFirstSelectedNode(node)) {
      invalidSelection(
          "Cannot extract a constructor initializer. Select expression part of initializer.",
          RefactoringStatusContext.create(node));
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    super.visitForStatement(node);
    if (node.getVariables() == getFirstSelectedNode()) {
      invalidSelection("Cannot extract initialization part of a 'for' statement.");
    } else if (node.getUpdaters().contains(getLastSelectedNode())) {
      invalidSelection("Cannot extract increment part of a 'for' statement.");
    }
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    super.visitSimpleIdentifier(node);
    if (isFirstSelectedNode(node)) {
      // name of declaration
      if (node.inDeclarationContext()) {
        invalidSelection("Cannot extract the name part of a declaration.");
      }
      // method name
      Element element = node.getElement();
      if (element instanceof FunctionElement || element instanceof MethodElement) {
        invalidSelection("Cannot extract a single method name.");
      }
      // name in property access
      if (node.getParent() instanceof PrefixedIdentifier
          && ((PrefixedIdentifier) node.getParent()).getIdentifier() == node) {
        invalidSelection("Can not extract name part of a property access.");
      }
    }
    return null;
  }

  @Override
  public Void visitTypeName(TypeName node) {
    super.visitTypeName(node);
    if (isFirstSelectedNode(node)) {
      invalidSelection("Cannot extract a single type reference.");
    }
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    if (isFirstSelectedNode(node)) {
      invalidSelection(
          "Cannot extract a variable declaration fragment. Select whole declaration statement.",
          RefactoringStatusContext.create(node));
    }
    return null;
  }

  @Override
  protected void handleNextSelectedNode(ASTNode node) {
    super.handleNextSelectedNode(node);
    checkParent(node);
  }

  @Override
  protected void handleSelectionEndsIn(ASTNode node) {
    super.handleSelectionEndsIn(node);
    invalidSelection("The selection does not cover a set of statements or an expression. "
        + "Extend selection to a valid range.");
  }

  private void checkParent(ASTNode node) {
    ASTNode firstParent = getFirstSelectedNode().getParent();
    do {
      node = node.getParent();
      if (node == firstParent) {
        return;
      }
    } while (node != null);
    invalidSelection("Not all selected statements are enclosed by the same parent statement.");
  }

  private boolean isFirstSelectedNode(ASTNode node) {
    return getFirstSelectedNode() == node;
  }
}
