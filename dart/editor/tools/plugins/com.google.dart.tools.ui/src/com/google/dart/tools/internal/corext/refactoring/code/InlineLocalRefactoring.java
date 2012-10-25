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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import java.util.List;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public class InlineLocalRefactoring extends Refactoring {

  private static ReplaceEdit createReplaceEdit(SourceRange range, String text) {
    return new ReplaceEdit(range.getOffset(), range.getLength(), text);
  }

  private CompilationUnit unit;
  private int selectionStart;
  private int selectionLength;

  private DartUnit unitNode;
  private DartVariable variableNode;
  private VariableElement variableElement;
  private List<DartIdentifier> references;

  public InlineLocalRefactoring(CompilationUnit unit, int selectionStart, int selectionLength) {
    Assert.isTrue(selectionStart >= 0);
    Assert.isTrue(selectionLength >= 0);
    this.selectionStart = selectionStart;
    this.selectionLength = selectionLength;
    this.unit = unit;
    initAST();
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask("", 1); //$NON-NLS-1$
      return new RefactoringStatus();
    } finally {
      pm.done();
    }
  }

  public RefactoringStatus checkIfTempSelected() {
    if (variableNode == null) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineLocalRefactoring_select_temp);
    }
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask("", 1); //$NON-NLS-1$
      RefactoringStatus result = new RefactoringStatus();
      result.merge(checkSelection());
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException {
    try {
      pm.beginTask(RefactoringCoreMessages.InlineLocalRefactoring_preview, 2);
      // prepare Change
      CompilationUnitChange change = new CompilationUnitChange(unit.getElementName(), unit);
      change.setEdit(new MultiTextEdit());
      change.setKeepPreviewEdits(true);
      // remove declaration
      {
        DartStatement declarationStatement = ASTNodes.getParent(variableNode, DartStatement.class);
        DartBlock block = (DartBlock) declarationStatement.getParent();
        List<DartStatement> statements = block.getStatements();
        int declarationIndex = statements.indexOf(declarationStatement);
        // remove declaration - to the next statement or end of block
        if (declarationIndex + 1 < statements.size()) {
          DartStatement nextStatement = statements.get(declarationIndex + 1);
          change.addEdit(createReplaceEdit(
              SourceRangeFactory.forStartStart(declarationStatement, nextStatement),
              ""));
        } else {
          // start = start of the declaration statement line
          int start = declarationStatement.getSourceInfo().getOffset();
          while (true) {
            char c = unit.getSource().charAt(start - 1);
            if (c == ' ' || c == '\t') {
              start--;
              continue;
            }
            break;
          }
          // end = position before closing "}"
          int end = block.getSourceInfo().getEnd() - 1;
          change.addEdit(createReplaceEdit(SourceRangeFactory.forStartEnd(start, end), ""));
        }
      }
      // prepare source
      String initializerSource;
      {
        SourceInfo sourceInfo = variableNode.getValue().getSourceInfo();
        initializerSource = unit.getSource().substring(sourceInfo.getOffset(), sourceInfo.getEnd());
      }
      // replace references
      for (DartIdentifier reference : getReferences()) {
        SourceRange range = SourceRangeFactory.create(reference);
        String sourceForReference = getSourceForReference(range, initializerSource);
        change.addEdit(createReplaceEdit(range, sourceForReference));
      }
      return change;
    } finally {
      pm.done();
    }
  }

  @Override
  public String getName() {
    return RefactoringCoreMessages.InlineLocalRefactoring_name;
  }

  public List<DartIdentifier> getReferences() {
    if (references == null) {
      references = Lists.newArrayList();
      unitNode.accept(new ASTVisitor<Void>() {
        @Override
        public Void visitIdentifier(DartIdentifier node) {
          if (Objects.equal(node.getElement(), variableElement)
              && PropertyDescriptorHelper.getLocationInParent(node) != PropertyDescriptorHelper.DART_VARIABLE_NAME) {
            references.add(node);
          }
          return null;
        }
      });
    }
    return references;
  }

  public VariableElement getVariableElement() {
    return variableElement;
  }

  private RefactoringStatus checkSelection() {
    RefactoringStatus selectionStatus = checkIfTempSelected();
    if (!selectionStatus.isOK()) {
      return selectionStatus;
    }
    // should be normal variable declaration statement
    if (!(variableNode.getParent() instanceof DartVariableStatement)
        || !(variableNode.getParent().getParent() instanceof DartBlock)) {
      return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineLocalRefactoring_declaration_inStatement);
    }
    // should have value at declaration
    if (variableNode.getValue() == null) {
      String message = Messages.format(
          RefactoringCoreMessages.InlineLocalRefactoring_not_initialized,
          variableElement.getName());
      return RefactoringStatus.createFatalErrorStatus(message);
    }
    // should not have assignments
    for (DartIdentifier reference : getReferences()) {
      if (ASTNodes.inSetterContext(reference)) {
        String message = Messages.format(
            RefactoringCoreMessages.InlineLocalRefactoring_assigned_more_once,
            variableElement.getName());
        return RefactoringStatus.createFatalErrorStatus(message);
      }
    }
    // OK
    return new RefactoringStatus();
  }

  /**
   * @return the source which should be used to replace reference with given {@link SourceRange}.
   */
  private String getSourceForReference(SourceRange range, String source) throws DartModelException {
    if (isIdentifierInStringInterpolation(range.getOffset())) {
      return "{" + source + "}";
    } else {
      return source;
    }
  }

  private void initAST() {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        unitNode = DartCompilerUtilities.resolveUnit(unit);
        // prepare some reference to variable
        DartNode referenceName;
        {
          SourceRange range = SourceRangeFactory.forStartLength(selectionStart, selectionLength);
          referenceName = NodeFinder.perform(unitNode, range);
          if (referenceName == null) {
            return;
          }
        }
        // prepare variable Element
        if (referenceName.getElement() instanceof VariableElement) {
          variableElement = (VariableElement) referenceName.getElement();
        }
        // prepare variable declaration node
        if (variableElement != null) {
          SourceInfo nameLocation = variableElement.getNameLocation();
          SourceRange range = SourceRangeFactory.forStartLength(
              nameLocation.getOffset(),
              nameLocation.getLength());
          DartNode nameInDeclaration = NodeFinder.perform(unitNode, range);
          if (nameInDeclaration != null && nameInDeclaration.getParent() instanceof DartVariable) {
            variableNode = (DartVariable) nameInDeclaration.getParent();
          }
        }
      }
    });
  }

  /**
   * @return <code>true</code> if given offset of the reference is identifier in
   *         {@link DartStringInterpolation}, so we should wrap inlined expression with
   *         <code>${}</code>.
   */
  private boolean isIdentifierInStringInterpolation(int offset) throws DartModelException {
    DartNode node = NodeFinder.find(unitNode, offset, 0).getCoveringNode();
    if (node instanceof DartIdentifier) {
      DartNode parent = node.getParent();
      if (parent instanceof DartStringInterpolation) {
        return ((DartStringInterpolation) parent).getExpressions().contains(node)
            && unit.getSource().substring(offset - 1, offset).equals("$");
      }
    }
    return false;
  }
}
