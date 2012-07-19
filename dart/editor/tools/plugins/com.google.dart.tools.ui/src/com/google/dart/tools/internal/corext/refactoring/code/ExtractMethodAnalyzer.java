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

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;

import org.eclipse.core.runtime.CoreException;

/**
 * {@link SelectionAnalyzer} for "Extract Method" refactoring.
 * <p>
 * TODO(scheglov) add checks
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractMethodAnalyzer extends StatementAnalyzer {

  public ExtractMethodAnalyzer(CompilationUnit unit, Selection selection) throws CoreException {
    super(unit, selection, false);
  }

//  @Override
//  public void endVisit(DartForStatement node) {
//    if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
//      if (node.initializers().contains(getFirstSelectedNode())) {
//        invalidSelection(
//            RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_initializer,
//            DartStatusContext.create(fCUnit, getSelection()));
//      } else if (node.updaters().contains(getLastSelectedNode())) {
//        invalidSelection(
//            RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_for_updater,
//            DartStatusContext.create(fCUnit, getSelection()));
//      }
//    }
//    super.endVisit(node);
//  }
//
//  @Override
//  public void endVisit(DartUnit node) {
//    RefactoringStatus status = getStatus();
//    superCall : {
//      if (status.hasFatalError()) {
//        break superCall;
//      }
//      if (!hasSelectedNodes()) {
//        DartNode coveringNode = getLastCoveringNode();
//        if (coveringNode instanceof DartBlock && coveringNode.getParent() instanceof DartFunction) {
//          DartFunction methodDecl = (DartFunction) coveringNode.getParent();
//          Message[] messages = ASTNodes.getMessages(methodDecl, ASTNodes.NODE_ONLY);
//          if (messages.length > 0) {
//            status.addFatalError(
//                Messages.format(
//                    RefactoringCoreMessages.ExtractMethodAnalyzer_compile_errors,
//                    BasicElementLabels.getJavaElementName(methodDecl.getName().getIdentifier())),
//                DartStatusContext.create(fCUnit, methodDecl));
//            break superCall;
//          }
//        }
//        status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_only_method_body);
//        break superCall;
//      }
//      fEnclosingBodyDeclaration = (BodyDeclaration) ASTNodes.getParent(
//          getFirstSelectedNode(),
//          BodyDeclaration.class);
//      if (fEnclosingBodyDeclaration == null
//          || (fEnclosingBodyDeclaration.getNodeType() != DartNode.METHOD_DECLARATION
//              && fEnclosingBodyDeclaration.getNodeType() != DartNode.FIELD_DECLARATION && fEnclosingBodyDeclaration.getNodeType() != DartNode.INITIALIZER)) {
//        status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_only_method_body);
//        break superCall;
//      } else if (ASTNodes.getEnclosingType(fEnclosingBodyDeclaration) == null) {
//        status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_compile_errors_no_parent_binding);
//        break superCall;
//      } else if (fEnclosingBodyDeclaration.getNodeType() == DartNode.METHOD_DECLARATION) {
//        fEnclosingMethodBinding = ((MethodDeclaration) fEnclosingBodyDeclaration).resolveBinding();
//      }
//      if (!isSingleExpressionOrStatementSet()) {
//        status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_single_expression_or_set);
//        break superCall;
//      }
//      if (isExpressionSelected()) {
//        DartNode expression = getFirstSelectedNode();
//        if (expression instanceof DartIdentifier) {
//          DartIdentifier name = (DartIdentifier) expression;
//          if (ElementKind.of(name.getElement()) == ElementKind.CLASS) {
//            status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_type_reference);
//            break superCall;
//          }
//          if (ElementKind.of(name.getElement()) == ElementKind.METHOD) {
//            status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_method_name_reference);
//            break superCall;
//          }
//          if (name.resolveBinding() instanceof IVariableBinding) {
//            StructuralPropertyDescriptor locationInParent = name.getLocationInParent();
//            if (locationInParent == QualifiedName.NAME_PROPERTY
//                || (locationInParent == FieldAccess.NAME_PROPERTY && !(((FieldAccess) name.getParent()).getExpression() instanceof ThisExpression))) {
//              status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_part_of_qualified_name);
//              break superCall;
//            }
//          }
//          if (name.isSimpleName() && ((SimpleName) name).isDeclaration()) {
//            status.addFatalError(RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_name_in_declaration);
//            break superCall;
//          }
//        }
//        fForceStatic = ASTNodes.getParent(expression, DartNode.SUPER_CONSTRUCTOR_INVOCATION) != null
//            || ASTNodes.getParent(expression, DartNode.CONSTRUCTOR_INVOCATION) != null;
//      }
//      status.merge(LocalTypeAnalyzer.perform(fEnclosingBodyDeclaration, getSelection()));
//      computeLastStatementSelected();
//    }
//    super.endVisit(node);
//  }
//
//  @Override
//  public void endVisit(VariableDeclarationExpression node) {
//    if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED
//        && getFirstSelectedNode() == node) {
//      if (node.getLocationInParent() == TryStatement.RESOURCES_PROPERTY) {
//        invalidSelection(
//            RefactoringCoreMessages.ExtractMethodAnalyzer_resource_in_try_with_resources,
//            DartStatusContext.create(fCUnit, getSelection()));
//      }
//    }
//    checkTypeInDeclaration(node.getType());
//    super.endVisit(node);
//  }
//
//  @Override
//  public void endVisit(VariableDeclarationStatement node) {
//    checkTypeInDeclaration(node.getType());
//    super.endVisit(node);
//  }
//
//  @Override
//  public boolean visit(AnonymousClassDeclaration node) {
//    boolean result = super.visit(node);
//    if (isFirstSelectedNode(node)) {
//      invalidSelection(
//          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_anonymous_type,
//          DartStatusContext.create(fCUnit, node));
//      return false;
//    }
//    return result;
//  }
//
//  @Override
//  public boolean visit(Assignment node) {
//    boolean result = super.visit(node);
//    if (getSelection().covers(node.getLeftHandSide())
//        || getSelection().coveredBy(node.getLeftHandSide())) {
//      invalidSelection(
//          RefactoringCoreMessages.ExtractMethodAnalyzer_leftHandSideOfAssignment,
//          DartStatusContext.create(fCUnit, node));
//      return false;
//    }
//    return result;
//  }
//
//  @Override
//  public boolean visit(ConstructorInvocation node) {
//    return visitConstructorInvocation(node, super.visit(node));
//  }
//
//  @Override
//  public boolean visit(DartDoWhileStatement node) {
//    boolean result = super.visit(node);
//
//    try {
//      int actionStart = getTokenScanner().getTokenEndOffset(
//          ITerminalSymbols.TokenNamedo,
//          node.getStartPosition());
//      if (getSelection().getOffset() == actionStart) {
//        invalidSelection(
//            RefactoringCoreMessages.ExtractMethodAnalyzer_after_do_keyword,
//            DartStatusContext.create(fCUnit, getSelection()));
//        return false;
//      }
//    } catch (CoreException e) {
//      // ignore
//    }
//
//    return result;
//  }
//
//  @Override
//  public boolean visit(DartFunction node) {
//    DartBlock body = node.getBody();
//    if (body == null) {
//      return false;
//    }
//    Selection selection = getSelection();
//    int nodeStart = body.getStartPosition();
//    int nodeExclusiveEnd = nodeStart + body.getLength();
//    // if selection node inside of the method body ignore method
//    if (!(nodeStart < selection.getOffset() && selection.getExclusiveEnd() < nodeExclusiveEnd)) {
//      return false;
//    }
//    return super.visit(node);
//  }
//
//  @Override
//  public boolean visit(SuperConstructorInvocation node) {
//    return visitConstructorInvocation(node, super.visit(node));
//  }
//
//  @Override
//  public boolean visit(VariableDeclarationFragment node) {
//    boolean result = super.visit(node);
//    if (isFirstSelectedNode(node)) {
//      invalidSelection(
//          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration_fragment,
//          DartStatusContext.create(fCUnit, node));
//      return false;
//    }
//    return result;
//  }

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

//  private void checkTypeInDeclaration(Type node) {
//    if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED
//        && getFirstSelectedNode() == node) {
//      invalidSelection(
//          RefactoringCoreMessages.ExtractMethodAnalyzer_cannot_extract_variable_declaration,
//          DartStatusContext.create(fCUnit, getSelection()));
//    }
//  }
//
//  private boolean isFirstSelectedNode(DartNode node) {
//    return getSelection().getVisitSelectionMode(node) == Selection.SELECTED
//        && getFirstSelectedNode() == node;
//  }
//
//  private boolean isSingleExpressionOrStatementSet() {
//    DartNode first = getFirstSelectedNode();
//    if (first == null) {
//      return true;
//    }
//    if (first instanceof DartExpression && getSelectedNodes().length != 1) {
//      return false;
//    }
//    return true;
//  }
//
//  private boolean visitConstructorInvocation(DartNode node, boolean superResult) {
//    if (getSelection().getVisitSelectionMode(node) == Selection.SELECTED) {
//      invalidSelection(
//          RefactoringCoreMessages.ExtractMethodAnalyzer_super_or_this,
//          DartStatusContext.create(fCUnit, node));
//      return false;
//    }
//    return superResult;
//  }
}
