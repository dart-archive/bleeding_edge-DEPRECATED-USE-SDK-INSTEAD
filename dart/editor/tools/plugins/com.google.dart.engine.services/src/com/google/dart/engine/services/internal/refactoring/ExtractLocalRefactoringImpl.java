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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.internal.correction.SelectionAnalyzer;
import com.google.dart.engine.services.internal.util.ExecutionUtils;
import com.google.dart.engine.services.internal.util.RunnableEx;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.services.refactoring.ExtractLocalRefactoring;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link ExtractLocalRefactoring}.
 */
public class ExtractLocalRefactoringImpl extends RefactoringImpl implements ExtractLocalRefactoring {
  private static final String TOKEN_SEPARATOR = "\uFFFF";

  private final AssistContext context;
  private final SourceRange selectionRange;
  private final int selectionStart;

  private final CompilationUnit unitNode;
  private final CorrectionUtils utils;
  private SelectionAnalyzer selectionAnalyzer;
  private Expression rootExpression;
  private Expression singleExpression;
  private String stringLiteralPart;

  private String localName;
  private boolean replaceAllOccurrences = true;

  private Set<String> excludedVariableNames;
  private String[] guessedNames;

  public ExtractLocalRefactoringImpl(AssistContext context) throws Exception {
    this.context = context;
    this.selectionRange = context.getSelectionRange();
    this.selectionStart = selectionRange.getOffset();
    this.unitNode = context.getCompilationUnit();
    this.utils = new CorrectionUtils(unitNode);
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking final conditions", 1);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // name
      result.merge(NamingConventions.validateVariableName(localName));
      if (getExcludedVariableNames().contains(localName)) {
        result.addWarning(MessageFormat.format(
            "A variable with name ''{0}'' is already defined in the visible scope.",
            localName));
      }
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking initial conditions", 2);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // selection
      result.merge(checkSelection());
      pm.worked(1);
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkLocalName(String newName) {
    return NamingConventions.validateVariableName(newName);
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    SourceChange change = new SourceChange(getRefactoringName(), context.getSource());
    // prepare occurrences
    List<SourceRange> occurrences;
    if (replaceAllOccurrences) {
      occurrences = getOccurrences();
    } else {
      occurrences = ImmutableList.of(selectionRange);
    }
    // add variable declaration
    {
      String declarationSource;
      if (stringLiteralPart != null) {
        declarationSource = "var " + localName + " = " + "'" + stringLiteralPart + "';";
      } else {
        String initializerSource = utils.getText(selectionRange);
        declarationSource = "var " + localName + " = " + initializerSource + ";";
      }
      // prepare location for declaration
      Statement targetStatement = findTargetStatement(occurrences);
      String prefix = utils.getNodePrefix(targetStatement);
      // insert variable declaration
      String eol = utils.getEndOfLine();
      Edit edit = new Edit(targetStatement.getOffset(), 0, declarationSource + eol + prefix);
      change.addEdit("Add variable declaration", edit);
    }
    // prepare replacement
    String occurrenceReplacement = localName;
    if (stringLiteralPart != null) {
      occurrenceReplacement = "${" + localName + "}";
    }
    // replace occurrences with variable reference
    for (SourceRange range : occurrences) {
      Edit edit = new Edit(range, occurrenceReplacement);
      change.addEdit("Replace expression with variable reference", edit);
    }
    return change;
  }

  @Override
  public String getRefactoringName() {
    return "Extract Local Variable";
  }

  @Override
  public String[] guessNames() {
    if (guessedNames == null) {
      Set<String> excluded = getExcludedVariableNames();
      if (stringLiteralPart != null) {
        return CorrectionUtils.getVariableNameSuggestions(stringLiteralPart, excluded);
      } else if (singleExpression != null) {
        guessedNames = CorrectionUtils.getVariableNameSuggestions(
            singleExpression.getStaticType(),
            singleExpression,
            excluded);
      } else {
        guessedNames = ArrayUtils.EMPTY_STRING_ARRAY;
      }
    }
    return guessedNames;
  }

  @Override
  public boolean replaceAllOccurrences() {
    return replaceAllOccurrences;
  }

  @Override
  public void setLocalName(String localName) {
    this.localName = localName;
  }

  @Override
  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    this.replaceAllOccurrences = replaceAllOccurrences;
  }

  /**
   * Checks if {@link #selectionRange} selects {@link Expression} which can be extracted, and
   * location of this {@link DartExpression} in AST allows extracting.
   */
  private RefactoringStatus checkSelection() {
    selectionAnalyzer = new SelectionAnalyzer(selectionRange);
    unitNode.accept(selectionAnalyzer);
    ASTNode coveringNode = selectionAnalyzer.getCoveringNode();
    // we need enclosing block to add variable declaration statement
    if (coveringNode == null || coveringNode.getAncestor(Block.class) == null) {
      return RefactoringStatus.createFatalErrorStatus("Expression inside of function must be selected to activate this refactoring.");
    }
    // part of string literal
    if (coveringNode instanceof StringLiteral) {
      stringLiteralPart = utils.getText(selectionRange);
      return new RefactoringStatus();
    }
    // single node selected
    if (selectionAnalyzer.getSelectedNodes().size() == 1
        && !utils.selectionIncludesNonWhitespaceOutsideNode(
            selectionRange,
            selectionAnalyzer.getFirstSelectedNode())) {
      ASTNode selectedNode = selectionAnalyzer.getFirstSelectedNode();
      if (selectedNode instanceof Expression) {
        rootExpression = (Expression) selectedNode;
        singleExpression = rootExpression;
        return new RefactoringStatus();
      }
    }
    // fragment of binary expression selected
    if (coveringNode instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) coveringNode;
      if (utils.validateBinaryExpressionRange(binaryExpression, selectionRange)) {
        rootExpression = binaryExpression;
        singleExpression = null;
        return new RefactoringStatus();
      }
    }
    // invalid selection
    return RefactoringStatus.createFatalErrorStatus("Expression must be selected to activate this refactoring.");
  }

  /**
   * @return the {@link ASTNode}s at given {@link SourceRange}s.
   */
  private List<ASTNode> findNodes(List<SourceRange> ranges) {
    List<ASTNode> nodes = Lists.newArrayList();
    for (SourceRange range : ranges) {
      ASTNode node = new NodeLocator(range.getOffset()).searchWithin(unitNode);
      nodes.add(node);
    }
    return nodes;
  }

  /**
   * @return the {@link Statement} such that variable declaration added before it will be visible in
   *         all given occurrences.
   */
  private Statement findTargetStatement(List<SourceRange> occurrences) {
    List<ASTNode> nodes = findNodes(occurrences);
    List<ASTNode> firstParents = CorrectionUtils.getParents(nodes.get(0));
    ASTNode commonParent = CorrectionUtils.getNearestCommonAncestor(nodes);
    if (commonParent instanceof Block) {
      int commonIndex = firstParents.indexOf(commonParent);
      return (Statement) firstParents.get(commonIndex + 1);
    } else {
      return commonParent.getAncestor(Statement.class);
    }
  }

  /**
   * @return the {@link Set} of local names that are visible at the place where "localName" will be
   *         used, "localName" should not be one of them.
   */
  private Set<String> getExcludedVariableNames() {
    if (excludedVariableNames == null) {
      excludedVariableNames = Sets.newHashSet();
      ExecutionUtils.runIgnore(new RunnableEx() {
        @Override
        public void run() throws Exception {
          ASTNode enclosingNode = new NodeLocator(selectionStart).searchWithin(unitNode);
          Block enclosingBlock = enclosingNode.getAncestor(Block.class);
          if (enclosingBlock != null) {
            final SourceRange newVariableVisibleRange = rangeStartEnd(
                selectionRange,
                enclosingBlock.getEnd());
            ExecutableElement enclosingExecutable = CorrectionUtils.getEnclosingExecutableElement(enclosingNode);
            if (enclosingExecutable != null) {
              enclosingExecutable.accept(new GeneralizingElementVisitor<Void>() {
                @Override
                public Void visitLocalElement(LocalElement element) {
                  SourceRange elementRange = element.getVisibleRange();
                  if (elementRange != null && elementRange.intersects(newVariableVisibleRange)) {
                    excludedVariableNames.add(element.getName());
                  }
                  return super.visitLocalElement(element);
                }
              });
            }
          }
        }
      });
    }
    return excludedVariableNames;
  }

  /**
   * @return all occurrences of the source which matches given selection, sorted by offset. First
   *         {@link SourceRange} is same as the given selection. May be empty, but not
   *         <code>null</code>.
   */
  private List<SourceRange> getOccurrences() {
    List<SourceRange> occurrences = Lists.newArrayList();
    // prepare selection
    String selectionSource = utils.getText(selectionRange);
    List<com.google.dart.engine.scanner.Token> selectionTokens = TokenUtils.getTokens(selectionSource);
    selectionSource = StringUtils.join(selectionTokens, TOKEN_SEPARATOR);
    // prepare enclosing function
    ASTNode enclosingFunction;
    {
      ASTNode selectionNode = new NodeLocator(selectionStart).searchWithin(unitNode);
      enclosingFunction = CorrectionUtils.getEnclosingExecutableNode(selectionNode);
    }
    // ...we need function
    if (enclosingFunction != null) {
      int functionOffset = enclosingFunction.getOffset();
      String functionSource = utils.getText(functionOffset, enclosingFunction.getLength());
      // prepare function tokens
      List<com.google.dart.engine.scanner.Token> functionTokens = TokenUtils.getTokens(functionSource);
      functionSource = StringUtils.join(functionTokens, TOKEN_SEPARATOR);
      // string part occurrences
      if (stringLiteralPart != null) {
        int occuLength = stringLiteralPart.length();
        for (com.google.dart.engine.scanner.Token token : functionTokens) {
          if (token.getType() == TokenType.STRING) {
            String tokenValue = token.getLexeme();
            int lastIndex = 0;
            while (true) {
              int index = tokenValue.indexOf(stringLiteralPart, lastIndex);
              if (index == -1) {
                break;
              }
              lastIndex = index + occuLength;
              int occuStart = functionOffset + token.getOffset() + index;
              SourceRange occuRange = rangeStartLength(occuStart, occuLength);
              occurrences.add(occuRange);
            }
          }
        }
        return occurrences;
      }
      // find "selection" in "function" tokens
      int lastIndex = 0;
      while (true) {
        // find next occurrence
        int index = functionSource.indexOf(selectionSource, lastIndex);
        if (index == -1) {
          break;
        }
        lastIndex = index + selectionSource.length();
        // find start/end tokens
        int startTokenIndex = StringUtils.countMatches(
            functionSource.substring(0, index),
            TOKEN_SEPARATOR);
        int endTokenIndex = StringUtils.countMatches(
            functionSource.substring(0, lastIndex),
            TOKEN_SEPARATOR);
        com.google.dart.engine.scanner.Token startToken = functionTokens.get(startTokenIndex);
        com.google.dart.engine.scanner.Token endToken = functionTokens.get(endTokenIndex);
        // add occurrence range
        int occuStart = functionOffset + startToken.getOffset();
        int occuEnd = functionOffset + endToken.getOffset() + endToken.getLength();
        SourceRange occuRange = rangeStartEnd(occuStart, occuEnd);
        if (occuRange.intersects(selectionRange)) {
          occurrences.add(selectionRange);
        } else {
          occurrences.add(occuRange);
        }
      }
    }
    // done
    return occurrences;
  }
}
