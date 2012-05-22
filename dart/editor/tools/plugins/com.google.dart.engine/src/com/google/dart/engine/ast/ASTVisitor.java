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
 * Instances of the class <code>ASTVisitor</code> can be used to visit an AST structure.
 */
public class ASTVisitor<R> {
  public R visitAdjacentStrings(AdjacentStrings node) {
    return visitStringLiteral(node);
  }

  public R visitArgumentList(ArgumentList node) {
    return visitNode(node);
  }

  public R visitArrayAccess(ArrayAccess node) {
    return visitExpression(node);
  }

  public R visitAssertStatement(AssertStatement node) {
    return visitStatement(node);
  }

  public R visitAssignmentExpression(AssignmentExpression node) {
    return visitExpression(node);
  }

  public R visitBinaryExpression(BinaryExpression node) {
    return visitExpression(node);
  }

  public R visitBlock(Block node) {
    return visitStatement(node);
  }

  public R visitBlockFunctionBody(BlockFunctionBody node) {
    return visitFunctionBody(node);
  }

  public R visitBooleanLiteral(BooleanLiteral node) {
    return visitLiteral(node);
  }

  public R visitBreakStatement(BreakStatement node) {
    return visitStatement(node);
  }

  public R visitCatchClause(CatchClause node) {
    return visitNode(node);
  }

  public R visitClassDeclaration(ClassDeclaration node) {
    return visitTypeDeclaration(node);
  }

  public R visitClassExtendsClause(ClassExtendsClause node) {
    return visitNode(node);
  }

  public R visitComment(Comment node) {
    return visitNode(node);
  }

  public R visitCommentReference(CommentReference node) {
    return visitNode(node);
  }

  public R visitCompilationUnit(CompilationUnit node) {
    return visitNode(node);
  }

  public R visitCompilationUnitMember(CompilationUnitMember node) {
    return visitDeclaration(node);
  }

  public R visitConditionalExpression(ConditionalExpression node) {
    return visitExpression(node);
  }

  public R visitConstructorDeclaration(ConstructorDeclaration node) {
    return visitTypeMember(node);
  }

  public R visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    return visitConstructorInitializer(node);
  }

  public R visitConstructorInitializer(ConstructorInitializer node) {
    return visitNode(node);
  }

  public R visitContinueStatement(ContinueStatement node) {
    return visitStatement(node);
  }

  public R visitDeclaration(Declaration node) {
    return visitNode(node);
  }

  public R visitDefaultClause(DefaultClause node) {
    return visitNode(node);
  }

  public R visitDirective(Directive node) {
    return visitNode(node);
  }

  public R visitDoStatement(DoStatement node) {
    return visitStatement(node);
  }

  public R visitDoubleLiteral(DoubleLiteral node) {
    return visitLiteral(node);
  }

  public R visitEmptyStatement(EmptyStatement node) {
    return visitStatement(node);
  }

  public R visitExpression(Expression node) {
    return visitNode(node);
  }

  public R visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return visitFunctionBody(node);
  }

  public R visitExpressionStatement(ExpressionStatement node) {
    return visitStatement(node);
  }

  public R visitFieldDeclaration(FieldDeclaration node) {
    return visitTypeMember(node);
  }

  public R visitForEachStatement(ForEachStatement node) {
    return visitStatement(node);
  }

  public R visitFormalParameter(FormalParameter node) {
    return visitNode(node);
  }

  public R visitFormalParameterList(FormalParameterList node) {
    return visitNode(node);
  }

  public R visitForStatement(ForStatement node) {
    return visitStatement(node);
  }

  public R visitFunctionBody(FunctionBody node) {
    return visitNode(node);
  }

  public R visitFunctionDeclaration(FunctionDeclaration node) {
    return visitNode(node);
  }

  public R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    return visitStatement(node);
  }

  public R visitFunctionExpression(FunctionExpression node) {
    return visitExpression(node);
  }

  public R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    return visitExpression(node);
  }

  public R visitIdentifier(Identifier node) {
    return visitExpression(node);
  }

  public R visitIfStatement(IfStatement node) {
    return visitStatement(node);
  }

  public R visitImplementsClause(ImplementsClause node) {
    return visitNode(node);
  }

  public R visitImportCombinator(ImportCombinator node) {
    return visitNode(node);
  }

  public R visitImportDirective(ImportDirective node) {
    return visitDirective(node);
  }

  public R visitImportExportCombinator(ImportExportCombinator node) {
    return visitImportCombinator(node);
  }

  public R visitImportHideCombinator(ImportHideCombinator node) {
    return visitImportCombinator(node);
  }

  public R visitImportPrefixCombinator(ImportPrefixCombinator node) {
    return visitImportCombinator(node);
  }

  public R visitImportShowCombinator(ImportShowCombinator node) {
    return visitImportCombinator(node);
  }

  public R visitInitializedFormalParameter(InitializedFormalParameter node) {
    return visitSimpleFormalParameter(node);
  }

  public R visitInstanceCreationExpression(InstanceCreationExpression node) {
    return visitExpression(node);
  }

//  public R visitNativeDirective(NativeDirective node) {
//    return visitDirective(node);
//  }

  public R visitIntegerLiteral(IntegerLiteral node) {
    return visitLiteral(node);
  }

  public R visitInterfaceDeclaration(InterfaceDeclaration node) {
    return visitTypeDeclaration(node);
  }

  public R visitInterfaceExtendsClause(InterfaceExtendsClause node) {
    return visitNode(node);
  }

  public R visitInterpolationElement(InterpolationElement node) {
    return visitNode(node);
  }

  public R visitInterpolationExpression(InterpolationExpression node) {
    return visitInterpolationElement(node);
  }

  public R visitInterpolationString(InterpolationString node) {
    return visitInterpolationElement(node);
  }

  public R visitIsExpression(IsExpression node) {
    return visitExpression(node);
  }

  public R visitLabel(Label node) {
    return visitNode(node);
  }

  public R visitLabeledStatement(LabeledStatement node) {
    return visitStatement(node);
  }

  public R visitLibraryDirective(LibraryDirective node) {
    return visitDirective(node);
  }

  public R visitListLiteral(ListLiteral node) {
    return visitTypedLiteral(node);
  }

  public R visitLiteral(Literal node) {
    return visitExpression(node);
  }

  public R visitMapLiteral(MapLiteral node) {
    return visitTypedLiteral(node);
  }

  public R visitMapLiteralEntry(MapLiteralEntry node) {
    return visitNode(node);
  }

//  public R visitNativeFunctionBody(NativeFunctionBody node) {
//    return visitFunctionBody(node);
//  }

  public R visitMethodDeclaration(MethodDeclaration node) {
    return visitTypeMember(node);
  }

  public R visitMethodInvocation(MethodInvocation node) {
    return visitNode(node);
  }

  public R visitNamedExpression(NamedExpression node) {
    return visitExpression(node);
  }

  public R visitNamedFormalParameter(NamedFormalParameter node) {
    return visitFormalParameter(node);
  }

  public R visitNode(ASTNode node) {
    node.visitChildren(this);
    return null;
  }

  public R visitNormalFormalParameter(NormalFormalParameter node) {
    return visitFormalParameter(node);
  }

  public R visitNullLiteral(NullLiteral node) {
    return visitLiteral(node);
  }

  public R visitParenthesizedExpression(ParenthesizedExpression node) {
    return visitExpression(node);
  }

  public R visitPostfixExpression(PostfixExpression node) {
    return visitExpression(node);
  }

  public R visitPrefixedIdentifier(PrefixedIdentifier node) {
    return visitIdentifier(node);
  }

  public R visitPrefixExpression(PrefixExpression node) {
    return visitExpression(node);
  }

  public R visitPropertyAccess(PropertyAccess node) {
    return visitExpression(node);
  }

  public R visitResourceDirective(ResourceDirective node) {
    return visitDirective(node);
  }

  public R visitReturnStatement(ReturnStatement node) {
    return visitStatement(node);
  }

  public R visitScriptTag(ScriptTag scriptTag) {
    return visitNode(scriptTag);
  }

  public R visitSimpleFormalParameter(SimpleFormalParameter node) {
    return visitNormalFormalParameter(node);
  }

  public R visitSimpleIdentifier(SimpleIdentifier node) {
    return visitIdentifier(node);
  }

  public R visitSingleStringLiteral(SimpleStringLiteral node) {
    return visitStringLiteral(node);
  }

  public R visitSourceDirective(SourceDirective node) {
    return visitDirective(node);
  }

  public R visitStatement(Statement node) {
    return visitNode(node);
  }

  public R visitStringInterpolation(StringInterpolation node) {
    return visitStringLiteral(node);
  }

  public R visitStringLiteral(StringLiteral node) {
    return visitLiteral(node);
  }

  public R visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    return visitConstructorInitializer(node);
  }

  public R visitSuperExpression(SuperExpression node) {
    return visitExpression(node);
  }

  public R visitSwitchCase(SwitchCase node) {
    return visitSwitchMember(node);
  }

  public R visitSwitchDefault(SwitchDefault node) {
    return visitSwitchMember(node);
  }

  public R visitSwitchMember(SwitchMember node) {
    return visitNode(node);
  }

  public R visitSwitchStatement(SwitchStatement node) {
    return visitStatement(node);
  }

  public R visitThisExpression(ThisExpression node) {
    return visitExpression(node);
  }

  public R visitThrowStatement(ThrowStatement node) {
    return visitStatement(node);
  }

  public R visitTryStatement(TryStatement node) {
    return visitStatement(node);
  }

  public R visitTypeAlias(TypeAlias node) {
    return visitCompilationUnitMember(node);
  }

  public R visitTypeArguments(TypeArgumentList node) {
    return visitNode(node);
  }

  public R visitTypeDeclaration(TypeDeclaration node) {
    return visitCompilationUnitMember(node);
  }

  public R visitTypedLiteral(TypedLiteral node) {
    return visitLiteral(node);
  }

  public R visitTypeMember(TypeMember node) {
    return visitDeclaration(node);
  }

  public R visitTypeName(TypeName node) {
    return visitNode(node);
  }

  public R visitTypeParameter(TypeParameter node) {
    return visitNode(node);
  }

  public R visitTypeParameterList(TypeParameterList node) {
    return visitNode(node);
  }

  public R visitVariableDeclaration(VariableDeclaration node) {
    return visitDeclaration(node);
  }

  public R visitVariableDeclarationList(VariableDeclarationList node) {
    return visitNode(node);
  }

  public R visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    return visitStatement(node);
  }

  public R visitWhileStatement(WhileStatement node) {
    return visitStatement(node);
  }
}
