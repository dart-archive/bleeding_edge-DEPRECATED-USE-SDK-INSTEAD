/*
 * Copyright (c) 2014, the Dart project authors.
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

/**
 * Instances of the class {@code NodeReplacer} implement an object that will replace one child node
 * in an AST node with another node.
 */
public class NodeReplacer implements AstVisitor<Boolean> {
  /**
   * Replace the old node with the new node in the AST structure containing the old node.
   * 
   * @param oldNode
   * @param newNode
   * @return {@code true} if the replacement was successful
   * @throws IllegalArgumentException if either node is {@code null}, if the old node does not have
   *           a parent node, or if the AST structure has been corrupted
   */
  public static boolean replace(AstNode oldNode, AstNode newNode) {
    if (oldNode == null || newNode == null) {
      throw new IllegalArgumentException("The old and new nodes must be non-null");
    } else if (oldNode == newNode) {
      return true;
    }
    AstNode parent = oldNode.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("The old node is not a child of another node");
    }
    NodeReplacer replacer = new NodeReplacer(oldNode, newNode);
    return parent.accept(replacer);
  }

  private AstNode oldNode;

  private AstNode newNode;

  private NodeReplacer(AstNode oldNode, AstNode newNode) {
    this.oldNode = oldNode;
    this.newNode = newNode;
  }

  @Override
  public Boolean visitAdjacentStrings(AdjacentStrings node) {
    if (replaceInList(node.getStrings())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  public Boolean visitAnnotatedNode(AnnotatedNode node) {
    if (node.getDocumentationComment() == oldNode) {
      node.setDocumentationComment((Comment) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getMetadata())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitAnnotation(Annotation node) {
    if (node.getArguments() == oldNode) {
      node.setArguments((ArgumentList) newNode);
      return Boolean.TRUE;
    } else if (node.getConstructorName() == oldNode) {
      node.setConstructorName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getName() == oldNode) {
      node.setName((Identifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitArgumentList(ArgumentList node) {
    if (replaceInList(node.getArguments())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitAsExpression(AsExpression node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getType() == oldNode) {
      node.setType((TypeName) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitAssertStatement(AssertStatement node) {
    if (node.getCondition() == oldNode) {
      node.setCondition((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitAssignmentExpression(AssignmentExpression node) {
    if (node.getLeftHandSide() == oldNode) {
      node.setLeftHandSide((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getRightHandSide() == oldNode) {
      node.setRightHandSide((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitAwaitExpression(AwaitExpression node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitBinaryExpression(BinaryExpression node) {
    if (node.getLeftOperand() == oldNode) {
      node.setLeftOperand((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getRightOperand() == oldNode) {
      node.setRightOperand((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitBlock(Block node) {
    if (replaceInList(node.getStatements())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitBlockFunctionBody(BlockFunctionBody node) {
    if (node.getBlock() == oldNode) {
      node.setBlock((Block) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitBooleanLiteral(BooleanLiteral node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitBreakStatement(BreakStatement node) {
    if (node.getLabel() == oldNode) {
      node.setLabel((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitCascadeExpression(CascadeExpression node) {
    if (node.getTarget() == oldNode) {
      node.setTarget((Expression) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getCascadeSections())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitCatchClause(CatchClause node) {
    if (node.getExceptionType() == oldNode) {
      node.setExceptionType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getExceptionParameter() == oldNode) {
      node.setExceptionParameter((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getStackTraceParameter() == oldNode) {
      node.setStackTraceParameter((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitClassDeclaration(ClassDeclaration node) {
    if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getTypeParameters() == oldNode) {
      node.setTypeParameters((TypeParameterList) newNode);
      return Boolean.TRUE;
    } else if (node.getExtendsClause() == oldNode) {
      node.setExtendsClause((ExtendsClause) newNode);
      return Boolean.TRUE;
    } else if (node.getWithClause() == oldNode) {
      node.setWithClause((WithClause) newNode);
      return Boolean.TRUE;
    } else if (node.getImplementsClause() == oldNode) {
      node.setImplementsClause((ImplementsClause) newNode);
      return Boolean.TRUE;
    } else if (node.getNativeClause() == oldNode) {
      node.setNativeClause((NativeClause) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getMembers())) {
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitClassTypeAlias(ClassTypeAlias node) {
    if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getTypeParameters() == oldNode) {
      node.setTypeParameters((TypeParameterList) newNode);
      return Boolean.TRUE;
    } else if (node.getSuperclass() == oldNode) {
      node.setSuperclass((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getWithClause() == oldNode) {
      node.setWithClause((WithClause) newNode);
      return Boolean.TRUE;
    } else if (node.getImplementsClause() == oldNode) {
      node.setImplementsClause((ImplementsClause) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitComment(Comment node) {
    if (replaceInList(node.getReferences())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitCommentReference(CommentReference node) {
    if (node.getIdentifier() == oldNode) {
      node.setIdentifier((Identifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitCompilationUnit(CompilationUnit node) {
    if (node.getScriptTag() == oldNode) {
      node.setScriptTag((ScriptTag) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getDirectives())) {
      return Boolean.TRUE;
    } else if (replaceInList(node.getDeclarations())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitConditionalExpression(ConditionalExpression node) {
    if (node.getCondition() == oldNode) {
      node.setCondition((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getThenExpression() == oldNode) {
      node.setThenExpression((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getElseExpression() == oldNode) {
      node.setElseExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitConstructorDeclaration(ConstructorDeclaration node) {
    if (node.getReturnType() == oldNode) {
      node.setReturnType((Identifier) newNode);
      return Boolean.TRUE;
    } else if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getParameters() == oldNode) {
      node.setParameters((FormalParameterList) newNode);
      return Boolean.TRUE;
    } else if (node.getRedirectedConstructor() == oldNode) {
      node.setRedirectedConstructor((ConstructorName) newNode);
      return Boolean.TRUE;
    } else if (node.getBody() == oldNode) {
      node.setBody((FunctionBody) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getInitializers())) {
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    if (node.getFieldName() == oldNode) {
      node.setFieldName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitConstructorName(ConstructorName node) {
    if (node.getType() == oldNode) {
      node.setType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitContinueStatement(ContinueStatement node) {
    if (node.getLabel() == oldNode) {
      node.setLabel((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitDeclaredIdentifier(DeclaredIdentifier node) {
    if (node.getType() == oldNode) {
      node.setType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getIdentifier() == oldNode) {
      node.setIdentifier((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitDefaultFormalParameter(DefaultFormalParameter node) {
    if (node.getParameter() == oldNode) {
      node.setParameter((NormalFormalParameter) newNode);
      return Boolean.TRUE;
    } else if (node.getDefaultValue() == oldNode) {
      node.setDefaultValue((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitDoStatement(DoStatement node) {
    if (node.getBody() == oldNode) {
      node.setBody((Statement) newNode);
      return Boolean.TRUE;
    } else if (node.getCondition() == oldNode) {
      node.setCondition((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitDoubleLiteral(DoubleLiteral node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitEmptyFunctionBody(EmptyFunctionBody node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitEmptyStatement(EmptyStatement node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitExportDirective(ExportDirective node) {
    return visitNamespaceDirective(node);
  }

  @Override
  public Boolean visitExpressionFunctionBody(ExpressionFunctionBody node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitExpressionStatement(ExpressionStatement node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitExtendsClause(ExtendsClause node) {
    if (node.getSuperclass() == oldNode) {
      node.setSuperclass((TypeName) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitFieldDeclaration(FieldDeclaration node) {
    if (node.getFields() == oldNode) {
      node.setFields((VariableDeclarationList) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitFieldFormalParameter(FieldFormalParameter node) {
    if (node.getType() == oldNode) {
      node.setType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getParameters() == oldNode) {
      node.setParameters((FormalParameterList) newNode);
      return Boolean.TRUE;
    }
    return visitNormalFormalParameter(node);
  }

  @Override
  public Boolean visitForEachStatement(ForEachStatement node) {
    if (node.getLoopVariable() == oldNode) {
      node.setLoopVariable((DeclaredIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getIdentifier() == oldNode) {
      node.setIdentifier((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getIterator() == oldNode) {
      node.setIterator((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getBody() == oldNode) {
      node.setBody((Statement) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitFormalParameterList(FormalParameterList node) {
    if (replaceInList(node.getParameters())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitForStatement(ForStatement node) {
    if (node.getVariables() == oldNode) {
      node.setVariables((VariableDeclarationList) newNode);
      return Boolean.TRUE;
    } else if (node.getInitialization() == oldNode) {
      node.setInitialization((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getCondition() == oldNode) {
      node.setCondition((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getBody() == oldNode) {
      node.setBody((Statement) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getUpdaters())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitFunctionDeclaration(FunctionDeclaration node) {
    if (node.getReturnType() == oldNode) {
      node.setReturnType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getFunctionExpression() == oldNode) {
      node.setFunctionExpression((FunctionExpression) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    if (node.getFunctionDeclaration() == oldNode) {
      node.setFunctionDeclaration((FunctionDeclaration) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitFunctionExpression(FunctionExpression node) {
    if (node.getParameters() == oldNode) {
      node.setParameters((FormalParameterList) newNode);
      return Boolean.TRUE;
    } else if (node.getBody() == oldNode) {
      node.setBody((FunctionBody) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    if (node.getFunction() == oldNode) {
      node.setFunction((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getArgumentList() == oldNode) {
      node.setArgumentList((ArgumentList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitFunctionTypeAlias(FunctionTypeAlias node) {
    if (node.getReturnType() == oldNode) {
      node.setReturnType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getTypeParameters() == oldNode) {
      node.setTypeParameters((TypeParameterList) newNode);
      return Boolean.TRUE;
    } else if (node.getParameters() == oldNode) {
      node.setParameters((FormalParameterList) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    if (node.getReturnType() == oldNode) {
      node.setReturnType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getParameters() == oldNode) {
      node.setParameters((FormalParameterList) newNode);
      return Boolean.TRUE;
    }
    return visitNormalFormalParameter(node);
  }

  @Override
  public Boolean visitHideCombinator(HideCombinator node) {
    if (replaceInList(node.getHiddenNames())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitIfStatement(IfStatement node) {
    if (node.getCondition() == oldNode) {
      node.setCondition((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getThenStatement() == oldNode) {
      node.setThenStatement((Statement) newNode);
      return Boolean.TRUE;
    } else if (node.getElseStatement() == oldNode) {
      node.setElseStatement((Statement) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitImplementsClause(ImplementsClause node) {
    if (replaceInList(node.getInterfaces())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitImportDirective(ImportDirective node) {
    if (node.getPrefix() == oldNode) {
      node.setPrefix((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNamespaceDirective(node);
  }

  @Override
  public Boolean visitIndexExpression(IndexExpression node) {
    if (node.getTarget() == oldNode) {
      node.setTarget((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getIndex() == oldNode) {
      node.setIndex((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitInstanceCreationExpression(InstanceCreationExpression node) {
    if (node.getConstructorName() == oldNode) {
      node.setConstructorName((ConstructorName) newNode);
      return Boolean.TRUE;
    } else if (node.getArgumentList() == oldNode) {
      node.setArgumentList((ArgumentList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitIntegerLiteral(IntegerLiteral node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitInterpolationExpression(InterpolationExpression node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitInterpolationString(InterpolationString node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitIsExpression(IsExpression node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getType() == oldNode) {
      node.setType((TypeName) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitLabel(Label node) {
    if (node.getLabel() == oldNode) {
      node.setLabel((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitLabeledStatement(LabeledStatement node) {
    if (node.getStatement() == oldNode) {
      node.setStatement((Statement) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getLabels())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitLibraryDirective(LibraryDirective node) {
    if (node.getName() == oldNode) {
      node.setName((LibraryIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitLibraryIdentifier(LibraryIdentifier node) {
    if (replaceInList(node.getComponents())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitListLiteral(ListLiteral node) {
    if (replaceInList(node.getElements())) {
      return Boolean.TRUE;
    }
    return visitTypedLiteral(node);
  }

  @Override
  public Boolean visitMapLiteral(MapLiteral node) {
    if (replaceInList(node.getEntries())) {
      return Boolean.TRUE;
    }
    return visitTypedLiteral(node);
  }

  @Override
  public Boolean visitMapLiteralEntry(MapLiteralEntry node) {
    if (node.getKey() == oldNode) {
      node.setKey((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getValue() == oldNode) {
      node.setValue((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitMethodDeclaration(MethodDeclaration node) {
    if (node.getReturnType() == oldNode) {
      node.setReturnType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getParameters() == oldNode) {
      node.setParameters((FormalParameterList) newNode);
      return Boolean.TRUE;
    } else if (node.getBody() == oldNode) {
      node.setBody((FunctionBody) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitMethodInvocation(MethodInvocation node) {
    if (node.getTarget() == oldNode) {
      node.setTarget((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getMethodName() == oldNode) {
      node.setMethodName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getArgumentList() == oldNode) {
      node.setArgumentList((ArgumentList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitNamedExpression(NamedExpression node) {
    if (node.getName() == oldNode) {
      node.setName((Label) newNode);
      return Boolean.TRUE;
    } else if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  public Boolean visitNamespaceDirective(NamespaceDirective node) {
    if (replaceInList(node.getCombinators())) {
      return Boolean.TRUE;
    }
    return visitUriBasedDirective(node);
  }

  @Override
  public Boolean visitNativeClause(NativeClause node) {
    if (node.getName() == oldNode) {
      node.setName((StringLiteral) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitNativeFunctionBody(NativeFunctionBody node) {
    if (node.getStringLiteral() == oldNode) {
      node.setStringLiteral((StringLiteral) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  public Boolean visitNode(AstNode node) {
    throw new IllegalArgumentException("The old node is not a child of it's parent");
  }

  public Boolean visitNormalFormalParameter(NormalFormalParameter node) {
    if (node.getDocumentationComment() == oldNode) {
      node.setDocumentationComment((Comment) newNode);
      return Boolean.TRUE;
    } else if (node.getIdentifier() == oldNode) {
      node.setIdentifier((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getMetadata())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitNullLiteral(NullLiteral node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitParenthesizedExpression(ParenthesizedExpression node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitPartDirective(PartDirective node) {
    return visitUriBasedDirective(node);
  }

  @Override
  public Boolean visitPartOfDirective(PartOfDirective node) {
    if (node.getLibraryName() == oldNode) {
      node.setLibraryName((LibraryIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitPostfixExpression(PostfixExpression node) {
    if (node.getOperand() == oldNode) {
      node.setOperand((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (node.getPrefix() == oldNode) {
      node.setPrefix((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getIdentifier() == oldNode) {
      node.setIdentifier((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitPrefixExpression(PrefixExpression node) {
    if (node.getOperand() == oldNode) {
      node.setOperand((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitPropertyAccess(PropertyAccess node) {
    if (node.getTarget() == oldNode) {
      node.setTarget((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getPropertyName() == oldNode) {
      node.setPropertyName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    if (node.getConstructorName() == oldNode) {
      node.setConstructorName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getArgumentList() == oldNode) {
      node.setArgumentList((ArgumentList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitRethrowExpression(RethrowExpression node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitReturnStatement(ReturnStatement node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitScriptTag(ScriptTag scriptTag) {
    return visitNode(scriptTag);
  }

  @Override
  public Boolean visitShowCombinator(ShowCombinator node) {
    if (replaceInList(node.getShownNames())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitSimpleFormalParameter(SimpleFormalParameter node) {
    if (node.getType() == oldNode) {
      node.setType((TypeName) newNode);
      return Boolean.TRUE;
    }
    return visitNormalFormalParameter(node);
  }

  @Override
  public Boolean visitSimpleIdentifier(SimpleIdentifier node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitSimpleStringLiteral(SimpleStringLiteral node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitStringInterpolation(StringInterpolation node) {
    if (replaceInList(node.getElements())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    if (node.getConstructorName() == oldNode) {
      node.setConstructorName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getArgumentList() == oldNode) {
      node.setArgumentList((ArgumentList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitSuperExpression(SuperExpression node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitSwitchCase(SwitchCase node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitSwitchMember(node);
  }

  @Override
  public Boolean visitSwitchDefault(SwitchDefault node) {
    return visitSwitchMember(node);
  }

  public Boolean visitSwitchMember(SwitchMember node) {
    if (replaceInList(node.getLabels())) {
      return Boolean.TRUE;
    } else if (replaceInList(node.getStatements())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitSwitchStatement(SwitchStatement node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getMembers())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitSymbolLiteral(SymbolLiteral node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitThisExpression(ThisExpression node) {
    return visitNode(node);
  }

  @Override
  public Boolean visitThrowExpression(ThrowExpression node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    if (node.getVariables() == oldNode) {
      node.setVariables((VariableDeclarationList) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitTryStatement(TryStatement node) {
    if (node.getBody() == oldNode) {
      node.setBody((Block) newNode);
      return Boolean.TRUE;
    } else if (node.getFinallyBlock() == oldNode) {
      node.setFinallyBlock((Block) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getCatchClauses())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitTypeArgumentList(TypeArgumentList node) {
    if (replaceInList(node.getArguments())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  public Boolean visitTypedLiteral(TypedLiteral node) {
    if (node.getTypeArguments() == oldNode) {
      node.setTypeArguments((TypeArgumentList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitTypeName(TypeName node) {
    if (node.getName() == oldNode) {
      node.setName((Identifier) newNode);
      return Boolean.TRUE;
    } else if (node.getTypeArguments() == oldNode) {
      node.setTypeArguments((TypeArgumentList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitTypeParameter(TypeParameter node) {
    if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getBound() == oldNode) {
      node.setBound((TypeName) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitTypeParameterList(TypeParameterList node) {
    if (replaceInList(node.getTypeParameters())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  public Boolean visitUriBasedDirective(UriBasedDirective node) {
    if (node.getUri() == oldNode) {
      node.setUri((StringLiteral) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitVariableDeclaration(VariableDeclaration node) {
    if (node.getName() == oldNode) {
      node.setName((SimpleIdentifier) newNode);
      return Boolean.TRUE;
    } else if (node.getInitializer() == oldNode) {
      node.setInitializer((Expression) newNode);
      return Boolean.TRUE;
    }
    return visitAnnotatedNode(node);
  }

  @Override
  public Boolean visitVariableDeclarationList(VariableDeclarationList node) {
    if (node.getType() == oldNode) {
      node.setType((TypeName) newNode);
      return Boolean.TRUE;
    } else if (replaceInList(node.getVariables())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    if (node.getVariables() == oldNode) {
      node.setVariables((VariableDeclarationList) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitWhileStatement(WhileStatement node) {
    if (node.getCondition() == oldNode) {
      node.setCondition((Expression) newNode);
      return Boolean.TRUE;
    } else if (node.getBody() == oldNode) {
      node.setBody((Statement) newNode);
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitWithClause(WithClause node) {
    if (replaceInList(node.getMixinTypes())) {
      return Boolean.TRUE;
    }
    return visitNode(node);
  }

  @Override
  public Boolean visitYieldStatement(YieldStatement node) {
    if (node.getExpression() == oldNode) {
      node.setExpression((Expression) newNode);
    }
    return visitNode(node);
  }

  @SuppressWarnings("unchecked")
  private <E extends AstNode> boolean replaceInList(NodeList<E> list) {
    int count = list.size();
    for (int i = 0; i < count; i++) {
      if (oldNode == list.get(i)) {
        list.set(i, (E) newNode);
        return true;
      }
    }
    return false;
  }
}
