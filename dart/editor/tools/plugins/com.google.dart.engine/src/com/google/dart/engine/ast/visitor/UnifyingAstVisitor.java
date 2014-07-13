/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.*;

/**
 * Instances of the class {@code UnifyingAstVisitor} implement an AST visitor that will recursively
 * visit all of the nodes in an AST structure (like instances of the class
 * {@link RecursiveAstVisitor}). In addition, every node will also be visited by using a single
 * unified {@link #visitNode(AstNode)} method.
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general {@link #visitNode(AstNode)} method. Failure to do so will
 * cause the children of the visited node to not be visited.
 * 
 * @coverage dart.engine.ast
 */
public class UnifyingAstVisitor<R> implements AstVisitor<R> {
  @Override
  public R visitAdjacentStrings(AdjacentStrings node) {
    return visitNode(node);
  }

  @Override
  public R visitAnnotation(Annotation node) {
    return visitNode(node);
  }

  @Override
  public R visitArgumentList(ArgumentList node) {
    return visitNode(node);
  }

  @Override
  public R visitAsExpression(AsExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitAssertStatement(AssertStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitAssignmentExpression(AssignmentExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitAwaitExpression(AwaitExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitBinaryExpression(BinaryExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitBlock(Block node) {
    return visitNode(node);
  }

  @Override
  public R visitBlockFunctionBody(BlockFunctionBody node) {
    return visitNode(node);
  }

  @Override
  public R visitBooleanLiteral(BooleanLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitBreakStatement(BreakStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitCascadeExpression(CascadeExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitCatchClause(CatchClause node) {
    return visitNode(node);
  }

  @Override
  public R visitClassDeclaration(ClassDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitClassTypeAlias(ClassTypeAlias node) {
    return visitNode(node);
  }

  @Override
  public R visitComment(Comment node) {
    return visitNode(node);
  }

  @Override
  public R visitCommentReference(CommentReference node) {
    return visitNode(node);
  }

  @Override
  public R visitCompilationUnit(CompilationUnit node) {
    return visitNode(node);
  }

  @Override
  public R visitConditionalExpression(ConditionalExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitConstructorDeclaration(ConstructorDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    return visitNode(node);
  }

  @Override
  public R visitConstructorName(ConstructorName node) {
    return visitNode(node);
  }

  @Override
  public R visitContinueStatement(ContinueStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitDeclaredIdentifier(DeclaredIdentifier node) {
    return visitNode(node);
  }

  @Override
  public R visitDefaultFormalParameter(DefaultFormalParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitDoStatement(DoStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitDoubleLiteral(DoubleLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitEmptyFunctionBody(EmptyFunctionBody node) {
    return visitNode(node);
  }

  @Override
  public R visitEmptyStatement(EmptyStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitEnumDeclaration(EnumDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitExportDirective(ExportDirective node) {
    return visitNode(node);
  }

  @Override
  public R visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return visitNode(node);
  }

  @Override
  public R visitExpressionStatement(ExpressionStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitExtendsClause(ExtendsClause node) {
    return visitNode(node);
  }

  @Override
  public R visitFieldDeclaration(FieldDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitFieldFormalParameter(FieldFormalParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitForEachStatement(ForEachStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitFormalParameterList(FormalParameterList node) {
    return visitNode(node);
  }

  @Override
  public R visitForStatement(ForStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionDeclaration(FunctionDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionExpression(FunctionExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionTypeAlias(FunctionTypeAlias node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitHideCombinator(HideCombinator node) {
    return visitNode(node);
  }

  @Override
  public R visitIfStatement(IfStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitImplementsClause(ImplementsClause node) {
    return visitNode(node);
  }

  @Override
  public R visitImportDirective(ImportDirective node) {
    return visitNode(node);
  }

  @Override
  public R visitIndexExpression(IndexExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitInstanceCreationExpression(InstanceCreationExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitIntegerLiteral(IntegerLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitInterpolationExpression(InterpolationExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitInterpolationString(InterpolationString node) {
    return visitNode(node);
  }

  @Override
  public R visitIsExpression(IsExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitLabel(Label node) {
    return visitNode(node);
  }

  @Override
  public R visitLabeledStatement(LabeledStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitLibraryDirective(LibraryDirective node) {
    return visitNode(node);
  }

  @Override
  public R visitLibraryIdentifier(LibraryIdentifier node) {
    return visitNode(node);
  }

  @Override
  public R visitListLiteral(ListLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitMapLiteral(MapLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitMapLiteralEntry(MapLiteralEntry node) {
    return visitNode(node);
  }

  @Override
  public R visitMethodDeclaration(MethodDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitMethodInvocation(MethodInvocation node) {
    return visitNode(node);
  }

  @Override
  public R visitNamedExpression(NamedExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitNativeClause(NativeClause node) {
    return visitNode(node);
  }

  @Override
  public R visitNativeFunctionBody(NativeFunctionBody node) {
    return visitNode(node);
  }

  public R visitNode(AstNode node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitNullLiteral(NullLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitParenthesizedExpression(ParenthesizedExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitPartDirective(PartDirective node) {
    return visitNode(node);
  }

  @Override
  public R visitPartOfDirective(PartOfDirective node) {
    return visitNode(node);
  }

  @Override
  public R visitPostfixExpression(PostfixExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitPrefixedIdentifier(PrefixedIdentifier node) {
    return visitNode(node);
  }

  @Override
  public R visitPrefixExpression(PrefixExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitPropertyAccess(PropertyAccess node) {
    return visitNode(node);
  }

  @Override
  public R visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    return visitNode(node);
  }

  @Override
  public R visitRethrowExpression(RethrowExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitReturnStatement(ReturnStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitScriptTag(ScriptTag scriptTag) {
    return visitNode(scriptTag);
  }

  @Override
  public R visitShowCombinator(ShowCombinator node) {
    return visitNode(node);
  }

  @Override
  public R visitSimpleFormalParameter(SimpleFormalParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitSimpleIdentifier(SimpleIdentifier node) {
    return visitNode(node);
  }

  @Override
  public R visitSimpleStringLiteral(SimpleStringLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitStringInterpolation(StringInterpolation node) {
    return visitNode(node);
  }

  @Override
  public R visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    return visitNode(node);
  }

  @Override
  public R visitSuperExpression(SuperExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitSwitchCase(SwitchCase node) {
    return visitNode(node);
  }

  @Override
  public R visitSwitchDefault(SwitchDefault node) {
    return visitNode(node);
  }

  @Override
  public R visitSwitchStatement(SwitchStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitSymbolLiteral(SymbolLiteral node) {
    return visitNode(node);
  }

  @Override
  public R visitThisExpression(ThisExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitThrowExpression(ThrowExpression node) {
    return visitNode(node);
  }

  @Override
  public R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitTryStatement(TryStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitTypeArgumentList(TypeArgumentList node) {
    return visitNode(node);
  }

  @Override
  public R visitTypeName(TypeName node) {
    return visitNode(node);
  }

  @Override
  public R visitTypeParameter(TypeParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitTypeParameterList(TypeParameterList node) {
    return visitNode(node);
  }

  @Override
  public R visitVariableDeclaration(VariableDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitVariableDeclarationList(VariableDeclarationList node) {
    return visitNode(node);
  }

  @Override
  public R visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitWhileStatement(WhileStatement node) {
    return visitNode(node);
  }

  @Override
  public R visitWithClause(WithClause node) {
    return visitNode(node);
  }

  @Override
  public R visitYieldStatement(YieldStatement node) {
    return visitNode(node);
  }
}
