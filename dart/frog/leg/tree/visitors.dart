// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class AbstractVisitor<R> implements Visitor<R> {
  abstract R visitNode(Node node);

  R visitBlock(Block node) => visitStatement(node);
  R visitClassNode(ClassNode node) => visitNode(node);
  R visitExpression(Expression node) => visitNode(node);
  R visitExpressionStatement(ExpressionStatement node) => visitStatement(node);
  R visitFor(For node) => visitStatement(node);
  R visitFunctionExpression(FunctionExpression node) => visitExpression(node);
  R visitIdentifier(Identifier node) => visitExpression(node);
  R visitIf(If node) => visitStatement(node);
  R visitLiteral(Literal node) => visitExpression(node);
  R visitLiteralBool(LiteralBool node) => visitLiteral(node);
  R visitLiteralDouble(LiteralDouble node) => visitLiteral(node);
  R visitLiteralInt(LiteralInt node) => visitLiteral(node);
  R visitLiteralString(LiteralString node) => visitLiteral(node);
  R visitNewExpression(NewExpression node) => visitExpression(node);
  R visitNodeList(NodeList node) => visitNode(node);
  R visitOperator(Operator node) => visitIdentifier(node);
  R visitPostfix(Postfix node) => visitNodeList(node);
  R visitPrefix(Prefix node) => visitNodeList(node);
  R visitReturn(Return node) => visitStatement(node);
  R visitSend(Send node) => visitExpression(node);
  R visitSendSet(SendSet node) => visitSend(node);
  R visitStatement(Statement node) => visitNode(node);
  R visitThrow(Throw node) => visitStatement(node);
  R visitTypeAnnotation(TypeAnnotation node) => visitNode(node);
  R visitVariableDefinitions(VariableDefinitions node) => visitStatement(node);
}
