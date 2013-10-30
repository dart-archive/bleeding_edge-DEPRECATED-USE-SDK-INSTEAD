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
public class ResolutionCopier implements ASTVisitor<Boolean> {
  /**
   * Copy resolution data from one node to another.
   * 
   * @param fromNode the node from which resolution information will be copied
   * @param toNode the node to which resolution information will be copied
   */
  public static void copyResolutionData(ASTNode fromNode, ASTNode toNode) {
    ResolutionCopier copier = new ResolutionCopier();
    copier.isEqual(fromNode, toNode);
  }

  /**
   * The AST node with which the node being visited is to be compared. This is only valid at the
   * beginning of each visit method (until {@link #isEqual(ASTNode, ASTNode)} is invoked).
   */
  private ASTNode toNode;

  @Override
  public Boolean visitAdjacentStrings(AdjacentStrings node) {
    AdjacentStrings toNode = (AdjacentStrings) this.toNode;
    return isEqual(node.getStrings(), toNode.getStrings());
  }

  @Override
  public Boolean visitAnnotation(Annotation node) {
    Annotation toNode = (Annotation) this.toNode;
    if (isEqual(node.getAtSign(), toNode.getAtSign()) & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getConstructorName(), toNode.getConstructorName())
        & isEqual(node.getArguments(), toNode.getArguments())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    ArgumentDefinitionTest toNode = (ArgumentDefinitionTest) this.toNode;
    if (isEqual(node.getQuestion(), toNode.getQuestion())
        & isEqual(node.getIdentifier(), toNode.getIdentifier())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitArgumentList(ArgumentList node) {
    ArgumentList toNode = (ArgumentList) this.toNode;
    return isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getArguments(), toNode.getArguments())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis());
  }

  @Override
  public Boolean visitAsExpression(AsExpression node) {
    AsExpression toNode = (AsExpression) this.toNode;
    if (isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getAsOperator(), toNode.getAsOperator())
        & isEqual(node.getType(), toNode.getType())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitAssertStatement(AssertStatement node) {
    AssertStatement toNode = (AssertStatement) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getCondition(), toNode.getCondition())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression toNode = (AssignmentExpression) this.toNode;
    if (isEqual(node.getLeftHandSide(), toNode.getLeftHandSide())
        & isEqual(node.getOperator(), toNode.getOperator())
        & isEqual(node.getRightHandSide(), toNode.getRightHandSide())) {
      toNode.setPropagatedElement(node.getPropagatedElement());
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticElement(node.getStaticElement());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitBinaryExpression(BinaryExpression node) {
    BinaryExpression toNode = (BinaryExpression) this.toNode;
    if (isEqual(node.getLeftOperand(), toNode.getLeftOperand())
        & isEqual(node.getOperator(), toNode.getOperator())
        & isEqual(node.getRightOperand(), toNode.getRightOperand())) {
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
    return isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getStatements(), toNode.getStatements())
        & isEqual(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitBlockFunctionBody(BlockFunctionBody node) {
    BlockFunctionBody toNode = (BlockFunctionBody) this.toNode;
    return isEqual(node.getBlock(), toNode.getBlock());
  }

  @Override
  public Boolean visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral toNode = (BooleanLiteral) this.toNode;
    if (isEqual(node.getLiteral(), toNode.getLiteral()) & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitBreakStatement(BreakStatement node) {
    BreakStatement toNode = (BreakStatement) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getLabel(), toNode.getLabel())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitCascadeExpression(CascadeExpression node) {
    CascadeExpression toNode = (CascadeExpression) this.toNode;
    if (isEqual(node.getTarget(), toNode.getTarget())
        & isEqual(node.getCascadeSections(), toNode.getCascadeSections())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitCatchClause(CatchClause node) {
    CatchClause toNode = (CatchClause) this.toNode;
    return isEqual(node.getOnKeyword(), toNode.getOnKeyword())
        & isEqual(node.getExceptionType(), toNode.getExceptionType())
        & isEqual(node.getCatchKeyword(), toNode.getCatchKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getExceptionParameter(), toNode.getExceptionParameter())
        & isEqual(node.getComma(), toNode.getComma())
        & isEqual(node.getStackTraceParameter(), toNode.getStackTraceParameter())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration toNode = (ClassDeclaration) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getAbstractKeyword(), toNode.getAbstractKeyword())
        & isEqual(node.getClassKeyword(), toNode.getClassKeyword())
        & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqual(node.getExtendsClause(), toNode.getExtendsClause())
        & isEqual(node.getWithClause(), toNode.getWithClause())
        & isEqual(node.getImplementsClause(), toNode.getImplementsClause())
        & isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getMembers(), toNode.getMembers())
        & isEqual(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitClassTypeAlias(ClassTypeAlias node) {
    ClassTypeAlias toNode = (ClassTypeAlias) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqual(node.getEquals(), toNode.getEquals())
        & isEqual(node.getAbstractKeyword(), toNode.getAbstractKeyword())
        & isEqual(node.getSuperclass(), toNode.getSuperclass())
        & isEqual(node.getWithClause(), toNode.getWithClause())
        & isEqual(node.getImplementsClause(), toNode.getImplementsClause())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitComment(Comment node) {
    Comment toNode = (Comment) this.toNode;
    return isEqual(node.getReferences(), toNode.getReferences());
  }

  @Override
  public Boolean visitCommentReference(CommentReference node) {
    CommentReference toNode = (CommentReference) this.toNode;
    return isEqual(node.getNewKeyword(), toNode.getNewKeyword())
        & isEqual(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitCompilationUnit(CompilationUnit node) {
    CompilationUnit toNode = (CompilationUnit) this.toNode;
    if (isEqual(node.getBeginToken(), toNode.getBeginToken())
        & isEqual(node.getScriptTag(), toNode.getScriptTag())
        & isEqual(node.getDirectives(), toNode.getDirectives())
        & isEqual(node.getDeclarations(), toNode.getDeclarations())
        & isEqual(node.getEndToken(), toNode.getEndToken())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression toNode = (ConditionalExpression) this.toNode;
    if (isEqual(node.getCondition(), toNode.getCondition())
        & isEqual(node.getQuestion(), toNode.getQuestion())
        & isEqual(node.getThenExpression(), toNode.getThenExpression())
        & isEqual(node.getColon(), toNode.getColon())
        & isEqual(node.getElseExpression(), toNode.getElseExpression())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration toNode = (ConstructorDeclaration) this.toNode;
    if (isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getExternalKeyword(), toNode.getExternalKeyword())
        & isEqual(node.getConstKeyword(), toNode.getConstKeyword())
        & isEqual(node.getFactoryKeyword(), toNode.getFactoryKeyword())
        & isEqual(node.getReturnType(), toNode.getReturnType())
        & isEqual(node.getPeriod(), toNode.getPeriod()) & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getParameters(), toNode.getParameters())
        & isEqual(node.getSeparator(), toNode.getSeparator())
        & isEqual(node.getInitializers(), toNode.getInitializers())
        & isEqual(node.getRedirectedConstructor(), toNode.getRedirectedConstructor())
        & isEqual(node.getBody(), toNode.getBody())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    ConstructorFieldInitializer toNode = (ConstructorFieldInitializer) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getFieldName(), toNode.getFieldName())
        & isEqual(node.getEquals(), toNode.getEquals())
        & isEqual(node.getExpression(), toNode.getExpression());
  }

  @Override
  public Boolean visitConstructorName(ConstructorName node) {
    ConstructorName toNode = (ConstructorName) this.toNode;
    if (isEqual(node.getType(), toNode.getType()) & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getName(), toNode.getName())) {
      toNode.setStaticElement(node.getStaticElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitContinueStatement(ContinueStatement node) {
    ContinueStatement toNode = (ContinueStatement) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getLabel(), toNode.getLabel())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitDeclaredIdentifier(DeclaredIdentifier node) {
    DeclaredIdentifier toNode = (DeclaredIdentifier) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getType(), toNode.getType())
        & isEqual(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitDefaultFormalParameter(DefaultFormalParameter node) {
    DefaultFormalParameter toNode = (DefaultFormalParameter) this.toNode;
    return isEqual(node.getParameter(), toNode.getParameter()) & node.getKind() == toNode.getKind()
        & isEqual(node.getSeparator(), toNode.getSeparator())
        & isEqual(node.getDefaultValue(), toNode.getDefaultValue());
  }

  @Override
  public Boolean visitDoStatement(DoStatement node) {
    DoStatement toNode = (DoStatement) this.toNode;
    return isEqual(node.getDoKeyword(), toNode.getDoKeyword())
        & isEqual(node.getBody(), toNode.getBody())
        & isEqual(node.getWhileKeyword(), toNode.getWhileKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getCondition(), toNode.getCondition())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral toNode = (DoubleLiteral) this.toNode;
    if (isEqual(node.getLiteral(), toNode.getLiteral()) & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitEmptyFunctionBody(EmptyFunctionBody node) {
    EmptyFunctionBody toNode = (EmptyFunctionBody) this.toNode;
    return isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitEmptyStatement(EmptyStatement node) {
    EmptyStatement toNode = (EmptyStatement) this.toNode;
    return isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitExportDirective(ExportDirective node) {
    ExportDirective toNode = (ExportDirective) this.toNode;
    if (isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword()) & isEqual(node.getUri(), toNode.getUri())
        & isEqual(node.getCombinators(), toNode.getCombinators())
        & isEqual(node.getSemicolon(), toNode.getSemicolon())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitExpressionFunctionBody(ExpressionFunctionBody node) {
    ExpressionFunctionBody toNode = (ExpressionFunctionBody) this.toNode;
    return isEqual(node.getFunctionDefinition(), toNode.getFunctionDefinition())
        & isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitExpressionStatement(ExpressionStatement node) {
    ExpressionStatement toNode = (ExpressionStatement) this.toNode;
    return isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitExtendsClause(ExtendsClause node) {
    ExtendsClause toNode = (ExtendsClause) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getSuperclass(), toNode.getSuperclass());
  }

  @Override
  public Boolean visitFieldDeclaration(FieldDeclaration node) {
    FieldDeclaration toNode = (FieldDeclaration) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getStaticKeyword(), toNode.getStaticKeyword())
        & isEqual(node.getFields(), toNode.getFields())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitFieldFormalParameter(FieldFormalParameter node) {
    FieldFormalParameter toNode = (FieldFormalParameter) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getType(), toNode.getType())
        & isEqual(node.getThisToken(), toNode.getThisToken())
        & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitForEachStatement(ForEachStatement node) {
    ForEachStatement toNode = (ForEachStatement) this.toNode;
    return isEqual(node.getForKeyword(), toNode.getForKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getLoopVariable(), toNode.getLoopVariable())
        & isEqual(node.getInKeyword(), toNode.getInKeyword())
        & isEqual(node.getIterator(), toNode.getIterator())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitFormalParameterList(FormalParameterList node) {
    FormalParameterList toNode = (FormalParameterList) this.toNode;
    return isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getParameters(), toNode.getParameters())
        & isEqual(node.getLeftDelimiter(), toNode.getLeftDelimiter())
        & isEqual(node.getRightDelimiter(), toNode.getRightDelimiter())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis());
  }

  @Override
  public Boolean visitForStatement(ForStatement node) {
    ForStatement toNode = (ForStatement) this.toNode;
    return isEqual(node.getForKeyword(), toNode.getForKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getVariables(), toNode.getVariables())
        & isEqual(node.getInitialization(), toNode.getInitialization())
        & isEqual(node.getLeftSeparator(), toNode.getLeftSeparator())
        & isEqual(node.getCondition(), toNode.getCondition())
        & isEqual(node.getRightSeparator(), toNode.getRightSeparator())
        & isEqual(node.getUpdaters(), toNode.getUpdaters())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitFunctionDeclaration(FunctionDeclaration node) {
    FunctionDeclaration toNode = (FunctionDeclaration) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getExternalKeyword(), toNode.getExternalKeyword())
        & isEqual(node.getReturnType(), toNode.getReturnType())
        & isEqual(node.getPropertyKeyword(), toNode.getPropertyKeyword())
        & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getFunctionExpression(), toNode.getFunctionExpression());
  }

  @Override
  public Boolean visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    FunctionDeclarationStatement toNode = (FunctionDeclarationStatement) this.toNode;
    return isEqual(node.getFunctionDeclaration(), toNode.getFunctionDeclaration());
  }

  @Override
  public Boolean visitFunctionExpression(FunctionExpression node) {
    FunctionExpression toNode = (FunctionExpression) this.toNode;
    if (isEqual(node.getParameters(), toNode.getParameters())
        & isEqual(node.getBody(), toNode.getBody())) {
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
    if (isEqual(node.getFunction(), toNode.getFunction())
        & isEqual(node.getArgumentList(), toNode.getArgumentList())) {
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
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getReturnType(), toNode.getReturnType())
        & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqual(node.getParameters(), toNode.getParameters())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    FunctionTypedFormalParameter toNode = (FunctionTypedFormalParameter) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getReturnType(), toNode.getReturnType())
        & isEqual(node.getIdentifier(), toNode.getIdentifier())
        & isEqual(node.getParameters(), toNode.getParameters());
  }

  @Override
  public Boolean visitHideCombinator(HideCombinator node) {
    HideCombinator toNode = (HideCombinator) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getHiddenNames(), toNode.getHiddenNames());
  }

  @Override
  public Boolean visitIfStatement(IfStatement node) {
    IfStatement toNode = (IfStatement) this.toNode;
    return isEqual(node.getIfKeyword(), toNode.getIfKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getCondition(), toNode.getCondition())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getThenStatement(), toNode.getThenStatement())
        & isEqual(node.getElseKeyword(), toNode.getElseKeyword())
        & isEqual(node.getElseStatement(), toNode.getElseStatement());
  }

  @Override
  public Boolean visitImplementsClause(ImplementsClause node) {
    ImplementsClause toNode = (ImplementsClause) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getInterfaces(), toNode.getInterfaces());
  }

  @Override
  public Boolean visitImportDirective(ImportDirective node) {
    ImportDirective toNode = (ImportDirective) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword()) & isEqual(node.getUri(), toNode.getUri())
        & isEqual(node.getAsToken(), toNode.getAsToken())
        & isEqual(node.getPrefix(), toNode.getPrefix())
        & isEqual(node.getCombinators(), toNode.getCombinators())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitIndexExpression(IndexExpression node) {
    IndexExpression toNode = (IndexExpression) this.toNode;
    if (isEqual(node.getTarget(), toNode.getTarget())
        & isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getIndex(), toNode.getIndex())
        & isEqual(node.getRightBracket(), toNode.getRightBracket())) {
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
    if (isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getConstructorName(), toNode.getConstructorName())
        & isEqual(node.getArgumentList(), toNode.getArgumentList())) {
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
    if (isEqual(node.getLiteral(), toNode.getLiteral()) & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitInterpolationExpression(InterpolationExpression node) {
    InterpolationExpression toNode = (InterpolationExpression) this.toNode;
    return isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitInterpolationString(InterpolationString node) {
    InterpolationString toNode = (InterpolationString) this.toNode;
    return isEqual(node.getContents(), toNode.getContents())
        & node.getValue().equals(toNode.getValue());
  }

  @Override
  public Boolean visitIsExpression(IsExpression node) {
    IsExpression toNode = (IsExpression) this.toNode;
    if (isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getIsOperator(), toNode.getIsOperator())
        & isEqual(node.getNotOperator(), toNode.getNotOperator())
        & isEqual(node.getType(), toNode.getType())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitLabel(Label node) {
    Label toNode = (Label) this.toNode;
    return isEqual(node.getLabel(), toNode.getLabel())
        & isEqual(node.getColon(), toNode.getColon());
  }

  @Override
  public Boolean visitLabeledStatement(LabeledStatement node) {
    LabeledStatement toNode = (LabeledStatement) this.toNode;
    return isEqual(node.getLabels(), toNode.getLabels())
        & isEqual(node.getStatement(), toNode.getStatement());
  }

  @Override
  public Boolean visitLibraryDirective(LibraryDirective node) {
    LibraryDirective toNode = (LibraryDirective) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getLibraryToken(), toNode.getLibraryToken())
        & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier toNode = (LibraryIdentifier) this.toNode;
    if (isEqual(node.getComponents(), toNode.getComponents())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitListLiteral(ListLiteral node) {
    ListLiteral toNode = (ListLiteral) this.toNode;
    if (isEqual(node.getConstKeyword(), toNode.getConstKeyword())
        & isEqual(node.getTypeArguments(), toNode.getTypeArguments())
        & isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getElements(), toNode.getElements())
        & isEqual(node.getRightBracket(), toNode.getRightBracket())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitMapLiteral(MapLiteral node) {
    MapLiteral toNode = (MapLiteral) this.toNode;
    if (isEqual(node.getConstKeyword(), toNode.getConstKeyword())
        & isEqual(node.getTypeArguments(), toNode.getTypeArguments())
        & isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getEntries(), toNode.getEntries())
        & isEqual(node.getRightBracket(), toNode.getRightBracket())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitMapLiteralEntry(MapLiteralEntry node) {
    MapLiteralEntry toNode = (MapLiteralEntry) this.toNode;
    return isEqual(node.getKey(), toNode.getKey())
        & isEqual(node.getSeparator(), toNode.getSeparator())
        & isEqual(node.getValue(), toNode.getValue());
  }

  @Override
  public Boolean visitMethodDeclaration(MethodDeclaration node) {
    MethodDeclaration toNode = (MethodDeclaration) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getExternalKeyword(), toNode.getExternalKeyword())
        & isEqual(node.getModifierKeyword(), toNode.getModifierKeyword())
        & isEqual(node.getReturnType(), toNode.getReturnType())
        & isEqual(node.getPropertyKeyword(), toNode.getPropertyKeyword())
        & isEqual(node.getPropertyKeyword(), toNode.getPropertyKeyword())
        & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getParameters(), toNode.getParameters())
        & isEqual(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitMethodInvocation(MethodInvocation node) {
    MethodInvocation toNode = (MethodInvocation) this.toNode;
    if (isEqual(node.getTarget(), toNode.getTarget())
        & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getMethodName(), toNode.getMethodName())
        & isEqual(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitNamedExpression(NamedExpression node) {
    NamedExpression toNode = (NamedExpression) this.toNode;
    if (isEqual(node.getName(), toNode.getName())
        & isEqual(node.getExpression(), toNode.getExpression())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitNativeClause(NativeClause node) {
    NativeClause toNode = (NativeClause) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getName(), toNode.getName());
  }

  @Override
  public Boolean visitNativeFunctionBody(NativeFunctionBody node) {
    NativeFunctionBody toNode = (NativeFunctionBody) this.toNode;
    return isEqual(node.getNativeToken(), toNode.getNativeToken())
        & isEqual(node.getStringLiteral(), toNode.getStringLiteral())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitNullLiteral(NullLiteral node) {
    NullLiteral toNode = (NullLiteral) this.toNode;
    if (isEqual(node.getLiteral(), toNode.getLiteral())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression toNode = (ParenthesizedExpression) this.toNode;
    if (isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPartDirective(PartDirective node) {
    PartDirective toNode = (PartDirective) this.toNode;
    if (isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getPartToken(), toNode.getPartToken())
        & isEqual(node.getUri(), toNode.getUri())
        & isEqual(node.getSemicolon(), toNode.getSemicolon())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPartOfDirective(PartOfDirective node) {
    PartOfDirective toNode = (PartOfDirective) this.toNode;
    if (isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getPartToken(), toNode.getPartToken())
        & isEqual(node.getOfToken(), toNode.getOfToken())
        & isEqual(node.getLibraryName(), toNode.getLibraryName())
        & isEqual(node.getSemicolon(), toNode.getSemicolon())) {
      toNode.setElement(node.getElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPostfixExpression(PostfixExpression node) {
    PostfixExpression toNode = (PostfixExpression) this.toNode;
    if (isEqual(node.getOperand(), toNode.getOperand())
        & isEqual(node.getOperator(), toNode.getOperator())) {
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
    if (isEqual(node.getPrefix(), toNode.getPrefix())
        & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getIdentifier(), toNode.getIdentifier())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitPrefixExpression(PrefixExpression node) {
    PrefixExpression toNode = (PrefixExpression) this.toNode;
    if (isEqual(node.getOperator(), toNode.getOperator())
        & isEqual(node.getOperand(), toNode.getOperand())) {
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
    if (isEqual(node.getTarget(), toNode.getTarget())
        & isEqual(node.getOperator(), toNode.getOperator())
        & isEqual(node.getPropertyName(), toNode.getPropertyName())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation toNode = (RedirectingConstructorInvocation) this.toNode;
    if (isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getConstructorName(), toNode.getConstructorName())
        & isEqual(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setStaticElement(node.getStaticElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitRethrowExpression(RethrowExpression node) {
    RethrowExpression toNode = (RethrowExpression) this.toNode;
    if (isEqual(node.getKeyword(), toNode.getKeyword())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitReturnStatement(ReturnStatement node) {
    ReturnStatement toNode = (ReturnStatement) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitScriptTag(ScriptTag node) {
    ScriptTag toNode = (ScriptTag) this.toNode;
    return isEqual(node.getScriptTag(), toNode.getScriptTag());
  }

  @Override
  public Boolean visitShowCombinator(ShowCombinator node) {
    ShowCombinator toNode = (ShowCombinator) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getShownNames(), toNode.getShownNames());
  }

  @Override
  public Boolean visitSimpleFormalParameter(SimpleFormalParameter node) {
    SimpleFormalParameter toNode = (SimpleFormalParameter) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getType(), toNode.getType())
        & isEqual(node.getIdentifier(), toNode.getIdentifier());
  }

  @Override
  public Boolean visitSimpleIdentifier(SimpleIdentifier node) {
    SimpleIdentifier toNode = (SimpleIdentifier) this.toNode;
    if (isEqual(node.getToken(), toNode.getToken())) {
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
    if (isEqual(node.getLiteral(), toNode.getLiteral()) & node.getValue() == toNode.getValue()) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitStringInterpolation(StringInterpolation node) {
    StringInterpolation toNode = (StringInterpolation) this.toNode;
    if (isEqual(node.getElements(), toNode.getElements())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation toNode = (SuperConstructorInvocation) this.toNode;
    if (isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getPeriod(), toNode.getPeriod())
        & isEqual(node.getConstructorName(), toNode.getConstructorName())
        & isEqual(node.getArgumentList(), toNode.getArgumentList())) {
      toNode.setStaticElement(node.getStaticElement());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitSuperExpression(SuperExpression node) {
    SuperExpression toNode = (SuperExpression) this.toNode;
    if (isEqual(node.getKeyword(), toNode.getKeyword())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitSwitchCase(SwitchCase node) {
    SwitchCase toNode = (SwitchCase) this.toNode;
    return isEqual(node.getLabels(), toNode.getLabels())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getColon(), toNode.getColon())
        & isEqual(node.getStatements(), toNode.getStatements());
  }

  @Override
  public Boolean visitSwitchDefault(SwitchDefault node) {
    SwitchDefault toNode = (SwitchDefault) this.toNode;
    return isEqual(node.getLabels(), toNode.getLabels())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getColon(), toNode.getColon())
        & isEqual(node.getStatements(), toNode.getStatements());
  }

  @Override
  public Boolean visitSwitchStatement(SwitchStatement node) {
    SwitchStatement toNode = (SwitchStatement) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getExpression(), toNode.getExpression())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getMembers(), toNode.getMembers())
        & isEqual(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral toNode = (SymbolLiteral) this.toNode;
    if (isEqual(node.getPoundSign(), toNode.getPoundSign())
        & isEqual(node.getComponents(), toNode.getComponents())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitThisExpression(ThisExpression node) {
    ThisExpression toNode = (ThisExpression) this.toNode;
    if (isEqual(node.getKeyword(), toNode.getKeyword())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitThrowExpression(ThrowExpression node) {
    ThrowExpression toNode = (ThrowExpression) this.toNode;
    if (isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getExpression(), toNode.getExpression())) {
      toNode.setPropagatedType(node.getPropagatedType());
      toNode.setStaticType(node.getStaticType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    TopLevelVariableDeclaration toNode = (TopLevelVariableDeclaration) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getVariables(), toNode.getVariables())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitTryStatement(TryStatement node) {
    TryStatement toNode = (TryStatement) this.toNode;
    return isEqual(node.getTryKeyword(), toNode.getTryKeyword())
        & isEqual(node.getBody(), toNode.getBody())
        & isEqual(node.getCatchClauses(), toNode.getCatchClauses())
        & isEqual(node.getFinallyKeyword(), toNode.getFinallyKeyword())
        & isEqual(node.getFinallyBlock(), toNode.getFinallyBlock());
  }

  @Override
  public Boolean visitTypeArgumentList(TypeArgumentList node) {
    TypeArgumentList toNode = (TypeArgumentList) this.toNode;
    return isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getArguments(), toNode.getArguments())
        & isEqual(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitTypeName(TypeName node) {
    TypeName toNode = (TypeName) this.toNode;
    if (isEqual(node.getName(), toNode.getName())
        & isEqual(node.getTypeArguments(), toNode.getTypeArguments())) {
      toNode.setType(node.getType());
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitTypeParameter(TypeParameter node) {
    TypeParameter toNode = (TypeParameter) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getName(), toNode.getName())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getBound(), toNode.getBound());
  }

  @Override
  public Boolean visitTypeParameterList(TypeParameterList node) {
    TypeParameterList toNode = (TypeParameterList) this.toNode;
    return isEqual(node.getLeftBracket(), toNode.getLeftBracket())
        & isEqual(node.getTypeParameters(), toNode.getTypeParameters())
        & isEqual(node.getRightBracket(), toNode.getRightBracket());
  }

  @Override
  public Boolean visitVariableDeclaration(VariableDeclaration node) {
    VariableDeclaration toNode = (VariableDeclaration) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getName(), toNode.getName()) & isEqual(node.getEquals(), toNode.getEquals())
        & isEqual(node.getInitializer(), toNode.getInitializer());
  }

  @Override
  public Boolean visitVariableDeclarationList(VariableDeclarationList node) {
    VariableDeclarationList toNode = (VariableDeclarationList) this.toNode;
    return isEqual(node.getDocumentationComment(), toNode.getDocumentationComment())
        & isEqual(node.getMetadata(), toNode.getMetadata())
        & isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getType(), toNode.getType())
        & isEqual(node.getVariables(), toNode.getVariables());
  }

  @Override
  public Boolean visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    VariableDeclarationStatement toNode = (VariableDeclarationStatement) this.toNode;
    return isEqual(node.getVariables(), toNode.getVariables())
        & isEqual(node.getSemicolon(), toNode.getSemicolon());
  }

  @Override
  public Boolean visitWhileStatement(WhileStatement node) {
    WhileStatement toNode = (WhileStatement) this.toNode;
    return isEqual(node.getKeyword(), toNode.getKeyword())
        & isEqual(node.getLeftParenthesis(), toNode.getLeftParenthesis())
        & isEqual(node.getCondition(), toNode.getCondition())
        & isEqual(node.getRightParenthesis(), toNode.getRightParenthesis())
        & isEqual(node.getBody(), toNode.getBody());
  }

  @Override
  public Boolean visitWithClause(WithClause node) {
    WithClause toNode = (WithClause) this.toNode;
    return isEqual(node.getWithKeyword(), toNode.getWithKeyword())
        & isEqual(node.getMixinTypes(), toNode.getMixinTypes());
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
  private boolean isEqual(ASTNode fromNode, ASTNode toNode) {
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
    boolean equal = true;
    for (int i = 0; i < size; i++) {
      if (!isEqual(first.get(i), second.get(i))) {
        equal = false;
      }
    }
    return equal;
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
    return first.getLexeme().equals(second.getLexeme());
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
      if (!isEqual(first[i], second[i])) {
        return false;
      }
    }
    return true;
  }
}
