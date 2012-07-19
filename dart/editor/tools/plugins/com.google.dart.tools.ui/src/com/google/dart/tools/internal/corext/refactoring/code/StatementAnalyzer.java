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

import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import java.util.List;

/**
 * Analyzer to check if a selection covers a valid set of statements of an abstract syntax tree. The
 * selection is valid iff
 * <ul>
 * <li>it does not start or end in the middle of a comment.</li>
 * <li>no extract characters except the empty statement ";" is included in the selection.</li>
 * </ul>
 */
public class StatementAnalyzer extends SelectionAnalyzer {

  protected static boolean contains(DartNode[] nodes, DartNode node) {
    for (int i = 0; i < nodes.length; i++) {
      if (nodes[i] == node) {
        return true;
      }
    }
    return false;
  }

  protected static boolean contains(DartNode[] nodes, List<DartExpression> list) {
    for (int i = 0; i < nodes.length; i++) {
      if (list.contains(nodes[i])) {
        return true;
      }
    }
    return false;
  }

//  private static List<SwitchCase> getSwitchCases(DartSwitchStatement node) {
//    List<SwitchCase> result = Lists.newArrayList();
//    for (Iterator<DartStatement> iter = node.statements().iterator(); iter.hasNext();) {
//      Object element = iter.next();
//      if (element instanceof SwitchCase) {
//        result.add((SwitchCase) element);
//      }
//    }
//    return result;
//  }

  protected CompilationUnit fCUnit;

//  private TokenScanner fScanner;

  private RefactoringStatus fStatus;

  public StatementAnalyzer(CompilationUnit cunit, Selection selection, boolean traverseSelectedNode)
      throws CoreException {
    super(selection, traverseSelectedNode);
    Assert.isNotNull(cunit);
    fCUnit = cunit;
    fStatus = new RefactoringStatus();
//    fScanner = new TokenScanner(fCUnit);
  }

  public RefactoringStatus getStatus() {
    return fStatus;
  }

  @Override
  public Void visitDoWhileStatement(DartDoWhileStatement node) {
    super.visitDoWhileStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    if (doAfterValidation(node, selectedNodes)) {
      if (contains(selectedNodes, node.getBody()) && contains(selectedNodes, node.getCondition())) {
        invalidSelection(RefactoringCoreMessages.StatementAnalyzer_do_body_expression);
      }
    }
    return null;
  }

  @Override
  public Void visitForStatement(DartForStatement node) {
    super.visitForStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    if (doAfterValidation(node, selectedNodes)) {
      boolean containsExpression = contains(selectedNodes, node.getCondition());
      boolean containsUpdaters = contains(selectedNodes, node.getIncrement());
      if (contains(selectedNodes, node.getInit()) && containsExpression) {
        invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_initializer_expression);
      } else if (containsExpression && containsUpdaters) {
        invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_expression_updater);
      } else if (containsUpdaters && contains(selectedNodes, node.getBody())) {
        invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_updater_body);
      }
    }
    return null;
  }

  @Override
  public Void visitSwitchStatement(DartSwitchStatement node) {
    super.visitSwitchStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    if (doAfterValidation(node, selectedNodes)) {
      List<DartSwitchMember> switchMembers = node.getMembers();
      for (DartNode topNode : selectedNodes) {
        if (switchMembers.contains(topNode)) {
          invalidSelection(RefactoringCoreMessages.StatementAnalyzer_switch_statement);
          break;
        }
      }
    }
    return null;
  }

  @Override
  public Void visitTryStatement(DartTryStatement node) {
    super.visitTryStatement(node);
    DartNode firstSelectedNode = getFirstSelectedNode();
    if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
      if (firstSelectedNode == node.getTryBlock() || firstSelectedNode == node.getFinallyBlock()) {
        invalidSelection(RefactoringCoreMessages.StatementAnalyzer_try_statement);
      } else {
        List<DartCatchBlock> catchBlocks = node.getCatchBlocks();
        for (DartCatchBlock catchBlock : catchBlocks) {
          if (catchBlock == firstSelectedNode || catchBlock.getBlock() == firstSelectedNode) {
            invalidSelection(RefactoringCoreMessages.StatementAnalyzer_try_statement);
          } else if (catchBlock.getException() == firstSelectedNode) {
            invalidSelection(RefactoringCoreMessages.StatementAnalyzer_catch_argument);
          }
        }
      }
    }
    return null;
  }

  @Override
  public Void visitUnit(DartUnit node) {
    super.visitUnit(node);
    if (!hasSelectedNodes()) {
      return null;
    }
    // TODO(scheglov) not sure if we need to check comments, at least right now
//    {
//      DartNode selectedNode = getFirstSelectedNode();
//      Selection selection = getSelection();
//      if (node != selectedNode) {
//        DartNode parent = selectedNode.getParent();
//        fStatus.merge(CommentAnalyzer.perform(
//            selection,
//            fScanner.getScanner(),
//            parent.getStartPosition(),
//            parent.getLength()));
//      }
//    }
    if (!fStatus.hasFatalError()) {
      checkSelectedNodes();
    }
    return null;
  }

  @Override
  public Void visitWhileStatement(DartWhileStatement node) {
    super.visitWhileStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    if (doAfterValidation(node, selectedNodes)) {
      if (contains(selectedNodes, node.getCondition()) && contains(selectedNodes, node.getBody())) {
        invalidSelection(RefactoringCoreMessages.StatementAnalyzer_while_expression_body);
      }
    }
    return null;
  }

  protected void checkSelectedNodes() {
    // TODO(scheglov) Check:
    // 1. StatementAnalyzer_end_of_selection = something not-blank selected after statement
    // 2. StatementAnalyzer_beginning_of_selection = something not-blank selected before statement
//    DartNode[] nodes = getSelectedNodes();
//    if (nodes.length == 0) {
//      return;
//    }
//
//    DartNode node = nodes[0];
//    int selectionOffset = getSelection().getOffset();
//    try {
//      int start = fScanner.getNextStartOffset(selectionOffset, true);
//      if (start == node.getSourceInfo().getOffset()) {
//        int lastNodeEnd = ASTNodes.getExclusiveEnd(nodes[nodes.length - 1]);
//        int pos = fScanner.getNextStartOffset(lastNodeEnd, true);
//        int selectionEnd = getSelection().getInclusiveEnd();
//        if (pos <= selectionEnd) {
//          IScanner scanner = fScanner.getScanner();
//          char[] token = scanner.getCurrentTokenSource(); //see https://bugs.eclipse.org/324237
//          if (start < lastNodeEnd && token.length == 1 && (token[0] == ';' || token[0] == ',')) {
//            setSelection(Selection.createFromStartEnd(start, lastNodeEnd - 1));
//          } else {
//            SourceRange range = new SourceRangeImpl(lastNodeEnd, pos - lastNodeEnd);
//            invalidSelection(
//                RefactoringCoreMessages.StatementAnalyzer_end_of_selection,
//                DartStatusContext.create(fCUnit, range));
//          }
//        }
//        return; // success
//      }
//    } catch (CoreException e) {
//      // fall through
//    }
//    SourceRange range = new SourceRangeImpl(selectionOffset, node.getSourceInfo().getOffset()
//        - selectionOffset + 1);
//    invalidSelection(
//        RefactoringCoreMessages.StatementAnalyzer_beginning_of_selection,
//        DartStatusContext.create(fCUnit, range));
  }

  protected CompilationUnit getCompilationUnit() {
    return fCUnit;
  }

//  protected TokenScanner getTokenScanner() {
//    return fScanner;
//  }

  protected void invalidSelection(String message) {
    fStatus.addFatalError(message);
    reset();
  }

  protected void invalidSelection(String message, RefactoringStatusContext context) {
    fStatus.addFatalError(message, context);
    reset();
  }

  private boolean doAfterValidation(DartNode node, DartNode[] selectedNodes) {
    return selectedNodes.length > 0 && node == selectedNodes[0].getParent()
        && getSelection().getEndVisitSelectionMode(node) == Selection.AFTER;
  }
}
