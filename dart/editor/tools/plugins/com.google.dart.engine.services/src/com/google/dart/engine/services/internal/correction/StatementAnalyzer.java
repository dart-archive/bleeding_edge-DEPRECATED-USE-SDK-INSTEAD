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
package com.google.dart.engine.services.internal.correction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.services.internal.util.ExecutionUtils;
import com.google.dart.engine.services.internal.util.RunnableObjectEx;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;

import java.util.List;

/**
 * Analyzer to check if a selection covers a valid set of statements of AST.
 */
public class StatementAnalyzer extends SelectionAnalyzer {

  /**
   * @return <code>true</code> if "nodes" contains "node".
   */
  private static boolean contains(List<ASTNode> nodes, ASTNode node) {
    return nodes.contains(node);
  }

  /**
   * @return <code>true</code> if "nodes" contains one of the "otherNodes".
   */
  private static boolean contains(List<ASTNode> nodes, List<? extends ASTNode> otherNodes) {
    for (ASTNode otherNode : otherNodes) {
      if (nodes.contains(otherNode)) {
        return true;
      }
    }
    return false;
  }

  protected final CorrectionUtils utils;
  private final RefactoringStatus status = new RefactoringStatus();

  public StatementAnalyzer(CompilationUnit cunit, SourceRange selection) throws Exception {
    this(new CorrectionUtils(cunit), selection);
  }

  public StatementAnalyzer(CorrectionUtils utils, SourceRange selection) {
    super(selection);
    this.utils = utils;
  }

  /**
   * @return the {@link RefactoringStatus} result of checking selection.
   */
  public RefactoringStatus getStatus() {
    return status;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    super.visitCompilationUnit(node);
    if (!hasSelectedNodes()) {
      return null;
    }
    // check that selection does not begin/end in comment
    {
      int selectionStart = selection.getOffset();
      int selectionEnd = selection.getEnd();
      List<SourceRange> commentRanges = utils.getCommentRanges();
      for (SourceRange commentRange : commentRanges) {
        if (commentRange.contains(selectionStart)) {
          invalidSelection("Selection begins inside a comment.");
        }
        if (commentRange.containsExclusive(selectionEnd)) {
          invalidSelection("Selection ends inside a comment.");
        }
      }
    }
    // more checks
    if (!status.hasFatalError()) {
      checkSelectedNodes(node);
    }
    return null;
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    super.visitDoStatement(node);
    List<ASTNode> selectedNodes = getSelectedNodes();
    if (contains(selectedNodes, node.getBody())) {
      invalidSelection("Operation not applicable to a 'do' statement's body and expression.");
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    super.visitForStatement(node);
    List<ASTNode> selectedNodes = getSelectedNodes();
    boolean containsInit = contains(selectedNodes, node.getInitialization())
        || contains(selectedNodes, node.getVariables());
    boolean containsCondition = contains(selectedNodes, node.getCondition());
    boolean containsUpdaters = contains(selectedNodes, node.getUpdaters());
    boolean containsBody = contains(selectedNodes, node.getBody());
    if (containsInit && containsCondition) {
      invalidSelection("Operation not applicable to a 'for' statement's initializer and condition.");
    } else if (containsCondition && containsUpdaters) {
      invalidSelection("Operation not applicable to a 'for' statement's condition and updaters.");
    } else if (containsUpdaters && containsBody) {
      invalidSelection("Operation not applicable to a 'for' statement's updaters and body.");
    }
    return null;
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    super.visitSwitchStatement(node);
    List<ASTNode> selectedNodes = getSelectedNodes();
    List<SwitchMember> switchMembers = node.getMembers();
    for (ASTNode selectedNode : selectedNodes) {
      if (switchMembers.contains(selectedNode)) {
        invalidSelection("Selection must either cover whole switch statement or parts of a single case block.");
        break;
      }
    }
    return null;
  }

  @Override
  public Void visitTryStatement(TryStatement node) {
    super.visitTryStatement(node);
    ASTNode firstSelectedNode = getFirstSelectedNode();
    if (firstSelectedNode != null) {
      if (firstSelectedNode == node.getBody() || firstSelectedNode == node.getFinallyClause()) {
        invalidSelection("Selection must either cover whole try statement or parts of try, catch, or finally block.");
      } else {
        List<CatchClause> catchClauses = node.getCatchClauses();
        for (CatchClause catchClause : catchClauses) {
          if (firstSelectedNode == catchClause || firstSelectedNode == catchClause.getBody()
              || firstSelectedNode == catchClause.getExceptionParameter()) {
            invalidSelection("Selection must either cover whole try statement or parts of try, catch, or finally block.");
          }
        }
      }
    }
    return null;
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    super.visitWhileStatement(node);
    List<ASTNode> selectedNodes = getSelectedNodes();
    if (contains(selectedNodes, node.getCondition()) && contains(selectedNodes, node.getBody())) {
      invalidSelection("Operation not applicable to a while statement's expression and body.");
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

  /**
   * Checks final selected {@link ASTNode}s after processing {@link CompilationUnit}.
   */
  private void checkSelectedNodes(CompilationUnit unit) {
    List<ASTNode> nodes = getSelectedNodes();
    // some tokens before first selected node
    {
      ASTNode firstNode = nodes.get(0);
      SourceRange rangeBeforeFirstNode = rangeStartStart(selection, firstNode);
      if (hasTokens(rangeBeforeFirstNode)) {
        invalidSelection(
            "The beginning of the selection contains characters that do not belong to a statement.",
            RefactoringStatusContext.create(unit, rangeBeforeFirstNode));
      }
    }
    // some tokens after last selected node
    {
      ASTNode lastNode = Iterables.getLast(nodes);
      SourceRange rangeAfterLastNode = rangeEndEnd(lastNode, selection);
      if (hasTokens(rangeAfterLastNode)) {
        invalidSelection(
            "The end of the selection contains characters that do not belong to a statement.",
            RefactoringStatusContext.create(unit, rangeAfterLastNode));
      }
    }
  }

  /**
   * @return the {@link Token}s in given {@link SourceRange}.
   */
  private List<Token> getTokens(final SourceRange range) {
    return ExecutionUtils.runObjectIgnore(new RunnableObjectEx<List<Token>>() {
      @Override
      public List<Token> runObject() throws Exception {
        String text = utils.getText(range);
        return TokenUtils.getTokens(text);
      }
    }, ImmutableList.<Token> of());
  }

  /**
   * @return <code>true</code> if there are {@link Token}s in the given {@link SourceRange}.
   */
  private boolean hasTokens(SourceRange range) {
    return !getTokens(range).isEmpty();
//    return ExecutionUtils.runObjectIgnore(new RunnableObjectEx<Boolean>() {
//      @Override
//      public Boolean runObject() throws Exception {
//        String text = utils.getText(start, end - start);
//        StringScanner scanner = new StringScanner(null, text, null);
//        com.google.dart.engine.scanner.Token token = scanner.tokenize();
//        return token.getType() != TokenType.EOF;
//      }
//    }, false);
  }
}
