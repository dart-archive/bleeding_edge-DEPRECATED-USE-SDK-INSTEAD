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
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
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
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenClass;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.CreateFileChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.ChangeCorrectionProposal;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.LinkedPositionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.internal.correction.CorrectionUtils.InsertDesc;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.ast.ScopedNameFinder;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.services.correction.CorrectionKind.QA_ADD_TYPE_ANNOTATION;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_ASSIGN_TO_LOCAL_VARIABLE;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_CONVERT_INTO_BLOCK_BODY;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_CONVERT_INTO_EXPRESSION_BODY;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_CONVERT_INTO_IS_NOT;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_CONVERT_INTO_IS_NOT_EMPTY;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_EXCHANGE_OPERANDS;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_EXTRACT_CLASS;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_IMPORT_ADD_SHOW;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_INVERT_IF_STATEMENT;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_JOIN_IF_WITH_INNER;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_JOIN_IF_WITH_OUTER;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_JOIN_VARIABLE_DECLARATION;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_REMOVE_TYPE_ANNOTATION;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_REPLACE_CONDITIONAL_WITH_IF_ELSE;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_REPLACE_IF_ELSE_WITH_CONDITIONAL;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SPLIT_AND_CONDITION;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SPLIT_VARIABLE_DECLARATION;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_BLOCK;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_DO_WHILE;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_FOR;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_FOR_IN;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_IF;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_TRY_CATCH;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_TRY_FINALLY;
import static com.google.dart.engine.services.correction.CorrectionKind.QA_SURROUND_WITH_WHILE;
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

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implementation of {@link QuickAssistProcessor}.
 */
public class QuickAssistProcessorImpl implements QuickAssistProcessor {
  private static final CorrectionProposal[] NO_PROPOSALS = {};

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
    AstNode left = binaryExpression.getLeftOperand();
    AstNode right = binaryExpression.getRightOperand();
    if (isSelectingOperator(left, right, offset, length)) {
      return left.getEndToken().getEnd();
    }
    return -1;
  }

  private static boolean isSelectingOperator(AstNode n1, AstNode n2, int offset, int length) {
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

  /**
   * Checks if given {@link Expression} should be wrapped with parenthesis when we want to use it as
   * operand of logical "and" expression.
   */
  private static boolean shouldWrapParenthesisBeforeAnd(Expression expr) {
    if (expr instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) expr;
      int precedence = binary.getOperator().getType().getPrecedence();
      return precedence < TokenClass.LOGICAL_AND_OPERATOR.getPrecedence();
    }
    return false;
  }

  private final List<CorrectionProposal> proposals = Lists.newArrayList();
  private final List<Edit> textEdits = Lists.newArrayList();

  private AssistContext assistContext;
  private AnalysisContext analysisContext;
  private Source source;
  private CompilationUnit unit;
  private AstNode node;

  private Source unitLibrarySource;
  private LibraryElement unitLibraryElement;
  private File unitLibraryFile;
  private File unitLibraryFolder;
  private int selectionOffset;
  private int selectionLength;
  private CorrectionUtils utils;

  private final Map<SourceRange, Edit> positionStopEdits = Maps.newHashMap();

  private final Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();

  private final Map<String, List<LinkedPositionProposal>> linkedPositionProposals = Maps.newHashMap();

  private SourceRange proposalEndRange = null;

  @Override
  public CorrectionProposal[] getProposals(AnalysisContext analysisContext, Source unitSource,
      CompilationUnit parsedUnit, int offset) throws Exception {
    this.analysisContext = analysisContext;
    this.source = unitSource;
    proposals.clear();
    // prepare node
    node = new NodeLocator(offset).searchWithin(parsedUnit);
    if (node == null) {
      return NO_PROPOSALS;
    }
    // call proposal methods
    addUnresolvedProposal_addPart();
    // done
    return proposals.toArray(new CorrectionProposal[proposals.size()]);
  }

  @Override
  public CorrectionProposal[] getProposals(AssistContext context) throws Exception {
    if (context == null) {
      return NO_PROPOSALS;
    }
    assistContext = context;
    proposals.clear();
    source = context.getSource();
    unit = context.getCompilationUnit();
    node = context.getCoveringNode();
    selectionOffset = context.getSelectionOffset();
    selectionLength = context.getSelectionLength();
    utils = new CorrectionUtils(context.getCompilationUnit());
    // prepare elements
    {
      CompilationUnitElement unitElement = unit.getElement();
      if (unitElement == null) {
        return NO_PROPOSALS;
      }
      unitLibraryElement = unitElement.getLibrary();
      if (unitLibraryElement == null) {
        return NO_PROPOSALS;
      }
      unitLibrarySource = unitLibraryElement.getSource();
      unitLibraryFile = QuickFixProcessorImpl.getSourceFile(unitLibrarySource);
      if (unitLibraryFile == null) {
        return NO_PROPOSALS;
      }
      unitLibraryFolder = unitLibraryFile.getParentFile();
    }
    this.analysisContext = unitLibraryElement.getContext();
    // run with instrumentation
    final InstrumentationBuilder instrumentation = Instrumentation.builder(this.getClass());
    try {
      for (Method method : QuickAssistProcessorImpl.class.getDeclaredMethods()) {
        if (method.getName().startsWith("addProposal_")) {
          resetProposalElements();
          try {
            method.invoke(QuickAssistProcessorImpl.this);
          } catch (Throwable e) {
            instrumentation.record(e);
          }
        }
      }
      instrumentation.metric("QuickAssist-Offset", selectionOffset);
      instrumentation.metric("QuickAssist-Length", selectionLength);
      instrumentation.metric("QuickAssist-ProposalCount", proposals.size());
      instrumentation.data("QuickAssist-Source", utils.getText());
      for (int index = 0; index < proposals.size(); index++) {
        instrumentation.data("QuickAssist-Proposal-" + index, proposals.get(index).getName());
      }
      return proposals.toArray(new CorrectionProposal[proposals.size()]);
    } finally {
      instrumentation.log();
    }
  }

  void addProposal_addTypeAnnotation() throws Exception {
    // prepare VariableDeclarationList
    VariableDeclarationList declarationList = node.getAncestor(VariableDeclarationList.class);
    if (declarationList == null) {
      return;
    }
    // may be has type annotation already
    if (declarationList.getType() != null) {
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
    // prepare Type source
    String typeSource;
    if (type instanceof InterfaceType) {
      typeSource = utils.getTypeSource(type);
    } else if (type instanceof FunctionType) {
      typeSource = "Function";
    } else {
      return;
    }
    // add edit
    {
      // find "var" token
      KeywordToken varToken;
      {
        SourceRange modifiersRange = rangeStartEnd(declarationList, variable);
        String modifiersSource = utils.getText(modifiersRange);
        List<Token> tokens = TokenUtils.getTokens(modifiersSource);
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
    addUnitCorrectionProposal(QA_ADD_TYPE_ANNOTATION);
  }

  void addProposal_assignToLocalVariable() throws Exception {
    // prepare enclosing ExpressionStatement
    Statement statement = node.getAncestor(Statement.class);
    if (!(statement instanceof ExpressionStatement)) {
      return;
    }
    ExpressionStatement expressionStatement = (ExpressionStatement) statement;
    // prepare expression
    Expression expression = expressionStatement.getExpression();
    int offset = expression.getOffset();
    // ignore if already assignment
    if (expression instanceof AssignmentExpression) {
      return;
    }
    // ignore "throw"
    if (expression instanceof ThrowExpression) {
      return;
    }
    // prepare expression type
    Type type = expression.getBestType();
    if (type.isVoid()) {
      return;
    }
    // prepare source
    SourceBuilder builder = new SourceBuilder(offset);
    builder.append("var ");
    // prepare excluded names
    Set<String> excluded = Sets.newHashSet();
    {
      ScopedNameFinder scopedNameFinder = new ScopedNameFinder(offset);
      expression.accept(scopedNameFinder);
      excluded.addAll(scopedNameFinder.getLocals().keySet());
    }
    // name(s)
    {
      String[] suggestions = CorrectionUtils.getVariableNameSuggestions(type, expression, excluded);
      builder.startPosition("NAME");
      for (int i = 0; i < suggestions.length; i++) {
        String name = suggestions[i];
        if (i == 0) {
          builder.append(name);
        }
        builder.addProposal(CorrectionImage.IMG_CORRECTION_CLASS, name);
      }
      builder.endPosition();
    }
    builder.append(" = ");
    // add proposal
    addInsertEdit(builder);
    addUnitCorrectionProposal(QA_ASSIGN_TO_LOCAL_VARIABLE);
  }

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
      AstNode bodyParent = body.getParent();
      prefix = utils.getNodePrefix(bodyParent);
    }
    // add change
    String eol = utils.getEndOfLine();
    String indent = utils.getIndent(1);
    String newBodySource = "{" + eol + prefix + indent + "return " + getSource(returnValue) + ";"
        + eol + prefix + "}";
    addReplaceEdit(rangeNode(body), newBodySource);
    // add proposal
    addUnitCorrectionProposal(QA_CONVERT_INTO_BLOCK_BODY);
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
    addUnitCorrectionProposal(QA_CONVERT_INTO_EXPRESSION_BODY);
  }

  void addProposal_convertToIsNot_onIs() throws Exception {
    // may be child of "is"
    AstNode node = this.node;
    while (node != null && !(node instanceof IsExpression)) {
      node = node.getParent();
    }
    // prepare "is"
    if (!(node instanceof IsExpression)) {
      return;
    }
    IsExpression isExpression = (IsExpression) node;
    if (isExpression.getNotOperator() != null) {
      return;
    }
    // prepare enclosing ()
    AstNode parent = isExpression.getParent();
    if (!(parent instanceof ParenthesizedExpression)) {
      return;
    }
    ParenthesizedExpression parExpression = (ParenthesizedExpression) parent;
    // prepare enclosing !()
    AstNode parent2 = parent.getParent();
    if (!(parent2 instanceof PrefixExpression)) {
      return;
    }
    PrefixExpression prefExpression = (PrefixExpression) parent2;
    if (prefExpression.getOperator().getType() != TokenType.BANG) {
      return;
    }
    // strip !()
    if (CorrectionUtils.getParentPrecedence(prefExpression) >= TokenType.IS.getPrecedence()) {
      addRemoveEdit(rangeToken(prefExpression.getOperator()));
    } else {
      addRemoveEdit(rangeStartEnd(prefExpression, parExpression.getLeftParenthesis()));
      addRemoveEdit(rangeStartEnd(parExpression.getRightParenthesis(), prefExpression));
    }
    addInsertEdit(isExpression.getIsOperator().getEnd(), "!");
    // add proposal
    addUnitCorrectionProposal(QA_CONVERT_INTO_IS_NOT);
  }

  void addProposal_convertToIsNot_onNot() throws Exception {
    // may be () in prefix expression
    if (node instanceof ParenthesizedExpression && node.getParent() instanceof PrefixExpression) {
      node = node.getParent();
    }
    // prepare !()
    if (!(node instanceof PrefixExpression)) {
      return;
    }
    PrefixExpression prefExpression = (PrefixExpression) node;
    // should be ! operator
    if (prefExpression.getOperator().getType() != TokenType.BANG) {
      return;
    }
    // prepare !()
    Expression operand = prefExpression.getOperand();
    if (!(operand instanceof ParenthesizedExpression)) {
      return;
    }
    ParenthesizedExpression parExpression = (ParenthesizedExpression) operand;
    operand = parExpression.getExpression();
    // prepare "is"
    if (!(operand instanceof IsExpression)) {
      return;
    }
    IsExpression isExpression = (IsExpression) operand;
    if (isExpression.getNotOperator() != null) {
      return;
    }
    // strip !()
    if (CorrectionUtils.getParentPrecedence(prefExpression) >= TokenType.IS.getPrecedence()) {
      addRemoveEdit(rangeToken(prefExpression.getOperator()));
    } else {
      addRemoveEdit(rangeStartEnd(prefExpression, parExpression.getLeftParenthesis()));
      addRemoveEdit(rangeStartEnd(parExpression.getRightParenthesis(), prefExpression));
    }
    addInsertEdit(isExpression.getIsOperator().getEnd(), "!");
    // add proposal
    addUnitCorrectionProposal(QA_CONVERT_INTO_IS_NOT);
  }

  /**
   * Converts "!isEmpty" -> "isNotEmpty" if possible.
   */
  void addProposal_convertToIsNotEmpty() throws Exception {
    // prepare "expr.isEmpty"
    AstNode isEmptyAccess = null;
    SimpleIdentifier isEmptyIdentifier = null;
    if (node instanceof SimpleIdentifier) {
      SimpleIdentifier identifier = (SimpleIdentifier) node;
      AstNode parent = identifier.getParent();
      // normal case (but rare)
      if (parent instanceof PropertyAccess) {
        PropertyAccess propertyAccess = (PropertyAccess) parent;
        isEmptyIdentifier = propertyAccess.getPropertyName();
        isEmptyAccess = propertyAccess;
      }
      // usual case
      if (parent instanceof PrefixedIdentifier) {
        PrefixedIdentifier prefixedIdentifier = (PrefixedIdentifier) parent;
        isEmptyIdentifier = prefixedIdentifier.getIdentifier();
        isEmptyAccess = prefixedIdentifier;
      }
    }
    if (isEmptyIdentifier == null) {
      return;
    }
    // should be "isEmpty"
    Element propertyElement = isEmptyIdentifier.getBestElement();
    if (propertyElement == null || !"isEmpty".equals(propertyElement.getName())) {
      return;
    }
    // should have "isNotEmpty"
    Element propertyTarget = propertyElement.getEnclosingElement();
    if (propertyTarget == null
        || CorrectionUtils.getChildren(propertyTarget, "isNotEmpty").isEmpty()) {
      return;
    }
    // should be in PrefixExpression
    if (!(isEmptyAccess.getParent() instanceof PrefixExpression)) {
      return;
    }
    PrefixExpression prefixExpression = (PrefixExpression) isEmptyAccess.getParent();
    // should be !
    if (prefixExpression.getOperator().getType() != TokenType.BANG) {
      return;
    }
    // do replace
    addRemoveEdit(rangeStartStart(prefixExpression, prefixExpression.getOperand()));
    addReplaceEdit(rangeNode(isEmptyIdentifier), "isNotEmpty");
    // add proposal
    addUnitCorrectionProposal(QA_CONVERT_INTO_IS_NOT_EMPTY);
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
    addUnitCorrectionProposal(QA_EXCHANGE_OPERANDS);
  }

  void addProposal_extractClassIntoPart() throws Exception {
    // should be on the name
    if (!(node instanceof SimpleIdentifier)) {
      return;
    }
    if (!(node.getParent() instanceof ClassDeclaration)) {
      return;
    }
    ClassDeclaration classDeclaration = (ClassDeclaration) node.getParent();
    SourceRange linesRange = utils.getLinesRange(rangeNode(classDeclaration));
    // prepare name
    String className = classDeclaration.getName().getName();
    String fileName = CorrectionUtils.getRecommentedFileNameForClass(className);
    // prepare new file
    File newFile = new File(unitLibraryFolder, fileName);
    if (newFile.exists()) {
      return;
    }
    // remove class from this unit
    SourceChange unitChange = new SourceChange(source.getShortName(), source);
    unitChange.addEdit(new Edit(linesRange, ""));
    // create new unit
    Change createFileChange;
    {
      String newContent = "part of " + unitLibraryElement.getDisplayName() + ";";
      newContent += utils.getEndOfLine();
      newContent += utils.getEndOfLine();
      newContent += getSource(linesRange);
      createFileChange = new CreateFileChange(fileName, newFile, newContent);
    }
    // add 'part'
    SourceChange libraryChange = getInsertPartDirectiveChange(unitLibrarySource, fileName);
    // add proposal
    Change compositeChange = new CompositeChange("", unitChange, createFileChange, libraryChange);
    proposals.add(new ChangeCorrectionProposal(compositeChange, QA_EXTRACT_CLASS, fileName));
  }

  void addProposal_importAddShow() throws Exception {
    // prepare ImportDirective
    ImportDirective importDirective = node.getAncestor(ImportDirective.class);
    if (importDirective == null) {
      return;
    }
    // there should be no existing combinators
    if (!importDirective.getCombinators().isEmpty()) {
      return;
    }
    // prepare whole import namespace
    ImportElement importElement = importDirective.getElement();
    Map<String, Element> namespace = CorrectionUtils.getImportNamespace(importElement);
    // prepare names of referenced elements (from this import)
    Set<String> referencedNames = Sets.newTreeSet();
    SearchEngine searchEngine = assistContext.getSearchEngine();
    for (Element element : namespace.values()) {
      List<SearchMatch> references = searchEngine.searchReferences(element, null, null);
      for (SearchMatch match : references) {
        LibraryElement library = match.getElement().getLibrary();
        if (unitLibraryElement.equals(library)) {
          referencedNames.add(element.getDisplayName());
          break;
        }
      }
    }
    // ignore if unused
    if (referencedNames.isEmpty()) {
      return;
    }
    // prepare change
    String sb = " show " + StringUtils.join(referencedNames, ", ");
    addInsertEdit(importDirective.getEnd() - 1, sb.toString());
    // add proposal
    addUnitCorrectionProposal(QA_IMPORT_ADD_SHOW);
  }

  void addProposal_invertIf() throws Exception {
    if (!(node instanceof IfStatement)) {
      return;
    }
    IfStatement ifStatement = (IfStatement) node;
    Expression condition = ifStatement.getCondition();
    // should have both "then" and "else"
    Statement thenStatement = ifStatement.getThenStatement();
    Statement elseStatement = ifStatement.getElseStatement();
    if (thenStatement == null || elseStatement == null) {
      return;
    }
    // prepare source
    String invertedCondition = utils.invertCondition(condition);
    String thenSource = getSource(thenStatement);
    String elseSource = getSource(elseStatement);
    // do replacements
    addReplaceEdit(rangeNode(condition), invertedCondition);
    addReplaceEdit(rangeNode(thenStatement), elseSource);
    addReplaceEdit(rangeNode(elseStatement), thenSource);
    // add proposal
    addUnitCorrectionProposal(QA_INVERT_IF_STATEMENT);
  }

  void addProposal_joinIfStatementInner() throws Exception {
    // climb up condition to the (supposedly) "if" statement
    AstNode node = this.node;
    while (node instanceof Expression) {
      node = node.getParent();
    }
    // prepare target "if" statement
    if (!(node instanceof IfStatement)) {
      return;
    }
    IfStatement targetIfStatement = (IfStatement) node;
    if (targetIfStatement.getElseStatement() != null) {
      return;
    }
    // prepare inner "if" statement
    Statement targetThenStatement = targetIfStatement.getThenStatement();
    Statement innerStatement = CorrectionUtils.getSingleStatement(targetThenStatement);
    if (!(innerStatement instanceof IfStatement)) {
      return;
    }
    IfStatement innerIfStatement = (IfStatement) innerStatement;
    if (innerIfStatement.getElseStatement() != null) {
      return;
    }
    // prepare environment
    String prefix = utils.getNodePrefix(targetIfStatement);
    String eol = utils.getEndOfLine();
    // merge conditions
    String condition;
    {
      Expression targetCondition = targetIfStatement.getCondition();
      Expression innerCondition = innerIfStatement.getCondition();
      String targetConditionSource = getSource(targetCondition);
      String innerConditionSource = getSource(innerCondition);
      if (shouldWrapParenthesisBeforeAnd(targetCondition)) {
        targetConditionSource = "(" + targetConditionSource + ")";
      }
      if (shouldWrapParenthesisBeforeAnd(innerCondition)) {
        innerConditionSource = "(" + innerConditionSource + ")";
      }
      condition = targetConditionSource + " && " + innerConditionSource;
    }
    // replace target "if" statement
    {
      Statement innerThenStatement = innerIfStatement.getThenStatement();
      List<Statement> innerThenStatements = CorrectionUtils.getStatements(innerThenStatement);
      SourceRange lineRanges = utils.getLinesRange(innerThenStatements);
      String oldSource = utils.getText(lineRanges);
      String newSource = utils.getIndentSource(oldSource, false);
      addReplaceEdit(
          rangeNode(targetIfStatement),
          MessageFormat.format("if ({0}) '{'{1}{2}{3}'}'", condition, eol, newSource, prefix));
    }
    // done
    addUnitCorrectionProposal(QA_JOIN_IF_WITH_INNER);
  }

  void addProposal_joinIfStatementOuter() throws Exception {
    // climb up condition to the (supposedly) "if" statement
    AstNode node = this.node;
    while (node instanceof Expression) {
      node = node.getParent();
    }
    // prepare target "if" statement
    if (!(node instanceof IfStatement)) {
      return;
    }
    IfStatement targetIfStatement = (IfStatement) node;
    if (targetIfStatement.getElseStatement() != null) {
      return;
    }
    // prepare outer "if" statement
    AstNode parent = targetIfStatement.getParent();
    if (parent instanceof Block) {
      parent = parent.getParent();
    }
    if (!(parent instanceof IfStatement)) {
      return;
    }
    IfStatement outerIfStatement = (IfStatement) parent;
    if (outerIfStatement.getElseStatement() != null) {
      return;
    }
    // prepare environment
    String prefix = utils.getNodePrefix(outerIfStatement);
    String eol = utils.getEndOfLine();
    // merge conditions
    String condition;
    {
      Expression targetCondition = targetIfStatement.getCondition();
      Expression outerCondition = outerIfStatement.getCondition();
      String targetConditionSource = getSource(targetCondition);
      String outerConditionSource = getSource(outerCondition);
      if (shouldWrapParenthesisBeforeAnd(targetCondition)) {
        targetConditionSource = "(" + targetConditionSource + ")";
      }
      if (shouldWrapParenthesisBeforeAnd(outerCondition)) {
        outerConditionSource = "(" + outerConditionSource + ")";
      }
      condition = outerConditionSource + " && " + targetConditionSource;
    }
    // replace outer "if" statement
    {
      Statement targetThenStatement = targetIfStatement.getThenStatement();
      List<Statement> targetThenStatements = CorrectionUtils.getStatements(targetThenStatement);
      SourceRange lineRanges = utils.getLinesRange(targetThenStatements);
      String oldSource = utils.getText(lineRanges);
      String newSource = utils.getIndentSource(oldSource, false);
      addReplaceEdit(
          rangeNode(outerIfStatement),
          MessageFormat.format("if ({0}) '{'{1}{2}{3}'}'", condition, eol, newSource, prefix));
    }
    // done
    addUnitCorrectionProposal(QA_JOIN_IF_WITH_OUTER);
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
    Element element = ((SimpleIdentifier) node).getStaticElement();
    if (element == null) {
      return;
    }
    int declOffset = element.getNameOffset();
    AstNode declNode = new NodeLocator(declOffset).searchWithin(unit);
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
    addUnitCorrectionProposal(QA_JOIN_VARIABLE_DECLARATION);
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
    addUnitCorrectionProposal(QA_JOIN_VARIABLE_DECLARATION);
  }

  void addProposal_removeTypeAnnotation() throws Exception {
    AstNode typeStart = null;
    AstNode typeEnd = null;
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
    addUnitCorrectionProposal(QA_REMOVE_TYPE_ANNOTATION);
  }

  void addProposal_replaceConditionalWithIfElse() throws Exception {
    ConditionalExpression conditional = null;
    // may be on Statement with Conditional
    Statement statement = node.getAncestor(Statement.class);
    if (statement == null) {
      return;
    }
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
    addUnitCorrectionProposal(QA_REPLACE_CONDITIONAL_WITH_IF_ELSE);
  }

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
    addUnitCorrectionProposal(QA_REPLACE_IF_ELSE_WITH_CONDITIONAL);
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
    // should be &&
    if (binaryExpression.getOperator().getType() != TokenType.AMPERSAND_AMPERSAND) {
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
    addUnitCorrectionProposal(QA_SPLIT_AND_CONDITION);
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
    addUnitCorrectionProposal(QA_SPLIT_VARIABLE_DECLARATION);
  }

//  private void addLinkedPositionProposal(String group, CorrectionImage icon, String text) {
//    List<TrackedNodeProposal> nodeProposals = linkedPositionProposals.get(group);
//    if (nodeProposals == null) {
//      nodeProposals = Lists.newArrayList();
//      linkedPositionProposals.put(group, nodeProposals);
//    }
//    nodeProposals.add(new TrackedNodeProposal(icon, text));
//  }

  void addProposal_surroundWith() throws Exception {
    // prepare selected statements
    List<Statement> selectedStatements;
    {
      SourceRange selection = rangeStartLength(selectionOffset, selectionLength);
      StatementAnalyzer selectionAnalyzer = new StatementAnalyzer(unit, selection);
      unit.accept(selectionAnalyzer);
      List<AstNode> selectedNodes = selectionAnalyzer.getSelectedNodes();
      // convert nodes to statements
      selectedStatements = Lists.newArrayList();
      for (AstNode selectedNode : selectedNodes) {
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_BLOCK);
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_IF);
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_WHILE);
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_FOR_IN);
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_FOR);
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_DO_WHILE);
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_TRY_CATCH);
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
      addUnitCorrectionProposal(QA_SURROUND_WITH_TRY_FINALLY);
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

  private void addLinkedPositionProposal(String group, LinkedPositionProposal proposal) {
    List<LinkedPositionProposal> nodeProposals = linkedPositionProposals.get(group);
    if (nodeProposals == null) {
      nodeProposals = Lists.newArrayList();
      linkedPositionProposals.put(group, nodeProposals);
    }
    nodeProposals.add(proposal);
  }

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
    for (Entry<String, List<LinkedPositionProposal>> entry : builder.getLinkedProposals().entrySet()) {
      String group = entry.getKey();
      for (LinkedPositionProposal proposal : entry.getValue()) {
        addLinkedPositionProposal(group, proposal);
      }
    }
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
  private void addUnitCorrectionProposal(CorrectionKind kind, Object... arguments) {
    if (!textEdits.isEmpty()) {
      // prepare SourceChange
      SourceChange change = new SourceChange(source.getShortName(), source);
      for (Edit edit : textEdits) {
        change.addEdit(edit);
      }
      // create SourceCorrectionProposal
      SourceCorrectionProposal proposal = new SourceCorrectionProposal(change, kind, arguments);
      proposal.setLinkedPositions(linkedPositions);
      proposal.setLinkedPositionProposals(linkedPositionProposals);
      // done
      proposals.add(proposal);
    }
    // reset
    resetProposalElements();
  }

  private void addUnresolvedProposal_addPart() throws Exception {
    // should be PartOfDirective selected
    PartOfDirective partOfDirective = node.getAncestor(PartOfDirective.class);
    if (partOfDirective == null) {
      return;
    }
    LibraryIdentifier partOfNameIdentifier = partOfDirective.getLibraryName();
    if (partOfNameIdentifier == null) {
      return;
    }
    String requiredLibraryName = partOfNameIdentifier.toString();
    // prepare unit File
    File unitFile = QuickFixProcessorImpl.getSourceFile(source);
    if (unitFile == null) {
      return;
    }
    // check all libraries
    Source[] librarySources = analysisContext.getLibrarySources();
    for (Source librarySource : librarySources) {
      LibraryElement libraryElement = analysisContext.getLibraryElement(librarySource);
      if (StringUtils.equals(libraryElement.getName(), requiredLibraryName)) {
        // prepare library File
        File libraryFile = QuickFixProcessorImpl.getSourceFile(librarySource);
        if (libraryFile == null) {
          continue;
        }
        // prepare relative URI
        URI libraryFolderUri = libraryFile.getParentFile().toURI();
        URI unitUri = unitFile.toURI();
        String relative = libraryFolderUri.relativize(unitUri).getPath();
        SourceChange change = getInsertPartDirectiveChange(librarySource, relative);
        if (change == null) {
          continue;
        }
        proposals.add(new SourceCorrectionProposal(change, CorrectionKind.QA_ADD_PART_DIRECTIVE));
      }
    }
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
   * @return the {@link SourceChange} to insert "part" directive with given URI into the given
   *         library.
   */
  private SourceChange getInsertPartDirectiveChange(Source librarySource, String uri)
      throws Exception {
    // prepare library CompilationUnit
    CompilationUnit libraryUnit = analysisContext.getResolvedCompilationUnit(
        librarySource,
        librarySource);
    if (libraryUnit == null) {
      return null;
    }
    // prepare location for "part" directive
    utils = new CorrectionUtils(libraryUnit);
    InsertDesc insertDesc = utils.getInsertDescPart();
    // build source to insert
    StringBuilder sb = new StringBuilder();
    sb.append(insertDesc.prefix);
    sb.append("part '");
    sb.append(uri);
    sb.append("';");
    sb.append(insertDesc.suffix);
    // add proposal
    SourceChange change = new SourceChange(librarySource.getShortName(), librarySource);
    change.addEdit(new Edit(insertDesc.offset, 0, sb.toString()));
    return change;
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(AstNode node) {
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
    linkedPositions.clear();
    positionStopEdits.clear();
    linkedPositionProposals.clear();
    proposalEndRange = null;
  }
}
