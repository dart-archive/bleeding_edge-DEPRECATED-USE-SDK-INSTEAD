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
import com.google.common.collect.Maps;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.services.internal.util.ExecutionUtils;
import com.google.dart.engine.services.internal.util.RunnableEx;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndLength;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeToken;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeWithBase;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation of {@link QuickAssistProcessor}.
 */
public class QuickAssistProcessorImpl implements QuickAssistProcessor {
  private static final CorrectionProposal[] NO_PROPOSALS = {};
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
  private final Map<SourceRange, Edit> positionStopEdits = Maps.newHashMap();
  private final Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
  private final Map<String, List<LinkedPositionProposal>> linkedPositionProposals = Maps.newHashMap();
  private SourceRange proposalEndRange = null;

  @Override
  public CorrectionProposal[] getProposals(AssistContext context) throws Exception {
    if (context == null) {
      return NO_PROPOSALS;
    }
    proposals.clear();
    source = context.getSource();
    unit = context.getCompilationUnit();
    node = context.getCoveringNode();
    selectionOffset = context.getSelectionOffset();
    selectionLength = context.getSelectionLength();
    utils = new CorrectionUtils(context.getCompilationUnit());
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
    return proposals.toArray(new CorrectionProposal[proposals.size()]);
  }

  void addProposal_addTypeAnnotation() throws Exception {
    // prepare VariableDeclarationList
    VariableDeclarationList declarationList = node.getAncestor(VariableDeclarationList.class);
    if (declarationList == null) {
      return;
    }
    // prepare single VariableDeclaration
    List<VariableDeclaration> variables = declarationList.getVariables();
    if (variables.size() != 1) {
      return;
    }
    VariableDeclaration variable = variables.get(0);
    // we need variable to get type from
    Expression value = variable.getInitializer();
    if (value == null) {
      return;
    }
    Type type = value.getStaticType();
    // check type
    if (type == null || type == DynamicTypeImpl.getInstance()
        || type == BottomTypeImpl.getInstance()) {
      return;
    }
    // add edit
    {
      String typeSource = utils.getTypeSource(type);
      // find "var" token
      KeywordToken varToken;
      {
        SourceRange modifiersRange = rangeStartEnd(declarationList, variable);
        String modifiersSource = utils.getText(modifiersRange);
        List<com.google.dart.engine.scanner.Token> tokens = TokenUtils.getTokens(modifiersSource);
        varToken = TokenUtils.findKeywordToken(tokens, Keyword.VAR);
      }
      // replace "var", or insert type before name
      if (varToken != null) {
        SourceRange range = rangeToken(varToken);
        range = rangeWithBase(declarationList, range);
        addReplaceEdit(range, typeSource);
      } else {
        SourceRange range = rangeStartLength(variable, 0);
        addReplaceEdit(range, typeSource + " ");
      }
    }
    // add proposal
    addUnitCorrectionProposal("Add type annotation", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

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
    FunctionBody body = getEnclosingFunctionBody();
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
    String newBodySource = "{" + eol + prefix + indent + "return " + getSource(returnValue) + ";"
        + eol + prefix + "}";
    addReplaceEdit(rangeNode(body), newBodySource);
    // add proposal
    addUnitCorrectionProposal("Convert to block body", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  void addProposal_convertToExpressionFunctionBody() throws Exception {
    // prepare current body
    FunctionBody body = getEnclosingFunctionBody();
    if (!(body instanceof BlockFunctionBody)) {
      return;
    }
    // prepare return statement
    List<Statement> statements = ((BlockFunctionBody) body).getBlock().getStatements();
    if (statements.size() != 1) {
      return;
    }
    if (!(statements.get(0) instanceof ReturnStatement)) {
      return;
    }
    ReturnStatement returnStatement = (ReturnStatement) statements.get(0);
    // prepare returned expression
    Expression returnExpression = returnStatement.getExpression();
    if (returnExpression == null) {
      return;
    }
    // add change
    String newBodySource = "=> " + getSource(returnExpression);
    if (!(body.getParent() instanceof FunctionExpression)
        || body.getParent().getParent() instanceof FunctionDeclaration) {
      newBodySource += ";";
    }
    addReplaceEdit(rangeNode(body), newBodySource);
    // add proposal
    addUnitCorrectionProposal(
        "Convert into using function with expression body",
        CorrectionImage.IMG_CORRECTION_CHANGE);
  }

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
      addReplaceEdit(leftRange, getSource(rightRange));
      addReplaceEdit(rightRange, getSource(leftRange));
    }
    // add proposal
    addUnitCorrectionProposal("Exchange operands", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  void addProposal_joinVariableDeclaration_onAssignment() throws Exception {
    // check that node is LHS in assignment
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
    Element element = ((SimpleIdentifier) node).getElement();
    if (element == null) {
      return;
    }
    int declOffset = element.getNameOffset();
    ASTNode declNode = new NodeLocator(declOffset).searchWithin(unit);
    if (declNode != null && declNode.getParent() instanceof VariableDeclaration
        && ((VariableDeclaration) declNode.getParent()).getName() == declNode
        && declNode.getParent().getParent() instanceof VariableDeclarationList
        && declNode.getParent().getParent().getParent() instanceof VariableDeclarationStatement) {
    } else {
      return;
    }
    VariableDeclaration decl = (VariableDeclaration) declNode.getParent();
    VariableDeclarationStatement declStatement = (VariableDeclarationStatement) decl.getParent().getParent();
    // may be has initializer
    if (decl.getInitializer() != null) {
      return;
    }
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

  void addProposal_joinVariableDeclaration_onDeclaration() throws Exception {
    // prepare enclosing VariableDeclarationList
    VariableDeclarationList declList = node.getAncestor(VariableDeclarationList.class);
    if (declList != null && declList.getVariables().size() == 1) {
    } else {
      return;
    }
    VariableDeclaration decl = declList.getVariables().get(0);
    // already initialized
    if (decl.getInitializer() != null) {
      return;
    }
    // prepare VariableDeclarationStatement in Block
    if (declList.getParent() instanceof VariableDeclarationStatement
        && declList.getParent().getParent() instanceof Block) {
    } else {
      return;
    }
    VariableDeclarationStatement declStatement = (VariableDeclarationStatement) declList.getParent();
    Block block = (Block) declStatement.getParent();
    List<Statement> statements = block.getStatements();
    // prepare assignment
    AssignmentExpression assignExpression;
    {
      // declaration should not be last Statement
      int declIndex = statements.indexOf(declStatement);
      if (declIndex < statements.size() - 1) {
      } else {
        return;
      }
      // next Statement should be assignment
      Statement assignStatement = statements.get(declIndex + 1);
      if (assignStatement instanceof ExpressionStatement) {
      } else {
        return;
      }
      ExpressionStatement expressionStatement = (ExpressionStatement) assignStatement;
      // expression should be assignment
      if (expressionStatement.getExpression() instanceof AssignmentExpression) {
      } else {
        return;
      }
      assignExpression = (AssignmentExpression) expressionStatement.getExpression();
    }
    // check that pure assignment
    if (assignExpression.getOperator().getType() != TokenType.EQ) {
      return;
    }
    // add edits
    {
      int assignOffset = assignExpression.getOperator().getOffset();
      addReplaceEdit(rangeEndStart(decl.getName(), assignOffset), " ");
    }
    // add proposal
    addUnitCorrectionProposal("Join variable declaration", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  void addProposal_removeTypeAnnotation() throws Exception {
    ASTNode typeStart = null;
    ASTNode typeEnd = null;
    // try top-level variable
    {
      TopLevelVariableDeclaration declaration = node.getAncestor(TopLevelVariableDeclaration.class);
      if (declaration != null) {
        TypeName typeNode = declaration.getVariables().getType();
        if (typeNode != null) {
          VariableDeclaration field = declaration.getVariables().getVariables().get(0);
          typeStart = declaration;
          typeEnd = field;
        }
      }
    }
    // try class field
    {
      FieldDeclaration fieldDeclaration = node.getAncestor(FieldDeclaration.class);
      if (fieldDeclaration != null) {
        TypeName typeNode = fieldDeclaration.getFields().getType();
        if (typeNode != null) {
          VariableDeclaration field = fieldDeclaration.getFields().getVariables().get(0);
          typeStart = fieldDeclaration;
          typeEnd = field;
        }
      }
    }
    // try local variable
    {
      VariableDeclarationStatement statement = node.getAncestor(VariableDeclarationStatement.class);
      if (statement != null) {
        TypeName typeNode = statement.getVariables().getType();
        if (typeNode != null) {
          VariableDeclaration variable = statement.getVariables().getVariables().get(0);
          typeStart = typeNode;
          typeEnd = variable;
        }
      }
    }
    // add edit
    if (typeStart != null && typeEnd != null) {
      SourceRange typeRange = rangeStartStart(typeStart, typeEnd);
      addReplaceEdit(typeRange, "var ");
    }
    // add proposal
    proposalRelevance -= 1;
    addUnitCorrectionProposal("Remove type annotation", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  void addProposal_replaceConditionalWithIfElse() throws Exception {
    // try to find Conditional under cursor
//    ConditionalExpression conditional = node.getAncestor(ConditionalExpression.class);
    ConditionalExpression conditional = null;
//    {
//      ASTNode currentNode = node;
//      while (currentNode instanceof Expression) {
//        if (currentNode instanceof ConditionalExpression) {
//          conditional = (ConditionalExpression) currentNode;
//          break;
//        }
//        currentNode = currentNode.getParent();
//      }
//    }
    // if no Conditional, may be on Statement with Conditional
    Statement statement = node.getAncestor(Statement.class);
    // variable declaration
    boolean inVariable = false;
    if (statement instanceof VariableDeclarationStatement) {
      VariableDeclarationStatement variableStatement = (VariableDeclarationStatement) statement;
      for (VariableDeclaration variable : variableStatement.getVariables().getVariables()) {
        if (variable.getInitializer() instanceof ConditionalExpression) {
          conditional = (ConditionalExpression) variable.getInitializer();
          inVariable = true;
          break;
        }
      }
    }
    // assignment
    boolean inAssignment = false;
    if (statement instanceof ExpressionStatement) {
      ExpressionStatement exprStmt = (ExpressionStatement) statement;
      if (exprStmt.getExpression() instanceof AssignmentExpression) {
        AssignmentExpression assignment = (AssignmentExpression) exprStmt.getExpression();
        if (assignment.getOperator().getType() == TokenType.EQ
            && assignment.getRightHandSide() instanceof ConditionalExpression) {
          conditional = (ConditionalExpression) assignment.getRightHandSide();
          inAssignment = true;
        }
      }
    }
    // return
    boolean inReturn = false;
    if (statement instanceof ReturnStatement) {
      ReturnStatement returnStatement = (ReturnStatement) statement;
      if (returnStatement.getExpression() instanceof ConditionalExpression) {
        conditional = (ConditionalExpression) returnStatement.getExpression();
        inReturn = true;
      }
    }
    // prepare environment
    String eol = utils.getEndOfLine();
    String indent = utils.getIndent(1);
    String prefix = utils.getNodePrefix(statement);
    // Type v = Conditional;
    if (inVariable) {
      VariableDeclaration variable = (VariableDeclaration) conditional.getParent();
      addRemoveEdit(rangeEndEnd(variable.getName(), conditional));
      addReplaceEdit(rangeEndLength(statement, 0), MessageFormat.format(
          "{3}{4}if ({0}) '{'{3}{4}{5}{6} = {1};{3}{4}'} else {'{3}{4}{5}{6} = {2};{3}{4}'}'",
          getSource(conditional.getCondition()),
          getSource(conditional.getThenExpression()),
          getSource(conditional.getElseExpression()),
          eol,
          prefix,
          indent,
          variable.getName()));
    }
    // v = Conditional;
    if (inAssignment) {
      AssignmentExpression assignment = (AssignmentExpression) conditional.getParent();
      Expression leftSide = assignment.getLeftHandSide();
      addReplaceEdit(rangeNode(statement), MessageFormat.format(
          "if ({0}) '{'{3}{4}{5}{6} = {1};{3}{4}'} else {'{3}{4}{5}{6} = {2};{3}{4}'}'",
          getSource(conditional.getCondition()),
          getSource(conditional.getThenExpression()),
          getSource(conditional.getElseExpression()),
          eol,
          prefix,
          indent,
          getSource(leftSide)));
    }
    // return Conditional;
    if (inReturn) {
      addReplaceEdit(rangeNode(statement), MessageFormat.format(
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
        "Replace conditional with 'if-else'",
        CorrectionImage.IMG_CORRECTION_CHANGE);
  }

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

  void addProposal_replaceIfElseWithConditional() throws Exception {
    // should be "if"
    if (!(node instanceof IfStatement)) {
      return;
    }
    IfStatement ifStatement = (IfStatement) node;
    // single then/else statements
    Statement thenStatement = CorrectionUtils.getSingleStatement(ifStatement.getThenStatement());
    Statement elseStatement = CorrectionUtils.getSingleStatement(ifStatement.getElseStatement());
    if (thenStatement == null || elseStatement == null) {
      return;
    }
    // returns
    if (thenStatement instanceof ReturnStatement || elseStatement instanceof ReturnStatement) {
      ReturnStatement thenReturn = (ReturnStatement) thenStatement;
      ReturnStatement elseReturn = (ReturnStatement) elseStatement;
      addReplaceEdit(rangeNode(ifStatement), MessageFormat.format(
          "return {0} ? {1} : {2};",
          getSource(ifStatement.getCondition()),
          getSource(thenReturn.getExpression()),
          getSource(elseReturn.getExpression())));
    }
    // assignments -> v = Conditional;
    if (thenStatement instanceof ExpressionStatement
        && elseStatement instanceof ExpressionStatement) {
      Expression thenExpression = ((ExpressionStatement) thenStatement).getExpression();
      Expression elseExpression = ((ExpressionStatement) elseStatement).getExpression();
      if (thenExpression instanceof AssignmentExpression
          && elseExpression instanceof AssignmentExpression) {
        AssignmentExpression thenAssignment = (AssignmentExpression) thenExpression;
        AssignmentExpression elseAssignment = (AssignmentExpression) elseExpression;
        String thenTarget = getSource(thenAssignment.getLeftHandSide());
        String elseTarget = getSource(elseAssignment.getLeftHandSide());
        if (thenAssignment.getOperator().getType() == TokenType.EQ
            && elseAssignment.getOperator().getType() == TokenType.EQ
            && StringUtils.equals(thenTarget, elseTarget)) {
          addReplaceEdit(
              rangeNode(ifStatement),
              MessageFormat.format(
                  "{0} = {1} ? {2} : {3};",
                  thenTarget,
                  getSource(ifStatement.getCondition()),
                  getSource(thenAssignment.getRightHandSide()),
                  getSource(elseAssignment.getRightHandSide())));
        }
      }
    }
    // add proposal
    addUnitCorrectionProposal(
        "Replace 'if-else' with conditional ('c ? x : y')",
        CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  void addProposal_splitAndCondition() throws Exception {
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
    // prepare "if"
    Statement statement = node.getAncestor(Statement.class);
    if (!(statement instanceof IfStatement)) {
      return;
    }
    IfStatement ifStatement = (IfStatement) statement;
    // check that binary expression is part of first level && condition of "if"
    BinaryExpression condition = binaryExpression;
    while (condition.getParent() instanceof BinaryExpression
        && ((BinaryExpression) condition.getParent()).getOperator().getType() == TokenType.AMPERSAND_AMPERSAND) {
      condition = (BinaryExpression) condition.getParent();
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
      SourceRange rightConditionRange = rangeStartEnd(binaryExpression.getRightOperand(), condition);
      rightConditionSource = getSource(rightConditionRange);
    }
    // remove "&& rightCondition"
    addRemoveEdit(rangeEndEnd(binaryExpression.getLeftOperand(), condition));
    // update "then" statement
    Statement thenStatement = ifStatement.getThenStatement();
    Statement elseStatement = ifStatement.getElseStatement();
    if (thenStatement instanceof Block) {
      Block thenBlock = (Block) thenStatement;
      SourceRange thenBlockRange = rangeNode(thenBlock);
      // insert inner "if" with right part of "condition"
      {
        String source = eol + prefix + indent + "if (" + rightConditionSource + ") {";
        int thenBlockInsideOffset = thenBlockRange.getOffset() + 1;
        addInsertEdit(thenBlockInsideOffset, source);
      }
      // insert closing "}" for inner "if"
      {
        int thenBlockEnd = thenBlockRange.getEnd();
        String source = indent + "}";
        // may be move "else" statements
        if (elseStatement != null) {
          List<Statement> elseStatements = CorrectionUtils.getStatements(elseStatement);
          SourceRange elseLinesRange = utils.getLinesRange(elseStatements);
          String elseIndentOld = prefix + indent;
          String elseIndentNew = elseIndentOld + indent;
          String newElseSource = utils.getIndentSource(elseLinesRange, elseIndentOld, elseIndentNew);
          // append "else" block
          source += " else {" + eol;
          source += newElseSource;
          source += prefix + indent + "}";
          // remove old "else" range
          addRemoveEdit(rangeStartEnd(thenBlockEnd, elseStatement));
        }
        // insert before outer "then" block "}"
        source += eol + prefix;
        addInsertEdit(thenBlockEnd - 1, source);
      }
    } else {
      // insert inner "if" with right part of "condition"
      {
        String source = eol + prefix + indent + "if (" + rightConditionSource + ")";
        addInsertEdit(ifStatement.getRightParenthesis().getOffset() + 1, source);
      }
      // indent "else" statements to correspond inner "if"
      if (elseStatement != null) {
        SourceRange elseRange = rangeStartEnd(
            ifStatement.getElseKeyword().getOffset(),
            elseStatement);
        SourceRange elseLinesRange = utils.getLinesRange(elseRange);
        String elseIndentOld = prefix;
        String elseIndentNew = elseIndentOld + indent;
        textEdits.add(utils.createIndentEdit(elseLinesRange, elseIndentOld, elseIndentNew));
      }
    }
    // indent "then" statements to correspond inner "if"
    {
      List<Statement> thenStatements = CorrectionUtils.getStatements(thenStatement);
      SourceRange linesRange = utils.getLinesRange(thenStatements);
      String thenIndentOld = prefix + indent;
      String thenIndentNew = thenIndentOld + indent;
      textEdits.add(utils.createIndentEdit(linesRange, thenIndentOld, thenIndentNew));
    }
    // add proposal
    addUnitCorrectionProposal("Split && condition", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

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
        getSource(variable.getInitializer()));
    SourceRange assignRange = rangeEndLength(statement, 0);
    addReplaceEdit(assignRange, eol + indent + assignSource);
    // add proposal
    addUnitCorrectionProposal("Split variable declaration", CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  void addProposal_surroundWith() throws Exception {
    // prepare selected statements
    List<Statement> selectedStatements;
    {
      SourceRange selection = rangeStartLength(selectionOffset, selectionLength);
      StatementAnalyzer selectionAnalyzer = new StatementAnalyzer(unit, selection);
      unit.accept(selectionAnalyzer);
      List<ASTNode> selectedNodes = selectionAnalyzer.getSelectedNodes();
      // convert nodes to statements
      selectedStatements = Lists.newArrayList();
      for (ASTNode selectedNode : selectedNodes) {
        if (selectedNode instanceof Statement) {
          selectedStatements.add((Statement) selectedNode);
        }
      }
      // we want only statements
      if (selectedStatements.isEmpty() || selectedStatements.size() != selectedNodes.size()) {
        return;
      }
    }
    // prepare statement information
    Statement firstStatement = selectedStatements.get(0);
    Statement lastStatement = selectedStatements.get(selectedStatements.size() - 1);
    SourceRange statementsRange = utils.getLinesRange(selectedStatements);
    // prepare environment
    String eol = utils.getEndOfLine();
    String indentOld = utils.getNodePrefix(firstStatement);
    String indentNew = indentOld + utils.getIndent(1);
    // "block"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "{" + eol);
      {
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(statementsRange.getEnd(), indentOld + "}" + eol);
      proposalEndRange = rangeEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal("Surround with block", CorrectionImage.IMG_CORRECTION_CHANGE);
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
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(statementsRange.getEnd(), indentOld + "}" + eol);
      proposalEndRange = rangeEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal("Surround with 'if'", CorrectionImage.IMG_CORRECTION_CHANGE);
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
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(statementsRange.getEnd(), indentOld + "}" + eol);
      proposalEndRange = rangeEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal("Surround with 'while'", CorrectionImage.IMG_CORRECTION_CHANGE);
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
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(statementsRange.getEnd(), indentOld + "}" + eol);
      proposalEndRange = rangeEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal("Surround with 'for-in'", CorrectionImage.IMG_CORRECTION_CHANGE);
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
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      addInsertEdit(statementsRange.getEnd(), indentOld + "}" + eol);
      proposalEndRange = rangeEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal("Surround with 'for'", CorrectionImage.IMG_CORRECTION_CHANGE);
    }
    // "do-while"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "do {" + eol);
      {
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      {
        int offset = statementsRange.getEnd();
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
      proposalEndRange = rangeEndLength(lastStatement, 0);
      // add proposal
      addUnitCorrectionProposal("Surround with 'do-while'", CorrectionImage.IMG_CORRECTION_CHANGE);
    }
    // "try-catch"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "try {" + eol);
      {
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      {
        int offset = statementsRange.getEnd();
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
      addUnitCorrectionProposal("Surround with 'try-catch'", CorrectionImage.IMG_CORRECTION_CHANGE);
    }
    // "try-finally"
    {
      addInsertEdit(statementsRange.getOffset(), indentOld + "try {" + eol);
      {
        Edit edit = utils.createIndentEdit(statementsRange, indentOld, indentNew);
        textEdits.add(edit);
      }
      {
        int offset = statementsRange.getEnd();
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
          "Surround with 'try-finally'",
          CorrectionImage.IMG_CORRECTION_CHANGE);
    }
  }

  private void addInsertEdit(int offset, String text) {
    textEdits.add(createInsertEdit(offset, text));
  }

  private void addInsertEdit(SourceBuilder builder) {
    int offset = builder.getOffset();
    Edit edit = createInsertEdit(offset, builder.toString());
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

//  private void addLinkedPositionProposal(String group, CorrectionImage icon, String text) {
//    List<TrackedNodeProposal> nodeProposals = linkedPositionProposals.get(group);
//    if (nodeProposals == null) {
//      nodeProposals = Lists.newArrayList();
//      linkedPositionProposals.put(group, nodeProposals);
//    }
//    nodeProposals.add(new TrackedNodeProposal(icon, text));
//  }

  /**
   * Adds positions from the given {@link SourceBuilder} to the {@link #linkedPositions}.
   */
  private void addLinkedPositions(SourceBuilder builder, Edit edit) {
    // end position
    {
      int endPosition = builder.getEndPosition();
      if (endPosition != -1) {
        proposalEndRange = rangeStartLength(endPosition, 0);
        positionStopEdits.put(proposalEndRange, edit);
      }
    }
    // positions
    Map<String, List<SourceRange>> builderPositions = builder.getLinkedPositions();
    for (Entry<String, List<SourceRange>> entry : builderPositions.entrySet()) {
      String groupId = entry.getKey();
      for (SourceRange position : entry.getValue()) {
        addLinkedPosition(groupId, position);
        positionStopEdits.put(position, edit);
      }
    }
    // proposals for positions
//    Map<String, List<TrackedNodeProposal>> builderProposals = builder.getTrackedProposals();
//    for (Entry<String, List<TrackedNodeProposal>> entry : builderProposals.entrySet()) {
//      String groupId = entry.getKey();
//      for (TrackedNodeProposal nodeProposal : entry.getValue()) {
//        addLinkedPositionProposal(groupId, nodeProposal.getIcon(), nodeProposal.getText());
//      }
//    }
  }

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
    if (!textEdits.isEmpty()) {
      CorrectionProposal proposal = new CorrectionProposal(image, name, proposalRelevance);
      proposal.setLinkedPositions(linkedPositions);
      proposal.setLinkedPositionProposals(linkedPositionProposals);
      // add change
      SourceChange change = new SourceChange(name, source);
      for (Edit edit : textEdits) {
        change.addEdit(edit);
      }
      proposal.addChange(change);
      // done
      proposals.add(proposal);
    }
    // reset
    resetProposalElements();
  }

  private Edit createInsertEdit(int offset, String text) {
    return new Edit(offset, 0, text);
  }

  private FunctionBody getEnclosingFunctionBody() {
    {
      FunctionExpression function = node.getAncestor(FunctionExpression.class);
      if (function != null) {
        return function.getBody();
      }
    }
    {
      FunctionDeclaration function = node.getAncestor(FunctionDeclaration.class);
      if (function != null) {
        return function.getFunctionExpression().getBody();
      }
    }
    {
      MethodDeclaration method = node.getAncestor(MethodDeclaration.class);
      if (method != null) {
        return method.getBody();
      }
    }
    return null;
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(ASTNode node) {
    return utils.getText(node);
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(SourceRange range) {
    return utils.getText(range);
  }

  private void resetProposalElements() {
    textEdits.clear();
    proposalRelevance = DEFAULT_RELEVANCE;
    linkedPositions.clear();
    positionStopEdits.clear();
    linkedPositionProposals.clear();
    proposalEndRange = null;
  }
}
