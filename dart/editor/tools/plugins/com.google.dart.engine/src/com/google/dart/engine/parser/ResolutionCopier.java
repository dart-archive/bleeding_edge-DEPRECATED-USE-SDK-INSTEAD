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
package com.google.dart.engine.parser;

import com.google.dart.engine.ast.*;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code ResolutionCopier} copies resolution information from one AST
 * structure to another as long as the structures of the corresponding children of a pair of nodes
 * are the same.
 */
public class ResolutionCopier implements AstVisitor<Boolean> {
  /**
   * Copy resolution data from one node to another.
   * 
   * @param fromNode the node from which resolution information will be copied
   * @param toNode the node to which resolution information will be copied
   */
  public static void copyResolutionData(AstNode fromNode, AstNode toNode) {
    ResolutionCopier copier = new ResolutionCopier();
    copier.isEqualNodes(fromNode, toNode);
  }

  /**
   * The AST node with which the node being visited is to be compared. This is only valid at the
   * beginning of each visit method (until {@link #isEqualNodes(AstNode, AstNode)} is invoked).
   */
  private AstNode toNode;

  @Override
  public Boolean visitAdjacentStrings(AdjacentStrings node) {
    AdjacentStrings toNode = (AdjacentStrings) this.toNode;
    return isEqualNodeLists(node.getStrings(), toNode.getStrings());
  }

  @Override
  public Boolean visitAnnotation(Annotation node) {
    Annotation toNode = (Annotation) this.toNode;
    if (isEqualTokens(node.getAtSign(), toNode.getAtSign())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getConstructorName(), toNode.getConstructorName())
        & isEqualNodes(node.getArguments(), toNode.getArguments())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitArgumentList(ArgumentList node) {
    ArgumentList toNode = (ArgumentList) this.toNode;
    return isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodeLists(node.getArguments(), toNode.getArguments())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis());
  }

  @Override
  public Boolean visitAsExpression(AsExpression node) {
    AsExpression toNode = (AsExpression) this.toNode;
    if (isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getAsOperator(), toNode.getAsOperator())
        & isEqualNodes(node.getType(), toNode.getType())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitAssertStatement(AssertStatement node) {
    AssertStatement toNode = (AssertStatement) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getCondition(), toNode.getCondition())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression toNode = (AssignmentExpression) this.toNode;
    if (isEqualNodes(node.getLeftHandSide(), toNode.getLeftHandSide())
        & isEqualTokens(node.getOperator(), toNode.getOperator())
        & isEqualNodes(node.getRightHandSide(), toNode.getRightHandSide())) {
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitAwaitExpression(AwaitExpression node) {
    AwaitExpression toNode = (AwaitExpression) this.toNode;
    return isEqualTokens(node.getAwaitKeyword(), toNode.getAwaitKeyword())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitBinaryExpression(BinaryExpression node) {
    BinaryExpression toNode = (BinaryExpression) this.toNode;
    if (isEqualNodes(node.getLeftOperand(), toNode.getLeftOperand())
        & isEqualTokens(node.getOperator(), toNode.getOperator())
        & isEqualNodes(node.getRightOperand(), toNode.getRightOperand())) {
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitBlock(Block node) {
    Block toNode = (Block) this.toNode;
    return isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodeLists(node.getStatements(), toNode.getStatements())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitBlockFunctionBody(BlockFunctionBody node) {
    BlockFunctionBody toNode = (BlockFunctionBody) this.toNode;
    return isEqualNodes(node.getBlock(), toNode.getBlock());
  }

  @Override
  public Boolean visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral toNode = (BooleanLiteral) this.toNode;
    if (isEqualTokens(node.getLiteral(), toNode.getLiteral())
        & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitBreakStatement(BreakStatement node) {
    BreakStatement toNode = (BreakStatement) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getLabel(), toNode.getLabel())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitCascadeExpression(CascadeExpression node) {
    CascadeExpression toNode = (CascadeExpression) this.toNode;
    if (isEqualNodes(node.getTarget(), toNode.getTarget())
        & isEqualNodeLists(node.getCascadeSections(), toNode.getCascadeSections())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitCatchClause(CatchClause node) {
    CatchClause toNode = (CatchClause) this.toNode;
    return isEqualTokens(node.getOnKeyword(), toNode.getOnKeyword())
        & isEqualNodes(node.getExceptionType(), toNode.getExceptionType())
        & isEqualTokens(node.getCatchKeyword(), toNode.getCatchKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getExceptionParameter(), toNode.getExceptionParameter())
        & isEqualTokens(node.getComma(), toNode.getComma())
        & isEqualNodes(node.getStackTraceParameter(), toNode.getStackTraceParameter())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualNodes(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration toNode = (ClassDeclaration) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getAbstractKeyword(), toNode.getAbstractKeyword())
        & isEqualTokens(node.getClassKeyword(), toNode.getClassKeyword())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqualNodes(node.getExtendsClause(), toNode.getExtendsClause())
        & isEqualNodes(node.getWithClause(), toNode.getWithClause())
        & isEqualNodes(node.getImplementsClause(), toNode.getImplementsClause())
        & isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodeLists(node.getMembers(), toNode.getMembers())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitClassTypeAlias(ClassTypeAlias node) {
    ClassTypeAlias toNode = (ClassTypeAlias) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqualTokens(node.getEquals(), toNode.getEquals())
        & isEqualTokens(node.getAbstractKeyword(), toNode.getAbstractKeyword())
        & isEqualNodes(node.getSuperclass(), toNode.getSuperclass())
        & isEqualNodes(node.getWithClause(), toNode.getWithClause())
        & isEqualNodes(node.getImplementsClause(), toNode.getImplementsClause())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitComment(Comment node) {
    Comment toNode = (Comment) this.toNode;
    return isEqualNodeLists(node.getReferences(), toNode.getReferences());
  }

  @Override
  public Boolean visitCommentReference(CommentReference node) {
    CommentReference toNode = (CommentReference) this.toNode;
    return isEqualTokens(node.getNewKeyword(), toNode.getNewKeyword())
        & isEqualNodes(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitCompilationUnit(CompilationUnit node) {
    CompilationUnit toNode = (CompilationUnit) this.toNode;
    if (isEqualTokens(node.getBeginToken(), toNode.getBeginToken())
        & isEqualNodes(node.getScriptTag(), toNode.getScriptTag())
        & isEqualNodeLists(node.getDirectives(), toNode.getDirectives())
        & isEqualNodeLists(node.getDeclarations(), toNode.getDeclarations())
        & isEqualTokens(node.getEndToken(), toNode.getEndToken())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression toNode = (ConditionalExpression) this.toNode;
    if (isEqualNodes(node.getCondition(), toNode.getCondition())
        & isEqualTokens(node.getQuestion(), toNode.getQuestion())
        & isEqualNodes(node.getThenExpression(), toNode.getThenExpression())
        & isEqualTokens(node.getColon(), toNode.getColon())
        & isEqualNodes(node.getElseExpression(), toNode.getElseExpression())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration toNode = (ConstructorDeclaration) this.toNode;
    if (isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getExternalKeyword(), toNode.getExternalKeyword())
        & isEqualTokens(node.getConstKeyword(), toNode.getConstKeyword())
        & isEqualTokens(node.getFactoryKeyword(), toNode.getFactoryKeyword())
        & isEqualNodes(node.getReturnType(), toNode.getReturnType())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getParameters(), toNode.getParameters())
        & isEqualTokens(node.getSeparator(), toNode.getSeparator())
        & isEqualNodeLists(node.getInitializers(), toNode.getInitializers())
        & isEqualNodes(node.getRedirectedConstructor(), toNode.getRedirectedConstructor())
        & isEqualNodes(node.getBody(), toNode.getBody())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    ConstructorFieldInitializer toNode = (ConstructorFieldInitializer) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getFieldName(), toNode.getFieldName())
        & isEqualTokens(node.getEquals(), toNode.getEquals())
        & isEqualNodes(node.getExpression(), toNode.getExpression());
  }

  @Override
  public Boolean visitConstructorName(ConstructorName node) {
    ConstructorName toNode = (ConstructorName) this.toNode;
    if (isEqualNodes(node.getType(), toNode.getType())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getName(), toNode.getName())) {
      toNode.setStaticElement(node.getStaticElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitContinueStatement(ContinueStatement node) {
    ContinueStatement toNode = (ContinueStatement) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getLabel(), toNode.getLabel())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitDeclaredIdentifier(DeclaredIdentifier node) {
    DeclaredIdentifier toNode = (DeclaredIdentifier) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getType(), toNode.getType())
        & isEqualNodes(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitDefaultFormalParameter(DefaultFormalParameter node) {
    DefaultFormalParameter toNode = (DefaultFormalParameter) this.toNode;
    return isEqualNodes(node.getParameter(), toNode.getParameter())
        & node.getKind() == toNode.getKind()
        & isEqualTokens(node.getSeparator(), toNode.getSeparator())
        & isEqualNodes(node.getDefaultValue(), toNode.getDefaultValue());
  }

  @Override
  public Boolean visitDoStatement(DoStatement node) {
    DoStatement toNode = (DoStatement) this.toNode;
    return isEqualTokens(node.getDoKeyword(), toNode.getDoKeyword())
        & isEqualNodes(node.getBody(), toNode.getBody())
        & isEqualTokens(node.getWhileKeyword(), toNode.getWhileKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getCondition(), toNode.getCondition())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral toNode = (DoubleLiteral) this.toNode;
    if (isEqualTokens(node.getLiteral(), toNode.getLiteral())
        & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitEmptyFunctionBody(EmptyFunctionBody node) {
    EmptyFunctionBody toNode = (EmptyFunctionBody) this.toNode;
    return isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitEmptyStatement(EmptyStatement node) {
    EmptyStatement toNode = (EmptyStatement) this.toNode;
    return isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitExportDirective(ExportDirective node) {
    ExportDirective toNode = (ExportDirective) this.toNode;
    if (isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getUri(), toNode.getUri())
        & isEqualNodeLists(node.getCombinators(), toNode.getCombinators())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitExpressionFunctionBody(ExpressionFunctionBody node) {
    ExpressionFunctionBody toNode = (ExpressionFunctionBody) this.toNode;
    return isEqualTokens(node.getFunctionDefinition(), toNode.getFunctionDefinition())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitExpressionStatement(ExpressionStatement node) {
    ExpressionStatement toNode = (ExpressionStatement) this.toNode;
    return isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitExtendsClause(ExtendsClause node) {
    ExtendsClause toNode = (ExtendsClause) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getSuperclass(), toNode.getSuperclass());
  }

  @Override
  public Boolean visitFieldDeclaration(FieldDeclaration node) {
    FieldDeclaration toNode = (FieldDeclaration) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getStaticKeyword(), toNode.getStaticKeyword())
        & isEqualNodes(node.getFields(), toNode.getFields())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitFieldFormalParameter(FieldFormalParameter node) {
    FieldFormalParameter toNode = (FieldFormalParameter) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getType(), toNode.getType())
        & isEqualTokens(node.getThisToken(), toNode.getThisToken())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitForEachStatement(ForEachStatement node) {
    ForEachStatement toNode = (ForEachStatement) this.toNode;
    return isEqualTokens(node.getForKeyword(), toNode.getForKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getLoopVariable(), toNode.getLoopVariable())
        & isEqualTokens(node.getInKeyword(), toNode.getInKeyword())
        & isEqualNodes(node.getIterator(), toNode.getIterator())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualNodes(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitFormalParameterList(FormalParameterList node) {
    FormalParameterList toNode = (FormalParameterList) this.toNode;
    return isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodeLists(node.getParameters(), toNode.getParameters())
        & isEqualTokens(node.getLeftDelimiter(), toNode.getLeftDelimiter())
        & isEqualTokens(node.getRightDelimiter(), toNode.getRightDelimiter())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis());
  }

  @Override
  public Boolean visitForStatement(ForStatement node) {
    ForStatement toNode = (ForStatement) this.toNode;
    return isEqualTokens(node.getForKeyword(), toNode.getForKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getVariables(), toNode.getVariables())
        & isEqualNodes(node.getInitialization(), toNode.getInitialization())
        & isEqualTokens(node.getLeftSeparator(), toNode.getLeftSeparator())
        & isEqualNodes(node.getCondition(), toNode.getCondition())
        & isEqualTokens(node.getRightSeparator(), toNode.getRightSeparator())
        & isEqualNodeLists(node.getUpdaters(), toNode.getUpdaters())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualNodes(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitFunctionDeclaration(FunctionDeclaration node) {
    FunctionDeclaration toNode = (FunctionDeclaration) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getExternalKeyword(), toNode.getExternalKeyword())
        & isEqualNodes(node.getReturnType(), toNode.getReturnType())
        & isEqualTokens(node.getPropertyKeyword(), toNode.getPropertyKeyword())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getFunctionExpression(), toNode.getFunctionExpression());
  }

  @Override
  public Boolean visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    FunctionDeclarationStatement toNode = (FunctionDeclarationStatement) this.toNode;
    return isEqualNodes(node.getFunctionDeclaration(), toNode.getFunctionDeclaration());
  }

  @Override
  public Boolean visitFunctionExpression(FunctionExpression node) {
    FunctionExpression toNode = (FunctionExpression) this.toNode;
    if (isEqualNodes(node.getParameters(), toNode.getParameters())
        & isEqualNodes(node.getBody(), toNode.getBody())) {
      toNode.setElement(node.getElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    FunctionExpressionInvocation toNode = (FunctionExpressionInvocation) this.toNode;
    if (isEqualNodes(node.getFunction(), toNode.getFunction())
        & isEqualNodes(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitFunctionTypeAlias(FunctionTypeAlias node) {
    FunctionTypeAlias toNode = (FunctionTypeAlias) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getReturnType(), toNode.getReturnType())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqualNodes(node.getParameters(), toNode.getParameters())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    FunctionTypedFormalParameter toNode = (FunctionTypedFormalParameter) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualNodes(node.getReturnType(), toNode.getReturnType())
        & isEqualNodes(node.getIdentifier(), toNode.getIdentifier())
        & isEqualNodes(node.getParameters(), toNode.getParameters());
  }

  @Override
  public Boolean visitHideCombinator(HideCombinator node) {
    HideCombinator toNode = (HideCombinator) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodeLists(node.getHiddenNames(), toNode.getHiddenNames());
  }

  @Override
  public Boolean visitIfStatement(IfStatement node) {
    IfStatement toNode = (IfStatement) this.toNode;
    return isEqualTokens(node.getIfKeyword(), toNode.getIfKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getCondition(), toNode.getCondition())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualNodes(node.getThenStatement(), toNode.getThenStatement())
        & isEqualTokens(node.getElseKeyword(), toNode.getElseKeyword())
        & isEqualNodes(node.getElseStatement(), toNode.getElseStatement());
  }

  @Override
  public Boolean visitImplementsClause(ImplementsClause node) {
    ImplementsClause toNode = (ImplementsClause) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodeLists(node.getInterfaces(), toNode.getInterfaces());
  }

  @Override
  public Boolean visitImportDirective(ImportDirective node) {
    ImportDirective toNode = (ImportDirective) this.toNode;
    if (isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getUri(), toNode.getUri())
        & isEqualTokens(node.getAsToken(), toNode.getAsToken())
        & isEqualNodes(node.getPrefix(), toNode.getPrefix())
        & isEqualNodeLists(node.getCombinators(), toNode.getCombinators())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitIndexExpression(IndexExpression node) {
    IndexExpression toNode = (IndexExpression) this.toNode;
    if (isEqualNodes(node.getTarget(), toNode.getTarget())
        & isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodes(node.getIndex(), toNode.getIndex())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket())) {
      toNode.setAuxiliaryElements(node.getAuxiliaryElements());
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitInstanceCreationExpression(InstanceCreationExpression node) {
    InstanceCreationExpression toNode = (InstanceCreationExpression) this.toNode;
    if (isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getConstructorName(), toNode.getConstructorName())
        & isEqualNodes(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitIntegerLiteral(IntegerLiteral node) {
    IntegerLiteral toNode = (IntegerLiteral) this.toNode;
    if (isEqualTokens(node.getLiteral(), toNode.getLiteral())
        & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitInterpolationExpression(InterpolationExpression node) {
    InterpolationExpression toNode = (InterpolationExpression) this.toNode;
    return isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitInterpolationString(InterpolationString node) {
    InterpolationString toNode = (InterpolationString) this.toNode;
    return isEqualTokens(node.getContents(), toNode.getContents())
        & node.getValue().equals(toNode.getValue());
  }

  @Override
  public Boolean visitIsExpression(IsExpression node) {
    IsExpression toNode = (IsExpression) this.toNode;
    if (isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getIsOperator(), toNode.getIsOperator())
        & isEqualTokens(node.getNotOperator(), toNode.getNotOperator())
        & isEqualNodes(node.getType(), toNode.getType())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitLabel(Label node) {
    Label toNode = (Label) this.toNode;
    return isEqualNodes(node.getLabel(), toNode.getLabel())
        & isEqualTokens(node.getColon(), toNode.getColon());
  }

  @Override
  public Boolean visitLabeledStatement(LabeledStatement node) {
    LabeledStatement toNode = (LabeledStatement) this.toNode;
    return isEqualNodeLists(node.getLabels(), toNode.getLabels())
        & isEqualNodes(node.getStatement(), toNode.getStatement());
  }

  @Override
  public Boolean visitLibraryDirective(LibraryDirective node) {
    LibraryDirective toNode = (LibraryDirective) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getLibraryToken(), toNode.getLibraryToken())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier toNode = (LibraryIdentifier) this.toNode;
    if (isEqualNodeLists(node.getComponents(), toNode.getComponents())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitListLiteral(ListLiteral node) {
    ListLiteral toNode = (ListLiteral) this.toNode;
    if (isEqualTokens(node.getConstKeyword(), toNode.getConstKeyword())
        & isEqualNodes(node.getTypeArguments(), toNode.getTypeArguments())
        & isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodeLists(node.getElements(), toNode.getElements())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitMapLiteral(MapLiteral node) {
    MapLiteral toNode = (MapLiteral) this.toNode;
    if (isEqualTokens(node.getConstKeyword(), toNode.getConstKeyword())
        & isEqualNodes(node.getTypeArguments(), toNode.getTypeArguments())
        & isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodeLists(node.getEntries(), toNode.getEntries())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitMapLiteralEntry(MapLiteralEntry node) {
    MapLiteralEntry toNode = (MapLiteralEntry) this.toNode;
    return isEqualNodes(node.getKey(), toNode.getKey())
        & isEqualTokens(node.getSeparator(), toNode.getSeparator())
        & isEqualNodes(node.getValue(), toNode.getValue());
  }

  @Override
  public Boolean visitMethodDeclaration(MethodDeclaration node) {
    MethodDeclaration toNode = (MethodDeclaration) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getExternalKeyword(), toNode.getExternalKeyword())
        & isEqualTokens(node.getModifierKeyword(), toNode.getModifierKeyword())
        & isEqualNodes(node.getReturnType(), toNode.getReturnType())
        & isEqualTokens(node.getPropertyKeyword(), toNode.getPropertyKeyword())
        & isEqualTokens(node.getPropertyKeyword(), toNode.getPropertyKeyword())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getParameters(), toNode.getParameters())
        & isEqualNodes(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitMethodInvocation(MethodInvocation node) {
    MethodInvocation toNode = (MethodInvocation) this.toNode;
    if (isEqualNodes(node.getTarget(), toNode.getTarget())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getMethodName(), toNode.getMethodName())
        & isEqualNodes(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitNamedExpression(NamedExpression node) {
    NamedExpression toNode = (NamedExpression) this.toNode;
    if (isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getExpression(), toNode.getExpression())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitNativeClause(NativeClause node) {
    NativeClause toNode = (NativeClause) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getName(), toNode.getName());
  }

  @Override
  public Boolean visitNativeFunctionBody(NativeFunctionBody node) {
    NativeFunctionBody toNode = (NativeFunctionBody) this.toNode;
    return isEqualTokens(node.getNativeToken(), toNode.getNativeToken())
        & isEqualNodes(node.getStringLiteral(), toNode.getStringLiteral())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitNullLiteral(NullLiteral node) {
    NullLiteral toNode = (NullLiteral) this.toNode;
    if (isEqualTokens(node.getLiteral(), toNode.getLiteral())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression toNode = (ParenthesizedExpression) this.toNode;
    if (isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPartDirective(PartDirective node) {
    PartDirective toNode = (PartDirective) this.toNode;
    if (isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getPartToken(), toNode.getPartToken())
        & isEqualNodes(node.getUri(), toNode.getUri())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPartOfDirective(PartOfDirective node) {
    PartOfDirective toNode = (PartOfDirective) this.toNode;
    if (isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getPartToken(), toNode.getPartToken())
        & isEqualTokens(node.getOfToken(), toNode.getOfToken())
        & isEqualNodes(node.getLibraryName(), toNode.getLibraryName())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPostfixExpression(PostfixExpression node) {
    PostfixExpression toNode = (PostfixExpression) this.toNode;
    if (isEqualNodes(node.getOperand(), toNode.getOperand())
        & isEqualTokens(node.getOperator(), toNode.getOperator())) {
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPrefixedIdentifier(PrefixedIdentifier node) {
    PrefixedIdentifier toNode = (PrefixedIdentifier) this.toNode;
    if (isEqualNodes(node.getPrefix(), toNode.getPrefix())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getIdentifier(), toNode.getIdentifier())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPrefixExpression(PrefixExpression node) {
    PrefixExpression toNode = (PrefixExpression) this.toNode;
    if (isEqualTokens(node.getOperator(), toNode.getOperator())
        & isEqualNodes(node.getOperand(), toNode.getOperand())) {
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPropertyAccess(PropertyAccess node) {
    PropertyAccess toNode = (PropertyAccess) this.toNode;
    if (isEqualNodes(node.getTarget(), toNode.getTarget())
        & isEqualTokens(node.getOperator(), toNode.getOperator())
        & isEqualNodes(node.getPropertyName(), toNode.getPropertyName())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation toNode = (RedirectingConstructorInvocation) this.toNode;
    if (isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getConstructorName(), toNode.getConstructorName())
        & isEqualNodes(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setStaticElement(node.getStaticElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitRethrowExpression(RethrowExpression node) {
    RethrowExpression toNode = (RethrowExpression) this.toNode;
    if (isEqualTokens(node.getKeyword(), toNode.getKeyword())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitReturnStatement(ReturnStatement node) {
    ReturnStatement toNode = (ReturnStatement) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitScriptTag(ScriptTag node) {
    ScriptTag toNode = (ScriptTag) this.toNode;
    return isEqualTokens(node.getScriptTag(), toNode.getScriptTag());
  }

  @Override
  public Boolean visitShowCombinator(ShowCombinator node) {
    ShowCombinator toNode = (ShowCombinator) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodeLists(node.getShownNames(), toNode.getShownNames());
  }

  @Override
  public Boolean visitSimpleFormalParameter(SimpleFormalParameter node) {
    SimpleFormalParameter toNode = (SimpleFormalParameter) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getType(), toNode.getType())
        & isEqualNodes(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitSimpleIdentifier(SimpleIdentifier node) {
    SimpleIdentifier toNode = (SimpleIdentifier) this.toNode;
    if (isEqualTokens(node.getToken(), toNode.getToken())) {
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setAuxiliaryElements(node.getAuxiliaryElements());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitSimpleStringLiteral(SimpleStringLiteral node) {
    SimpleStringLiteral toNode = (SimpleStringLiteral) this.toNode;
    if (isEqualTokens(node.getLiteral(), toNode.getLiteral())
        & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitStringInterpolation(StringInterpolation node) {
    StringInterpolation toNode = (StringInterpolation) this.toNode;
    if (isEqualNodeLists(node.getElements(), toNode.getElements())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation toNode = (SuperConstructorInvocation) this.toNode;
    if (isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualTokens(node.getPeriod(), toNode.getPeriod())
        & isEqualNodes(node.getConstructorName(), toNode.getConstructorName())
        & isEqualNodes(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setStaticElement(node.getStaticElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitSuperExpression(SuperExpression node) {
    SuperExpression toNode = (SuperExpression) this.toNode;
    if (isEqualTokens(node.getKeyword(), toNode.getKeyword())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitSwitchCase(SwitchCase node) {
    SwitchCase toNode = (SwitchCase) this.toNode;
    return isEqualNodeLists(node.getLabels(), toNode.getLabels())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getColon(), toNode.getColon())
        & isEqualNodeLists(node.getStatements(), toNode.getStatements());
  }

  @Override
  public Boolean visitSwitchDefault(SwitchDefault node) {
    SwitchDefault toNode = (SwitchDefault) this.toNode;
    return isEqualNodeLists(node.getLabels(), toNode.getLabels())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualTokens(node.getColon(), toNode.getColon())
        & isEqualNodeLists(node.getStatements(), toNode.getStatements());
  }

  @Override
  public Boolean visitSwitchStatement(SwitchStatement node) {
    SwitchStatement toNode = (SwitchStatement) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodeLists(node.getMembers(), toNode.getMembers())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral toNode = (SymbolLiteral) this.toNode;
    if (isEqualTokens(node.getPoundSign(), toNode.getPoundSign())
        & isEqualTokenLists(node.getComponents(), toNode.getComponents())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitThisExpression(ThisExpression node) {
    ThisExpression toNode = (ThisExpression) this.toNode;
    if (isEqualTokens(node.getKeyword(), toNode.getKeyword())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitThrowExpression(ThrowExpression node) {
    ThrowExpression toNode = (ThrowExpression) this.toNode;
    if (isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getExpression(), toNode.getExpression())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    TopLevelVariableDeclaration toNode = (TopLevelVariableDeclaration) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualNodes(node.getVariables(), toNode.getVariables())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitTryStatement(TryStatement node) {
    TryStatement toNode = (TryStatement) this.toNode;
    return isEqualTokens(node.getTryKeyword(), toNode.getTryKeyword())
        & isEqualNodes(node.getBody(), toNode.getBody())
        & isEqualNodeLists(node.getCatchClauses(), toNode.getCatchClauses())
        & isEqualTokens(node.getFinallyKeyword(), toNode.getFinallyKeyword())
        & isEqualNodes(node.getFinallyBlock(), toNode.getFinallyBlock());
  }

  @Override
  public Boolean visitTypeArgumentList(TypeArgumentList node) {
    TypeArgumentList toNode = (TypeArgumentList) this.toNode;
    return isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodeLists(node.getArguments(), toNode.getArguments())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitTypeName(TypeName node) {
    TypeName toNode = (TypeName) this.toNode;
    if (isEqualNodes(node.getName(), toNode.getName())
        & isEqualNodes(node.getTypeArguments(), toNode.getTypeArguments())) {
      toNode.setType(node.getType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitTypeParameter(TypeParameter node) {
    TypeParameter toNode = (TypeParameter) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getBound(), toNode.getBound());
  }

  @Override
  public Boolean visitTypeParameterList(TypeParameterList node) {
    TypeParameterList toNode = (TypeParameterList) this.toNode;
    return isEqualTokens(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqualNodeLists(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqualTokens(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitVariableDeclaration(VariableDeclaration node) {
    VariableDeclaration toNode = (VariableDeclaration) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualNodes(node.getName(), toNode.getName())
        & isEqualTokens(node.getEquals(), toNode.getEquals())
        & isEqualNodes(node.getInitializer(), toNode.getInitializer());
  }

  @Override
  public Boolean visitVariableDeclarationList(VariableDeclarationList node) {
    VariableDeclarationList toNode = (VariableDeclarationList) this.toNode;
    return isEqualNodes(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqualNodeLists(node.getMetadata(), toNode.getMetadata())
        & isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualNodes(node.getType(), toNode.getType())
        & isEqualNodeLists(node.getVariables(), toNode.getVariables());
  }

  @Override
  public Boolean visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    VariableDeclarationStatement toNode = (VariableDeclarationStatement) this.toNode;
    return isEqualNodes(node.getVariables(), toNode.getVariables())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitWhileStatement(WhileStatement node) {
    WhileStatement toNode = (WhileStatement) this.toNode;
    return isEqualTokens(node.getKeyword(), toNode.getKeyword())
        & isEqualTokens(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqualNodes(node.getCondition(), toNode.getCondition())
        & isEqualTokens(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqualNodes(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitWithClause(WithClause node) {
    WithClause toNode = (WithClause) this.toNode;
    return isEqualTokens(node.getWithKeyword(), toNode.getWithKeyword())
        & isEqualNodeLists(node.getMixinTypes(), toNode.getMixinTypes());
  }

  @Override
  public Boolean visitYieldStatement(YieldStatement node) {
    YieldStatement toNode = (YieldStatement) this.toNode;
    return isEqualTokens(node.getYieldKeyword(), toNode.getYieldKeyword())
        & isEqualNodes(node.getExpression(), toNode.getExpression())
        & isEqualTokens(node.getSemicolon(), toNode.getSemicolon());
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
    boolean equal = true;
    for (int i = 0; i < size; i++) {
      if (!isEqualNodes(first.get(i), second.get(i))) {
        equal = false;
      }
    }
    return equal;
  }

  /**
   * Return {@code true} if the given AST nodes have the same structure. As a side-effect, if the
   * nodes do have the same structure, any resolution data from the first node will be copied to the
   * second node.
   * 
   * @param fromNode the node from which resolution information will be copied
   * @param toNode the node to which resolution information will be copied
   * @return {@code true} if the given AST nodes have the same structure
   */
  private boolean isEqualNodes(AstNode fromNode, AstNode toNode) {
    if (fromNode == null) {
      return toNode == null;
    } else if (toNode == null) {
      return false;
    } else if (fromNode.getClass() == toNode.getClass()) {
      this.toNode = toNode;
      return fromNode.accept(this);
    }
    //
    // Check for a simple transformation caused by entering a period.
    //
    if (toNode instanceof PrefixedIdentifier) {
      SimpleIdentifier prefix = ((PrefixedIdentifier) toNode).getPrefix();
      if (fromNode.getClass() == prefix.getClass()) {
        this.toNode = prefix;
        return fromNode.accept(this);
      }
    } else if (toNode instanceof PropertyAccess) {
      Expression target = ((PropertyAccess) toNode).getTarget();
      if (fromNode.getClass() == target.getClass()) {
        this.toNode = target;
        return fromNode.accept(this);
      }
    }
    return false;
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
    }
    return first.getLexeme().equals(second.getLexeme());
  }
}
