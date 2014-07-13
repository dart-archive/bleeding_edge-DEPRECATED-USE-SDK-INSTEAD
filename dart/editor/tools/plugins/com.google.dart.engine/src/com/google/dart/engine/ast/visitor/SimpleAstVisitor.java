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
 * Instances of the class {@code SimpleAstVisitor} implement an AST visitor that will do nothing
 * when visiting an AST node. It is intended to be a superclass for classes that use the visitor
 * pattern primarily as a dispatch mechanism (and hence don't need to recursively visit a whole
 * structure) and that only need to visit a small number of node types.
 * 
 * @coverage dart.engine.ast
 */
public class SimpleAstVisitor<R> implements AstVisitor<R> {
  @Override
  public R visitAdjacentStrings(AdjacentStrings node) {
    return null;
  }

  @Override
  public R visitAnnotation(Annotation node) {
    return null;
  }

  @Override
  public R visitArgumentList(ArgumentList node) {
    return null;
  }

  @Override
  public R visitAsExpression(AsExpression node) {
    return null;
  }

  @Override
  public R visitAssertStatement(AssertStatement node) {
    return null;
  }

  @Override
  public R visitAssignmentExpression(AssignmentExpression node) {
    return null;
  }

  @Override
  public R visitAwaitExpression(AwaitExpression node) {
    return null;
  }

  @Override
  public R visitBinaryExpression(BinaryExpression node) {
    return null;
  }

  @Override
  public R visitBlock(Block node) {
    return null;
  }

  @Override
  public R visitBlockFunctionBody(BlockFunctionBody node) {
    return null;
  }

  @Override
  public R visitBooleanLiteral(BooleanLiteral node) {
    return null;
  }

  @Override
  public R visitBreakStatement(BreakStatement node) {
    return null;
  }

  @Override
  public R visitCascadeExpression(CascadeExpression node) {
    return null;
  }

  @Override
  public R visitCatchClause(CatchClause node) {
    return null;
  }

  @Override
  public R visitClassDeclaration(ClassDeclaration node) {
    return null;
  }

  @Override
  public R visitClassTypeAlias(ClassTypeAlias node) {
    return null;
  }

  @Override
  public R visitComment(Comment node) {
    return null;
  }

  @Override
  public R visitCommentReference(CommentReference node) {
    return null;
  }

  @Override
  public R visitCompilationUnit(CompilationUnit node) {
    return null;
  }

  @Override
  public R visitConditionalExpression(ConditionalExpression node) {
    return null;
  }

  @Override
  public R visitConstructorDeclaration(ConstructorDeclaration node) {
    return null;
  }

  @Override
  public R visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    return null;
  }

  @Override
  public R visitConstructorName(ConstructorName node) {
    return null;
  }

  @Override
  public R visitContinueStatement(ContinueStatement node) {
    return null;
  }

  @Override
  public R visitDeclaredIdentifier(DeclaredIdentifier node) {
    return null;
  }

  @Override
  public R visitDefaultFormalParameter(DefaultFormalParameter node) {
    return null;
  }

  @Override
  public R visitDoStatement(DoStatement node) {
    return null;
  }

  @Override
  public R visitDoubleLiteral(DoubleLiteral node) {
    return null;
  }

  @Override
  public R visitEmptyFunctionBody(EmptyFunctionBody node) {
    return null;
  }

  @Override
  public R visitEmptyStatement(EmptyStatement node) {
    return null;
  }

  @Override
  public R visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    return null;
  }

  @Override
  public R visitEnumDeclaration(EnumDeclaration node) {
    return null;
  }

  @Override
  public R visitExportDirective(ExportDirective node) {
    return null;
  }

  @Override
  public R visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return null;
  }

  @Override
  public R visitExpressionStatement(ExpressionStatement node) {
    return null;
  }

  @Override
  public R visitExtendsClause(ExtendsClause node) {
    return null;
  }

  @Override
  public R visitFieldDeclaration(FieldDeclaration node) {
    return null;
  }

  @Override
  public R visitFieldFormalParameter(FieldFormalParameter node) {
    return null;
  }

  @Override
  public R visitForEachStatement(ForEachStatement node) {
    return null;
  }

  @Override
  public R visitFormalParameterList(FormalParameterList node) {
    return null;
  }

  @Override
  public R visitForStatement(ForStatement node) {
    return null;
  }

  @Override
  public R visitFunctionDeclaration(FunctionDeclaration node) {
    return null;
  }

  @Override
  public R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    return null;
  }

  @Override
  public R visitFunctionExpression(FunctionExpression node) {
    return null;
  }

  @Override
  public R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    return null;
  }

  @Override
  public R visitFunctionTypeAlias(FunctionTypeAlias node) {
    return null;
  }

  @Override
  public R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    return null;
  }

  @Override
  public R visitHideCombinator(HideCombinator node) {
    return null;
  }

  @Override
  public R visitIfStatement(IfStatement node) {
    return null;
  }

  @Override
  public R visitImplementsClause(ImplementsClause node) {
    return null;
  }

  @Override
  public R visitImportDirective(ImportDirective node) {
    return null;
  }

  @Override
  public R visitIndexExpression(IndexExpression node) {
    return null;
  }

  @Override
  public R visitInstanceCreationExpression(InstanceCreationExpression node) {
    return null;
  }

  @Override
  public R visitIntegerLiteral(IntegerLiteral node) {
    return null;
  }

  @Override
  public R visitInterpolationExpression(InterpolationExpression node) {
    return null;
  }

  @Override
  public R visitInterpolationString(InterpolationString node) {
    return null;
  }

  @Override
  public R visitIsExpression(IsExpression node) {
    return null;
  }

  @Override
  public R visitLabel(Label node) {
    return null;
  }

  @Override
  public R visitLabeledStatement(LabeledStatement node) {
    return null;
  }

  @Override
  public R visitLibraryDirective(LibraryDirective node) {
    return null;
  }

  @Override
  public R visitLibraryIdentifier(LibraryIdentifier node) {
    return null;
  }

  @Override
  public R visitListLiteral(ListLiteral node) {
    return null;
  }

  @Override
  public R visitMapLiteral(MapLiteral node) {
    return null;
  }

  @Override
  public R visitMapLiteralEntry(MapLiteralEntry node) {
    return null;
  }

  @Override
  public R visitMethodDeclaration(MethodDeclaration node) {
    return null;
  }

  @Override
  public R visitMethodInvocation(MethodInvocation node) {
    return null;
  }

  @Override
  public R visitNamedExpression(NamedExpression node) {
    return null;
  }

  @Override
  public R visitNativeClause(NativeClause node) {
    return null;
  }

  @Override
  public R visitNativeFunctionBody(NativeFunctionBody node) {
    return null;
  }

  @Override
  public R visitNullLiteral(NullLiteral node) {
    return null;
  }

  @Override
  public R visitParenthesizedExpression(ParenthesizedExpression node) {
    return null;
  }

  @Override
  public R visitPartDirective(PartDirective node) {
    return null;
  }

  @Override
  public R visitPartOfDirective(PartOfDirective node) {
    return null;
  }

  @Override
  public R visitPostfixExpression(PostfixExpression node) {
    return null;
  }

  @Override
  public R visitPrefixedIdentifier(PrefixedIdentifier node) {
    return null;
  }

  @Override
  public R visitPrefixExpression(PrefixExpression node) {
    return null;
  }

  @Override
  public R visitPropertyAccess(PropertyAccess node) {
    return null;
  }

  @Override
  public R visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    return null;
  }

  @Override
  public R visitRethrowExpression(RethrowExpression node) {
    return null;
  }

  @Override
  public R visitReturnStatement(ReturnStatement node) {
    return null;
  }

  @Override
  public R visitScriptTag(ScriptTag node) {
    return null;
  }

  @Override
  public R visitShowCombinator(ShowCombinator node) {
    return null;
  }

  @Override
  public R visitSimpleFormalParameter(SimpleFormalParameter node) {
    return null;
  }

  @Override
  public R visitSimpleIdentifier(SimpleIdentifier node) {
    return null;
  }

  @Override
  public R visitSimpleStringLiteral(SimpleStringLiteral node) {
    return null;
  }

  @Override
  public R visitStringInterpolation(StringInterpolation node) {
    return null;
  }

  @Override
  public R visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    return null;
  }

  @Override
  public R visitSuperExpression(SuperExpression node) {
    return null;
  }

  @Override
  public R visitSwitchCase(SwitchCase node) {
    return null;
  }

  @Override
  public R visitSwitchDefault(SwitchDefault node) {
    return null;
  }

  @Override
  public R visitSwitchStatement(SwitchStatement node) {
    return null;
  }

  @Override
  public R visitSymbolLiteral(SymbolLiteral node) {
    return null;
  }

  @Override
  public R visitThisExpression(ThisExpression node) {
    return null;
  }

  @Override
  public R visitThrowExpression(ThrowExpression node) {
    return null;
  }

  @Override
  public R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    return null;
  }

  @Override
  public R visitTryStatement(TryStatement node) {
    return null;
  }

  @Override
  public R visitTypeArgumentList(TypeArgumentList node) {
    return null;
  }

  @Override
  public R visitTypeName(TypeName node) {
    return null;
  }

  @Override
  public R visitTypeParameter(TypeParameter node) {
    return null;
  }

  @Override
  public R visitTypeParameterList(TypeParameterList node) {
    return null;
  }

  @Override
  public R visitVariableDeclaration(VariableDeclaration node) {
    return null;
  }

  @Override
  public R visitVariableDeclarationList(VariableDeclarationList node) {
    return null;
  }

  @Override
  public R visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    return null;
  }

  @Override
  public R visitWhileStatement(WhileStatement node) {
    return null;
  }

  @Override
  public R visitWithClause(WithClause node) {
    return null;
  }

  @Override
  public R visitYieldStatement(YieldStatement node) {
    return null;
  }
}
