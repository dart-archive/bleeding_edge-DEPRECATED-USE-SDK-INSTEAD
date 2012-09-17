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
package com.google.dart.tools.internal.corext.refactoring.code;

import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.resolver.ElementKind;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;

import org.eclipse.core.runtime.CoreException;

/**
 * {@link SelectionAnalyzer} for "Extract Method" refactoring.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractMethodAnalyzer extends StatementAnalyzer {

  public ExtractMethodAnalyzer(CompilationUnit unit, Selection selection) throws CoreException {
    super(unit, selection, false);
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    super.visitBinaryExpression(node);
    DartExpression lhs = node.getArg1();
    if (isFirstSelectedNode(lhs) && ASTNodes.inSetterContext(lhs)) {
      invalidSelection(
          RefactoringCoreMessages.ExtractMethodAnalyzer_leftHandSideOfAssignment,
          DartStatusContext.create(fCUnit, node));
    }
    return null;
  }

  @Override
  public Void visitExpression(DartExpression node) {
    super.visitExpression(node);
    // "name" part of qualified name
    if (isFirstSelectedNode(node)
        && PropertyDescriptorHelper.getLocationInParent(node) == PropertyDescriptorHelper.DART_PROPERTY_ACCESS_NAME) {
      invalidSelection(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_part_of_qualified_name,
          DartStatusContext.create(fCUnit, getSelection()));
    }
    return null;
  }

  @Override
  public Void visitForStatement(DartForStatement node) {
    super.visitForStatement(node);
    if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
      if (node.getInit() == getFirstSelectedNode()) {
        invalidSelection(
            RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_initializer,
            DartStatusContext.create(fCUnit, getSelection()));
      } else if (node.getIncrement() == getLastSelectedNode()) {
        invalidSelection(
            RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_updater,
            DartStatusContext.create(fCUnit, getSelection()));
      }
    }
    return null;
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {
    super.visitIdentifier(node);
    if (isFirstSelectedNode(node)) {
      // name of declaration
      if (ASTNodes.isNameOfDeclaration(node)) {
        invalidSelection(
            RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_name_in_declaration,
            DartStatusContext.create(fCUnit, getSelection()));
      }
      // method reference name
      if (ElementKind.of(node.getElement()) == ElementKind.METHOD) {
        invalidSelection(
            RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_method_name_reference,
            DartStatusContext.create(fCUnit, getSelection()));
      }
    }
    return null;
  }

  @Override
  public Void visitInitializer(DartInitializer node) {
    super.visitInitializer(node);
    if (isFirstSelectedNode(node)) {
      invalidSelection(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_initializer,
          DartStatusContext.create(fCUnit, node));
    }
    return null;
  }

  @Override
  public Void visitTypeNode(DartTypeNode node) {
    super.visitTypeNode(node);
    if (isFirstSelectedNode(node)) {
      invalidSelection(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_type_reference,
          DartStatusContext.create(fCUnit, getSelection()));
    }
    return null;
  }

  @Override
  public Void visitVariable(DartVariable node) {
    super.visitVariable(node);
    if (isFirstSelectedNode(node)) {
      invalidSelection(
          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration_fragment,
          DartStatusContext.create(fCUnit, node));
    }
    return null;
  }

  @Override
  protected void handleNextSelectedNode(DartNode node) {
    super.handleNextSelectedNode(node);
    checkParent(node);
  }

  @Override
  protected boolean handleSelectionEndsIn(DartNode node) {
    invalidSelection(
        RefactoringCoreMessages.StatementAnalyzer_doesNotCover,
        DartStatusContext.create(fCUnit, node));
    return super.handleSelectionEndsIn(node);
  }

  private void checkParent(DartNode node) {
    DartNode firstParent = getFirstSelectedNode().getParent();
    do {
      node = node.getParent();
      if (node == firstParent) {
        return;
      }
    } while (node != null);
    invalidSelection(RefactoringCoreMessages.ExtractMethodAnalyzer_parent_mismatch);
  }

  private boolean isFirstSelectedNode(DartNode node) {
    return getFirstSelectedNode() == node;
  }
}
