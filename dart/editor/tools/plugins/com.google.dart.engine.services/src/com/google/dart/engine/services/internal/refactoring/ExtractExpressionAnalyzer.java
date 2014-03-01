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
package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.services.util.SelectionAnalyzer;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * {@link SelectionAnalyzer} for "Extract Expression" refactoring.
 */
public class ExtractExpressionAnalyzer extends SelectionAnalyzer {
  private final RefactoringStatus status = new RefactoringStatus();

  public ExtractExpressionAnalyzer(SourceRange selection) {
    super(selection);
  }

  /**
   * @return the {@link RefactoringStatus} result of checking selection.
   */
  public RefactoringStatus getStatus() {
    return status;
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
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    super.visitSimpleIdentifier(node);
    if (isFirstSelectedNode(node)) {
      // name of declaration
      if (node.inDeclarationContext()) {
        invalidSelection("Cannot extract the name part of a declaration.");
      }
      // method name
      Element element = node.getBestElement();
      if (element instanceof FunctionElement || element instanceof MethodElement) {
        invalidSelection("Cannot extract a single method name.");
      }
      // name in property access
      AstNode parent = node.getParent();
      if (parent instanceof PrefixedIdentifier
          && ((PrefixedIdentifier) parent).getIdentifier() == node) {
        invalidSelection("Can not extract name part of a property access.");
      }
      if (parent instanceof PropertyAccess && ((PropertyAccess) parent).getPropertyName() == node) {
        invalidSelection("Can not extract name part of a property access.");
      }
    }
    return null;
  }

  /**
   * Records fatal error with given message.
   */
  protected final void invalidSelection(String message) {
    invalidSelection(message, null);
  }

  /**
   * Records fatal error with given message and {@link RefactoringStatusContext}.
   */
  protected final void invalidSelection(String message, RefactoringStatusContext context) {
    status.addFatalError(message, context);
    reset();
  }

  private boolean isFirstSelectedNode(AstNode node) {
    return getFirstSelectedNode() == node;
  }
}
