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

import com.google.dart.engine.ast.ASTVisitor;
import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.ArrayAccess;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CascadeExpression;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.EmptyFunctionBody;
import com.google.dart.engine.ast.EmptyStatement;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionDeclarationStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.InterpolationString;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.ScriptTag;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;

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
  public R visitAnnotation(Annotation node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
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
  public R visitAssertStatement(AssertStatement node) {
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
  public R visitCascadeExpression(CascadeExpression node) {
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

  @Override
  public R visitContinueStatement(ContinueStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitDefaultFormalParameter(DefaultFormalParameter node) {
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
  public R visitEmptyFunctionBody(EmptyFunctionBody node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitEmptyStatement(EmptyStatement node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitExportDirective(ExportDirective node) {
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
  public R visitExtendsClause(ExtendsClause node) {
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

  @Override
  public R visitHideCombinator(HideCombinator node) {
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

  @Override
  public R visitImportDirective(ImportDirective node) {
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
  public R visitPartDirective(PartDirective node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitPartOfDirective(PartOfDirective node) {
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
  public R visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
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
  public R visitShowCombinator(ShowCombinator node) {
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
  public R visitSimpleStringLiteral(SimpleStringLiteral node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitStringInterpolation(StringInterpolation node) {
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
  public R visitThrowExpression(ThrowExpression node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public R visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
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
  public R visitTypeArgumentList(TypeArgumentList node) {
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
