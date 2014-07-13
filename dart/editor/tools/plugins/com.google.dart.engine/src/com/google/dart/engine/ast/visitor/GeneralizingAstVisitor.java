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
 * Instances of the class {@code GeneralizingAstVisitor} implement an AST visitor that will
 * recursively visit all of the nodes in an AST structure (like instances of the class
 * {@link RecursiveAstVisitor}). In addition, when a node of a specific type is visited not only
 * will the visit method for that specific type of node be invoked, but additional methods for the
 * superclasses of that node will also be invoked. For example, using an instance of this class to
 * visit a {@link Block} will cause the method {@link #visitBlock(Block)} to be invoked but will
 * also cause the methods {@link #visitStatement(Statement)} and {@link #visitNode(AstNode)} to be
 * subsequently invoked. This allows visitors to be written that visit all statements without
 * needing to override the visit method for each of the specific subclasses of {@link Statement}.
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general visit method. Failure to do so will cause the visit methods
 * for superclasses of the node to not be invoked and will cause the children of the visited node to
 * not be visited.
 * 
 * @coverage dart.engine.ast
 */
public class GeneralizingAstVisitor<R> implements AstVisitor<R> {
  @Override
  public R visitAdjacentStrings(AdjacentStrings node) {
    return visitStringLiteral(node);
  }

  public R visitAnnotatedNode(AnnotatedNode node) {
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
    return visitExpression(node);
  }

  @Override
  public R visitAssertStatement(AssertStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitAssignmentExpression(AssignmentExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitAwaitExpression(AwaitExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitBinaryExpression(BinaryExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitBlock(Block node) {
    return visitStatement(node);
  }

  @Override
  public R visitBlockFunctionBody(BlockFunctionBody node) {
    return visitFunctionBody(node);
  }

  @Override
  public R visitBooleanLiteral(BooleanLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitBreakStatement(BreakStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitCascadeExpression(CascadeExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitCatchClause(CatchClause node) {
    return visitNode(node);
  }

  @Override
  public R visitClassDeclaration(ClassDeclaration node) {
    return visitCompilationUnitMember(node);
  }

  public R visitClassMember(ClassMember node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitClassTypeAlias(ClassTypeAlias node) {
    return visitTypeAlias(node);
  }

  public R visitCombinator(Combinator node) {
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

  public R visitCompilationUnitMember(CompilationUnitMember node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitConditionalExpression(ConditionalExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitConstructorDeclaration(ConstructorDeclaration node) {
    return visitClassMember(node);
  }

  @Override
  public R visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    return visitConstructorInitializer(node);
  }

  public R visitConstructorInitializer(ConstructorInitializer node) {
    return visitNode(node);
  }

  @Override
  public R visitConstructorName(ConstructorName node) {
    return visitNode(node);
  }

  @Override
  public R visitContinueStatement(ContinueStatement node) {
    return visitStatement(node);
  }

  public R visitDeclaration(Declaration node) {
    return visitAnnotatedNode(node);
  }

  @Override
  public R visitDeclaredIdentifier(DeclaredIdentifier node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitDefaultFormalParameter(DefaultFormalParameter node) {
    return visitFormalParameter(node);
  }

  public R visitDirective(Directive node) {
    return visitAnnotatedNode(node);
  }

  @Override
  public R visitDoStatement(DoStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitDoubleLiteral(DoubleLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitEmptyFunctionBody(EmptyFunctionBody node) {
    return visitFunctionBody(node);
  }

  @Override
  public R visitEmptyStatement(EmptyStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitEnumDeclaration(EnumDeclaration node) {
    return visitCompilationUnitMember(node);
  }

  @Override
  public R visitExportDirective(ExportDirective node) {
    return visitNamespaceDirective(node);
  }

  public R visitExpression(Expression node) {
    return visitNode(node);
  }

  @Override
  public R visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return visitFunctionBody(node);
  }

  @Override
  public R visitExpressionStatement(ExpressionStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitExtendsClause(ExtendsClause node) {
    return visitNode(node);
  }

  @Override
  public R visitFieldDeclaration(FieldDeclaration node) {
    return visitClassMember(node);
  }

  @Override
  public R visitFieldFormalParameter(FieldFormalParameter node) {
    return visitNormalFormalParameter(node);
  }

  @Override
  public R visitForEachStatement(ForEachStatement node) {
    return visitStatement(node);
  }

  public R visitFormalParameter(FormalParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitFormalParameterList(FormalParameterList node) {
    return visitNode(node);
  }

  @Override
  public R visitForStatement(ForStatement node) {
    return visitStatement(node);
  }

  public R visitFunctionBody(FunctionBody node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionDeclaration(FunctionDeclaration node) {
    return visitCompilationUnitMember(node);
  }

  @Override
  public R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitFunctionExpression(FunctionExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    return visitExpression(node);
  }

  @Override
  public R visitFunctionTypeAlias(FunctionTypeAlias node) {
    return visitTypeAlias(node);
  }

  @Override
  public R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    return visitNormalFormalParameter(node);
  }

  @Override
  public R visitHideCombinator(HideCombinator node) {
    return visitCombinator(node);
  }

  public R visitIdentifier(Identifier node) {
    return visitExpression(node);
  }

  @Override
  public R visitIfStatement(IfStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitImplementsClause(ImplementsClause node) {
    return visitNode(node);
  }

  @Override
  public R visitImportDirective(ImportDirective node) {
    return visitNamespaceDirective(node);
  }

  @Override
  public R visitIndexExpression(IndexExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitInstanceCreationExpression(InstanceCreationExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitIntegerLiteral(IntegerLiteral node) {
    return visitLiteral(node);
  }

  public R visitInterpolationElement(InterpolationElement node) {
    return visitNode(node);
  }

  @Override
  public R visitInterpolationExpression(InterpolationExpression node) {
    return visitInterpolationElement(node);
  }

  @Override
  public R visitInterpolationString(InterpolationString node) {
    return visitInterpolationElement(node);
  }

  @Override
  public R visitIsExpression(IsExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitLabel(Label node) {
    return visitNode(node);
  }

  @Override
  public R visitLabeledStatement(LabeledStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitLibraryDirective(LibraryDirective node) {
    return visitDirective(node);
  }

  @Override
  public R visitLibraryIdentifier(LibraryIdentifier node) {
    return visitIdentifier(node);
  }

  @Override
  public R visitListLiteral(ListLiteral node) {
    return visitTypedLiteral(node);
  }

  public R visitLiteral(Literal node) {
    return visitExpression(node);
  }

  @Override
  public R visitMapLiteral(MapLiteral node) {
    return visitTypedLiteral(node);
  }

  @Override
  public R visitMapLiteralEntry(MapLiteralEntry node) {
    return visitNode(node);
  }

  @Override
  public R visitMethodDeclaration(MethodDeclaration node) {
    return visitClassMember(node);
  }

  @Override
  public R visitMethodInvocation(MethodInvocation node) {
    return visitExpression(node);
  }

  @Override
  public R visitNamedExpression(NamedExpression node) {
    return visitExpression(node);
  }

  public R visitNamespaceDirective(NamespaceDirective node) {
    return visitUriBasedDirective(node);
  }

  @Override
  public R visitNativeClause(NativeClause node) {
    return visitNode(node);
  }

  @Override
  public R visitNativeFunctionBody(NativeFunctionBody node) {
    return visitFunctionBody(node);
  }

  public R visitNode(AstNode node) {
    node.visitChildren(this);
    return null;
  }

  public R visitNormalFormalParameter(NormalFormalParameter node) {
    return visitFormalParameter(node);
  }

  @Override
  public R visitNullLiteral(NullLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitParenthesizedExpression(ParenthesizedExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitPartDirective(PartDirective node) {
    return visitUriBasedDirective(node);
  }

  @Override
  public R visitPartOfDirective(PartOfDirective node) {
    return visitDirective(node);
  }

  @Override
  public R visitPostfixExpression(PostfixExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitPrefixedIdentifier(PrefixedIdentifier node) {
    return visitIdentifier(node);
  }

  @Override
  public R visitPrefixExpression(PrefixExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitPropertyAccess(PropertyAccess node) {
    return visitExpression(node);
  }

  @Override
  public R visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    return visitConstructorInitializer(node);
  }

  @Override
  public R visitRethrowExpression(RethrowExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitReturnStatement(ReturnStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitScriptTag(ScriptTag scriptTag) {
    return visitNode(scriptTag);
  }

  @Override
  public R visitShowCombinator(ShowCombinator node) {
    return visitCombinator(node);
  }

  @Override
  public R visitSimpleFormalParameter(SimpleFormalParameter node) {
    return visitNormalFormalParameter(node);
  }

  @Override
  public R visitSimpleIdentifier(SimpleIdentifier node) {
    return visitIdentifier(node);
  }

  @Override
  public R visitSimpleStringLiteral(SimpleStringLiteral node) {
    return visitStringLiteral(node);
  }

  public R visitStatement(Statement node) {
    return visitNode(node);
  }

  @Override
  public R visitStringInterpolation(StringInterpolation node) {
    return visitStringLiteral(node);
  }

  public R visitStringLiteral(StringLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    return visitConstructorInitializer(node);
  }

  @Override
  public R visitSuperExpression(SuperExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitSwitchCase(SwitchCase node) {
    return visitSwitchMember(node);
  }

  @Override
  public R visitSwitchDefault(SwitchDefault node) {
    return visitSwitchMember(node);
  }

  public R visitSwitchMember(SwitchMember node) {
    return visitNode(node);
  }

  @Override
  public R visitSwitchStatement(SwitchStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitSymbolLiteral(SymbolLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitThisExpression(ThisExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitThrowExpression(ThrowExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    return visitCompilationUnitMember(node);
  }

  @Override
  public R visitTryStatement(TryStatement node) {
    return visitStatement(node);
  }

  public R visitTypeAlias(TypeAlias node) {
    return visitCompilationUnitMember(node);
  }

  @Override
  public R visitTypeArgumentList(TypeArgumentList node) {
    return visitNode(node);
  }

  public R visitTypedLiteral(TypedLiteral node) {
    return visitLiteral(node);
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

  public R visitUriBasedDirective(UriBasedDirective node) {
    return visitDirective(node);
  }

  @Override
  public R visitVariableDeclaration(VariableDeclaration node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitVariableDeclarationList(VariableDeclarationList node) {
    return visitNode(node);
  }

  @Override
  public R visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitWhileStatement(WhileStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitWithClause(WithClause node) {
    return visitNode(node);
  }

  @Override
  public R visitYieldStatement(YieldStatement node) {
    return visitStatement(node);
  }
}
