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
import com.google.dart.engine.utilities.general.ObjectUtilities;

/**
 * Instances of the class {@code AstComparator} compare the structure of two ASTNodes to see whether
 * they are equal.
 */
public class AstComparator implements AstVisitor<Boolean> {
  /**
   * Return {@code true} if the two AST nodes are equal.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the two AST nodes are equal
   */
  public static boolean equalNodes(AstNode first, AstNode second) {
    AstComparator comparator = new AstComparator();
    return comparator.isEqualNodes(first, second);
  }

  /**
   * The AST node with which the node being visited is to be compared. This is only valid at the
   * beginning of each visit method (until {@link #isEqualNodes(AstNode, AstNode)} is invoked).
   */
  private AstNode other;

  @Override
  public Boolean visitAdjacentStrings(AdjacentStrings node) {
    AdjacentStrings other = (AdjacentStrings) this.other;
    return isEqualNodeLists(node.getStrings(), other.getStrings());
  }

  @Override
  public Boolean visitAnnotation(Annotation node) {
    Annotation other = (Annotation) this.other;
    return isEqualTokens(node.getAtSign(), other.getAtSign())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getConstructorName(), other.getConstructorName())
        && isEqualNodes(node.getArguments(), other.getArguments());
  }

  @Override
  public Boolean visitArgumentList(ArgumentList node) {
    ArgumentList other = (ArgumentList) this.other;
    return isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodeLists(node.getArguments(), other.getArguments())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis());
  }

  @Override
  public Boolean visitAsExpression(AsExpression node) {
    AsExpression other = (AsExpression) this.other;
    return isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getAsOperator(), other.getAsOperator())
        && isEqualNodes(node.getType(), other.getType());
  }

  @Override
  public Boolean visitAssertStatement(AssertStatement node) {
    AssertStatement other = (AssertStatement) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getCondition(), other.getCondition())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression other = (AssignmentExpression) this.other;
    return isEqualNodes(node.getLeftHandSide(), other.getLeftHandSide())
        && isEqualTokens(node.getOperator(), other.getOperator())
        && isEqualNodes(node.getRightHandSide(), other.getRightHandSide());
  }

  @Override
  public Boolean visitAwaitExpression(AwaitExpression node) {
    AwaitExpression other = (AwaitExpression) this.other;
    return isEqualTokens(node.getAwaitKeyword(), other.getAwaitKeyword())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitBinaryExpression(BinaryExpression node) {
    BinaryExpression other = (BinaryExpression) this.other;
    return isEqualNodes(node.getLeftOperand(), other.getLeftOperand())
        && isEqualTokens(node.getOperator(), other.getOperator())
        && isEqualNodes(node.getRightOperand(), other.getRightOperand());
  }

  @Override
  public Boolean visitBlock(Block node) {
    Block other = (Block) this.other;
    return isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getStatements(), other.getStatements())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitBlockFunctionBody(BlockFunctionBody node) {
    BlockFunctionBody other = (BlockFunctionBody) this.other;
    return isEqualNodes(node.getBlock(), other.getBlock());
  }

  @Override
  public Boolean visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral other = (BooleanLiteral) this.other;
    return isEqualTokens(node.getLiteral(), other.getLiteral())
        && node.getValue() == other.getValue();
  }

  @Override
  public Boolean visitBreakStatement(BreakStatement node) {
    BreakStatement other = (BreakStatement) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getLabel(), other.getLabel())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitCascadeExpression(CascadeExpression node) {
    CascadeExpression other = (CascadeExpression) this.other;
    return isEqualNodes(node.getTarget(), other.getTarget())
        && isEqualNodeLists(node.getCascadeSections(), other.getCascadeSections());
  }

  @Override
  public Boolean visitCatchClause(CatchClause node) {
    CatchClause other = (CatchClause) this.other;
    return isEqualTokens(node.getOnKeyword(), other.getOnKeyword())
        && isEqualNodes(node.getExceptionType(), other.getExceptionType())
        && isEqualTokens(node.getCatchKeyword(), other.getCatchKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getExceptionParameter(), other.getExceptionParameter())
        && isEqualTokens(node.getComma(), other.getComma())
        && isEqualNodes(node.getStackTraceParameter(), other.getStackTraceParameter())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualNodes(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration other = (ClassDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getAbstractKeyword(), other.getAbstractKeyword())
        && isEqualTokens(node.getClassKeyword(), other.getClassKeyword())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getTypeParameters(), other.getTypeParameters())
        && isEqualNodes(node.getExtendsClause(), other.getExtendsClause())
        && isEqualNodes(node.getWithClause(), other.getWithClause())
        && isEqualNodes(node.getImplementsClause(), other.getImplementsClause())
        && isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getMembers(), other.getMembers())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitClassTypeAlias(ClassTypeAlias node) {
    ClassTypeAlias other = (ClassTypeAlias) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getTypeParameters(), other.getTypeParameters())
        && isEqualTokens(node.getEquals(), other.getEquals())
        && isEqualTokens(node.getAbstractKeyword(), other.getAbstractKeyword())
        && isEqualNodes(node.getSuperclass(), other.getSuperclass())
        && isEqualNodes(node.getWithClause(), other.getWithClause())
        && isEqualNodes(node.getImplementsClause(), other.getImplementsClause())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitComment(Comment node) {
    Comment other = (Comment) this.other;
    return isEqualNodeLists(node.getReferences(), other.getReferences());
  }

  @Override
  public Boolean visitCommentReference(CommentReference node) {
    CommentReference other = (CommentReference) this.other;
    return isEqualTokens(node.getNewKeyword(), other.getNewKeyword())
        && isEqualNodes(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitCompilationUnit(CompilationUnit node) {
    CompilationUnit other = (CompilationUnit) this.other;
    return isEqualTokens(node.getBeginToken(), other.getBeginToken())
        && isEqualNodes(node.getScriptTag(), other.getScriptTag())
        && isEqualNodeLists(node.getDirectives(), other.getDirectives())
        && isEqualNodeLists(node.getDeclarations(), other.getDeclarations())
        && isEqualTokens(node.getEndToken(), other.getEndToken());
  }

  @Override
  public Boolean visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression other = (ConditionalExpression) this.other;
    return isEqualNodes(node.getCondition(), other.getCondition())
        && isEqualTokens(node.getQuestion(), other.getQuestion())
        && isEqualNodes(node.getThenExpression(), other.getThenExpression())
        && isEqualTokens(node.getColon(), other.getColon())
        && isEqualNodes(node.getElseExpression(), other.getElseExpression());
  }

  @Override
  public Boolean visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration other = (ConstructorDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getExternalKeyword(), other.getExternalKeyword())
        && isEqualTokens(node.getConstKeyword(), other.getConstKeyword())
        && isEqualTokens(node.getFactoryKeyword(), other.getFactoryKeyword())
        && isEqualNodes(node.getReturnType(), other.getReturnType())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getParameters(), other.getParameters())
        && isEqualTokens(node.getSeparator(), other.getSeparator())
        && isEqualNodeLists(node.getInitializers(), other.getInitializers())
        && isEqualNodes(node.getRedirectedConstructor(), other.getRedirectedConstructor())
        && isEqualNodes(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    ConstructorFieldInitializer other = (ConstructorFieldInitializer) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getFieldName(), other.getFieldName())
        && isEqualTokens(node.getEquals(), other.getEquals())
        && isEqualNodes(node.getExpression(), other.getExpression());
  }

  @Override
  public Boolean visitConstructorName(ConstructorName node) {
    ConstructorName other = (ConstructorName) this.other;
    return isEqualNodes(node.getType(), other.getType())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getName(), other.getName());
  }

  @Override
  public Boolean visitContinueStatement(ContinueStatement node) {
    ContinueStatement other = (ContinueStatement) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getLabel(), other.getLabel())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitDeclaredIdentifier(DeclaredIdentifier node) {
    DeclaredIdentifier other = (DeclaredIdentifier) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getType(), other.getType())
        && isEqualNodes(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitDefaultFormalParameter(DefaultFormalParameter node) {
    DefaultFormalParameter other = (DefaultFormalParameter) this.other;
    return isEqualNodes(node.getParameter(), other.getParameter())
        && node.getKind() == other.getKind()
        && isEqualTokens(node.getSeparator(), other.getSeparator())
        && isEqualNodes(node.getDefaultValue(), other.getDefaultValue());
  }

  @Override
  public Boolean visitDoStatement(DoStatement node) {
    DoStatement other = (DoStatement) this.other;
    return isEqualTokens(node.getDoKeyword(), other.getDoKeyword())
        && isEqualNodes(node.getBody(), other.getBody())
        && isEqualTokens(node.getWhileKeyword(), other.getWhileKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getCondition(), other.getCondition())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral other = (DoubleLiteral) this.other;
    return isEqualTokens(node.getLiteral(), other.getLiteral())
        && node.getValue() == other.getValue();
  }

  @Override
  public Boolean visitEmptyFunctionBody(EmptyFunctionBody node) {
    EmptyFunctionBody other = (EmptyFunctionBody) this.other;
    return isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitEmptyStatement(EmptyStatement node) {
    EmptyStatement other = (EmptyStatement) this.other;
    return isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    EnumConstantDeclaration other = (EnumConstantDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualNodes(node.getName(), other.getName());
  }

  @Override
  public Boolean visitEnumDeclaration(EnumDeclaration node) {
    EnumDeclaration other = (EnumDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getConstants(), other.getConstants())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitExportDirective(ExportDirective node) {
    ExportDirective other = (ExportDirective) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getUri(), other.getUri())
        && isEqualNodeLists(node.getCombinators(), other.getCombinators())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitExpressionFunctionBody(ExpressionFunctionBody node) {
    ExpressionFunctionBody other = (ExpressionFunctionBody) this.other;
    return isEqualTokens(node.getFunctionDefinition(), other.getFunctionDefinition())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitExpressionStatement(ExpressionStatement node) {
    ExpressionStatement other = (ExpressionStatement) this.other;
    return isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitExtendsClause(ExtendsClause node) {
    ExtendsClause other = (ExtendsClause) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getSuperclass(), other.getSuperclass());
  }

  @Override
  public Boolean visitFieldDeclaration(FieldDeclaration node) {
    FieldDeclaration other = (FieldDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getStaticKeyword(), other.getStaticKeyword())
        && isEqualNodes(node.getFields(), other.getFields())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitFieldFormalParameter(FieldFormalParameter node) {
    FieldFormalParameter other = (FieldFormalParameter) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getType(), other.getType())
        && isEqualTokens(node.getThisToken(), other.getThisToken())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitForEachStatement(ForEachStatement node) {
    ForEachStatement other = (ForEachStatement) this.other;
    return isEqualTokens(node.getForKeyword(), other.getForKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getLoopVariable(), other.getLoopVariable())
        && isEqualTokens(node.getInKeyword(), other.getInKeyword())
        && isEqualNodes(node.getIterator(), other.getIterator())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualNodes(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitFormalParameterList(FormalParameterList node) {
    FormalParameterList other = (FormalParameterList) this.other;
    return isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodeLists(node.getParameters(), other.getParameters())
        && isEqualTokens(node.getLeftDelimiter(), other.getLeftDelimiter())
        && isEqualTokens(node.getRightDelimiter(), other.getRightDelimiter())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis());
  }

  @Override
  public Boolean visitForStatement(ForStatement node) {
    ForStatement other = (ForStatement) this.other;
    return isEqualTokens(node.getForKeyword(), other.getForKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getVariables(), other.getVariables())
        && isEqualNodes(node.getInitialization(), other.getInitialization())
        && isEqualTokens(node.getLeftSeparator(), other.getLeftSeparator())
        && isEqualNodes(node.getCondition(), other.getCondition())
        && isEqualTokens(node.getRightSeparator(), other.getRightSeparator())
        && isEqualNodeLists(node.getUpdaters(), other.getUpdaters())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualNodes(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitFunctionDeclaration(FunctionDeclaration node) {
    FunctionDeclaration other = (FunctionDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getExternalKeyword(), other.getExternalKeyword())
        && isEqualNodes(node.getReturnType(), other.getReturnType())
        && isEqualTokens(node.getPropertyKeyword(), other.getPropertyKeyword())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getFunctionExpression(), other.getFunctionExpression());
  }

  @Override
  public Boolean visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    FunctionDeclarationStatement other = (FunctionDeclarationStatement) this.other;
    return isEqualNodes(node.getFunctionDeclaration(), other.getFunctionDeclaration());
  }

  @Override
  public Boolean visitFunctionExpression(FunctionExpression node) {
    FunctionExpression other = (FunctionExpression) this.other;
    return isEqualNodes(node.getParameters(), other.getParameters())
        && isEqualNodes(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    FunctionExpressionInvocation other = (FunctionExpressionInvocation) this.other;
    return isEqualNodes(node.getFunction(), other.getFunction())
        && isEqualNodes(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitFunctionTypeAlias(FunctionTypeAlias node) {
    FunctionTypeAlias other = (FunctionTypeAlias) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getReturnType(), other.getReturnType())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getTypeParameters(), other.getTypeParameters())
        && isEqualNodes(node.getParameters(), other.getParameters())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    FunctionTypedFormalParameter other = (FunctionTypedFormalParameter) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualNodes(node.getReturnType(), other.getReturnType())
        && isEqualNodes(node.getIdentifier(), other.getIdentifier())
        && isEqualNodes(node.getParameters(), other.getParameters());
  }

  @Override
  public Boolean visitHideCombinator(HideCombinator node) {
    HideCombinator other = (HideCombinator) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodeLists(node.getHiddenNames(), other.getHiddenNames());
  }

  @Override
  public Boolean visitIfStatement(IfStatement node) {
    IfStatement other = (IfStatement) this.other;
    return isEqualTokens(node.getIfKeyword(), other.getIfKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getCondition(), other.getCondition())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualNodes(node.getThenStatement(), other.getThenStatement())
        && isEqualTokens(node.getElseKeyword(), other.getElseKeyword())
        && isEqualNodes(node.getElseStatement(), other.getElseStatement());
  }

  @Override
  public Boolean visitImplementsClause(ImplementsClause node) {
    ImplementsClause other = (ImplementsClause) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodeLists(node.getInterfaces(), other.getInterfaces());
  }

  @Override
  public Boolean visitImportDirective(ImportDirective node) {
    ImportDirective other = (ImportDirective) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getUri(), other.getUri())
        && isEqualTokens(node.getAsToken(), other.getAsToken())
        && isEqualNodes(node.getPrefix(), other.getPrefix())
        && isEqualNodeLists(node.getCombinators(), other.getCombinators())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitIndexExpression(IndexExpression node) {
    IndexExpression other = (IndexExpression) this.other;
    return isEqualNodes(node.getTarget(), other.getTarget())
        && isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodes(node.getIndex(), other.getIndex())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitInstanceCreationExpression(InstanceCreationExpression node) {
    InstanceCreationExpression other = (InstanceCreationExpression) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getConstructorName(), other.getConstructorName())
        && isEqualNodes(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitIntegerLiteral(IntegerLiteral node) {
    IntegerLiteral other = (IntegerLiteral) this.other;
    return isEqualTokens(node.getLiteral(), other.getLiteral())
        && ObjectUtilities.equals(node.getValue(), other.getValue());
  }

  @Override
  public Boolean visitInterpolationExpression(InterpolationExpression node) {
    InterpolationExpression other = (InterpolationExpression) this.other;
    return isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitInterpolationString(InterpolationString node) {
    InterpolationString other = (InterpolationString) this.other;
    return isEqualTokens(node.getContents(), other.getContents())
        && node.getValue().equals(other.getValue());
  }

  @Override
  public Boolean visitIsExpression(IsExpression node) {
    IsExpression other = (IsExpression) this.other;
    return isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getIsOperator(), other.getIsOperator())
        && isEqualTokens(node.getNotOperator(), other.getNotOperator())
        && isEqualNodes(node.getType(), other.getType());
  }

  @Override
  public Boolean visitLabel(Label node) {
    Label other = (Label) this.other;
    return isEqualNodes(node.getLabel(), other.getLabel())
        && isEqualTokens(node.getColon(), other.getColon());
  }

  @Override
  public Boolean visitLabeledStatement(LabeledStatement node) {
    LabeledStatement other = (LabeledStatement) this.other;
    return isEqualNodeLists(node.getLabels(), other.getLabels())
        && isEqualNodes(node.getStatement(), other.getStatement());
  }

  @Override
  public Boolean visitLibraryDirective(LibraryDirective node) {
    LibraryDirective other = (LibraryDirective) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getLibraryToken(), other.getLibraryToken())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier other = (LibraryIdentifier) this.other;
    return isEqualNodeLists(node.getComponents(), other.getComponents());
  }

  @Override
  public Boolean visitListLiteral(ListLiteral node) {
    ListLiteral other = (ListLiteral) this.other;
    return isEqualTokens(node.getConstKeyword(), other.getConstKeyword())
        && isEqualNodes(node.getTypeArguments(), other.getTypeArguments())
        && isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getElements(), other.getElements())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitMapLiteral(MapLiteral node) {
    MapLiteral other = (MapLiteral) this.other;
    return isEqualTokens(node.getConstKeyword(), other.getConstKeyword())
        && isEqualNodes(node.getTypeArguments(), other.getTypeArguments())
        && isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getEntries(), other.getEntries())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitMapLiteralEntry(MapLiteralEntry node) {
    MapLiteralEntry other = (MapLiteralEntry) this.other;
    return isEqualNodes(node.getKey(), other.getKey())
        && isEqualTokens(node.getSeparator(), other.getSeparator())
        && isEqualNodes(node.getValue(), other.getValue());
  }

  @Override
  public Boolean visitMethodDeclaration(MethodDeclaration node) {
    MethodDeclaration other = (MethodDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getExternalKeyword(), other.getExternalKeyword())
        && isEqualTokens(node.getModifierKeyword(), other.getModifierKeyword())
        && isEqualNodes(node.getReturnType(), other.getReturnType())
        && isEqualTokens(node.getPropertyKeyword(), other.getPropertyKeyword())
        && isEqualTokens(node.getPropertyKeyword(), other.getPropertyKeyword())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getParameters(), other.getParameters())
        && isEqualNodes(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitMethodInvocation(MethodInvocation node) {
    MethodInvocation other = (MethodInvocation) this.other;
    return isEqualNodes(node.getTarget(), other.getTarget())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getMethodName(), other.getMethodName())
        && isEqualNodes(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitNamedExpression(NamedExpression node) {
    NamedExpression other = (NamedExpression) this.other;
    return isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getExpression(), other.getExpression());
  }

  @Override
  public Boolean visitNativeClause(NativeClause node) {
    NativeClause other = (NativeClause) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getName(), other.getName());
  }

  @Override
  public Boolean visitNativeFunctionBody(NativeFunctionBody node) {
    NativeFunctionBody other = (NativeFunctionBody) this.other;
    return isEqualTokens(node.getNativeToken(), other.getNativeToken())
        && isEqualNodes(node.getStringLiteral(), other.getStringLiteral())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitNullLiteral(NullLiteral node) {
    NullLiteral other = (NullLiteral) this.other;
    return isEqualTokens(node.getLiteral(), other.getLiteral());
  }

  @Override
  public Boolean visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression other = (ParenthesizedExpression) this.other;
    return isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis());
  }

  @Override
  public Boolean visitPartDirective(PartDirective node) {
    PartDirective other = (PartDirective) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getPartToken(), other.getPartToken())
        && isEqualNodes(node.getUri(), other.getUri())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitPartOfDirective(PartOfDirective node) {
    PartOfDirective other = (PartOfDirective) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getPartToken(), other.getPartToken())
        && isEqualTokens(node.getOfToken(), other.getOfToken())
        && isEqualNodes(node.getLibraryName(), other.getLibraryName())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitPostfixExpression(PostfixExpression node) {
    PostfixExpression other = (PostfixExpression) this.other;
    return isEqualNodes(node.getOperand(), other.getOperand())
        && isEqualTokens(node.getOperator(), other.getOperator());
  }

  @Override
  public Boolean visitPrefixedIdentifier(PrefixedIdentifier node) {
    PrefixedIdentifier other = (PrefixedIdentifier) this.other;
    return isEqualNodes(node.getPrefix(), other.getPrefix())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitPrefixExpression(PrefixExpression node) {
    PrefixExpression other = (PrefixExpression) this.other;
    return isEqualTokens(node.getOperator(), other.getOperator())
        && isEqualNodes(node.getOperand(), other.getOperand());
  }

  @Override
  public Boolean visitPropertyAccess(PropertyAccess node) {
    PropertyAccess other = (PropertyAccess) this.other;
    return isEqualNodes(node.getTarget(), other.getTarget())
        && isEqualTokens(node.getOperator(), other.getOperator())
        && isEqualNodes(node.getPropertyName(), other.getPropertyName());
  }

  @Override
  public Boolean visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation other = (RedirectingConstructorInvocation) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getConstructorName(), other.getConstructorName())
        && isEqualNodes(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitRethrowExpression(RethrowExpression node) {
    RethrowExpression other = (RethrowExpression) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword());
  }

  @Override
  public Boolean visitReturnStatement(ReturnStatement node) {
    ReturnStatement other = (ReturnStatement) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitScriptTag(ScriptTag node) {
    ScriptTag other = (ScriptTag) this.other;
    return isEqualTokens(node.getScriptTag(), other.getScriptTag());
  }

  @Override
  public Boolean visitShowCombinator(ShowCombinator node) {
    ShowCombinator other = (ShowCombinator) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodeLists(node.getShownNames(), other.getShownNames());
  }

  @Override
  public Boolean visitSimpleFormalParameter(SimpleFormalParameter node) {
    SimpleFormalParameter other = (SimpleFormalParameter) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getType(), other.getType())
        && isEqualNodes(node.getIdentifier(), other.getIdentifier());
  }

  @Override
  public Boolean visitSimpleIdentifier(SimpleIdentifier node) {
    SimpleIdentifier other = (SimpleIdentifier) this.other;
    return isEqualTokens(node.getToken(), other.getToken());
  }

  @Override
  public Boolean visitSimpleStringLiteral(SimpleStringLiteral node) {
    SimpleStringLiteral other = (SimpleStringLiteral) this.other;
    return isEqualTokens(node.getLiteral(), other.getLiteral())
        && ObjectUtilities.equals(node.getValue(), other.getValue());
  }

  @Override
  public Boolean visitStringInterpolation(StringInterpolation node) {
    StringInterpolation other = (StringInterpolation) this.other;
    return isEqualNodeLists(node.getElements(), other.getElements());
  }

  @Override
  public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation other = (SuperConstructorInvocation) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualTokens(node.getPeriod(), other.getPeriod())
        && isEqualNodes(node.getConstructorName(), other.getConstructorName())
        && isEqualNodes(node.getArgumentList(), other.getArgumentList());
  }

  @Override
  public Boolean visitSuperExpression(SuperExpression node) {
    SuperExpression other = (SuperExpression) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword());
  }

  @Override
  public Boolean visitSwitchCase(SwitchCase node) {
    SwitchCase other = (SwitchCase) this.other;
    return isEqualNodeLists(node.getLabels(), other.getLabels())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getColon(), other.getColon())
        && isEqualNodeLists(node.getStatements(), other.getStatements());
  }

  @Override
  public Boolean visitSwitchDefault(SwitchDefault node) {
    SwitchDefault other = (SwitchDefault) this.other;
    return isEqualNodeLists(node.getLabels(), other.getLabels())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualTokens(node.getColon(), other.getColon())
        && isEqualNodeLists(node.getStatements(), other.getStatements());
  }

  @Override
  public Boolean visitSwitchStatement(SwitchStatement node) {
    SwitchStatement other = (SwitchStatement) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getMembers(), other.getMembers())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral other = (SymbolLiteral) this.other;
    return isEqualTokens(node.getPoundSign(), other.getPoundSign())
        && isEqualTokenLists(node.getComponents(), other.getComponents());
  }

  @Override
  public Boolean visitThisExpression(ThisExpression node) {
    ThisExpression other = (ThisExpression) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword());
  }

  @Override
  public Boolean visitThrowExpression(ThrowExpression node) {
    ThrowExpression other = (ThrowExpression) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getExpression(), other.getExpression());
  }

  @Override
  public Boolean visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    TopLevelVariableDeclaration other = (TopLevelVariableDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualNodes(node.getVariables(), other.getVariables())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitTryStatement(TryStatement node) {
    TryStatement other = (TryStatement) this.other;
    return isEqualTokens(node.getTryKeyword(), other.getTryKeyword())
        && isEqualNodes(node.getBody(), other.getBody())
        && isEqualNodeLists(node.getCatchClauses(), other.getCatchClauses())
        && isEqualTokens(node.getFinallyKeyword(), other.getFinallyKeyword())
        && isEqualNodes(node.getFinallyBlock(), other.getFinallyBlock());
  }

  @Override
  public Boolean visitTypeArgumentList(TypeArgumentList node) {
    TypeArgumentList other = (TypeArgumentList) this.other;
    return isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getArguments(), other.getArguments())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitTypeName(TypeName node) {
    TypeName other = (TypeName) this.other;
    return isEqualNodes(node.getName(), other.getName())
        && isEqualNodes(node.getTypeArguments(), other.getTypeArguments());
  }

  @Override
  public Boolean visitTypeParameter(TypeParameter node) {
    TypeParameter other = (TypeParameter) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getBound(), other.getBound());
  }

  @Override
  public Boolean visitTypeParameterList(TypeParameterList node) {
    TypeParameterList other = (TypeParameterList) this.other;
    return isEqualTokens(node.getLeftBracket(), other.getLeftBracket())
        && isEqualNodeLists(node.getTypeParameters(), other.getTypeParameters())
        && isEqualTokens(node.getRightBracket(), other.getRightBracket());
  }

  @Override
  public Boolean visitVariableDeclaration(VariableDeclaration node) {
    VariableDeclaration other = (VariableDeclaration) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualNodes(node.getName(), other.getName())
        && isEqualTokens(node.getEquals(), other.getEquals())
        && isEqualNodes(node.getInitializer(), other.getInitializer());
  }

  @Override
  public Boolean visitVariableDeclarationList(VariableDeclarationList node) {
    VariableDeclarationList other = (VariableDeclarationList) this.other;
    return isEqualNodes(node.getDocumentationComment(), other.getDocumentationComment())
        && isEqualNodeLists(node.getMetadata(), other.getMetadata())
        && isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualNodes(node.getType(), other.getType())
        && isEqualNodeLists(node.getVariables(), other.getVariables());
  }

  @Override
  public Boolean visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    VariableDeclarationStatement other = (VariableDeclarationStatement) this.other;
    return isEqualNodes(node.getVariables(), other.getVariables())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
  }

  @Override
  public Boolean visitWhileStatement(WhileStatement node) {
    WhileStatement other = (WhileStatement) this.other;
    return isEqualTokens(node.getKeyword(), other.getKeyword())
        && isEqualTokens(node.getLeftParenthesis(), other.getLeftParenthesis())
        && isEqualNodes(node.getCondition(), other.getCondition())
        && isEqualTokens(node.getRightParenthesis(), other.getRightParenthesis())
        && isEqualNodes(node.getBody(), other.getBody());
  }

  @Override
  public Boolean visitWithClause(WithClause node) {
    WithClause other = (WithClause) this.other;
    return isEqualTokens(node.getWithKeyword(), other.getWithKeyword())
        && isEqualNodeLists(node.getMixinTypes(), other.getMixinTypes());
  }

  @Override
  public Boolean visitYieldStatement(YieldStatement node) {
    YieldStatement other = (YieldStatement) this.other;
    return isEqualTokens(node.getYieldKeyword(), other.getYieldKeyword())
        && isEqualNodes(node.getExpression(), other.getExpression())
        && isEqualTokens(node.getSemicolon(), other.getSemicolon());
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
  private <E extends AstNode> boolean isEqualNodeLists(NodeList<E> first, NodeList<E> second) {
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
      if (!isEqualNodes(first.get(i), second.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return {@code true} if the given AST nodes have the same structure.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the given AST nodes have the same structure
   */
  private boolean isEqualNodes(AstNode first, AstNode second) {
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
   * Return {@code true} if the given arrays of tokens have the same length and corresponding
   * elements are equal.
   * 
   * @param first the first node being compared
   * @param second the second node being compared
   * @return {@code true} if the given arrays of tokens have the same length and corresponding
   *         elements are equal
   */
  private boolean isEqualTokenLists(Token[] first, Token[] second) {
    int length = first.length;
    if (second.length != length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (!isEqualTokens(first[i], second[i])) {
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
  private boolean isEqualTokens(Token first, Token second) {
    if (first == null) {
      return second == null;
    } else if (second == null) {
      return false;
    } else if (first == second) {
      return true;
    }
    return first.getOffset() == second.getOffset() && first.getLength() == second.getLength()
        && first.getLexeme().equals(second.getLexeme());
  }
}
