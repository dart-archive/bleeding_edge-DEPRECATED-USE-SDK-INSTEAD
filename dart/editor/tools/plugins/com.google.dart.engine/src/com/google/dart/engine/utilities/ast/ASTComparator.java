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
package com.google.dart.engine.utilities.ast;

import com.google.dart.engine.ast.*;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code ASTComparator} compare the structure of two ASTNodes to see whether
 * they are equal.
 */
public class ASTComparator implements ASTVisitor<Boolean> {
  /**
   * Return {@code true} if the two AST nodes are equal.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the two AST nodes are equal
   */
  public static boolean equals(CompilationUnit first, CompilationUnit second) {
    ASTComparator comparator = new ASTComparator();
    return comparator.isEqual(first, second);
  }

  /**
   * The AST node with which the node being visited is to be compared. This is only valid at the
   * beginning of each visit method (until {@link #isEqual(ASTNode, ASTNode)} is invoked).
   */
  private ASTNode other;

  @Override
  public Boolean visitAdjacentStrings(AdjacentStrings node) {
    AdjacentStrings other = (AdjacentStrings) this.other;
    return isEqual(node.getStrings(), other.getStrings());
  }

  @Override
  public Boolean visitAnnotation(Annotation node) {
    Annotation other = (Annotation) this.other;
    return isEqual(node.getAtSign(), other.getAtSign()) && isEqual(node.getName(), other.getName())
        && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getConstructorName(), other.getConstructorName())
        && isEqual(node.getArguments(), other.getArguments());
  }

  @Override
  public Boolean visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    ArgumentDefinitionTest other = (ArgumentDefinitionTest) this.other;
    return isEqual(node.getQuestion(), other.getQuestion())
        && isEqual(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitArgumentList(ArgumentList node) {
    ArgumentList other = (ArgumentList) this.other;
    return isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getArguments(), other.getArguments())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis());
  }

  @Override
  public Boolean visitAsExpression(AsExpression node) {
    AsExpression other = (AsExpression) this.other;
    return isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getAsOperator(), other.getAsOperator())
        && isEqual(node.getType(), other.getType());
  }

  @Override
  public Boolean visitAssertStatement(AssertStatement node) {
    AssertStatement other = (AssertStatement) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getCondition(), other.getCondition())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression other = (AssignmentExpression) this.other;
    return isEqual(node.getLeftHandSide(), other.getLeftHandSide())
        && isEqual(node.getOperator(), other.getOperator())
        && isEqual(node.getRightHandSide(), other.getRightHandSide());
  }

  @Override
  public Boolean visitBinaryExpression(BinaryExpression node) {
    BinaryExpression other = (BinaryExpression) this.other;
    return isEqual(node.getLeftOperand(), other.getLeftOperand())
        && isEqual(node.getOperator(), other.getOperator())
        && isEqual(node.getRightOperand(), other.getRightOperand());
  }

  @Override
  public Boolean visitBlock(Block node) {
    Block other = (Block) this.other;
    return isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getStatements(), other.getStatements())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitBlockFunctionBody(BlockFunctionBody node) {
    BlockFunctionBody other = (BlockFunctionBody) this.other;
    return isEqual(node.getBlock(), other.getBlock());
  }

  @Override
  public Boolean visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral other = (BooleanLiteral) this.other;
    return isEqual(node.getLiteral(), other.getLiteral()) && node.getValue() == other.getValue();
  }

  @Override
  public Boolean visitBreakStatement(BreakStatement node) {
    BreakStatement other = (BreakStatement) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getLabel(), other.getLabel())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitCascadeExpression(CascadeExpression node) {
    CascadeExpression other = (CascadeExpression) this.other;
    return isEqual(node.getTarget(), other.getTarget())
        && isEqual(node.getCascadeSections(), other.getCascadeSections());
  }

  @Override
  public Boolean visitCatchClause(CatchClause node) {
    CatchClause other = (CatchClause) this.other;
    return isEqual(node.getOnKeyword(), other.getOnKeyword())
        && isEqual(node.getExceptionType(), other.getExceptionType())
        && isEqual(node.getCatchKeyword(), other.getCatchKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getExceptionParameter(), other.getExceptionParameter())
        && isEqual(node.getComma(), other.getComma())
        && isEqual(node.getStackTraceParameter(), other.getStackTraceParameter())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration other = (ClassDeclaration) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getAbstractKeyword(), other.getAbstractKeyword())
        && isEqual(node.getClassKeyword(), other.getClassKeyword())
        && isEqual(node.getName(), other.getName())
        && isEqual(node.getTypeParameters(), other.getTypeParameters())
        && isEqual(node.getExtendsClause(), other.getExtendsClause())
        && isEqual(node.getWithClause(), other.getWithClause())
        && isEqual(node.getImplementsClause(), other.getImplementsClause())
        && isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getMembers(), other.getMembers())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitClassTypeAlias(ClassTypeAlias node) {
    ClassTypeAlias other = (ClassTypeAlias) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getName(), other.getName())
        && isEqual(node.getTypeParameters(), other.getTypeParameters())
        && isEqual(node.getEquals(), other.getEquals())
        && isEqual(node.getAbstractKeyword(), other.getAbstractKeyword())
        && isEqual(node.getSuperclass(), other.getSuperclass())
        && isEqual(node.getWithClause(), other.getWithClause())
        && isEqual(node.getImplementsClause(), other.getImplementsClause())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitComment(Comment node) {
    Comment other = (Comment) this.other;
    return isEqual(node.getReferences(), other.getReferences());
  }

  @Override
  public Boolean visitCommentReference(CommentReference node) {
    CommentReference other = (CommentReference) this.other;
    return isEqual(node.getNewKeyword(), other.getNewKeyword())
        && isEqual(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitCompilationUnit(CompilationUnit node) {
    CompilationUnit other = (CompilationUnit) this.other;
    return isEqual(node.getBeginToken(), other.getBeginToken())
        && isEqual(node.getScriptTag(), other.getScriptTag())
        && isEqual(node.getDirectives(), other.getDirectives())
        && isEqual(node.getDeclarations(), other.getDeclarations())
        && isEqual(node.getEndToken(), other.getEndToken());
  }

  @Override
  public Boolean visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression other = (ConditionalExpression) this.other;
    return isEqual(node.getCondition(), other.getCondition())
        && isEqual(node.getQuestion(), other.getQuestion())
        && isEqual(node.getThenExpression(), other.getThenExpression())
        && isEqual(node.getColon(), other.getColon())
        && isEqual(node.getElseExpression(), other.getElseExpression());
  }

  @Override
  public Boolean visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration other = (ConstructorDeclaration) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getExternalKeyword(), other.getExternalKeyword())
        && isEqual(node.getConstKeyword(), other.getConstKeyword())
        && isEqual(node.getFactoryKeyword(), other.getFactoryKeyword())
        && isEqual(node.getReturnType(), other.getReturnType())
        && isEqual(node.getPeriod(), other.getPeriod()) && isEqual(node.getName(), other.getName())
        && isEqual(node.getParameters(), other.getParameters())
        && isEqual(node.getSeparator(), other.getSeparator())
        && isEqual(node.getInitializers(), other.getInitializers())
        && isEqual(node.getRedirectedConstructor(), other.getRedirectedConstructor())
        && isEqual(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    ConstructorFieldInitializer other = (ConstructorFieldInitializer) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getFieldName(), other.getFieldName())
        && isEqual(node.getEquals(), other.getEquals())
        && isEqual(node.getExpression(), other.getExpression());
  }

  @Override
  public Boolean visitConstructorName(ConstructorName node) {
    ConstructorName other = (ConstructorName) this.other;
    return isEqual(node.getType(), other.getType()) && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getName(), other.getName());
  }

  @Override
  public Boolean visitContinueStatement(ContinueStatement node) {
    ContinueStatement other = (ContinueStatement) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getLabel(), other.getLabel())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitDeclaredIdentifier(DeclaredIdentifier node) {
    DeclaredIdentifier other = (DeclaredIdentifier) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getType(), other.getType())
        && isEqual(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitDefaultFormalParameter(DefaultFormalParameter node) {
    DefaultFormalParameter other = (DefaultFormalParameter) this.other;
    return isEqual(node.getParameter(), other.getParameter()) && node.getKind() == other.getKind()
        && isEqual(node.getSeparator(), other.getSeparator())
        && isEqual(node.getDefaultValue(), other.getDefaultValue());
  }

  @Override
  public Boolean visitDoStatement(DoStatement node) {
    DoStatement other = (DoStatement) this.other;
    return isEqual(node.getDoKeyword(), other.getDoKeyword())
        && isEqual(node.getBody(), other.getBody())
        && isEqual(node.getWhileKeyword(), other.getWhileKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getCondition(), other.getCondition())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral other = (DoubleLiteral) this.other;
    return isEqual(node.getLiteral(), other.getLiteral()) && node.getValue() == other.getValue();
  }

  @Override
  public Boolean visitEmptyFunctionBody(EmptyFunctionBody node) {
    EmptyFunctionBody other = (EmptyFunctionBody) this.other;
    return isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitEmptyStatement(EmptyStatement node) {
    EmptyStatement other = (EmptyStatement) this.other;
    return isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitExportDirective(ExportDirective node) {
    ExportDirective other = (ExportDirective) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword()) && isEqual(node.getUri(), other.getUri())
        && isEqual(node.getCombinators(), other.getCombinators())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitExpressionFunctionBody(ExpressionFunctionBody node) {
    ExpressionFunctionBody other = (ExpressionFunctionBody) this.other;
    return isEqual(node.getFunctionDefinition(), other.getFunctionDefinition())
        && isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitExpressionStatement(ExpressionStatement node) {
    ExpressionStatement other = (ExpressionStatement) this.other;
    return isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitExtendsClause(ExtendsClause node) {
    ExtendsClause other = (ExtendsClause) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getSuperclass(), other.getSuperclass());
  }

  @Override
  public Boolean visitFieldDeclaration(FieldDeclaration node) {
    FieldDeclaration other = (FieldDeclaration) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getStaticKeyword(), other.getStaticKeyword())
        && isEqual(node.getFields(), other.getFields())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitFieldFormalParameter(FieldFormalParameter node) {
    FieldFormalParameter other = (FieldFormalParameter) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getType(), other.getType())
        && isEqual(node.getThisToken(), other.getThisToken())
        && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitForEachStatement(ForEachStatement node) {
    ForEachStatement other = (ForEachStatement) this.other;
    return isEqual(node.getForKeyword(), other.getForKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getLoopVariable(), other.getLoopVariable())
        && isEqual(node.getInKeyword(), other.getInKeyword())
        && isEqual(node.getIterator(), other.getIterator())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitFormalParameterList(FormalParameterList node) {
    FormalParameterList other = (FormalParameterList) this.other;
    return isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getParameters(), other.getParameters())
        && isEqual(node.getLeftDelimiter(), other.getLeftDelimiter())
        && isEqual(node.getRightDelimiter(), other.getRightDelimiter())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis());
  }

  @Override
  public Boolean visitForStatement(ForStatement node) {
    ForStatement other = (ForStatement) this.other;
    return isEqual(node.getForKeyword(), other.getForKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getVariables(), other.getVariables())
        && isEqual(node.getInitialization(), other.getInitialization())
        && isEqual(node.getLeftSeparator(), other.getLeftSeparator())
        && isEqual(node.getCondition(), other.getCondition())
        && isEqual(node.getRightSeparator(), other.getRightSeparator())
        && isEqual(node.getUpdaters(), other.getUpdaters())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitFunctionDeclaration(FunctionDeclaration node) {
    FunctionDeclaration other = (FunctionDeclaration) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getExternalKeyword(), other.getExternalKeyword())
        && isEqual(node.getReturnType(), other.getReturnType())
        && isEqual(node.getPropertyKeyword(), other.getPropertyKeyword())
        && isEqual(node.getName(), other.getName())
        && isEqual(node.getFunctionExpression(), other.getFunctionExpression());
  }

  @Override
  public Boolean visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    FunctionDeclarationStatement other = (FunctionDeclarationStatement) this.other;
    return isEqual(node.getFunctionDeclaration(), other.getFunctionDeclaration());
  }

  @Override
  public Boolean visitFunctionExpression(FunctionExpression node) {
    FunctionExpression other = (FunctionExpression) this.other;
    return isEqual(node.getParameters(), other.getParameters())
        && isEqual(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    FunctionExpressionInvocation other = (FunctionExpressionInvocation) this.other;
    return isEqual(node.getFunction(), other.getFunction())
        && isEqual(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitFunctionTypeAlias(FunctionTypeAlias node) {
    FunctionTypeAlias other = (FunctionTypeAlias) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getReturnType(), other.getReturnType())
        && isEqual(node.getName(), other.getName())
        && isEqual(node.getTypeParameters(), other.getTypeParameters())
        && isEqual(node.getParameters(), other.getParameters())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    FunctionTypedFormalParameter other = (FunctionTypedFormalParameter) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getReturnType(), other.getReturnType())
        && isEqual(node.getIdentifier(), other.getIdentifier())
        && isEqual(node.getParameters(), other.getParameters());
  }

  @Override
  public Boolean visitHideCombinator(HideCombinator node) {
    HideCombinator other = (HideCombinator) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getHiddenNames(), other.getHiddenNames());
  }

  @Override
  public Boolean visitIfStatement(IfStatement node) {
    IfStatement other = (IfStatement) this.other;
    return isEqual(node.getIfKeyword(), other.getIfKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getCondition(), other.getCondition())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getThenStatement(), other.getThenStatement())
        && isEqual(node.getElseKeyword(), other.getElseKeyword())
        && isEqual(node.getElseStatement(), other.getElseStatement());
  }

  @Override
  public Boolean visitImplementsClause(ImplementsClause node) {
    ImplementsClause other = (ImplementsClause) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getInterfaces(), other.getInterfaces());
  }

  @Override
  public Boolean visitImportDirective(ImportDirective node) {
    ImportDirective other = (ImportDirective) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword()) && isEqual(node.getUri(), other.getUri())
        && isEqual(node.getAsToken(), other.getAsToken())
        && isEqual(node.getPrefix(), other.getPrefix())
        && isEqual(node.getCombinators(), other.getCombinators())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitIndexExpression(IndexExpression node) {
    IndexExpression other = (IndexExpression) this.other;
    return isEqual(node.getTarget(), other.getTarget())
        && isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getIndex(), other.getIndex())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitInstanceCreationExpression(InstanceCreationExpression node) {
    InstanceCreationExpression other = (InstanceCreationExpression) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getConstructorName(), other.getConstructorName())
        && isEqual(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitIntegerLiteral(IntegerLiteral node) {
    IntegerLiteral other = (IntegerLiteral) this.other;
    return isEqual(node.getLiteral(), other.getLiteral()) && node.getValue() == other.getValue();
  }

  @Override
  public Boolean visitInterpolationExpression(InterpolationExpression node) {
    InterpolationExpression other = (InterpolationExpression) this.other;
    return isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitInterpolationString(InterpolationString node) {
    InterpolationString other = (InterpolationString) this.other;
    return isEqual(node.getContents(), other.getContents())
        && node.getValue().equals(other.getValue());
  }

  @Override
  public Boolean visitIsExpression(IsExpression node) {
    IsExpression other = (IsExpression) this.other;
    return isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getIsOperator(), other.getIsOperator())
        && isEqual(node.getNotOperator(), other.getNotOperator())
        && isEqual(node.getType(), other.getType());
  }

  @Override
  public Boolean visitLabel(Label node) {
    Label other = (Label) this.other;
    return isEqual(node.getLabel(), other.getLabel()) && isEqual(node.getColon(), other.getColon());
  }

  @Override
  public Boolean visitLabeledStatement(LabeledStatement node) {
    LabeledStatement other = (LabeledStatement) this.other;
    return isEqual(node.getLabels(), other.getLabels())
        && isEqual(node.getStatement(), other.getStatement());
  }

  @Override
  public Boolean visitLibraryDirective(LibraryDirective node) {
    LibraryDirective other = (LibraryDirective) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getLibraryToken(), other.getLibraryToken())
        && isEqual(node.getName(), other.getName())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier other = (LibraryIdentifier) this.other;
    return isEqual(node.getComponents(), other.getComponents());
  }

  @Override
  public Boolean visitListLiteral(ListLiteral node) {
    ListLiteral other = (ListLiteral) this.other;
    return isEqual(node.getConstKeyword(), other.getConstKeyword())
        && isEqual(node.getTypeArguments(), other.getTypeArguments())
        && isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getElements(), other.getElements())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitMapLiteral(MapLiteral node) {
    MapLiteral other = (MapLiteral) this.other;
    return isEqual(node.getConstKeyword(), other.getConstKeyword())
        && isEqual(node.getTypeArguments(), other.getTypeArguments())
        && isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getEntries(), other.getEntries())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitMapLiteralEntry(MapLiteralEntry node) {
    MapLiteralEntry other = (MapLiteralEntry) this.other;
    return isEqual(node.getKey(), other.getKey())
        && isEqual(node.getSeparator(), other.getSeparator())
        && isEqual(node.getValue(), other.getValue());
  }

  @Override
  public Boolean visitMethodDeclaration(MethodDeclaration node) {
    MethodDeclaration other = (MethodDeclaration) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getExternalKeyword(), other.getExternalKeyword())
        && isEqual(node.getModifierKeyword(), other.getModifierKeyword())
        && isEqual(node.getReturnType(), other.getReturnType())
        && isEqual(node.getPropertyKeyword(), other.getPropertyKeyword())
        && isEqual(node.getPropertyKeyword(), other.getPropertyKeyword())
        && isEqual(node.getName(), other.getName())
        && isEqual(node.getParameters(), other.getParameters())
        && isEqual(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitMethodInvocation(MethodInvocation node) {
    MethodInvocation other = (MethodInvocation) this.other;
    return isEqual(node.getTarget(), other.getTarget())
        && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getMethodName(), other.getMethodName())
        && isEqual(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitNamedExpression(NamedExpression node) {
    NamedExpression other = (NamedExpression) this.other;
    return isEqual(node.getName(), other.getName())
        && isEqual(node.getExpression(), other.getExpression());
  }

  @Override
  public Boolean visitNativeClause(NativeClause node) {
    NativeClause other = (NativeClause) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getName(), other.getName());
  }

  @Override
  public Boolean visitNativeFunctionBody(NativeFunctionBody node) {
    NativeFunctionBody other = (NativeFunctionBody) this.other;
    return isEqual(node.getNativeToken(), other.getNativeToken())
        && isEqual(node.getStringLiteral(), other.getStringLiteral())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitNullLiteral(NullLiteral node) {
    NullLiteral other = (NullLiteral) this.other;
    return isEqual(node.getLiteral(), other.getLiteral());
  }

  @Override
  public Boolean visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression other = (ParenthesizedExpression) this.other;
    return isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis());
  }

  @Override
  public Boolean visitPartDirective(PartDirective node) {
    PartDirective other = (PartDirective) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getPartToken(), other.getPartToken())
        && isEqual(node.getUri(), other.getUri())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitPartOfDirective(PartOfDirective node) {
    PartOfDirective other = (PartOfDirective) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getPartToken(), other.getPartToken())
        && isEqual(node.getOfToken(), other.getOfToken())
        && isEqual(node.getLibraryName(), other.getLibraryName())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitPostfixExpression(PostfixExpression node) {
    PostfixExpression other = (PostfixExpression) this.other;
    return isEqual(node.getOperand(), other.getOperand())
        && isEqual(node.getOperator(), other.getOperator());
  }

  @Override
  public Boolean visitPrefixedIdentifier(PrefixedIdentifier node) {
    PrefixedIdentifier other = (PrefixedIdentifier) this.other;
    return isEqual(node.getPrefix(), other.getPrefix())
        && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitPrefixExpression(PrefixExpression node) {
    PrefixExpression other = (PrefixExpression) this.other;
    return isEqual(node.getOperator(), other.getOperator())
        && isEqual(node.getOperand(), other.getOperand());
  }

  @Override
  public Boolean visitPropertyAccess(PropertyAccess node) {
    PropertyAccess other = (PropertyAccess) this.other;
    return isEqual(node.getTarget(), other.getTarget())
        && isEqual(node.getOperator(), other.getOperator())
        && isEqual(node.getPropertyName(), other.getPropertyName());
  }

  @Override
  public Boolean visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation other = (RedirectingConstructorInvocation) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getConstructorName(), other.getConstructorName())
        && isEqual(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitRethrowExpression(RethrowExpression node) {
    RethrowExpression other = (RethrowExpression) this.other;
    return isEqual(node.getKeyword(), other.getKeyword());
  }

  @Override
  public Boolean visitReturnStatement(ReturnStatement node) {
    ReturnStatement other = (ReturnStatement) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitScriptTag(ScriptTag node) {
    ScriptTag other = (ScriptTag) this.other;
    return isEqual(node.getScriptTag(), other.getScriptTag());
  }

  @Override
  public Boolean visitShowCombinator(ShowCombinator node) {
    ShowCombinator other = (ShowCombinator) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getShownNames(), other.getShownNames());
  }

  @Override
  public Boolean visitSimpleFormalParameter(SimpleFormalParameter node) {
    SimpleFormalParameter other = (SimpleFormalParameter) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getType(), other.getType())
        && isEqual(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitSimpleIdentifier(SimpleIdentifier node) {
    SimpleIdentifier other = (SimpleIdentifier) this.other;
    return isEqual(node.getToken(), other.getToken());
  }

  @Override
  public Boolean visitSimpleStringLiteral(SimpleStringLiteral node) {
    SimpleStringLiteral other = (SimpleStringLiteral) this.other;
    return isEqual(node.getLiteral(), other.getLiteral()) && node.getValue() == other.getValue();
  }

  @Override
  public Boolean visitStringInterpolation(StringInterpolation node) {
    StringInterpolation other = (StringInterpolation) this.other;
    return isEqual(node.getElements(), other.getElements());
  }

  @Override
  public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation other = (SuperConstructorInvocation) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getPeriod(), other.getPeriod())
        && isEqual(node.getConstructorName(), other.getConstructorName())
        && isEqual(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitSuperExpression(SuperExpression node) {
    SuperExpression other = (SuperExpression) this.other;
    return isEqual(node.getKeyword(), other.getKeyword());
  }

  @Override
  public Boolean visitSwitchCase(SwitchCase node) {
    SwitchCase other = (SwitchCase) this.other;
    return isEqual(node.getLabels(), other.getLabels())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getColon(), other.getColon())
        && isEqual(node.getStatements(), other.getStatements());
  }

  @Override
  public Boolean visitSwitchDefault(SwitchDefault node) {
    SwitchDefault other = (SwitchDefault) this.other;
    return isEqual(node.getLabels(), other.getLabels())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getColon(), other.getColon())
        && isEqual(node.getStatements(), other.getStatements());
  }

  @Override
  public Boolean visitSwitchStatement(SwitchStatement node) {
    SwitchStatement other = (SwitchStatement) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getExpression(), other.getExpression())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getMembers(), other.getMembers())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral other = (SymbolLiteral) this.other;
    return isEqual(node.getPoundSign(), other.getPoundSign())
        && isEqual(node.getComponents(), other.getComponents());
  }

  @Override
  public Boolean visitThisExpression(ThisExpression node) {
    ThisExpression other = (ThisExpression) this.other;
    return isEqual(node.getKeyword(), other.getKeyword());
  }

  @Override
  public Boolean visitThrowExpression(ThrowExpression node) {
    ThrowExpression other = (ThrowExpression) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getExpression(), other.getExpression());
  }

  @Override
  public Boolean visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    TopLevelVariableDeclaration other = (TopLevelVariableDeclaration) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getVariables(), other.getVariables())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitTryStatement(TryStatement node) {
    TryStatement other = (TryStatement) this.other;
    return isEqual(node.getTryKeyword(), other.getTryKeyword())
        && isEqual(node.getBody(), other.getBody())
        && isEqual(node.getCatchClauses(), other.getCatchClauses())
        && isEqual(node.getFinallyKeyword(), other.getFinallyKeyword())
        && isEqual(node.getFinallyBlock(), other.getFinallyBlock());
  }

  @Override
  public Boolean visitTypeArgumentList(TypeArgumentList node) {
    TypeArgumentList other = (TypeArgumentList) this.other;
    return isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getArguments(), other.getArguments())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitTypeName(TypeName node) {
    TypeName other = (TypeName) this.other;
    return isEqual(node.getName(), other.getName())
        && isEqual(node.getTypeArguments(), other.getTypeArguments());
  }

  @Override
  public Boolean visitTypeParameter(TypeParameter node) {
    TypeParameter other = (TypeParameter) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getName(), other.getName())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getBound(), other.getBound());
  }

  @Override
  public Boolean visitTypeParameterList(TypeParameterList node) {
    TypeParameterList other = (TypeParameterList) this.other;
    return isEqual(node.getLeftBracket(), other.getLeftBracket())
        && isEqual(node.getTypeParameters(), other.getTypeParameters())
        && isEqual(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitVariableDeclaration(VariableDeclaration node) {
    VariableDeclaration other = (VariableDeclaration) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getName(), other.getName()) && isEqual(node.getEquals(), other.getEquals())
        && isEqual(node.getInitializer(), other.getInitializer());
  }

  @Override
  public Boolean visitVariableDeclarationList(VariableDeclarationList node) {
    VariableDeclarationList other = (VariableDeclarationList) this.other;
    return isEqual(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqual(node.getMetadata(), other.getMetadata())
        && isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getType(), other.getType())
        && isEqual(node.getVariables(), other.getVariables());
  }

  @Override
  public Boolean visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    VariableDeclarationStatement other = (VariableDeclarationStatement) this.other;
    return isEqual(node.getVariables(), other.getVariables())
        && isEqual(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitWhileStatement(WhileStatement node) {
    WhileStatement other = (WhileStatement) this.other;
    return isEqual(node.getKeyword(), other.getKeyword())
        && isEqual(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqual(node.getCondition(), other.getCondition())
        && isEqual(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqual(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitWithClause(WithClause node) {
    WithClause other = (WithClause) this.other;
    return isEqual(node.getWithKeyword(), other.getWithKeyword())
        && isEqual(node.getMixinTypes(), other.getMixinTypes());
  }

  /**
   * Return {@code true} if the given AST nodes have the same structure.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the given AST nodes have the same structure
   */
  private boolean isEqual(ASTNode first, ASTNode second) {
    if (first == null) {
      return second == null;
    } else if (second == null) {
      return false;
    } else if (first.getClass() != second.getClass()) {
      return false;
    }
    other = second;
    return first.accept(this);
  }

  /**
   * Return {@code true} if the given lists of AST nodes have the same size and corresponding
   * elements are equal.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the given AST nodes have the same size and corresponding elements are
   *         equal
   */
  private <E extends ASTNode> boolean isEqual(NodeList<E> first, NodeList<E> second) {
    if (first == null) {
      return second == null;
    } else if (second == null) {
      return false;
    }
    int size = first.size();
    if (second.size() != size) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!isEqual(first.get(i), second.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return {@code true} if the given tokens have the same structure.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the given tokens have the same structure
   */
  private boolean isEqual(Token first, Token second) {
    if (first == null) {
      return second == null;
    } else if (second == null) {
      return false;
    }
    return first.getOffset() == second.getOffset() && first.getLength() == second.getLength()
        && first.getLexeme().equals(second.getLexeme());
  }

  /**
   * Return {@code true} if the given arrays of tokens have the same length and corresponding
   * elements are equal.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the given arrays of tokens have the same length and corresponding
   *         elements are equal
   */
  private boolean isEqual(Token[] first, Token[] second) {
    int length = first.length;
    if (second.length != length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (isEqual(first[i], second[i])) {
        return false;
      }
    }
    return true;
  }
}
