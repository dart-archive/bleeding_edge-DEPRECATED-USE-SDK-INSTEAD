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

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.services.correction.SourceChange;
import com.google.dart.engine.services.internal.util.ExecutionUtils;
import com.google.dart.engine.services.internal.util.RunnableEx;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndLength;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;

/**
 * Implementation of {@link QuickAssistProcessor}.
 */
public class QuickAssistProcessorImpl implements QuickAssistProcessor {
  private static final int DEFAULT_RELEVANCE = 30;

  /**
   * @return the {@link Edit} to replace {@link SourceRange} with "text".
   */
  private static Edit createReplaceEdit(SourceRange range, String text) {
    return new Edit(range.getOffset(), range.getLength(), text);
  }

  /**
   * @return <code>true</code> if selection covers operator of the given {@link BinaryExpression}.
   */
  private static int isOperatorSelected(BinaryExpression binaryExpression, int offset, int length) {
    ASTNode left = binaryExpression.getLeftOperand();
    ASTNode right = binaryExpression.getRightOperand();
    if (isSelectingOperator(left, right, offset, length)) {
      return left.getEndToken().getEnd();
    }
    return -1;
  }

  private static boolean isSelectingOperator(ASTNode n1, ASTNode n2, int offset, int length) {
    // between the nodes
    if (offset >= n1.getEndToken().getEnd() && offset + length <= n2.getOffset()) {
      return true;
    }
    // or exactly select the node (but not with infix expressions)
    if (offset == n1.getOffset() && offset + length == n2.getEndToken().getEnd()) {
      if (n1 instanceof BinaryExpression || n2 instanceof BinaryExpression) {
        return false;
      }
      return true;
    }
    // invalid selection (part of node, etc)
    return false;
  }

  private final List<CorrectionProposal> proposals = Lists.newArrayList();
  private final List<Edit> textEdits = Lists.newArrayList();
  private Source source;
  private CompilationUnit unit;
  private ASTNode node;
  private int selectionOffset;
  private int selectionLength;
  private CorrectionUtils utils;

  private int proposalRelevance = DEFAULT_RELEVANCE;

  @Override
  public CorrectionProposal[] getProposals(AssistContext context) throws Exception {
    proposals.clear();
    source = context.getSource();
    unit = context.getCompilationUnit();
    node = context.getCoveringNode();
    selectionOffset = context.getSelectionOffset();
    selectionLength = context.getSelectionLength();
    utils = new CorrectionUtils(context.getCompilationUnit());
    // TODO(scheglov) use ExecutionUtils
    for (final Method method : QuickAssistProcessorImpl.class.getDeclaredMethods()) {
      if (method.getName().startsWith("addProposal_")) {
        resetProposalElements();
        ExecutionUtils.runIgnore(new RunnableEx() {
          @Override
          public void run() throws Exception {
            method.invoke(QuickAssistProcessorImpl.this);
          }
        });
      }
    }
//    try {
//      textEdits.clear();
//      addProposal_exchangeOperands();
//    } catch (Throwable e) {
//    }
    return proposals.toArray(new CorrectionProposal[proposals.size()]);
  }

  // TODO(scheglov) implement after type inference
//  void addProposal_addTypeAnnotation() throws Exception {
//    Type type = null;
//    HasSourceInfo declarationStart = null;
//    HasSourceInfo nameStart = null;
//    // try local variable
//    {
//      DartVariableStatement statement = ASTNodes.getAncestor(node, DartVariableStatement.class);
//      if (statement != null && statement.getTypeNode() == null) {
//        List<DartVariable> variables = statement.getVariables();
//        if (variables.size() == 1) {
//          DartVariable variable = variables.get(0);
//          type = variable.getElement().getType();
//          declarationStart = statement;
//          nameStart = variable;
//          // language style guide recommends to use "var" for locals, so deprioritize
//          proposalRelevance -= 1;
//        }
//      }
//    }
//    // try field
//    {
//      DartFieldDefinition fieldDefinition = ASTNodes.getAncestor(node, DartFieldDefinition.class);
//      if (fieldDefinition != null && fieldDefinition.getTypeNode() == null) {
//        List<DartField> fields = fieldDefinition.getFields();
//        if (fields.size() == 1) {
//          DartField field = fields.get(0);
//          DartExpression value = field.getValue();
//          if (value != null) {
//            type = value.getType();
//            declarationStart = fieldDefinition;
//            nameStart = field;
//          }
//        }
//      }
//    }
//    // check type
//    if (type == null || TypeKind.of(type) == TypeKind.DYNAMIC) {
//      return;
//    }
//    // add edit
//    if (declarationStart != null && nameStart != null) {
//      String typeSource = ExtractUtils.getTypeSource(type);
//      // find "var" token
//      KeywordToken varToken;
//      {
//        SourceRange modifiersRange = SourceRangeFactory.forStartEnd(declarationStart, nameStart);
//        String modifiersSource = utils.getText(modifiersRange);
//        List<com.google.dart.engine.scanner.Token> tokens = TokenUtils.getTokens(modifiersSource);
//        varToken = TokenUtils.findKeywordToken(tokens, Keyword.VAR);
//      }
//      // replace "var", or insert type before name
//      if (varToken != null) {
//        SourceRange range = SourceRangeFactory.forToken(varToken);
//        range = SourceRangeFactory.withBase(declarationStart, range);
//        addReplaceEdit(range, typeSource);
//      } else {
//        SourceRange range = SourceRangeFactory.forStartLength(nameStart, 0);
//        addReplaceEdit(range, typeSource + " ");
//      }
//    }
//    // add proposal
//    addUnitCorrectionProposal(
//        CorrectionMessages.QuickAssistProcessor_addTypeAnnotation,
//        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//  }

  // TODO(scheglov) implement later
//  void addProposal_convertGetterToMethodRefactoring() throws CoreException {
//    if (!RefactoringAvailabilityTester.isConvertGetterToMethodAvailable(selectionFunction)) {
//      return;
//    }
//    // we need DartEditor
//    if (context instanceof AssistContext) {
//      IEditorPart editor = ((AssistContext) context).getEditor();
//      if (editor instanceof DartEditor) {
//        DartEditor dartEditor = (DartEditor) editor;
//        // add proposal
//        ICommandAccess proposal = new ConvertGetterToMethodRefactoringProposal(
//            dartEditor,
//            selectionFunction,
//            proposalRelevance);
//        proposals.add(proposal);
//      }
//    }
//  }

  // TODO(scheglov) implement later
//  void addProposal_convertMethodToGetterRefactoring() throws CoreException {
//    if (!RefactoringAvailabilityTester.isConvertMethodToGetterAvailable(selectionFunction)) {
//      return;
//    }
//    // we need DartEditor
//    if (context instanceof AssistContext) {
//      IEditorPart editor = ((AssistContext) context).getEditor();
//      if (editor instanceof DartEditor) {
//        DartEditor dartEditor = (DartEditor) editor;
//        // add proposal
//        ICommandAccess proposal = new ConvertMethodToGetterRefactoringProposal(
//            dartEditor,
//            selectionFunction,
//            proposalRelevance);
//        proposals.add(proposal);
//      }
//    }
//  }

  // TODO(scheglov) implement later
//  void addProposal_ConvertOptionalParametersToNamedRefactoring() throws CoreException {
//    if (!RefactoringAvailabilityTester.isConvertOptionalParametersToNamedAvailable(selectionFunction)) {
//      return;
//    }
//    // we need DartEditor
//    if (context instanceof AssistContext) {
//      IEditorPart editor = ((AssistContext) context).getEditor();
//      if (editor instanceof DartEditor) {
//        DartEditor dartEditor = (DartEditor) editor;
//        // add proposal
//        ICommandAccess proposal = new ConvertOptionalParametersToNamedRefactoringProposal(
//            dartEditor,
//            selectionFunction,
//            proposalRelevance);
//        proposals.add(proposal);
//      }
//    }
//  }

  void addProposal_convertToBlockFunctionBody() throws Exception {
    // prepare current body
    FunctionBody body = null;
    {
      FunctionDeclaration function = node.getAncestor(FunctionDeclaration.class);
      if (function != null) {
        body = function.getFunctionExpression().getBody();
      }
    }
    {
      FunctionExpression function = node.getAncestor(FunctionExpression.class);
      if (function != null) {
        body = function.getBody();
      }
    }
    if (body == null) {
      MethodDeclaration method = node.getAncestor(MethodDeclaration.class);
      if (method != null) {
        body = method.getBody();
      }
    }
    // prepare expression body
    if (!(body instanceof ExpressionFunctionBody)) {
      return;
    }
    Expression returnValue = ((ExpressionFunctionBody) body).getExpression();
    // prepare prefix
    String prefix;
    {
      ASTNode bodyParent = body.getParent();
      prefix = utils.getNodePrefix(bodyParent);
    }
    // add change
    String eol = utils.getEndOfLine();
    String indent = utils.getIndent(1);
    String newBodySource = "{" + eol + prefix + indent + "return " + utils.getText(returnValue)
        + ";" + eol + prefix + "}";
    addReplaceEdit(rangeNode(body), newBodySource);
    // add proposal
    addUnitCorrectionProposal("Convert to block body", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  // TODO(scheglov) implement later
//  void addProposal_convertToExpressionFunctionBody() throws Exception {
//    // prepare enclosing function
//    DartFunction function = getEnclosingFunctionOrMethodFunction();
//    if (function == null) {
//      return;
//    }
//    // prepare body
//    DartBlock body = function.getBody();
//    if (body instanceof DartReturnBlock) {
//      return;
//    }
//    // prepare return statement
//    List<DartStatement> statements = body.getStatements();
//    if (statements.size() != 1) {
//      return;
//    }
//    if (!(statements.get(0) instanceof DartReturnStatement)) {
//      return;
//    }
//    DartReturnStatement returnStatement = (DartReturnStatement) statements.get(0);
//    // prepare returned value
//    DartExpression returnValue = returnStatement.getValue();
//    if (returnValue == null) {
//      return;
//    }
//    // add change
//    String newBodySource = "=> " + getSource(returnValue) + ";";
//    addReplaceEdit(SourceRangeFactory.create(body), newBodySource);
//    // add proposal
//    addUnitCorrectionProposal(
//        CorrectionMessages.QuickAssistProcessor_convertToExpressionBody,
//        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//  }

  void addProposal_exchangeOperands() throws Exception {
    // check that user invokes quick assist on binary expression
    if (!(node instanceof BinaryExpression)) {
      return;
    }
    BinaryExpression binaryExpression = (BinaryExpression) node;
    // prepare operator position
    int offset = isOperatorSelected(binaryExpression, selectionOffset, selectionLength);
    if (offset == -1) {
      return;
    }
    // add edits
    {
      Expression leftOperand = binaryExpression.getLeftOperand();
      Expression rightOperand = binaryExpression.getRightOperand();
      // find "wide" enclosing binary expression with same operator
      while (binaryExpression.getParent() instanceof BinaryExpression) {
        BinaryExpression newBinaryExpression = (BinaryExpression) binaryExpression.getParent();
        if (newBinaryExpression.getOperator().getType() != binaryExpression.getOperator().getType()) {
          break;
        }
        binaryExpression = newBinaryExpression;
      }
      // exchange parts of "wide" expression parts
      SourceRange leftRange = rangeStartEnd(binaryExpression, leftOperand);
      SourceRange rightRange = rangeStartEnd(rightOperand, binaryExpression);
      addReplaceEdit(leftRange, utils.getText(rightRange));
      addReplaceEdit(rightRange, utils.getText(leftRange));
    }
    // add proposal
    addUnitCorrectionProposal("Exchange operands", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  void addProposal_joinVariableDeclaration() throws Exception {
    // check that node is LHS in binary expression
    if (node instanceof SimpleIdentifier && node.getParent() instanceof AssignmentExpression
        && ((AssignmentExpression) node.getParent()).getLeftHandSide() == node
        && node.getParent().getParent() instanceof ExpressionStatement) {
    } else {
      return;
    }
    AssignmentExpression assignExpression = (AssignmentExpression) node.getParent();
    // check that binary expression is assignment
    if (assignExpression.getOperator().getType() != TokenType.EQ) {
      return;
    }
    // prepare "declaration" statement
    int declOffset = ((SimpleIdentifier) node).getElement().getNameOffset();
    ASTNode declNode = new NodeLocator(declOffset).searchWithin(unit);
    if (declNode != null && declNode.getParent() instanceof VariableDeclaration
        && ((VariableDeclaration) declNode.getParent()).getName() == declNode
        && declNode.getParent().getParent() instanceof VariableDeclarationList
        && declNode.getParent().getParent().getParent() instanceof VariableDeclarationStatement) {
    } else {
      return;
    }
    VariableDeclarationStatement declStatement = (VariableDeclarationStatement) declNode.getParent().getParent().getParent();
    // check that "declaration" statement declared only one variable
    if (declStatement.getVariables().getVariables().size() != 1) {
      return;
    }
    // check that "declaration" and "assignment" statements are part of same Block
    ExpressionStatement assignStatement = (ExpressionStatement) node.getParent().getParent();
    if (assignStatement.getParent() instanceof Block
        && assignStatement.getParent() == declStatement.getParent()) {
    } else {
      return;
    }
    Block block = (Block) assignStatement.getParent();
    // check that "declaration" and "assignment" statements are adjacent
    List<Statement> statements = block.getStatements();
    if (statements.indexOf(assignStatement) == statements.indexOf(declStatement) + 1) {
    } else {
      return;
    }
    // add edits
    {
      int assignOffset = assignExpression.getOperator().getOffset();
      addReplaceEdit(rangeEndStart(declNode, assignOffset), " ");
    }
    // add proposal
    addUnitCorrectionProposal("Join variable declaration", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  // TODO(scheglov) implement later
//  void addProposal_removeTypeAnnotation() throws Exception {
//    HasSourceInfo typeStart = null;
//    HasSourceInfo typeEnd = null;
//    // try local variable
//    {
//      DartVariableStatement statement = ASTNodes.getAncestor(node, DartVariableStatement.class);
//      if (statement != null && statement.getTypeNode() != null) {
//        DartVariable variable = statement.getVariables().get(0);
//        typeStart = statement.getTypeNode();
//        typeEnd = variable;
//      }
//    }
//    // try top-level field
//    {
//      DartFieldDefinition fieldDefinition = ASTNodes.getAncestor(node, DartFieldDefinition.class);
//      if (fieldDefinition != null && fieldDefinition.getTypeNode() != null) {
//        DartField field = fieldDefinition.getFields().get(0);
//        typeStart = fieldDefinition;
//        typeEnd = field;
//      }
//    }
//    // add edit
//    if (typeStart != null && typeEnd != null) {
//      SourceRange typeRange = SourceRangeFactory.forStartStart(typeStart, typeEnd);
//      addReplaceEdit(typeRange, "var ");
//    }
//    // add proposal
//    proposalRelevance -= 1;
//    addUnitCorrectionProposal(
//        CorrectionMessages.QuickAssistProcessor_removeTypeAnnotation,
//        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//  }

  // TODO(scheglov) implement later
//  void addProposal_renameRefactoring() throws CoreException {
//    // check that we can rename DartElement under cursor
//    DartElement[] elements = unit.codeSelect(selectionOffset, 0);
//    if (elements.length == 0) {
//      return;
//    }
//    DartElement element = elements[0];
//    if (element == null || !RefactoringAvailabilityTester.isRenameElementAvailable(element)) {
//      return;
//    }
//    // we need DartEditor
//    if (context instanceof AssistContext) {
//      IEditorPart editor = ((AssistContext) context).getEditor();
//      if (editor instanceof DartEditor) {
//        DartEditor dartEditor = (DartEditor) editor;
//        // add proposal
//        ICommandAccess proposal = new RenameRefactoringProposal(dartEditor);
//        proposals.add(proposal);
//      }
//    }
//  }

  // TODO(scheglov) implement later
//  void addProposal_replaceConditionalWithIfElse() throws Exception {
//    // try to find Conditional under cursor
//    DartConditional conditional = null;
//    {
//      DartNode currentNode = node;
//      while (currentNode instanceof DartExpression) {
//        if (currentNode instanceof DartConditional) {
//          conditional = (DartConditional) currentNode;
//          break;
//        }
//        currentNode = currentNode.getParent();
//      }
//    }
//    // if no Conditional, may be on Statement with Conditional
//    DartStatement statement = ASTNodes.getAncestor(node, DartStatement.class);
//    if (conditional == null) {
//      // variable declaration
//      if (statement instanceof DartVariableStatement) {
//        DartVariableStatement variableStatement = (DartVariableStatement) statement;
//        for (DartVariable variable : variableStatement.getVariables()) {
//          if (variable.getValue() instanceof DartConditional) {
//            conditional = (DartConditional) variable.getValue();
//            break;
//          }
//        }
//      }
//      // assignment
//      if (statement instanceof DartExprStmt) {
//        DartExprStmt exprStmt = (DartExprStmt) statement;
//        if (exprStmt.getExpression() instanceof DartBinaryExpression) {
//          DartBinaryExpression binaryExpression = (DartBinaryExpression) exprStmt.getExpression();
//          if (binaryExpression.getOperator() == Token.ASSIGN
//              && binaryExpression.getArg2() instanceof DartConditional) {
//            conditional = (DartConditional) binaryExpression.getArg2();
//          }
//        }
//      }
//      // return
//      if (statement instanceof DartReturnStatement) {
//        DartReturnStatement returnStatement = (DartReturnStatement) statement;
//        if (returnStatement.getValue() instanceof DartConditional) {
//          conditional = (DartConditional) returnStatement.getValue();
//        }
//      }
//    }
//    // prepare environment
//    StructuralPropertyDescriptor locationInParent = getLocationInParent(conditional);
//    String eol = utils.getEndOfLine();
//    String indent = utils.getIndent(1);
//    String prefix = utils.getNodePrefix(statement);
//    // Type v = Conditional;
//    if (locationInParent == DART_VARIABLE_VALUE) {
//      DartVariable variable = (DartVariable) conditional.getParent();
//      addRemoveEdit(SourceRangeFactory.forEndEnd(variable.getName(), conditional));
//      addReplaceEdit(SourceRangeFactory.forEndLength(statement, 0), MessageFormat.format(
//          "{3}{4}if ({0}) '{'{3}{4}{5}{6} = {1};{3}{4}'} else {'{3}{4}{5}{6} = {2};{3}{4}'}'",
//          getSource(conditional.getCondition()),
//          getSource(conditional.getThenExpression()),
//          getSource(conditional.getElseExpression()),
//          eol,
//          prefix,
//          indent,
//          variable.getVariableName()));
//    }
//    // v = Conditional;
//    if (locationInParent == DART_BINARY_EXPRESSION_RIGHT_OPERAND
//        && conditional.getParent() instanceof DartBinaryExpression) {
//      DartBinaryExpression binaryExpression = (DartBinaryExpression) conditional.getParent();
//      if (binaryExpression.getOperator() == Token.ASSIGN) {
//        DartExpression leftSide = binaryExpression.getArg1();
//        addReplaceEdit(SourceRangeFactory.create(statement), MessageFormat.format(
//            "if ({0}) '{'{3}{4}{5}{6} = {1};{3}{4}'} else {'{3}{4}{5}{6} = {2};{3}{4}'}'",
//            getSource(conditional.getCondition()),
//            getSource(conditional.getThenExpression()),
//            getSource(conditional.getElseExpression()),
//            eol,
//            prefix,
//            indent,
//            getSource(leftSide)));
//      }
//    }
//    // return Conditional;
//    if (locationInParent == DART_RETURN_STATEMENT_VALUE) {
//      addReplaceEdit(SourceRangeFactory.create(statement), MessageFormat.format(
//          "if ({0}) '{'{3}{4}{5}return {1};{3}{4}'} else {'{3}{4}{5}return {2};{3}{4}'}'",
//          getSource(conditional.getCondition()),
//          getSource(conditional.getThenExpression()),
//          getSource(conditional.getElseExpression()),
//          eol,
//          prefix,
//          indent));
//    }
//    // add proposal
//    addUnitCorrectionProposal(
//        CorrectionMessages.QuickAssistProcessor_replaceConditionalWithIfElse,
//        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//  }

  // TODO(scheglov) implement later
//  void addProposal_replaceIfElseWithConditional() throws Exception {
//    // should be "if"
//    if (!(node instanceof DartIfStatement)) {
//      return;
//    }
//    DartIfStatement ifStatement = (DartIfStatement) node;
//    // single then/else statements
//    DartStatement thenStatement = ASTNodes.getSingleStatement(ifStatement.getThenStatement());
//    DartStatement elseStatement = ASTNodes.getSingleStatement(ifStatement.getElseStatement());
//    if (thenStatement == null || elseStatement == null) {
//      return;
//    }
//    // returns
//    if (thenStatement instanceof DartReturnStatement
//        || elseStatement instanceof DartReturnStatement) {
//      DartReturnStatement thenReturn = (DartReturnStatement) thenStatement;
//      DartReturnStatement elseReturn = (DartReturnStatement) elseStatement;
//      addReplaceEdit(SourceRangeFactory.create(ifStatement), MessageFormat.format(
//          "return {0} ? {1} : {2};",
//          getSource(ifStatement.getCondition()),
//          getSource(thenReturn.getValue()),
//          getSource(elseReturn.getValue())));
//    }
//    // assignments -> v = Conditional;
//    if (thenStatement instanceof DartExprStmt && elseStatement instanceof DartExprStmt) {
//      DartExpression thenExpression = ((DartExprStmt) thenStatement).getExpression();
//      DartExpression elseExpression = ((DartExprStmt) elseStatement).getExpression();
//      if (thenExpression instanceof DartBinaryExpression
//          && elseExpression instanceof DartBinaryExpression) {
//        DartBinaryExpression thenBinary = (DartBinaryExpression) thenExpression;
//        DartBinaryExpression elseBinary = (DartBinaryExpression) elseExpression;
//        String thenTarget = getSource(thenBinary.getArg1());
//        String elseTarget = getSource(elseBinary.getArg1());
//        if (thenBinary.getOperator() == Token.ASSIGN && elseBinary.getOperator() == Token.ASSIGN
//            && StringUtils.equals(thenTarget, elseTarget)) {
//          addReplaceEdit(
//              SourceRangeFactory.create(ifStatement),
//              MessageFormat.format(
//                  "{0} = {1} ? {2} : {3};",
//                  thenTarget,
//                  getSource(ifStatement.getCondition()),
//                  getSource(thenBinary.getArg2()),
//                  getSource(elseBinary.getArg2())));
//        }
//      }
//    }
//    // add proposal
//    addUnitCorrectionProposal(
//        CorrectionMessages.QuickAssistProcessor_replaceIfElseWithConditional,
//        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//  }

  // TODO(scheglov) implement later
//  void addProposal_splitAndCondition() throws Exception {
//    // check that user invokes quick assist on binary expression
//    if (!(node instanceof DartBinaryExpression)) {
//      return;
//    }
//    DartBinaryExpression binaryExpression = (DartBinaryExpression) node;
//    // prepare operator position
//    int offset = isOperatorSelected(binaryExpression, selectionOffset, selectionLength);
//    if (offset == -1) {
//      return;
//    }
//    // prepare "if"
//    DartStatement statement = ASTNodes.getAncestor(node, DartStatement.class);
//    if (!(statement instanceof DartIfStatement)) {
//      return;
//    }
//    DartIfStatement ifStatement = (DartIfStatement) statement;
//    // check that binary expression is part of first level && condition of "if"
//    DartBinaryExpression condition = binaryExpression;
//    while (condition.getParent() instanceof DartBinaryExpression
//        && ((DartBinaryExpression) condition.getParent()).getOperator() == Token.AND) {
//      condition = (DartBinaryExpression) condition.getParent();
//    }
//    if (ifStatement.getCondition() != condition) {
//      return;
//    }
//    // prepare environment
//    String prefix = utils.getNodePrefix(ifStatement);
//    String eol = utils.getEndOfLine();
//    String indent = utils.getIndent(1);
//    // prepare "rightCondition"
//    String rightConditionSource;
//    {
//      SourceRange rightConditionRange = SourceRangeFactory.forStartEnd(
//          binaryExpression.getArg2(),
//          condition);
//      rightConditionSource = getSource(rightConditionRange);
//    }
//    // remove "&& rightCondition"
//    addRemoveEdit(SourceRangeFactory.forEndEnd(binaryExpression.getArg1(), condition));
//    // update "then" statement
//    DartStatement thenStatement = ifStatement.getThenStatement();
//    DartStatement elseStatement = ifStatement.getElseStatement();
//    if (thenStatement instanceof DartBlock) {
//      DartBlock thenBlock = (DartBlock) thenStatement;
//      SourceRange thenBlockRange = SourceRangeFactory.create(thenBlock);
//      // insert inner "if" with right part of "condition"
//      {
//        String source = eol + prefix + indent + "if (" + rightConditionSource + ") {";
//        int thenBlockInsideOffset = thenBlockRange.getOffset() + 1;
//        addInsertEdit(thenBlockInsideOffset, source);
//      }
//      // insert closing "}" for inner "if"
//      {
//        int thenBlockEnd = SourceRangeUtils.getEnd(thenBlockRange);
//        String source = indent + "}";
//        // may be move "else" statements
//        if (elseStatement != null) {
//          List<DartStatement> elseStatements = ASTNodes.getStatements(elseStatement);
//          SourceRange elseLinesRange = utils.getLinesRange(elseStatements);
//          String elseIndentOld = prefix + indent;
//          String elseIndentNew = elseIndentOld + indent;
//          String newElseSource = utils.getIndentSource(elseLinesRange, elseIndentOld, elseIndentNew);
//          // append "else" block
//          source += " else {" + eol;
//          source += newElseSource;
//          source += prefix + indent + "}";
//          // remove old "else" range
//          addRemoveEdit(SourceRangeFactory.forStartEnd(thenBlockEnd, elseStatement));
//        }
//        // insert before outer "then" block "}"
//        source += eol + prefix;
//        addInsertEdit(thenBlockEnd - 1, source);
//      }
//    } else {
//      // insert inner "if" with right part of "condition"
//      {
//        String source = eol + prefix + indent + "if (" + rightConditionSource + ")";
//        addInsertEdit(ifStatement.getCloseParenOffset() + 1, source);
//      }
//      // indent "else" statements to correspond inner "if"
//      if (elseStatement != null) {
//        SourceRange elseRange = SourceRangeFactory.forStartEnd(
//            ifStatement.getElseTokenOffset(),
//            elseStatement);
//        SourceRange elseLinesRange = utils.getLinesRange(elseRange);
//        String elseIndentOld = prefix;
//        String elseIndentNew = elseIndentOld + indent;
//        textEdits.add(utils.createIndentEdit(elseLinesRange, elseIndentOld, elseIndentNew));
//      }
//    }
//    // indent "then" statements to correspond inner "if"
//    {
//      List<DartStatement> thenStatements = ASTNodes.getStatements(thenStatement);
//      SourceRange linesRange = utils.getLinesRange(thenStatements);
//      String thenIndentOld = prefix + indent;
//      String thenIndentNew = thenIndentOld + indent;
//      textEdits.add(utils.createIndentEdit(linesRange, thenIndentOld, thenIndentNew));
//    }
//    // add proposal
//    addUnitCorrectionProposal(
//        CorrectionMessages.QuickAssistProcessor_splitAndCondition,
//        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//  }

  void addProposal_splitVariableDeclaration() throws Exception {
    // prepare DartVariableStatement, should be part of Block
    VariableDeclarationStatement statement = node.getAncestor(VariableDeclarationStatement.class);
    if (statement != null && statement.getParent() instanceof Block) {
    } else {
      return;
    }
    // check that statement declares single variable
    List<VariableDeclaration> variables = statement.getVariables().getVariables();
    if (variables.size() != 1) {
      return;
    }
    VariableDeclaration variable = variables.get(0);
    // remove initializer value
    addRemoveEdit(rangeEndStart(variable.getName(), statement.getSemicolon()));
    // add assignment statement
    String eol = utils.getEndOfLine();
    String indent = utils.getNodePrefix(statement);
    String assignSource = MessageFormat.format(
        "{0} = {1};",
        variable.getName().getName(),
        utils.getText(variable.getInitializer()));
    SourceRange assignRange = rangeEndLength(statement, 0);
    addReplaceEdit(assignRange, eol + indent + assignSource);
    // add proposal
    addUnitCorrectionProposal("Split variable declaration", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  // TODO(scheglov) implement later
//  void addProposal_surroundWith() throws CoreException {
//    // prepare selected statements
//    List<DartStatement> selectedStatements;
//    {
//      Selection selection = Selection.createFromStartLength(selectionOffset, selectionLength);
//      StatementAnalyzer selectionAnalyzer = new StatementAnalyzer(unit, selection, false);
//      unitNode.accept(selectionAnalyzer);
//      DartNode[] selectedNodes = selectionAnalyzer.getSelectedNodes();
//      // convert nodes to statements
//      selectedStatements = Lists.newArrayList();
//      for (DartNode selectedNode : selectedNodes) {
//        if (selectedNode instanceof DartStatement) {
//          selectedStatements.add((DartStatement) selectedNode);
//        }
//      }
//      // we want only statements
//      if (selectedStatements.isEmpty() || selectedStatements.size() != selectedNodes.length) {
//        return;
//      }
//    }
//    // prepare statement information
//    DartStatement firstStatement = selectedStatements.get(0);
//    DartStatement lastStatement = selectedStatements.get(selectedStatements.size() - 1);
//    SourceRange statementsRange = utils.getLinesRange(selectedStatements);
//    // prepare environment
//    String eol = utils.getEndOfLine();
//    String indentOld = utils.getNodePrefix(firstStatement);
//    String indentNew = indentOld + utils.getIndent(1);
//    // "block"
//    {
//      addInsertEdit(statementsRange.getOffset(), indentOld + "{" + eol);
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
//      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_block,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//    // "if"
//    {
//      {
//        int offset = statementsRange.getOffset();
//        SourceBuilder sb = new SourceBuilder(offset);
//        sb.append(indentOld);
//        sb.append("if (");
//        {
//          sb.startPosition("CONDITION");
//          sb.append("condition");
//          sb.endPosition();
//        }
//        sb.append(") {");
//        sb.append(eol);
//        addInsertEdit(sb);
//      }
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
//      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_if,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//    // "while"
//    {
//      {
//        int offset = statementsRange.getOffset();
//        SourceBuilder sb = new SourceBuilder(offset);
//        sb.append(indentOld);
//        sb.append("while (");
//        {
//          sb.startPosition("CONDITION");
//          sb.append("condition");
//          sb.endPosition();
//        }
//        sb.append(") {");
//        sb.append(eol);
//        addInsertEdit(sb);
//      }
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
//      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_while,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//    // "for-in"
//    {
//      {
//        int offset = statementsRange.getOffset();
//        SourceBuilder sb = new SourceBuilder(offset);
//        sb.append(indentOld);
//        sb.append("for (var ");
//        {
//          sb.startPosition("NAME");
//          sb.append("item");
//          sb.endPosition();
//        }
//        sb.append(" in ");
//        {
//          sb.startPosition("ITERABLE");
//          sb.append("iterable");
//          sb.endPosition();
//        }
//        sb.append(") {");
//        sb.append(eol);
//        addInsertEdit(sb);
//      }
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
//      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_forIn,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//    // "for"
//    {
//      {
//        int offset = statementsRange.getOffset();
//        SourceBuilder sb = new SourceBuilder(offset);
//        sb.append(indentOld);
//        sb.append("for (var ");
//        {
//          sb.startPosition("VAR");
//          sb.append("v");
//          sb.endPosition();
//        }
//        sb.append(" = ");
//        {
//          sb.startPosition("INIT");
//          sb.append("init");
//          sb.endPosition();
//        }
//        sb.append("; ");
//        {
//          sb.startPosition("CONDITION");
//          sb.append("condition");
//          sb.endPosition();
//        }
//        sb.append("; ");
//        {
//          sb.startPosition("INCREMENT");
//          sb.append("increment");
//          sb.endPosition();
//        }
//        sb.append(") {");
//        sb.append(eol);
//        addInsertEdit(sb);
//      }
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      addInsertEdit(SourceRangeUtils.getEnd(statementsRange), indentOld + "}" + eol);
//      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_for,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//    // "do-while"
//    {
//      addInsertEdit(statementsRange.getOffset(), indentOld + "do {" + eol);
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      {
//        int offset = SourceRangeUtils.getEnd(statementsRange);
//        SourceBuilder sb = new SourceBuilder(offset);
//        sb.append(indentOld);
//        sb.append("} while (");
//        {
//          sb.startPosition("CONDITION");
//          sb.append("condition");
//          sb.endPosition();
//        }
//        sb.append(");");
//        sb.append(eol);
//        addInsertEdit(sb);
//      }
//      proposalEndRange = SourceRangeFactory.forEndLength(lastStatement, 0);
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_doWhile,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//    // "try-catch"
//    {
//      addInsertEdit(statementsRange.getOffset(), indentOld + "try {" + eol);
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      {
//        int offset = SourceRangeUtils.getEnd(statementsRange);
//        SourceBuilder sb = new SourceBuilder(offset);
//        sb.append(indentOld);
//        sb.append("} on ");
//        {
//          sb.startPosition("EXCEPTION_TYPE");
//          sb.append("Exception");
//          sb.endPosition();
//        }
//        sb.append(" catch (");
//        {
//          sb.startPosition("EXCEPTION_VAR");
//          sb.append("e");
//          sb.endPosition();
//        }
//        sb.append(") {");
//        sb.append(eol);
//        //
//        sb.append(indentNew);
//        {
//          sb.startPosition("CATCH");
//          sb.append("// TODO");
//          sb.endPosition();
//          sb.setEndPosition();
//        }
//        sb.append(eol);
//        //
//        sb.append(indentOld);
//        sb.append("}");
//        sb.append(eol);
//        //
//        addInsertEdit(sb);
//      }
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_tryCatch,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//    // "try-finally"
//    {
//      addInsertEdit(statementsRange.getOffset(), indentOld + "try {" + eol);
//      {
//        ReplaceEdit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
//        textEdits.add(edit);
//      }
//      {
//        int offset = SourceRangeUtils.getEnd(statementsRange);
//        SourceBuilder sb = new SourceBuilder(offset);
//        //
//        sb.append(indentOld);
//        sb.append("} finally {");
//        sb.append(eol);
//        //
//        sb.append(indentNew);
//        {
//          sb.startPosition("FINALLY");
//          sb.append("// TODO");
//          sb.endPosition();
//        }
//        sb.setEndPosition();
//        sb.append(eol);
//        //
//        sb.append(indentOld);
//        sb.append("}");
//        sb.append(eol);
//        //
//        addInsertEdit(sb);
//      }
//      // add proposal
//      addUnitCorrectionProposal(
//          CorrectionMessages.QuickAssistProcessor_surroundWith_tryFinally,
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//  }

  /**
   * Adds {@link Edit} which removes source in the given {@link SourceRange}.
   */
  private void addRemoveEdit(SourceRange range) {
    addReplaceEdit(range, "");
  }

  /**
   * Adds {@link Edit} to {@link #textEdits}.
   */
  private void addReplaceEdit(SourceRange range, String text) {
    textEdits.add(createReplaceEdit(range, text));
  }

  /**
   * Adds {@link CorrectionProposal} with single {@link SourceChange} to {@link #proposals}.
   */
  private void addUnitCorrectionProposal(String name, CorrectionImage image) {
    CorrectionProposal proposal = new CorrectionProposal(image, name, proposalRelevance);
    SourceChange change = new SourceChange(source);
    for (Edit edit : textEdits) {
      change.addEdit(edit);
    }
    proposal.addChange(change);
    proposals.add(proposal);
  }

  // TODO(scheglov) add more reset operations
  private void resetProposalElements() {
    textEdits.clear();
    proposalRelevance = DEFAULT_RELEVANCE;
//    linkedPositions.clear();
//    positionStopEdits.clear();
//    linkedPositionProposals.clear();
//    proposal = null;
//    proposalEndRange = null;
  }
}
