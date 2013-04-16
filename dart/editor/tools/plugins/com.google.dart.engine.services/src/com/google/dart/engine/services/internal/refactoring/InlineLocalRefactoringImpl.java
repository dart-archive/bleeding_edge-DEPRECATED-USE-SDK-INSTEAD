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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.InlineLocalRefactoring;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;

import java.text.MessageFormat;
import java.util.List;

/**
 * Implementation of {@link InlineLocalRefactoring}.
 */
public class InlineLocalRefactoringImpl extends RefactoringImpl implements InlineLocalRefactoring {
  private final AssistContext context;
  private final CompilationUnit unitNode;
  private final CorrectionUtils utils;

  private VariableDeclaration variableNode;
  private LocalVariableElement variableElement;
  private List<SearchMatch> references;

  public InlineLocalRefactoringImpl(AssistContext context) throws Exception {
    this.context = context;
    this.unitNode = context.getCompilationUnit();
    this.utils = new CorrectionUtils(unitNode);
  }

  @Override
  public RefactoringStatus checkFinalConditions(ProgressMonitor pm) throws Exception {
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    pm.beginTask("Checking initial conditions", 5);
    try {
      RefactoringStatus result = new RefactoringStatus();
      // prepare variable
      variableElement = null;
      {
        ASTNode coveringNode = context.getCoveringNode();
        if (coveringNode instanceof SimpleIdentifier) {
          SimpleIdentifier coveringIdentifier = (SimpleIdentifier) coveringNode;
          Element element = coveringIdentifier.getElement();
          if (element instanceof LocalVariableElement) {
            variableElement = (LocalVariableElement) element;
            variableNode = utils.findNode(
                variableElement.getNameOffset(),
                VariableDeclaration.class);
          }
        }
      }
      if (variableNode == null) {
        return RefactoringStatus.createFatalErrorStatus("Local variable declaration or reference must be selected to activate this refactoring.");
      }
      pm.worked(1);
      // should be normal variable declaration statement
      if (!(variableNode.getParent() instanceof VariableDeclarationList)
          || !(variableNode.getParent().getParent() instanceof VariableDeclarationStatement)
          || !(variableNode.getParent().getParent().getParent() instanceof Block)) {
        return RefactoringStatus.createFatalErrorStatus("Local variable declared in statement should be selected to activate this refactoring.");
      }
      pm.worked(1);
      // should have initializer at declaration
      if (variableNode.getInitializer() == null) {
        String message = MessageFormat.format(
            "Local variable ''{0}'' is not initialized at declaration.",
            variableElement.getName());
        return RefactoringStatus.createFatalErrorStatus(
            message,
            RefactoringStatusContext.create(variableNode));
      }
      pm.worked(1);
      // should not have assignments
      references = context.getSearchEngine().searchReferences(variableElement, null, null);
      for (SearchMatch reference : references) {
        if (reference.getKind() != MatchKind.VARIABLE_READ) {
          String message = MessageFormat.format(
              "Local variable ''{0}'' is assigned more than once.",
              variableElement.getName());
          return RefactoringStatus.createFatalErrorStatus(
              message,
              RefactoringStatusContext.create(reference));
        }
      }
      // done
      return result;
    } finally {
      pm.done();
    }
  }

  @Override
  public Change createChange(ProgressMonitor pm) throws Exception {
    pm = checkProgressMonitor(pm);
    SourceChange change = new SourceChange(getRefactoringName(), context.getSource());
    // remove declaration
    {
      Statement declarationStatement = variableNode.getAncestor(VariableDeclarationStatement.class);
      Block block = (Block) declarationStatement.getParent();
      List<Statement> statements = block.getStatements();
      int declarationIndex = statements.indexOf(declarationStatement);
      // remove declaration - to the next statement or end of block
      if (declarationIndex + 1 < statements.size()) {
        Statement nextStatement = statements.get(declarationIndex + 1);
        change.addEdit(new Edit(rangeStartStart(declarationStatement, nextStatement), ""));
      } else {
        // start = start of the declaration statement line
        int start = declarationStatement.getOffset();
        while (true) {
          char c = utils.getText().charAt(start - 1);
          if (c == ' ' || c == '\t') {
            start--;
            continue;
          }
          break;
        }
        // end = position before closing "}"
        int end = block.getEnd() - 1;
        change.addEdit(new Edit(rangeStartEnd(start, end), ""));
      }
    }
    // prepare source
    String initializerSource;
    {
      Expression initializer = variableNode.getInitializer();
      initializerSource = utils.getText(initializer);
    }
    // replace references
    for (SearchMatch reference : references) {
      SourceRange range = reference.getSourceRange();
      String sourceForReference = getSourceForReference(range, initializerSource);
      change.addEdit(new Edit(range, sourceForReference));
    }
    return change;
  }

  @Override
  public String getRefactoringName() {
    return "Inline Local Variable";
  }

  @Override
  public int getReferenceCount() {
    return references.size();
  }

  @Override
  public String getVariableName() {
    return variableElement.getName();
  }

  /**
   * @return the source which should be used to replace reference with given {@link SourceRange}.
   */
  private String getSourceForReference(SourceRange range, String source) {
    if (isIdentifierInStringInterpolation(range.getOffset())) {
      return "{" + source + "}";
    } else {
      return source;
    }
  }

  /**
   * @return <code>true</code> if given offset of the reference has form <code>$name</code>.
   */
  private boolean isIdentifierInStringInterpolation(int offset) {
    ASTNode node = utils.findNode(offset, ASTNode.class);
    ASTNode parent = node.getParent();
    if (parent instanceof InterpolationExpression) {
      InterpolationExpression element = (InterpolationExpression) parent;
      return element.getBeginToken().getType() == TokenType.STRING_INTERPOLATION_IDENTIFIER;
    }
    return false;
  }
}
