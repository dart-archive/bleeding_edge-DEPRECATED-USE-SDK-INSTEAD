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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.util.apache.ArrayUtils;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.codemanipulation.StubUtility;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.Checks;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.rename.FunctionLocalElement;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameAnalyzeUtil;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.List;
import java.util.Set;

/**
 * Extract Local Variable (from selected expression inside method or initializer).
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractLocalRefactoring extends Refactoring {
  private static final String TOKEN_SEPARATOR = "\uFFFF";

  private final CompilationUnit unit;

  private final int selectionStart;
  private final int selectionLength;
  private final SourceRange selectionRange;
  private final CompilationUnitChange change;
  private ExtractUtils utils;

  private DartUnit unitNode;
  private SelectionAnalyzer selectionAnalyzer;
  private DartExpression rootExpression;
  private DartExpression singleExpression;
  private String localName;

  private String[] guessedNames;
  private Set<String> excludedVariableNames;
  private boolean replaceAllOccurrences;

  public ExtractLocalRefactoring(CompilationUnit unit, int selectionStart, int selectionLength) {
    Assert.isTrue(selectionStart >= 0);
    Assert.isTrue(selectionLength >= 0);
    this.unit = unit;
    this.selectionStart = selectionStart;
    this.selectionLength = selectionLength;
    this.selectionRange = SourceRangeFactory.forStartLength(selectionStart, selectionLength);
    change = new CompilationUnitChange(unit.getElementName(), unit);
    localName = ""; //$NON-NLS-1$
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask(RefactoringCoreMessages.ExtractLocalRefactoring_checking_preconditions, 4);
      RefactoringStatus result = new RefactoringStatus();
      // check variable name
      if (getExcludedVariableNames().contains(localName)) {
        result.addWarning(Messages.format(
            RefactoringCoreMessages.ExtractTempRefactoring_another_variable,
            localName));
      }
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask("", 4); //$NON-NLS-1$
      RefactoringStatus result = new RefactoringStatus();
      // prepare AST
      utils = new ExtractUtils(unit);
      unitNode = utils.getUnitNode();
      pm.worked(1);
      // check selection
      result.merge(checkSelection(new SubProgressMonitor(pm, 3)));
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  public RefactoringStatus checkLocalName(String newName) {
    RefactoringStatus status = Checks.checkVariableName(newName);
    if (getExcludedVariableNames().contains(newName)) {
      status.addWarning(Messages.format(
          RefactoringCoreMessages.ExtractTempRefactoring_another_variable,
          newName));
    }
    return status;
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask(RefactoringCoreMessages.ExtractLocalRefactoring_checking_preconditions, 1);
      // configure Change
      change.setEdit(new MultiTextEdit());
      change.setKeepPreviewEdits(true);
      // prepare occurrences
      List<SourceRange> occurences;
      if (replaceAllOccurrences) {
        occurences = getOccurrences();
      } else {
        occurences = ImmutableList.of(SourceRangeFactory.forStartLength(
            selectionStart,
            selectionLength));
      }
      // add variable declaration
      {
        // prepare expression type
        String typeSource = ExtractUtils.getTypeSource(rootExpression);
        if (typeSource == null || typeSource.equals("Dynamic")) {
          typeSource = "var";
        }
        // prepare variable declaration source
        String initializerSource = utils.getText(selectionStart, selectionLength);
        String declarationSource = typeSource + " " + localName + " = " + initializerSource + ";";
        // prepare location for declaration
        DartStatement targetStatement = findTargetStatement(occurences);
        String prefix = utils.getNodePrefix(targetStatement);
        // insert variable declaration
        String eol = utils.getEndOfLine();
        TextEdit edit = new ReplaceEdit(
            targetStatement.getSourceInfo().getOffset(),
            0,
            declarationSource + eol + prefix);
        change.addEdit(edit);
        change.addTextEditGroup(new TextEditGroup(
            RefactoringCoreMessages.ExtractLocalRefactoring_declare_local_variable,
            edit));
      }
      // replace occurrences with variable reference
      for (SourceRange range : occurences) {
        TextEdit edit = new ReplaceEdit(range.getOffset(), range.getLength(), localName);
        change.addEdit(edit);
        change.addTextEditGroup(new TextEditGroup(
            RefactoringCoreMessages.ExtractLocalRefactoring_replace,
            edit));
      }
      // done
      return change;
    } finally {
      pm.done();
    }
  }

  @Override
  public String getName() {
    return RefactoringCoreMessages.ExtractLocalRefactoring_name;
  }

  /**
   * @return proposed variable names (may be empty, but not null). The first proposal should be used
   *         as "best guess" (if it exists).
   */
  public String[] guessNames() {
    if (guessedNames == null) {
      if (singleExpression != null) {
        guessedNames = StubUtility.getVariableNameSuggestions(
            singleExpression.getType(),
            singleExpression,
            getExcludedVariableNames());
      } else {
        guessedNames = ArrayUtils.EMPTY_STRING_ARRAY;
      }
    }
    return guessedNames;
  }

  public boolean replaceAllOccurrences() {
    return replaceAllOccurrences;
  }

  /**
   * Sets the name for new local variable.
   */
  public void setLocalName(String newName) {
    localName = newName;
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    this.replaceAllOccurrences = replaceAllOccurrences;
  }

  /**
   * Checks if {@link #selectionRange} selects {@link DartExpression} which can be extracted, and
   * location of this {@link DartExpression} in AST allows extracting.
   */
  private RefactoringStatus checkSelection(IProgressMonitor pm) throws DartModelException {
    Selection selection = Selection.createFromStartLength(
        selectionRange.getOffset(),
        selectionRange.getLength());
    selectionAnalyzer = new SelectionAnalyzer(selection, false);
    unitNode.accept(selectionAnalyzer);
    // single node selected
    if (selectionAnalyzer.getSelectedNodes().length == 1
        && !utils.rangeIncludesNonWhitespaceOutsideNode(
            selectionRange,
            selectionAnalyzer.getFirstSelectedNode())) {
      DartNode selectedNode = selectionAnalyzer.getFirstSelectedNode();
      if (selectedNode instanceof DartExpression) {
        rootExpression = (DartExpression) selectedNode;
        singleExpression = rootExpression;
        return new RefactoringStatus();
      }
    }
    // fragment of binary expression selected
    {
      DartNode coveringNode = selectionAnalyzer.getLastCoveringNode();
      if (coveringNode instanceof DartBinaryExpression) {
        DartBinaryExpression binaryExpression = (DartBinaryExpression) coveringNode;
        if (utils.validateBinaryExpressionRange(binaryExpression, selectionRange)) {
          rootExpression = binaryExpression;
          singleExpression = null;
          return new RefactoringStatus();
        }
      }
    }
    // invalid selection
    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractLocalRefactoring_select_expression);
  }

  /**
   * @return the {@link DartNode}s at given {@link SourceRange}s.
   */
  private List<DartNode> findNodes(List<SourceRange> ranges) {
    List<DartNode> nodes = Lists.newArrayList();
    for (SourceRange range : ranges) {
      DartNode node = NodeFinder.perform(unitNode, range.getOffset(), 0);
      nodes.add(node);
    }
    return nodes;
  }

  private DartStatement findTargetStatement(List<SourceRange> occurences) {
    List<DartNode> nodes = findNodes(occurences);
    List<DartNode> firstParents = ASTNodes.getParents(nodes.get(0));
    List<DartNode> commonPath = ASTNodes.findDeepestCommonPath(nodes);
    DartNode commonParent = firstParents.get(commonPath.size() - 1);
    if (commonParent instanceof DartBlock) {
      return (DartStatement) firstParents.get(commonPath.size());
    } else {
      return ASTNodes.getAncestor(commonParent, DartStatement.class);
    }
  }

  private Set<String> getExcludedVariableNames() {
    if (excludedVariableNames == null) {
      excludedVariableNames = Sets.newHashSet();
      ExecutionUtils.runIgnore(new RunnableEx() {
        @Override
        public void run() throws Exception {
          DartElement element = unit.getElementAt(selectionStart);
          DartNode enclosingNode = NodeFinder.perform(unitNode, selectionStart, 0);
          DartBlock enclosingBlock = ASTNodes.getParent(enclosingNode, DartBlock.class);
          if (element != null && enclosingBlock != null) {
            SourceRange newVariableVisibleRange = SourceRangeFactory.forStartEnd(
                selectionStart,
                enclosingBlock.getSourceInfo().getEnd());
            DartFunction enclosingFunction = element.getAncestor(DartFunction.class);
            if (enclosingFunction != null) {
              List<FunctionLocalElement> localElements = RenameAnalyzeUtil.getFunctionLocalElements(enclosingFunction);
              for (FunctionLocalElement variable : localElements) {
                if (SourceRangeUtils.intersects(newVariableVisibleRange, variable.getVisibleRange())) {
                  excludedVariableNames.add(variable.getElementName());
                }
              }
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
  private List<SourceRange> getOccurrences() throws DartModelException {
    List<SourceRange> occurrences = Lists.newArrayList();
    // prepare selection
    String selectionSource = utils.getText(selectionStart, selectionLength);
    List<com.google.dart.engine.scanner.Token> selectionTokens = TokenUtils.getTokens(selectionSource);
    selectionSource = StringUtils.join(selectionTokens, TOKEN_SEPARATOR);
    // prepare enclosing function
    com.google.dart.compiler.ast.DartFunction function;
    {
      DartNode selectionNode = NodeFinder.perform(unitNode, selectionStart, 0);
      function = ASTNodes.getAncestor(
          selectionNode,
          com.google.dart.compiler.ast.DartFunction.class);
    }
    // ...we need function
    if (function != null) {
      SourceInfo functionSourceInfo = function.getBody().getSourceInfo();
      int functionOffset = functionSourceInfo.getOffset();
      String functionSource = utils.getText(functionOffset, functionSourceInfo.getLength());
      // prepare function tokens
      List<com.google.dart.engine.scanner.Token> functionTokens = TokenUtils.getTokens(functionSource);
      functionSource = StringUtils.join(functionTokens, TOKEN_SEPARATOR);
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
        SourceRange occuRange = SourceRangeFactory.forStartEnd(occuStart, occuEnd);
        if (SourceRangeUtils.intersects(occuRange, selectionRange)) {
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
