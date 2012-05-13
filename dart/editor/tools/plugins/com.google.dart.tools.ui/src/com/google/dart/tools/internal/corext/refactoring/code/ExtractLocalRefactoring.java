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

import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
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

/**
 * Extract Local Variable (from selected expression inside method or initializer).
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractLocalRefactoring extends Refactoring {

  private final CompilationUnit unit;
  private final int selectionLength;
  private final int selectionStart;
  private final SourceRange selectionRange;

  private final CompilationUnitChange change;
  private Buffer buffer;
  private DartUnit unitNode;
  private String localName;

  private String[] guessedNames;
  private SelectionAnalyzer selectionAnalyzer;

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
      if (ExtractUtils.isSingleNodeSelected(selectionAnalyzer, selectionRange, unit)) {
        DartNode selectedNode = selectionAnalyzer.getFirstSelectedNode();
        if (selectedNode instanceof DartExpression) {
          DartExpression expression = (DartExpression) selectedNode;
          // prepare expression type
          String typeSource = ExtractUtils.getTypeSource(expression);
          if (typeSource == null || typeSource.equals("Dynamic")) {
            typeSource = "var";
          }
          // prepare variable declaration source
          String initializerSource = buffer.getText(selectionStart, selectionLength);
          String declarationSource = typeSource + " " + localName + " = " + initializerSource + ";";
          // prepare location for declaration
          DartStatement parentStatement = ASTNodes.getParent(selectedNode, DartStatement.class);
          String prefix = ExtractUtils.getNodePrefix(buffer, parentStatement);
          // insert variable declaration
          String eol = getEol();
          TextEdit edit = new ReplaceEdit(
              parentStatement.getSourceInfo().getOffset(),
              0,
              declarationSource + eol + prefix);
          rootEdit.addChild(edit);
          change.addTextEditGroup(new TextEditGroup(
              RefactoringCoreMessages.ExtractLocalRefactoring_declare_local_variable,
              edit));
        }
      }
      // TODO(scheglov) more cases
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
      buffer = unit.getBuffer();
      unitNode = DartCompilerUtilities.resolveUnit(unit);
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

  public void setLocalName(String newName) {
    localName = newName;
  }

  public void setReplaceAllOccurrences(boolean replaceAllOccurrences) {
    // TODO(scheglov)
    //fReplaceAllOccurrences = replaceAllOccurrences;
  }

  private RefactoringStatus checkSelection(IProgressMonitor pm) throws DartModelException {
    Selection selection = Selection.createFromStartLength(
        selectionRange.getOffset(),
        selectionRange.getLength());
    selectionAnalyzer = new SelectionAnalyzer(selection, false);
    unitNode.accept(selectionAnalyzer);
    // TODO(scheglov)
    if (!ExtractUtils.isSingleNodeSelected(selectionAnalyzer, selectionRange, unit)) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractLocalRefactoring_select_expression);
    }
    return new RefactoringStatus();
  }

  private String getEol() {
    // TODO(scheglov) prepare from Buffer and cache
    return ExtractUtils.DEFAULT_END_OF_LINE;
  }
}
