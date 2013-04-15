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
package com.google.dart.tools.ui.internal.text.correction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartReturnBlock;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.common.HasSourceInfo;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.parser.Token;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.RefactoringAvailabilityTester;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.code.StatementAnalyzer;
import com.google.dart.tools.internal.corext.refactoring.code.TokenUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertGetterToMethodRefactoringProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertMethodToGetterRefactoringProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertOptionalParametersToNamedRefactoringProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.RenameRefactoringProposal_OLD;
import com.google.dart.tools.ui.internal.text.correction.proposals.LinkedCorrectionProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.SourceBuilder;
import com.google.dart.tools.ui.internal.text.correction.proposals.TrackedNodeProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.TrackedPositions;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IInvocationContext;
import com.google.dart.tools.ui.text.dart.IProblemLocation;
import com.google.dart.tools.ui.text.dart.IQuickAssistProcessor;

import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_BINARY_EXPRESSION_LEFT_OPERAND;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_BINARY_EXPRESSION_RIGHT_OPERAND;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_RETURN_STATEMENT_VALUE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_VARIABLE_NAME;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_VARIABLE_VALUE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.getLocationInParent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Standard {@link IQuickAssistProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickAssistProcessor_OLD implements IQuickAssistProcessor {

  private static final int DEFAULT_RELEVANCE = 30;

  private static ReplaceEdit createInsertEdit(int offset, String text) {
    return new ReplaceEdit(offset, 0, text);
  }

  private static ReplaceEdit createRemoveEdit(SourceRange range) {
    return createReplaceEdit(range, "");
  }

  private static ReplaceEdit createReplaceEdit(SourceRange range, String text) {
    return new ReplaceEdit(range.getOffset(), range.getLength(), text);
  }

  private static int isOperatorSelected(DartBinaryExpression binaryExpression, int offset,
      int length) {
    DartNode left = binaryExpression.getArg1();
    DartNode right = binaryExpression.getArg2();
    if (isSelectingOperator(left, right, offset, length)) {
      return left.getSourceInfo().getEnd();
    }
    return -1;
  }

  private static boolean isSelectingOperator(DartNode n1, DartNode n2, int offset, int length) {
    // between the nodes
    if (offset >= n1.getSourceInfo().getEnd() && offset + length <= n2.getSourceInfo().getOffset()) {
      return true;
    }
    // or exactly select the node (but not with infix expressions)
    if (offset == n1.getSourceInfo().getOffset() && offset + length == n2.getSourceInfo().getEnd()) {
      if (n1 instanceof DartBinaryExpression || n2 instanceof DartBinaryExpression) {
        return false;
      }
      return true;
    }
    // invalid selection (part of node, etc)
    return false;
  }

  private static boolean noErrorsAtLocation(IProblemLocation[] locations) {
    if (locations != null) {
      for (IProblemLocation location : locations) {
        if (location.isError()) {
          return false;
        }
      }
    }
    return true;
  }

  private CompilationUnit unit;
  private DartUnit unitNode;
  private int selectionOffset;
  private int selectionLength;
  private ExtractUtils utils;
  private DartNode node;
  private IInvocationContext context;
  private com.google.dart.tools.core.model.DartFunction selectionFunction;
  private final List<ICommandAccess> proposals = Lists.newArrayList();
  private final List<TextEdit> textEdits = Lists.newArrayList();
  private int proposalRelevance = DEFAULT_RELEVANCE;
  private final Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
  private final Map<SourceRange, TextEdit> positionStopEdits = Maps.newHashMap();
  private final Map<String, List<TrackedNodeProposal>> linkedPositionProposals = Maps.newHashMap();
  private SourceRange proposalEndRange = null;
  private LinkedCorrectionProposal proposal;

  @Override
  public synchronized IDartCompletionProposal[] getAssists(IInvocationContext context,
      IProblemLocation[] locations) throws CoreException {
    if (context.getContext() != null) {
      return new IDartCompletionProposal[0];
    }
    this.context = context;
    proposals.clear();
    unit = context.getOldCompilationUnit();
    unitNode = context.getOldASTRoot();
    selectionOffset = context.getSelectionOffset();
    selectionLength = context.getSelectionLength();
    selectionFunction = DartModelUtil.findFunction(unit, selectionOffset);
    node = context.getOldCoveringNode();
    if (node != null) {
      utils = new ExtractUtils(unit, (DartUnit) node.getRoot());
      if (node != null) {
        boolean noErrorsAtLocation = noErrorsAtLocation(locations);
        if (noErrorsAtLocation) {
          // invoke each "addProposal_" method
          for (final Method method : QuickAssistProcessor_OLD.class.getDeclaredMethods()) {
            if (method.getName().startsWith("addProposal_")) {
              ExecutionUtils.runIgnore(new RunnableEx() {
                @Override
                public void run() throws Exception {
                  method.invoke(QuickAssistProcessor_OLD.this);
                }
              });
              resetProposalElements();
            }
          }
        }
      }
    }
    return proposals.toArray(new IDartCompletionProposal[proposals.size()]);
  }

  @Override
  public boolean hasAssists(IInvocationContext context) throws CoreException {
    return context.getContext() == null;
  }

  void addProposal_addTypeAnnotation() throws Exception {
    Type type = null;
    HasSourceInfo declarationStart = null;
    HasSourceInfo nameStart = null;
    // try local variable
    {
      DartVariableStatement statement = ASTNodes.getAncestor(node, DartVariableStatement.class);
      if (statement != null && statement.getTypeNode() == null) {
        List<DartVariable> variables = statement.getVariables();
        if (variables.size() == 1) {
          DartVariable variable = variables.get(0);
          type = variable.getElement().getType();
          declarationStart = statement;
          nameStart = variable;
          // language style guide recommends to use "var" for locals, so deprioritize
          proposalRelevance -= 1;
        }
      }
    }
    // try field
    {
      DartFieldDefinition fieldDefinition = ASTNodes.getAncestor(node, DartFieldDefinition.class);
      if (fieldDefinition != null && fieldDefinition.getTypeNode() == null) {
        List<DartField> fields = fieldDefinition.getFields();
        if (fields.size() == 1) {
          DartField field = fields.get(0);
          DartExpression value = field.getValue();
          if (value != null) {
            type = value.getType();
            declarationStart = fieldDefinition;
            nameStart = field;
          }
        }
      }
    }
    // check type
    if (type == null || TypeKind.of(type) == TypeKind.DYNAMIC) {
      return;
    }
    // add edit
    if (declarationStart != null && nameStart != null) {
      String typeSource = ExtractUtils.getTypeSource(type);
      // find "var" token
      KeywordToken varToken;
      {
        SourceRange modifiersRange = SourceRangeFactory.forStartEnd(declarationStart, nameStart);
        String modifiersSource = utils.getText(modifiersRange);
        List<com.google.dart.engine.scanner.Token> tokens = TokenUtils.getTokens(modifiersSource);
        varToken = TokenUtils.findKeywordToken(tokens, Keyword.VAR);
      }
      // replace "var", or insert type before name
      if (varToken != null) {
        SourceRange range = SourceRangeFactory.forToken(varToken);
        range = SourceRangeFactory.withBase(declarationStart, range);
        addReplaceEdit(range, typeSource);
      } else {
        SourceRange range = SourceRangeFactory.forStartLength(nameStart, 0);
        addReplaceEdit(range, typeSource + " ");
      }
    }
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_addTypeAnnotation,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_convertGetterToMethodRefactoring() throws CoreException {
    if (!RefactoringAvailabilityTester.isConvertGetterToMethodAvailable(selectionFunction)) {
      return;
    }
    // we need DartEditor
    if (context instanceof AssistContext) {
      IEditorPart editor = ((AssistContext) context).getEditor();
      if (editor instanceof DartEditor) {
        DartEditor dartEditor = (DartEditor) editor;
        // add proposal
        ICommandAccess proposal = new ConvertGetterToMethodRefactoringProposal(
            dartEditor,
            selectionFunction,
            proposalRelevance);
        proposals.add(proposal);
      }
    }
  }

  void addProposal_convertMethodToGetterRefactoring() throws CoreException {
    if (!RefactoringAvailabilityTester.isConvertMethodToGetterAvailable(selectionFunction)) {
      return;
    }
    // we need DartEditor
    if (context instanceof AssistContext) {
      IEditorPart editor = ((AssistContext) context).getEditor();
      if (editor instanceof DartEditor) {
        DartEditor dartEditor = (DartEditor) editor;
        // add proposal
        ICommandAccess proposal = new ConvertMethodToGetterRefactoringProposal(
            dartEditor,
            selectionFunction,
            proposalRelevance);
        proposals.add(proposal);
      }
    }
  }

  void addProposal_ConvertOptionalParametersToNamedRefactoring() throws CoreException {
    if (!RefactoringAvailabilityTester.isConvertOptionalParametersToNamedAvailable(selectionFunction)) {
      return;
    }
    // we need DartEditor
    if (context instanceof AssistContext) {
      IEditorPart editor = ((AssistContext) context).getEditor();
      if (editor instanceof DartEditor) {
        DartEditor dartEditor = (DartEditor) editor;
        // add proposal
        ICommandAccess proposal = new ConvertOptionalParametersToNamedRefactoringProposal(
            dartEditor,
            selectionFunction,
            proposalRelevance);
        proposals.add(proposal);
      }
    }
  }

  void addProposal_convertToBlockFunctionBody() throws Exception {
    // prepare enclosing function
    DartFunction function = getEnclosingFunctionOrMethodFunction();
    if (function == null) {
      return;
    }
    // prepare return statement
    if (!(function.getBody() instanceof DartReturnBlock)) {
      return;
    }
    DartReturnBlock returnBlock = (DartReturnBlock) function.getBody();
    DartExpression returnValue = returnBlock.getValue();
    // add change
    String eol = utils.getEndOfLine();
    String indent = utils.getIndent(1);
    String prefix = utils.getNodePrefix(function);
    String newBodySource = "{" + eol + prefix + indent + "return " + getSource(returnValue) + ";"
        + eol + prefix + "}";
    addReplaceEdit(SourceRangeFactory.create(returnBlock), newBodySource);
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_convertToBlockBody,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_convertToExpressionFunctionBody() throws Exception {
    // prepare enclosing function
    DartFunction function = getEnclosingFunctionOrMethodFunction();
    if (function == null) {
      return;
    }
    // prepare body
    DartBlock body = function.getBody();
    if (body instanceof DartReturnBlock) {
      return;
    }
    // prepare return statement
    List<DartStatement> statements = body.getStatements();
    if (statements.size() != 1) {
      return;
    }
    if (!(statements.get(0) instanceof DartReturnStatement)) {
      return;
    }
    DartReturnStatement returnStatement = (DartReturnStatement) statements.get(0);
    // prepare returned value
    DartExpression returnValue = returnStatement.getValue();
    if (returnValue == null) {
      return;
    }
    // add change
    String newBodySource = "=> " + getSource(returnValue) + ";";
    addReplaceEdit(SourceRangeFactory.create(body), newBodySource);
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_convertToExpressionBody,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_exchangeOperands() throws Exception {
    // check that user invokes quick assist on binary expression
    if (!(node instanceof DartBinaryExpression)) {
      return;
    }
    DartBinaryExpression binaryExpression = (DartBinaryExpression) node;
    // prepare operator position
    int offset = isOperatorSelected(binaryExpression, selectionOffset, selectionLength);
    if (offset == -1) {
      return;
    }
    // add TextEdit-s
    {
      DartExpression arg1 = binaryExpression.getArg1();
      DartExpression arg2 = binaryExpression.getArg2();
      // find "wide" enclosing binary expression with same operator
      while (binaryExpression.getParent() instanceof DartBinaryExpression) {
        DartBinaryExpression newBinaryExpression = (DartBinaryExpression) binaryExpression.getParent();
        if (newBinaryExpression.getOperator() != binaryExpression.getOperator()) {
          break;
        }
        binaryExpression = newBinaryExpression;
      }
      // exchange parts of "wide" expression parts
      SourceRange range1 = SourceRangeFactory.forStartEnd(binaryExpression, arg1);
      SourceRange range2 = SourceRangeFactory.forStartEnd(arg2, binaryExpression);
      addReplaceEdit(range1, getSource(range2));
      addReplaceEdit(range2, getSource(range1));
    }
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_exchangeOperands,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_joinVariableDeclaration() throws Exception {
    // check that node is LHS in binary expression
    if (node instanceof DartIdentifier
        && getLocationInParent(node) == DART_BINARY_EXPRESSION_LEFT_OPERAND
        && node.getParent().getParent() instanceof DartExprStmt) {
    } else {
      return;
    }
    DartBinaryExpression binaryExpression = (DartBinaryExpression) node.getParent();
    // check that binary expression is assignment
    if (binaryExpression.getOperator() != Token.ASSIGN) {
      return;
    }
    // prepare "declaration" statement
    SourceInfo nameLocation = node.getElement().getNameLocation();
    DartNode nameNode = NodeFinder.perform(utils.getUnitNode(), nameLocation.getOffset(), 0);
    if (getLocationInParent(nameNode) == DART_VARIABLE_NAME
        && nameNode.getParent().getParent() instanceof DartVariableStatement) {
    } else {
      return;
    }
    DartVariableStatement nameStatement = (DartVariableStatement) nameNode.getParent().getParent();
    // check that "declaration" statement declared only one variable
    if (nameStatement.getVariables().size() != 1) {
      return;
    }
    // check that "declaration" and "assignment" statements are part of same Block
    DartExprStmt assignStatement = (DartExprStmt) node.getParent().getParent();
    if (assignStatement.getParent() instanceof DartBlock
        && assignStatement.getParent() == nameStatement.getParent()) {
    } else {
      return;
    }
    DartBlock block = (DartBlock) assignStatement.getParent();
    // check that "declaration" and "assignment" statements are adjacent
    List<DartStatement> statements = block.getStatements();
    if (statements.indexOf(assignStatement) == statements.indexOf(nameStatement) + 1) {
    } else {
      return;
    }
    // add edits
    {
      int assignOffset = binaryExpression.getOperatorOffset();
      addReplaceEdit(SourceRangeFactory.forEndStart(nameNode, assignOffset), " ");
    }
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_joinVariableDeclaration,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_removeTypeAnnotation() throws Exception {
    HasSourceInfo typeStart = null;
    HasSourceInfo typeEnd = null;
    // try local variable
    {
      DartVariableStatement statement = ASTNodes.getAncestor(node, DartVariableStatement.class);
      if (statement != null && statement.getTypeNode() != null) {
        DartVariable variable = statement.getVariables().get(0);
        typeStart = statement.getTypeNode();
        typeEnd = variable;
      }
    }
    // try top-level field
    {
      DartFieldDefinition fieldDefinition = ASTNodes.getAncestor(node, DartFieldDefinition.class);
      if (fieldDefinition != null && fieldDefinition.getTypeNode() != null) {
        DartField field = fieldDefinition.getFields().get(0);
        typeStart = fieldDefinition;
        typeEnd = field;
      }
    }
    // add edit
    if (typeStart != null && typeEnd != null) {
      SourceRange typeRange = SourceRangeFactory.forStartStart(typeStart, typeEnd);
      addReplaceEdit(typeRange, "var ");
    }
    // add proposal
    proposalRelevance -= 1;
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_removeTypeAnnotation,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_renameRefactoring() throws CoreException {
    // check that we can rename DartElement under cursor
    DartElement[] elements = unit.codeSelect(selectionOffset, 0);
    if (elements.length == 0) {
      return;
    }
    DartElement element = elements[0];
    if (element == null || !RefactoringAvailabilityTester.isRenameElementAvailable(element)) {
      return;
    }
    // we need DartEditor
    if (context instanceof AssistContext) {
      IEditorPart editor = ((AssistContext) context).getEditor();
      if (editor instanceof DartEditor) {
        DartEditor dartEditor = (DartEditor) editor;
        // add proposal
        ICommandAccess proposal = new RenameRefactoringProposal_OLD(dartEditor);
        proposals.add(proposal);
      }
    }
  }

  void addProposal_replaceConditionalWithIfElse() throws Exception {
    // try to find Conditional under cursor
    DartConditional conditional = null;
    {
      DartNode currentNode = node;
      while (currentNode instanceof DartExpression) {
        if ((currentNode instanceof DartConditional)) {
          conditional = (DartConditional) currentNode;
          break;
        }
        currentNode = currentNode.getParent();
      }
    }
    // if no Conditional, may be on Statement with Conditional
    DartStatement statement = ASTNodes.getAncestor(node, DartStatement.class);
    if (conditional == null) {
      // variable declaration
      if (statement instanceof DartVariableStatement) {
        DartVariableStatement variableStatement = (DartVariableStatement) statement;
        for (DartVariable variable : variableStatement.getVariables()) {
          if (variable.getValue() instanceof DartConditional) {
            conditional = (DartConditional) variable.getValue();
            break;
          }
        }
      }
      // assignment
      if (statement instanceof DartExprStmt) {
        DartExprStmt exprStmt = (DartExprStmt) statement;
        if (exprStmt.getExpression() instanceof DartBinaryExpression) {
          DartBinaryExpression binaryExpression = (DartBinaryExpression) exprStmt.getExpression();
          if (binaryExpression.getOperator() == Token.ASSIGN
              && binaryExpression.getArg2() instanceof DartConditional) {
            conditional = (DartConditional) binaryExpression.getArg2();
          }
        }
      }
      // return
      if (statement instanceof DartReturnStatement) {
        DartReturnStatement returnStatement = (DartReturnStatement) statement;
        if (returnStatement.getValue() instanceof DartConditional) {
          conditional = (DartConditional) returnStatement.getValue();
        }
      }
    }
    // prepare environment
    StructuralPropertyDescriptor locationInParent = getLocationInParent(conditional);
    String eol = utils.getEndOfLine();
    String indent = utils.getIndent(1);
    String prefix = utils.getNodePrefix(statement);
    // Type v = Conditional;
    if (locationInParent == DART_VARIABLE_VALUE) {
      DartVariable variable = (DartVariable) conditional.getParent();
      addRemoveEdit(SourceRangeFactory.forEndEnd(variable.getName(), conditional));
      addReplaceEdit(SourceRangeFactory.forEndLength(statement, 0), MessageFormat.format(
          "{3}{4}if ({0}) '{'{3}{4}{5}{6} = {1};{3}{4}'} else {'{3}{4}{5}{6} = {2};{3}{4}'}'",
          getSource(conditional.getCondition()),
          getSource(conditional.getThenExpression()),
          getSource(conditional.getElseExpression()),
          eol,
          prefix,
          indent,
          variable.getVariableName()));
    }
    // v = Conditional;
    if (locationInParent == DART_BINARY_EXPRESSION_RIGHT_OPERAND
        && conditional.getParent() instanceof DartBinaryExpression) {
      DartBinaryExpression binaryExpression = (DartBinaryExpression) conditional.getParent();
      if (binaryExpression.getOperator() == Token.ASSIGN) {
        DartExpression leftSide = binaryExpression.getArg1();
        addReplaceEdit(SourceRangeFactory.create(statement), MessageFormat.format(
            "if ({0}) '{'{3}{4}{5}{6} = {1};{3}{4}'} else {'{3}{4}{5}{6} = {2};{3}{4}'}'",
            getSource(conditional.getCondition()),
            getSource(conditional.getThenExpression()),
            getSource(conditional.getElseExpression()),
            eol,
            prefix,
            indent,
            getSource(leftSide)));
      }
    }
    // return Conditional;
    if (locationInParent == DART_RETURN_STATEMENT_VALUE) {
      addReplaceEdit(SourceRangeFactory.create(statement), MessageFormat.format(
          "if ({0}) '{'{3}{4}{5}return {1};{3}{4}'} else {'{3}{4}{5}return {2};{3}{4}'}'",
          getSource(conditional.getCondition()),
          getSource(conditional.getThenExpression()),
          getSource(conditional.getElseExpression()),
          eol,
          prefix,
          indent));
    }
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_replaceConditionalWithIfElse,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_replaceIfElseWithConditional() throws Exception {
    // should be "if"
    if (!(node instanceof DartIfStatement)) {
      return;
    }
    DartIfStatement ifStatement = (DartIfStatement) node;
    // single then/else statements
    DartStatement thenStatement = ASTNodes.getSingleStatement(ifStatement.getThenStatement());
    DartStatement elseStatement = ASTNodes.getSingleStatement(ifStatement.getElseStatement());
    if (thenStatement == null || elseStatement == null) {
      return;
    }
    // returns
    if (thenStatement instanceof DartReturnStatement
        || elseStatement instanceof DartReturnStatement) {
      DartReturnStatement thenReturn = (DartReturnStatement) thenStatement;
      DartReturnStatement elseReturn = (DartReturnStatement) elseStatement;
      addReplaceEdit(SourceRangeFactory.create(ifStatement), MessageFormat.format(
          "return {0} ? {1} : {2};",
          getSource(ifStatement.getCondition()),
          getSource(thenReturn.getValue()),
          getSource(elseReturn.getValue())));
    }
    // assignments -> v = Conditional;
    if (thenStatement instanceof DartExprStmt && elseStatement instanceof DartExprStmt) {
      DartExpression thenExpression = ((DartExprStmt) thenStatement).getExpression();
      DartExpression elseExpression = ((DartExprStmt) elseStatement).getExpression();
      if (thenExpression instanceof DartBinaryExpression
          && elseExpression instanceof DartBinaryExpression) {
        DartBinaryExpression thenBinary = (DartBinaryExpression) thenExpression;
        DartBinaryExpression elseBinary = (DartBinaryExpression) elseExpression;
        String thenTarget = getSource(thenBinary.getArg1());
        String elseTarget = getSource(elseBinary.getArg1());
        if (thenBinary.getOperator() == Token.ASSIGN && elseBinary.getOperator() == Token.ASSIGN
            && StringUtils.equals(thenTarget, elseTarget)) {
          addReplaceEdit(
              SourceRangeFactory.create(ifStatement),
              MessageFormat.format(
                  "{0} = {1} ? {2} : {3};",
                  thenTarget,
                  getSource(ifStatement.getCondition()),
                  getSource(thenBinary.getArg2()),
                  getSource(elseBinary.getArg2())));
        }
      }
    }
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_replaceIfElseWithConditional,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_splitAndCondition() throws Exception {
    // check that user invokes quick assist on binary expression
    if (!(node instanceof DartBinaryExpression)) {
      return;
    }
    DartBinaryExpression binaryExpression = (DartBinaryExpression) node;
    // prepare operator position
    int offset = isOperatorSelected(binaryExpression, selectionOffset, selectionLength);
    if (offset == -1) {
      return;
    }
    // prepare "if"
    DartStatement statement = ASTNodes.getAncestor(node, DartStatement.class);
    if (!(statement instanceof DartIfStatement)) {
      return;
    }
    DartIfStatement ifStatement = (DartIfStatement) statement;
    // check that binary expression is part of first level && condition of "if"
    DartBinaryExpression condition = binaryExpression;
    while (condition.getParent() instanceof DartBinaryExpression
        && ((DartBinaryExpression) condition.getParent()).getOperator() == Token.AND) {
      condition = (DartBinaryExpression) condition.getParent();
    }
    if (ifStatement.getCondition() != condition) {
      return;
    }
    // prepare environment
    String prefix = utils.getNodePrefix(ifStatement);
    String eol = utils.getEndOfLine();
    String indent = utils.getIndent(1);
    // prepare "rightCondition"
    String rightConditionSource;
    {
      SourceRange rightConditionRange = SourceRangeFactory.forStartEnd(
          binaryExpression.getArg2(),
          condition);
      rightConditionSource = getSource(rightConditionRange);
    }
    // remove "&& rightCondition"
    addRemoveEdit(SourceRangeFactory.forEndEnd(binaryExpression.getArg1(), condition));
    // update "then" statement
    DartStatement thenStatement = ifStatement.getThenStatement();
    DartStatement elseStatement = ifStatement.getElseStatement();
    if (thenStatement instanceof DartBlock) {
      DartBlock thenBlock = (DartBlock) thenStatement;
      SourceRange thenBlockRange = SourceRangeFactory.create(thenBlock);
      // insert inner "if" with right part of "condition"
      {
        String source = eol + prefix + indent + "if (" + rightConditionSource + ") {";
        int thenBlockInsideOffset = thenBlockRange.getOffset() + 1;
        addInsertEdit(thenBlockInsideOffset, source);
      }
      // insert closing "}" for inner "if"
      {
        int thenBlockEnd = SourceRangeUtils.getEnd(thenBlockRange);
        String source = indent + "}";
        // may be move "else" statements
        if (elseStatement != null) {
          List<DartStatement> elseStatements = ASTNodes.getStatements(elseStatement);
          SourceRange elseLinesRange = utils.getLinesRange(elseStatements);
          String elseIndentOld = prefix + indent;
          String elseIndentNew = elseIndentOld + indent;
          String newElseSource = utils.getIndentSource(elseLinesRange, elseIndentOld, elseIndentNew);
          // append "else" block
          source += " else {" + eol;
          source += newElseSource;
          source += prefix + indent + "}";
          // remove old "else" range
          addRemoveEdit(SourceRangeFactory.forStartEnd(thenBlockEnd, elseStatement));
        }
        // insert before outer "then" block "}"
        source += eol + prefix;
        addInsertEdit(thenBlockEnd - 1, source);
      }
    } else {
      // insert inner "if" with right part of "condition"
      {
        String source = eol + prefix + indent + "if (" + rightConditionSource + ")";
        addInsertEdit(ifStatement.getCloseParenOffset() + 1, source);
      }
      // indent "else" statements to correspond inner "if"
      if (elseStatement != null) {
        SourceRange elseRange = SourceRangeFactory.forStartEnd(
            ifStatement.getElseTokenOffset(),
            elseStatement);
        SourceRange elseLinesRange = utils.getLinesRange(elseRange);
        String elseIndentOld = prefix;
        String elseIndentNew = elseIndentOld + indent;
        textEdits.add(utils.createIndentEdit(elseLinesRange, elseIndentOld, elseIndentNew));
      }
    }
    // indent "then" statements to correspond inner "if"
    {
      List<DartStatement> thenStatements = ASTNodes.getStatements(thenStatement);
      SourceRange linesRange = utils.getLinesRange(thenStatements);
      String thenIndentOld = prefix + indent;
      String thenIndentNew = thenIndentOld + indent;
      textEdits.add(utils.createIndentEdit(linesRange, thenIndentOld, thenIndentNew));
    }
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_splitAndCondition,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_splitVariableDeclaration() throws Exception {
    // prepare DartVariableStatement, should be part of Block
    DartVariableStatement statement = ASTNodes.getAncestor(node, DartVariableStatement.class);
    if (statement != null && statement.getParent() instanceof DartBlock) {
    } else {
      return;
    }
    // check that statement declares single variable
    List<DartVariable> variables = statement.getVariables();
    if (variables.size() != 1) {
      return;
    }
    DartVariable variable = variables.get(0);
    // remove initializer value
    addRemoveEdit(SourceRangeFactory.forStartEnd(
        variable.getName().getSourceInfo().getEnd(),
        statement.getSourceInfo().getEnd() - 1));
    // add assignment statement
    String eol = utils.getEndOfLine();
    String indent = utils.getNodePrefix(statement);
    String assignSource = MessageFormat.format(
        "{0} = {1};",
        variable.getName().getName(),
        utils.getText(variable.getValue()));
    SourceRange assignRange = SourceRangeFactory.forEndLength(statement, 0);
    addReplaceEdit(assignRange, eol + indent + assignSource);
    // add proposal
    addUnitCorrectionProposal(
        CorrectionMessages.QuickAssistProcessor_splitVariableDeclaration,
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  void addProposal_surroundWith() throws CoreException {
    // prepare selected statements
    List<DartStatement> selectedStatements;
    {
      Selection selection = Selection.createFromStartLength(selectionOffset, selectionLength);
      StatementAnalyzer selectionAnalyzer = new StatementAnalyzer(unit, selection, false);
      unitNode.accept(selectionAnalyzer);
      DartNode[] selectedNodes = selectionAnalyzer.getSelectedNodes();
      // convert nodes to statements
      selectedStatements = Lists.newArrayList();
      for (DartNode selectedNode : selectedNodes) {
        if (selectedNode instanceof DartStatement) {
          selectedStatements.add((DartStatement) selectedNode);
        }
      }
      // we want only statements
      if (selectedStatements.isEmpty() || selectedStatements.size() != selectedNodes.length) {
        return;
      }
    }
    // prepare statement information
    DartStatement firstStatement = selectedStatements.get(0);
    DartStatement lastStatement = selectedStatements.get(selectedStatements.size() - 1);
    SourceRange statementsRange = utils.getLinesRange(selectedStatements);
    // prepare environment
    String eol = utils.getEndOfLine();
    String indentOld = utils.getNodePrefix(firstStatement);
    String indentNew = indentOld + utils.getIndent(1);
    // "block"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "{" + eol);
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_block,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // "if"
    {
      {
        int offset = statementsRange.getOffset();
        SourceBuilder sb = new SourceBuilder(offset);
        sb.append(indentOld);
        sb.append("if (");
        {
          sb.startPosition("CONDITION");
          sb.append("condition");
          sb.endPosition();
        }
        sb.append(") {");
        sb.append(eol);
        addInsertEdit(sb);
      }
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_if,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // "while"
    {
      {
        int offset = statementsRange.getOffset();
        SourceBuilder sb = new SourceBuilder(offset);
        sb.append(indentOld);
        sb.append("while (");
        {
          sb.startPosition("CONDITION");
          sb.append("condition");
          sb.endPosition();
        }
        sb.append(") {");
        sb.append(eol);
        addInsertEdit(sb);
      }
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_while,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // "for-in"
    {
      {
        int offset = statementsRange.getOffset();
        SourceBuilder sb = new SourceBuilder(offset);
        sb.append(indentOld);
        sb.append("for (var ");
        {
          sb.startPosition("NAME");
          sb.append("item");
          sb.endPosition();
        }
        sb.append(" in ");
        {
          sb.startPosition("ITERABLE");
          sb.append("iterable");
          sb.endPosition();
        }
        sb.append(") {");
        sb.append(eol);
        addInsertEdit(sb);
      }
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_forIn,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // "for"
    {
      {
        int offset = statementsRange.getOffset();
        SourceBuilder sb = new SourceBuilder(offset);
        sb.append(indentOld);
        sb.append("for (var ");
        {
          sb.startPosition("VAR");
          sb.append("v");
          sb.endPosition();
        }
        sb.append(" = ");
        {
          sb.startPosition("INIT");
          sb.append("init");
          sb.endPosition();
        }
        sb.append("; ");
        {
          sb.startPosition("CONDITION");
          sb.append("condition");
          sb.endPosition();
        }
        sb.append("; ");
        {
          sb.startPosition("INCREMENT");
          sb.append("increment");
          sb.endPosition();
        }
        sb.append(") {");
        sb.append(eol);
        addInsertEdit(sb);
      }
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_for,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // "do-while"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "do {" + eol);
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      {
        int offset = SourceRangeUtils.getEnd(statementsRange);
        SourceBuilder sb = new SourceBuilder(offset);
        sb.append(indentOld);
        sb.append("} while (");
        {
          sb.startPosition("CONDITION");
          sb.append("condition");
          sb.endPosition();
        }
        sb.append(");");
        sb.append(eol);
        addInsertEdit(sb);
      }
      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_doWhile,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // "try-catch"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "try {" + eol);
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      {
        int offset = SourceRangeUtils.getEnd(statementsRange);
        SourceBuilder sb = new SourceBuilder(offset);
        sb.append(indentOld);
        sb.append("} on ");
        {
          sb.startPosition("EXCEPTION_TYPE");
          sb.append("Exception");
          sb.endPosition();
        }
        sb.append(" catch (");
        {
          sb.startPosition("EXCEPTION_VAR");
          sb.append("e");
          sb.endPosition();
        }
        sb.append(") {");
        sb.append(eol);
        //
        sb.append(indentNew);
        {
          sb.startPosition("CATCH");
          sb.append("// TODO");
          sb.endPosition();
          sb.setEndPosition();
        }
        sb.append(eol);
        //
        sb.append(indentOld);
        sb.append("}");
        sb.append(eol);
        //
        addInsertEdit(sb);
      }
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_tryCatch,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // "try-finally"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "try {" + eol);
      {
        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      {
        int offset = SourceRangeUtils.getEnd(statementsRange);
        SourceBuilder sb = new SourceBuilder(offset);
        //
        sb.append(indentOld);
        sb.append("} finally {");
        sb.append(eol);
        //
        sb.append(indentNew);
        {
          sb.startPosition("FINALLY");
          sb.append("// TODO");
          sb.endPosition();
        }
        sb.setEndPosition();
        sb.append(eol);
        //
        sb.append(indentOld);
        sb.append("}");
        sb.append(eol);
        //
        addInsertEdit(sb);
      }
      // add proposal
      addUnitCorrectionProposal(
          CorrectionMessages.QuickAssistProcessor_surroundWith_tryFinally,
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
  }

  private void addInsertEdit(int offset, String text) {
    textEdits.add(createInsertEdit(offset, text));
  }

  private void addInsertEdit(SourceBuilder builder) {
    int offset = builder.getOffset();
    TextEdit edit = createInsertEdit(offset, builder.toString());
    textEdits.add(edit);
    addLinkedPositions(builder, edit);
  }

  private void addLinkedPosition(String group, SourceRange range) {
    List<SourceRange> positions = linkedPositions.get(group);
    if (positions == null) {
      positions = Lists.newArrayList();
      linkedPositions.put(group, positions);
    }
    positions.add(range);
  }

  private void addLinkedPositionProposal(String group, Image icon, String text) {
    List<TrackedNodeProposal> nodeProposals = linkedPositionProposals.get(group);
    if (nodeProposals == null) {
      nodeProposals = Lists.newArrayList();
      linkedPositionProposals.put(group, nodeProposals);
    }
    nodeProposals.add(new TrackedNodeProposal(icon, text));
  }

  /**
   * Adds positions from the given {@link SourceBuilder} to the {@link #linkedPositions}.
   */
  private void addLinkedPositions(SourceBuilder builder, TextEdit edit) {
    // end position
    {
      int endPosition = builder.getEndPosition();
      if (endPosition != -1) {
        proposalEndRange = SourceRangeFactory.forStartLength(endPosition, 0);
        positionStopEdits.put(proposalEndRange, edit);
      }
    }
    // positions
    Map<String, List<TrackedNodePosition>> builderPositions = builder.getTrackedPositions();
    for (Entry<String, List<TrackedNodePosition>> entry : builderPositions.entrySet()) {
      String groupId = entry.getKey();
      for (TrackedNodePosition position : entry.getValue()) {
        SourceRange range = SourceRangeFactory.forStartLength(
            position.getStartPosition(),
            position.getLength());
        addLinkedPosition(groupId, range);
        positionStopEdits.put(range, edit);
      }
    }
    // proposals for positions
    Map<String, List<TrackedNodeProposal>> builderProposals = builder.getTrackedProposals();
    for (Entry<String, List<TrackedNodeProposal>> entry : builderProposals.entrySet()) {
      String groupId = entry.getKey();
      for (TrackedNodeProposal nodeProposal : entry.getValue()) {
        addLinkedPositionProposal(groupId, nodeProposal.getIcon(), nodeProposal.getText());
      }
    }
  }

  /**
   * Adds {@link #linkedPositions} to the current {@link #proposal}.
   */
  private void addLinkedPositionsToProposal() {
    // positions
    for (Entry<String, List<SourceRange>> entry : linkedPositions.entrySet()) {
      String groupId = entry.getKey();
      for (SourceRange range : entry.getValue()) {
        range = translateRangeAfterTextEdits(range);
        TrackedNodePosition position = TrackedPositions.forRange(range);
        proposal.addLinkedPosition(position, false, groupId);
      }
    }
    // proposals for positions
    for (Entry<String, List<TrackedNodeProposal>> entry : linkedPositionProposals.entrySet()) {
      String groupId = entry.getKey();
      for (TrackedNodeProposal nodeProposal : entry.getValue()) {
        proposal.addLinkedPositionProposal(groupId, nodeProposal.getText(), nodeProposal.getIcon());
      }
    }
  }

  private void addRemoveEdit(SourceRange range) {
    textEdits.add(createRemoveEdit(range));
  }

  private void addReplaceEdit(SourceRange range, String text) {
    textEdits.add(createReplaceEdit(range, text));
  }

  /**
   * Adds new {@link LinkedCorrectionProposal} using {@link #unit} and {@link #textEdits}.
   */
  private void addUnitCorrectionProposal(String label, Image image) {
    // sort edits
    Collections.sort(textEdits, new Comparator<TextEdit>() {
      @Override
      public int compare(TextEdit o1, TextEdit o2) {
        return o1.getOffset() - o2.getOffset();
      }
    });
    // prepare change
    CompilationUnitChange change = new CompilationUnitChange(label, unit);
    change.setSaveMode(TextFileChange.LEAVE_DIRTY);
    change.setEdit(new MultiTextEdit());
    // add edits
    for (TextEdit textEdit : textEdits) {
      change.addEdit(textEdit);
    }
    // add proposal
    if (!textEdits.isEmpty()) {
      proposal = new LinkedCorrectionProposal(label, unit, change, proposalRelevance, image);
      addLinkedPositionsToProposal();
      if (proposalEndRange != null) {
        proposalEndRange = translateRangeAfterTextEdits(proposalEndRange);
        TrackedNodePosition endPosition = TrackedPositions.forRange(proposalEndRange);
        proposal.setEndPosition(endPosition);
      }
      proposals.add(proposal);
    }
    // reset
    resetProposalElements();
  }

  /**
   * @return the enclosing {@link DartFunction} for {@link #node}, or is part of enclosing
   *         {@link DartMethodDefinition}. May be <code>null</code>.
   */
  private DartFunction getEnclosingFunctionOrMethodFunction() {
    DartFunction function = ASTNodes.getAncestor(node, DartFunction.class);
    if (function == null) {
      DartMethodDefinition method = ASTNodes.getAncestor(node, DartMethodDefinition.class);
      if (method != null) {
        function = method.getFunction();
      }
    }
    return function;
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(HasSourceInfo hasSourceInfo) throws Exception {
    SourceRange range = SourceRangeFactory.create(hasSourceInfo);
    return getSource(range);
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(int offset, int length) throws Exception {
    return unit.getBuffer().getText(offset, length);
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(SourceRange range) throws Exception {
    return getSource(range.getOffset(), range.getLength());
  }

  private void resetProposalElements() {
    textEdits.clear();
    proposalRelevance = DEFAULT_RELEVANCE;
    linkedPositions.clear();
    positionStopEdits.clear();
    linkedPositionProposals.clear();
    proposal = null;
    proposalEndRange = null;
  }

  /**
   * @return the updated {@link SourceRange} which should be used after applying {@link #textEdits}.
   */
  private SourceRange translateRangeAfterTextEdits(SourceRange range) {
    int delta = 0;
    {
      int rangeOffset = range.getOffset();
      TextEdit rangeStopEdit = positionStopEdits.get(range);
      for (TextEdit textEdit : textEdits) {
        if (textEdit.getOffset() <= rangeOffset) {
          if (rangeStopEdit == textEdit) {
            break;
          }
          delta += ExtractUtils.getDeltaOffset(textEdit);
        }
      }
    }
    int offset = delta + range.getOffset();
    return SourceRangeFactory.forStartLength(offset, range.getLength());
  }
}
