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
 * Instances of the class {@code RecursiveASTVisitor} implement an AST visitor that will recursively
 * visit all of the nodes in an AST structure. For example, using an instance of this class to visit
 * a {@link Block} will also cause all of the statements in the block to be visited.
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or must
 * explicitly ask the visited node to visit its children. Failure to do so will cause the children
 * of the visited node to not be visited.
 */
public class RecursiveASTVisitor<R> implements ASTVisitor<R> {
  @Override
  public R visitAdjacentStrings(AdjacentStrings node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitArgumentList(ArgumentList node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitArrayAccess(ArrayAccess node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitAssignmentExpression(AssignmentExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitBinaryExpression(BinaryExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitBlock(Block node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitBlockFunctionBody(BlockFunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitBooleanLiteral(BooleanLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitBreakStatement(BreakStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitCatchClause(CatchClause node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitClassDeclaration(ClassDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitClassExtendsClause(ClassExtendsClause node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitComment(Comment node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitCommentReference(CommentReference node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitCompilationUnit(CompilationUnit node) {
    node.visitChildren(this);
    return null;
  }

  public R visitCompilationUnitMember(CompilationUnitMember node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitConditionalExpression(ConditionalExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitConstructorDeclaration(ConstructorDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    node.visitChildren(this);
    return null;
  }

  public R visitConstructorInitializer(ConstructorInitializer node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitContinueStatement(ContinueStatement node) {
    node.visitChildren(this);
    return null;
  }

  public R visitDeclaration(Declaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitDefaultClause(DefaultClause node) {
    node.visitChildren(this);
    return null;
  }

  public R visitDirective(Directive node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitDoStatement(DoStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitDoubleLiteral(DoubleLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitEmptyStatement(EmptyStatement node) {
    node.visitChildren(this);
    return null;
  }

  public R visitExpression(Expression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitExpressionFunctionBody(ExpressionFunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitExpressionStatement(ExpressionStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFieldDeclaration(FieldDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFieldFormalParameter(FieldFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitForEachStatement(ForEachStatement node) {
    node.visitChildren(this);
    return null;
  }

  public R visitFormalParameter(FormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFormalParameterList(FormalParameterList node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitForStatement(ForStatement node) {
    node.visitChildren(this);
    return null;
  }

  public R visitFunctionBody(FunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFunctionDeclaration(FunctionDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFunctionExpression(FunctionExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  public R visitIdentifier(Identifier node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitIfStatement(IfStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitImplementsClause(ImplementsClause node) {
    node.visitChildren(this);
    return null;
  }

  public R visitImportCombinator(ImportCombinator node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitImportDirective(ImportDirective node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitImportExportCombinator(ImportExportCombinator node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitImportHideCombinator(ImportHideCombinator node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitImportPrefixCombinator(ImportPrefixCombinator node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitImportShowCombinator(ImportShowCombinator node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitInstanceCreationExpression(InstanceCreationExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitIntegerLiteral(IntegerLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitInterfaceDeclaration(InterfaceDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitInterfaceExtendsClause(InterfaceExtendsClause node) {
    node.visitChildren(this);
    return null;
  }

  public R visitInterpolationElement(InterpolationElement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitInterpolationExpression(InterpolationExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitInterpolationString(InterpolationString node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitIsExpression(IsExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitLabel(Label node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitLabeledStatement(LabeledStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitLibraryDirective(LibraryDirective node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitListLiteral(ListLiteral node) {
    node.visitChildren(this);
    return null;
  }

  public R visitLiteral(Literal node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitMapLiteral(MapLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitMapLiteralEntry(MapLiteralEntry node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitMethodDeclaration(MethodDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitMethodInvocation(MethodInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitNamedExpression(NamedExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitNamedFormalParameter(NamedFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  public R visitNode(ASTNode node) {
    node.visitChildren(this);
    return null;
  }

  public R visitNormalFormalParameter(NormalFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitNullLiteral(NullLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitParenthesizedExpression(ParenthesizedExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitPostfixExpression(PostfixExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitPrefixedIdentifier(PrefixedIdentifier node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitPrefixExpression(PrefixExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitPropertyAccess(PropertyAccess node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitResourceDirective(ResourceDirective node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitReturnStatement(ReturnStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitScriptTag(ScriptTag node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSimpleFormalParameter(SimpleFormalParameter node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSimpleIdentifier(SimpleIdentifier node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSingleStringLiteral(SimpleStringLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSourceDirective(SourceDirective node) {
    node.visitChildren(this);
    return null;
  }

  public R visitStatement(Statement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitStringInterpolation(StringInterpolation node) {
    node.visitChildren(this);
    return null;
  }

  public R visitStringLiteral(StringLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSuperExpression(SuperExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSwitchCase(SwitchCase node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSwitchDefault(SwitchDefault node) {
    node.visitChildren(this);
    return null;
  }

  public R visitSwitchMember(SwitchMember node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitSwitchStatement(SwitchStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitThisExpression(ThisExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitThrowStatement(ThrowStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitTryStatement(TryStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitTypeAlias(TypeAlias node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitTypeArguments(TypeArgumentList node) {
    node.visitChildren(this);
    return null;
  }

  public R visitTypeDeclaration(TypeDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  public R visitTypedLiteral(TypedLiteral node) {
    node.visitChildren(this);
    return null;
  }

  public R visitTypeMember(TypeMember node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitTypeName(TypeName node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitTypeParameter(TypeParameter node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitTypeParameterList(TypeParameterList node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitVariableDeclaration(VariableDeclaration node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitVariableDeclarationList(VariableDeclarationList node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitWhileStatement(WhileStatement node) {
    node.visitChildren(this);
    return null;
  }
}
