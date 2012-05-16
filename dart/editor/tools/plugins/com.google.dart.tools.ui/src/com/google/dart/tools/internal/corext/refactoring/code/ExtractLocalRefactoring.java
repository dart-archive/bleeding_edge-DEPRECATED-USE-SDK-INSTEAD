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

import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
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

/**
 * Extract Local Variable (from selected expression inside method or initializer).
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractLocalRefactoring extends Refactoring {

  /**
   * @return {@link DartExpression}s from <code>operands</code> which are completely covered by
   *         given {@link SourceRange}. Range should start and end between given
   *         {@link DartExpression}s.
   */
  private static List<DartExpression> getOperandsForSourceRange(
      List<DartExpression> operands,
      SourceRange range) {
    Assert.isTrue(!operands.isEmpty());
    List<DartExpression> subOperands = Lists.newArrayList();
    // track range enter/exit
    boolean entered = false;
    boolean exited = false;
    // may be range starts exactly on first operand
    if (range.getOffset() == operands.get(0).getSourceInfo().getOffset()) {
      entered = true;
    }
    // iterate over gaps between operands
    for (int i = 0; i < operands.size() - 1; i++) {
      DartExpression operand = operands.get(i);
      DartExpression nextOperand = operands.get(i + 1);
      // add operand, if already entered range
      if (entered) {
        subOperands.add(operand);
        // may be last operand in range
        if (ExtractUtils.rangeEndsBetween(range, operand, nextOperand)) {
          exited = true;
        }
      } else {
        // may be first operand in range
        if (ExtractUtils.rangeStartsBetween(range, operand, nextOperand)) {
          entered = true;
        }
      }
    }
    // check if last operand is in range
    DartExpression lastGroupMember = operands.get(operands.size() - 1);
    if (SourceRangeUtils.getEnd(range) == lastGroupMember.getSourceInfo().getEnd()) {
      subOperands.add(lastGroupMember);
      exited = true;
    }
    // we expect that range covers only given operands
    if (!exited) {
      return Lists.newArrayList();
    }
    // done
    return subOperands;
  }

  /**
   * @return all operands of the given {@link DartBinaryExpression} and its children with the same
   *         operator.
   */
  private static List<DartExpression> getOperandsInOrderFor(final DartBinaryExpression groupRoot) {
    final List<DartExpression> operands = Lists.newArrayList();
    groupRoot.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitExpression(DartExpression node) {
        if (node instanceof DartBinaryExpression
            && ((DartBinaryExpression) node).getOperator() == groupRoot.getOperator()) {
          return super.visitNode(node);
        }
        operands.add(node);
        return null;
      }
    });
    return operands;
  }

  /**
   * @return the {@link SourceRange} which covers given ordered list of operands.
   */
  private static SourceRange getRangeOfOperands(List<DartExpression> operands) {
    DartExpression first = operands.get(0);
    DartExpression last = operands.get(operands.size() - 1);
    int offset = first.getSourceInfo().getOffset();
    int length = last.getSourceInfo().getEnd() - offset;
    return new SourceRangeImpl(offset, length);
  }

  private final CompilationUnit unit;
  private final int selectionLength;

  private final int selectionStart;
  private final SourceRange selectionRange;
  private final CompilationUnitChange change;

  private ExtractUtils utils;

  private DartUnit unitNode;

  private String localName;

  private String[] guessedNames;

  private SelectionAnalyzer selectionAnalyzer;

  private DartExpression rootExpression;

  public ExtractLocalRefactoring(CompilationUnit unit, int selectionStart, int selectionLength) {
    Assert.isTrue(selectionStart >= 0);
    Assert.isTrue(selectionLength >= 0);
    this.unit = unit;
    this.selectionStart = selectionStart;
    this.selectionLength = selectionLength;
    this.selectionRange = new SourceRangeImpl(selectionStart, selectionLength);
    change = new CompilationUnitChange(unit.getElementName(), unit);
    localName = ""; //$NON-NLS-1$
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask(RefactoringCoreMessages.ExtractLocalRefactoring_checking_preconditions, 4);
      RefactoringStatus result = new RefactoringStatus();
      // XXX
      MultiTextEdit rootEdit = new MultiTextEdit();
      change.setEdit(rootEdit);
      change.setKeepPreviewEdits(true);
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
        DartStatement parentStatement = ASTNodes.getParent(rootExpression, DartStatement.class);
        String prefix = utils.getNodePrefix(parentStatement);
        // insert variable declaration
        String eol = utils.getEndOfLine();
        TextEdit edit = new ReplaceEdit(
            parentStatement.getSourceInfo().getOffset(),
            0,
            declarationSource + eol + prefix);
        rootEdit.addChild(edit);
        change.addTextEditGroup(new TextEditGroup(
            RefactoringCoreMessages.ExtractLocalRefactoring_declare_local_variable,
            edit));
      }
      // replace selection with variable reference
      {
        TextEdit edit = new ReplaceEdit(selectionStart, selectionLength, localName);
        rootEdit.addChild(edit);
        change.addTextEditGroup(new TextEditGroup(
            RefactoringCoreMessages.ExtractLocalRefactoring_replace,
            edit));
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
    return new RefactoringStatus();
    // TODO(scheglov)
//    RefactoringStatus status = Checks.checkTempName(newName, fCu);
//    if (Arrays.asList(getExcludedVariableNames()).contains(newName)) {
//      status.addWarning(Messages.format(
//          RefactoringCoreMessages.ExtractTempRefactoring_another_variable,
//          newName));
//    }
//    return status;
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask(RefactoringCoreMessages.ExtractLocalRefactoring_checking_preconditions, 1);
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
      // TODO(scheglov)
//      try {
//        DartExpression expression = getSelectedExpression().getAssociatedExpression();
//        if (expression != null) {
//          ITypeBinding binding = guessBindingForReference(expression);
//          fGuessedTempNames = StubUtility.getVariableNameSuggestions(
//              NamingConventions.VK_LOCAL,
//              fCu.getDartProject(),
//              binding,
//              expression,
//              Arrays.asList(getExcludedVariableNames()));
//        }
//      } catch (DartModelException e) {
//      }
      if (guessedNames == null) {
        guessedNames = new String[0];
      }
    }
    return guessedNames;
  }

  public boolean replaceAllOccurrences() {
    // TODO(scheglov)
    return false;
//    return fReplaceAllOccurrences;
  }

  /**
   * Sets the name for new local variable.
   */
  public void setLocalName(String newName) {
    localName = newName;
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    // TODO(scheglov)
    //fReplaceAllOccurrences = replaceAllOccurrences;
  }

  /**
   * Checks if {@link #selectionRange} selects {@link DartExpression} which can be extracted, and
   * location of this {@link DartExpression} is AST allows extracting.
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
        return new RefactoringStatus();
      }
    }
    // fragment of binary expression selected
    {
      DartNode coveringNode = selectionAnalyzer.getLastCoveringNode();
      if (coveringNode instanceof DartBinaryExpression) {
        DartBinaryExpression binaryExpression = (DartBinaryExpression) coveringNode;
        if (ExtractUtils.isAssociative(binaryExpression)) {
          List<DartExpression> operands = getOperandsInOrderFor(binaryExpression);
          List<DartExpression> subOperands = getOperandsForSourceRange(operands, selectionRange);
          if (!subOperands.isEmpty()) {
            if (!selectionIncludesNonWhitespaceOutsideOperands(subOperands)) {
              rootExpression = binaryExpression;
              return new RefactoringStatus();
            }
          }
        }
      }
    }
    // invalid selection
    return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractLocalRefactoring_select_expression);
  }

  private boolean selectionIncludesNonWhitespaceOutsideOperands(List<DartExpression> operands) {
    return utils.rangeIncludesNonWhitespaceOutsideRange(
        selectionRange,
        getRangeOfOperands(operands));
  }
}
