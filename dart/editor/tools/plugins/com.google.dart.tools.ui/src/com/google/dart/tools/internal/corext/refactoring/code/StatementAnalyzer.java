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
import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
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
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class StatementAnalyzer extends SelectionAnalyzer {

  protected static boolean contains(DartNode[] nodes, DartNode node) {
    for (DartNode dartNode : nodes) {
      if (dartNode == node) {
        return true;
      }
    }
    return false;
  }

  protected CompilationUnit fCUnit;
  private RefactoringStatus fStatus;

  public StatementAnalyzer(CompilationUnit cunit, Selection selection, boolean traverseSelectedNode)
      throws CoreException {
    super(selection, traverseSelectedNode);
    Assert.isNotNull(cunit);
    fCUnit = cunit;
    fStatus = new RefactoringStatus();
  }

  public RefactoringStatus getStatus() {
    return fStatus;
  }

  @Override
  public Void visitDoWhileStatement(DartDoWhileStatement node) {
    super.visitDoWhileStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    if (contains(selectedNodes, node.getBody())) {
      invalidSelection(RefactoringCoreMessages.StatementAnalyzer_do_body);
    }
    return null;
  }

  @Override
  public Void visitForStatement(DartForStatement node) {
    super.visitForStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    boolean containsInit = contains(selectedNodes, node.getInit());
    boolean containsCondition = contains(selectedNodes, node.getCondition());
    boolean containsUpdaters = contains(selectedNodes, node.getIncrement());
    boolean containsBody = contains(selectedNodes, node.getBody());
    if (containsInit && containsCondition) {
      invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_initializer_condition);
    } else if (containsCondition && containsUpdaters) {
      invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_condition_updaters);
    } else if (containsUpdaters && containsBody) {
      invalidSelection(RefactoringCoreMessages.StatementAnalyzer_for_updaters_body);
    }
    return null;
  }

  @Override
  public Void visitSwitchStatement(DartSwitchStatement node) {
    super.visitSwitchStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    List<DartSwitchMember> switchMembers = node.getMembers();
    for (DartNode topNode : selectedNodes) {
      if (switchMembers.contains(topNode)) {
        invalidSelection(RefactoringCoreMessages.StatementAnalyzer_switch_statement);
        break;
      }
    }
    return null;
  }

  @Override
  public Void visitTryStatement(DartTryStatement node) {
    super.visitTryStatement(node);
    DartNode firstSelectedNode = getFirstSelectedNode();
    if (firstSelectedNode == node.getTryBlock() || firstSelectedNode == node.getFinallyBlock()) {
      invalidSelection(RefactoringCoreMessages.StatementAnalyzer_try_statement);
    } else {
      List<DartCatchBlock> catchBlocks = node.getCatchBlocks();
      for (DartCatchBlock catchBlock : catchBlocks) {
        if (catchBlock == firstSelectedNode || catchBlock.getBlock() == firstSelectedNode
            || catchBlock.getException() == firstSelectedNode) {
          invalidSelection(RefactoringCoreMessages.StatementAnalyzer_try_statement);
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
    // check that selection does not begin/end in comment
    {
      int selectionStart = getSelection().getOffset();
      int selectionEnd = getSelection().getInclusiveEnd();
      List<DartComment> comments = node.getComments();
      for (DartComment comment : comments) {
        SourceRange commentRange = SourceRangeFactory.create(comment);
        if (SourceRangeUtils.contains(commentRange, selectionStart)) {
          invalidSelection(RefactoringCoreMessages.CommentAnalyzer_starts_inside_comment);
        }
        if (SourceRangeUtils.contains(commentRange, selectionEnd)) {
          invalidSelection(RefactoringCoreMessages.CommentAnalyzer_ends_inside_comment);
        }
      }
    }
    // more checks
    if (!fStatus.hasFatalError()) {
      checkSelectedNodes(node);
    }
    return null;
  }

  @Override
  public Void visitWhileStatement(DartWhileStatement node) {
    super.visitWhileStatement(node);
    DartNode[] selectedNodes = getSelectedNodes();
    if (contains(selectedNodes, node.getCondition()) && contains(selectedNodes, node.getBody())) {
      invalidSelection(RefactoringCoreMessages.StatementAnalyzer_while_expression_body);
    }
    return null;
  }

  protected void checkSelectedNodes(DartUnit unit) {
    DartNode[] nodes = getSelectedNodes();
    // some tokens before first selected node
    {
      int selectionOffset = getSelection().getOffset();
      DartNode firstNode = nodes[0];
      int firstNodeOffset = firstNode.getSourceInfo().getOffset();
      if (hasTokens(selectionOffset, firstNodeOffset)) {
        SourceRange range = SourceRangeFactory.forStartEnd(selectionOffset, firstNodeOffset);
        invalidSelection(
            RefactoringCoreMessages.StatementAnalyzer_beginning_of_selection,
            DartStatusContext.create(fCUnit, range));
      }
    }
    // some tokens after last selected node
    {
      int selectionEnd = getSelection().getExclusiveEnd();
      DartNode lastNode = nodes[nodes.length - 1];
      int lastNodeEnd = lastNode.getSourceInfo().getEnd();
      if (hasTokens(lastNodeEnd, selectionEnd)) {
        SourceRange range = SourceRangeFactory.forStartEnd(lastNodeEnd, selectionEnd);
        invalidSelection(
            RefactoringCoreMessages.StatementAnalyzer_end_of_selection,
            DartStatusContext.create(fCUnit, range));
      }
    }
  }

  protected final void invalidSelection(String message) {
    fStatus.addFatalError(message);
    reset();
  }

  protected final void invalidSelection(String message, RefactoringStatusContext context) {
    fStatus.addFatalError(message, context);
    reset();
  }

  /**
   * @return <code>true</code> if there are tokens in the given source range.
   */
  private boolean hasTokens(final int start, final int end) {
    return ExecutionUtils.runObjectIgnore(new RunnableObjectEx<Boolean>() {
      @Override
      public Boolean runObject() throws Exception {
        String text = fCUnit.getBuffer().getText(start, end - start);
        StringScanner scanner = new StringScanner(null, text, null);
        com.google.dart.engine.scanner.Token token = scanner.tokenize();
        return token.getType() != TokenType.EOF;
      }
    }, false);
  }
}
