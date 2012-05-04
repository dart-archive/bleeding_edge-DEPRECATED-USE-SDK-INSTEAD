/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

/**
 * A collection of marks representing interesting parser states.
 */
public enum Mark {
  // some elements are roots of the kind-of relationships, they must be first
  Block,
  Expression,
  Literal(Expression),
  Parameters,
  Statement,
  // all others are alphabetical
  ArrayLiteral(Literal),
  BinaryExpression(Expression),
  BreakStatement(Statement),
  CatchClause,
  CatchParameter,
  ClassBody(Block),
  ClassMember,
  CompilationUnit,
  ConditionalExpression(Expression),
  FinalDeclaration,
  ConstExpression(Expression),
  ConstructorName,
  ContinueStatement(Statement),
  DoStatement(Statement),
  EmptyStatement(Statement),
  ExpressionList,
  ExpressionStatement(Statement),
  FieldInitializerOrRedirectedConstructor,
  ForInitialization,
  FormalParameter,
  ForStatement(Statement),
  FunctionDeclaration,
  FunctionLiteral(Literal),
  FunctionStatementBody(Block),
  FunctionTypeInterface,
  Identifier(Expression),
  IfStatement(Statement),
  Initializer,
  TypeExpression(Expression),
  Label,
  MapLiteral(Literal),
  MapLiteralEntry,
  MethodName,
  Native,
  NewExpression(Expression),
  OperatorName,
  ParenthesizedExpression(Expression),
  PostfixExpression(Expression),
  QualifiedIdentifier,
  ReturnStatement(Statement),
  SelectorExpression(Expression),
  SpreadExpression(Expression),
  StringInterpolation,
  StringSegment,
  SuperExpression(Expression),
  SuperInitializer,
  SwitchMember,
  SwitchStatement(Statement),
  ThisExpression(Expression),
  ThrowStatement(Statement),
  TopLevelElement,
  TryStatement(Statement),
  TypeAnnotation,
  TypeArguments,
  TypeFunctionOrVariable,
  TypeParameter,
  UnaryExpression(Expression),
  VarDeclaration,
  VariableDeclaration,
  WhileStatement(Statement);

  public final Mark kind;

  private Mark() {
    kind = null;
  }

  private Mark(Mark kind) {
    this.kind = kind;
  }

  /**
   * Return <code>true</code> if this Mark is the same kind as the given Mark.
   * 
   * @param other the Mark to test for a kind-of relation
   * @return <code>true</code> if the test succeeds
   */
  public boolean isKindOf(Mark other) {
    if (this == other) {
      return true;
    }
    if (kind == null) {
      return false;
    }
    return kind.isKindOf(other);
  }
}
