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
package com.google.dart.engine.ast;

/**
 * The interface {@code AstVisitor} defines the behavior of objects that can be used to visit an AST
 * structure.
 * 
 * @coverage dart.engine.ast
 */
public interface AstVisitor<R> {
  public R visitAdjacentStrings(AdjacentStrings node);

  public R visitAnnotation(Annotation node);

  public R visitArgumentList(ArgumentList node);

  public R visitAsExpression(AsExpression node);

  public R visitAssertStatement(AssertStatement assertStatement);

  public R visitAssignmentExpression(AssignmentExpression node);

  public R visitAwaitExpression(AwaitExpression node);

  public R visitBinaryExpression(BinaryExpression node);

  public R visitBlock(Block node);

  public R visitBlockFunctionBody(BlockFunctionBody node);

  public R visitBooleanLiteral(BooleanLiteral node);

  public R visitBreakStatement(BreakStatement node);

  public R visitCascadeExpression(CascadeExpression node);

  public R visitCatchClause(CatchClause node);

  public R visitClassDeclaration(ClassDeclaration node);

  public R visitClassTypeAlias(ClassTypeAlias node);

  public R visitComment(Comment node);

  public R visitCommentReference(CommentReference node);

  public R visitCompilationUnit(CompilationUnit node);

  public R visitConditionalExpression(ConditionalExpression node);

  public R visitConstructorDeclaration(ConstructorDeclaration node);

  public R visitConstructorFieldInitializer(ConstructorFieldInitializer node);

  public R visitConstructorName(ConstructorName node);

  public R visitContinueStatement(ContinueStatement node);

  public R visitDeclaredIdentifier(DeclaredIdentifier node);

  public R visitDefaultFormalParameter(DefaultFormalParameter node);

  public R visitDoStatement(DoStatement node);

  public R visitDoubleLiteral(DoubleLiteral node);

  public R visitEmptyFunctionBody(EmptyFunctionBody node);

  public R visitEmptyStatement(EmptyStatement node);

  public R visitEnumConstantDeclaration(EnumConstantDeclaration node);

  public R visitEnumDeclaration(EnumDeclaration node);

  public R visitExportDirective(ExportDirective node);

  public R visitExpressionFunctionBody(ExpressionFunctionBody node);

  public R visitExpressionStatement(ExpressionStatement node);

  public R visitExtendsClause(ExtendsClause node);

  public R visitFieldDeclaration(FieldDeclaration node);

  public R visitFieldFormalParameter(FieldFormalParameter node);

  public R visitForEachStatement(ForEachStatement node);

  public R visitFormalParameterList(FormalParameterList node);

  public R visitForStatement(ForStatement node);

  public R visitFunctionDeclaration(FunctionDeclaration node);

  public R visitFunctionDeclarationStatement(FunctionDeclarationStatement node);

  public R visitFunctionExpression(FunctionExpression node);

  public R visitFunctionExpressionInvocation(FunctionExpressionInvocation node);

  public R visitFunctionTypeAlias(FunctionTypeAlias functionTypeAlias);

  public R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node);

  public R visitHideCombinator(HideCombinator node);

  public R visitIfStatement(IfStatement node);

  public R visitImplementsClause(ImplementsClause node);

  public R visitImportDirective(ImportDirective node);

  public R visitIndexExpression(IndexExpression node);

  public R visitInstanceCreationExpression(InstanceCreationExpression node);

  public R visitIntegerLiteral(IntegerLiteral node);

  public R visitInterpolationExpression(InterpolationExpression node);

  public R visitInterpolationString(InterpolationString node);

  public R visitIsExpression(IsExpression node);

  public R visitLabel(Label node);

  public R visitLabeledStatement(LabeledStatement node);

  public R visitLibraryDirective(LibraryDirective node);

  public R visitLibraryIdentifier(LibraryIdentifier node);

  public R visitListLiteral(ListLiteral node);

  public R visitMapLiteral(MapLiteral node);

  public R visitMapLiteralEntry(MapLiteralEntry node);

  public R visitMethodDeclaration(MethodDeclaration node);

  public R visitMethodInvocation(MethodInvocation node);

  public R visitNamedExpression(NamedExpression node);

  public R visitNativeClause(NativeClause node);

  public R visitNativeFunctionBody(NativeFunctionBody node);

  public R visitNullLiteral(NullLiteral node);

  public R visitParenthesizedExpression(ParenthesizedExpression node);

  public R visitPartDirective(PartDirective node);

  public R visitPartOfDirective(PartOfDirective node);

  public R visitPostfixExpression(PostfixExpression node);

  public R visitPrefixedIdentifier(PrefixedIdentifier node);

  public R visitPrefixExpression(PrefixExpression node);

  public R visitPropertyAccess(PropertyAccess node);

  public R visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node);

  public R visitRethrowExpression(RethrowExpression node);

  public R visitReturnStatement(ReturnStatement node);

  public R visitScriptTag(ScriptTag node);

  public R visitShowCombinator(ShowCombinator node);

  public R visitSimpleFormalParameter(SimpleFormalParameter node);

  public R visitSimpleIdentifier(SimpleIdentifier node);

  public R visitSimpleStringLiteral(SimpleStringLiteral node);

  public R visitStringInterpolation(StringInterpolation node);

  public R visitSuperConstructorInvocation(SuperConstructorInvocation node);

  public R visitSuperExpression(SuperExpression node);

  public R visitSwitchCase(SwitchCase node);

  public R visitSwitchDefault(SwitchDefault node);

  public R visitSwitchStatement(SwitchStatement node);

  public R visitSymbolLiteral(SymbolLiteral node);

  public R visitThisExpression(ThisExpression node);

  public R visitThrowExpression(ThrowExpression node);

  public R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node);

  public R visitTryStatement(TryStatement node);

  public R visitTypeArgumentList(TypeArgumentList node);

  public R visitTypeName(TypeName node);

  public R visitTypeParameter(TypeParameter node);

  public R visitTypeParameterList(TypeParameterList node);

  public R visitVariableDeclaration(VariableDeclaration node);

  public R visitVariableDeclarationList(VariableDeclarationList node);

  public R visitVariableDeclarationStatement(VariableDeclarationStatement node);

  public R visitWhileStatement(WhileStatement node);

  public R visitWithClause(WithClause node);

  public R visitYieldStatement(YieldStatement node);
}
